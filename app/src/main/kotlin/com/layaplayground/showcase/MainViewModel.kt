package com.layaplayground.showcase

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.layaplayground.showcase.data.Presets
import com.layaplayground.showcase.data.QuestionDraft
import com.layaplayground.showcase.engine.LayaEngine
import com.layaplayground.showcase.engine.LayaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val stateBody: String = Presets.all.first().stateBody,
    val questions: List<QuestionDraft> = Presets.all.first().questions,
    val selectedPreset: Int = 0,
    val isEnginePreparing: Boolean = true,
    val isLoading: Boolean = false,
    val result: LayaResult? = null,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    // Opened once, off the main thread: extracting the checkpoint out of assets on first launch
    // and standing up the ONNX session both take real time, and both only need to happen once.
    private var engine: LayaEngine? = null

    init {
        viewModelScope.launch {
            try {
                val opened = withContext(Dispatchers.IO) { LayaEngine.open(getApplication()) }
                engine = opened
                _uiState.update { it.copy(isEnginePreparing = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEnginePreparing = false,
                        errorMessage = "Could not load the on-device model: ${e.message}"
                    )
                }
            }
        }
    }

    fun onStateBodyChange(text: String) {
        _uiState.update { it.copy(stateBody = text) }
    }

    fun applyPreset(index: Int) {
        val preset = Presets.all.getOrNull(index) ?: return
        _uiState.update {
            it.copy(
                selectedPreset = index,
                stateBody = preset.stateBody,
                questions = preset.questions,
                result = null,
                errorMessage = null
            )
        }
    }

    fun runPrediction() {
        val state = _uiState.value
        val activeEngine = engine
        if (activeEngine == null) {
            _uiState.update { it.copy(errorMessage = "The model is still loading, try again in a moment.") }
            return
        }
        if (state.stateBody.isBlank() || state.questions.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add some text and at least one question first.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) {
                    activeEngine.predict(state.stateBody, state.questions)
                }
                _uiState.update { it.copy(isLoading = false, result = result) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Inference failed: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        engine?.close()
        engine = null
    }
}
