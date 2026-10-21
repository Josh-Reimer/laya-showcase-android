package com.layaplayground.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.layaplayground.showcase.data.Presets
import com.layaplayground.showcase.data.QuestionDraft
import com.layaplayground.showcase.data.QuestionType
import com.layaplayground.showcase.net.LayaAnswer
import com.layaplayground.showcase.net.LayaResult
import com.layaplayground.showcase.ui.theme.LayaShowcaseTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LayaShowcaseTheme {
                Surface {
                    val state by viewModel.uiState.collectAsState()
                    LayaScreen(
                        state = state,
                        onServerUrlChange = viewModel::onServerUrlChange,
                        onStateBodyChange = viewModel::onStateBodyChange,
                        onPresetSelected = viewModel::applyPreset,
                        onRun = viewModel::runPrediction,
                        onDismissError = viewModel::dismissError
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayaScreen(
    state: UiState,
    onServerUrlChange: (String) -> Unit,
    onStateBodyChange: (String) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onRun: () -> Unit,
    onDismissError: () -> Unit
) {
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Laya Showcase", fontWeight = FontWeight.Bold)
                        Text(
                            "Open-source typed decisions — the open alternative to Jev",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Server settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PresetPicker(selected = state.selectedPreset, onPresetSelected = onPresetSelected)

            OutlinedTextField(
                value = state.stateBody,
                onValueChange = onStateBodyChange,
                label = { Text("Input text (the \"state\")") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )

            QuestionList(questions = state.questions)

            Button(
                onClick = onRun,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Asking Laya…")
                } else {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ask Laya")
                }
            }

            state.errorMessage?.let { message ->
                ErrorCard(message = message, onDismiss = onDismissError)
            }

            state.result?.let { result ->
                ResultCard(result = result)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSettings) {
        ServerSettingsSheet(
            currentUrl = state.serverUrl,
            onUrlChange = onServerUrlChange,
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun PresetPicker(selected: Int, onPresetSelected: (Int) -> Unit) {
    Column {
        Text("Try a preset scenario", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Presets.all.forEachIndexed { index, preset ->
                FilterChip(
                    selected = index == selected,
                    onClick = { onPresetSelected(index) },
                    label = { Text(preset.title) }
                )
            }
        }
    }
}

@Composable
fun QuestionList(questions: List<QuestionDraft>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Typed questions sent to Laya", style = MaterialTheme.typography.labelLarge)
        questions.forEach { q ->
            Card {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssistChip(onClick = {}, label = { Text(q.type.label) })
                        Spacer(Modifier.width(8.dp))
                        Text(q.id, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(q.instructions, style = MaterialTheme.typography.bodySmall)
                    when (q.type) {
                        QuestionType.CHOICE -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                q.choiceOptions.joinToString(" · ") { it.label },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        QuestionType.SCORE -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                q.scoreLevels.joinToString(" → "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        QuestionType.NOUL -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorCard(message: String, onDismiss: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
fun ResultCard(result: LayaResult) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Laya decided", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text(
                    "${result.clientLatencyMs} ms round-trip",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            result.routingModel?.takeIf { it.isNotBlank() }?.let {
                Text(
                    "Routed to checkpoint: $it" + (result.routingReason?.let { r -> " ($r)" } ?: ""),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            result.answers.forEach { (id, answer) ->
                AnswerRow(id = id, answer = answer)
            }
        }
    }
}

@Composable
fun AnswerRow(id: String, answer: LayaAnswer) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(id, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        when (answer) {
            is LayaAnswer.Choice -> {
                Text("→ ${answer.value.selected}  (confidence ${pct(answer.value.confidence)})")
                answer.value.probabilities.entries.sortedByDescending { it.value }.forEach { (label, p) ->
                    ProbabilityBar(label = label, probability = p)
                }
            }
            is LayaAnswer.Score -> {
                Text("→ score ${"%.2f".format(answer.value.score)}  (confidence ${pct(answer.value.confidence)})")
                answer.value.probabilities.forEachIndexed { index, p ->
                    ProbabilityBar(label = "level $index", probability = p)
                }
            }
            is LayaAnswer.Noul -> {
                Text("→ P(true) = ${pct(answer.value.probabilityTrue)}")
                ProbabilityBar(label = "true", probability = answer.value.probabilityTrue)
            }
        }
    }
}

@Composable
fun ProbabilityBar(label: String, probability: Double) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.width(80.dp), style = MaterialTheme.typography.labelSmall)
        LinearProgressIndicator(
            progress = { probability.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(8.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(pct(probability), style = MaterialTheme.typography.labelSmall)
    }
}

private fun pct(value: Double): String {
    if (value.isNaN()) return "—"
    return "${(value * 100).roundToInt()}%"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsSheet(
    currentUrl: String,
    onUrlChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Laya server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "This app is a client for a self-hosted `laya-serve` instance. " +
                    "Run `pip install \"laya[serve]\" && laya-serve` on your machine, then point this at it. " +
                    "Use 10.0.2.2 to reach your host machine from the Android emulator.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedTextField(
                value = currentUrl,
                onValueChange = onUrlChange,
                label = { Text("Base URL") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
