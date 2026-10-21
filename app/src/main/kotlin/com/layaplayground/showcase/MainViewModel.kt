package com.layaplayground.showcase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.layaplayground.showcase.data.Presets
import com.layaplayground.showcase.data.QuestionDraft
import com.layaplayground.showcase.net.LayaClient
import com.layaplayground.showcase.net.LayaRequestException
import com.layaplayground.showcase.net.LayaResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val serverUrl: String = "http://10.0.2.2:8000",
    val stateBody: String = Presets.all.first().stateBody,
    val questions: List<QuestionDraft> = Presets.all.first().questions,
    val selectedPreset: Int = 0,
    val isLoading: Boolean = false,
    val result: LayaResult? = null,
    val errorMessage: String? = null
)

class MainViewModel : ViewModel() {

    private val client = LayaClient()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onServerUrlChange(url: String) {
        _uiState.update { it.copy(serverUrl = url) }
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
        if (state.stateBody.isBlank() || state.questions.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Add some text and at least one question first.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    client.predict(state.serverUrl, state.stateBody, state.questions)
                }
                _uiState.update { it.copy(isLoading = false, result = result) }
            } catch (e: LayaRequestException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
