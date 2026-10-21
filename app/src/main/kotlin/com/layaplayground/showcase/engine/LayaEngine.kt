package com.layaplayground.showcase.engine

import android.content.Context
import com.convaiinnovations.laya.Agent
import com.convaiinnovations.laya.Answer
import com.convaiinnovations.laya.Question
import com.layaplayground.showcase.data.QuestionDraft
import com.layaplayground.showcase.data.QuestionType
import java.io.File

/**
 * Runs Laya entirely on-device: the checkpoint ships inside the APK's assets, is copied out to
 * app storage once, and every prediction afterwards is a local ONNX Runtime call with no network
 * involved.
 */
class LayaEngine private constructor(private val agent: Agent) : AutoCloseable {

    fun predict(stateBody: String, questions: List<QuestionDraft>): LayaResult {
        val questionMap = LinkedHashMap<String, Question>()
        for (q in questions) {
            questionMap[q.id] = when (q.type) {
                QuestionType.CHOICE -> {
                    val criteria = LinkedHashMap<String, Any?>()
                    for (opt in q.choiceOptions) criteria[opt.label] = opt.description
                    Question.choice(q.instructions, criteria)
                }
                QuestionType.SCORE -> Question.score(q.instructions, q.scoreLevels)
                QuestionType.NOUL -> Question.noul(q.instructions)
            }
        }

        val startNanos = System.nanoTime()
        val prediction = agent.predict(stateBody, questionMap, "en")
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        val answers = LinkedHashMap<String, LayaAnswer>()
        for ((id, answer) in prediction.answers()) {
            answers[id] = toLayaAnswer(answer)
        }

        return LayaResult(
            answers = answers,
            routingModel = prediction.model(),
            inputTokens = prediction.usage().inputTokens(),
            inferenceLatencyMs = elapsedMs
        )
    }

    private fun toLayaAnswer(answer: Answer): LayaAnswer = when (answer) {
        is Answer.Choice -> LayaAnswer.Choice(
            ChoiceAnswer(
                selected = answer.choice(),
                confidence = answer.answerConfidence(),
                probabilities = answer.probabilities()
            )
        )
        is Answer.Score -> LayaAnswer.Score(
            ScoreAnswer(
                score = answer.score(),
                confidence = answer.answerConfidence(),
                legend = answer.legend(),
                probabilities = answer.probabilities()
            )
        )
        is Answer.Noul -> LayaAnswer.Noul(NoulAnswer(probabilityTrue = answer.noul()))
    }

    override fun close() = agent.close()

    companion object {
        private const val ASSET_ROOT = "laya-model"
        private const val EXTRACTED_MARKER = ".extracted"

        /** Blocking: copies the checkpoint out of assets on first run, then opens the ONNX graph. */
        fun open(context: Context): LayaEngine {
            val modelDir = ensureModelFiles(context)
            return LayaEngine(Agent.open(modelDir.toPath(), modelDir.toPath()))
        }

        private fun ensureModelFiles(context: Context): File {
            val targetDir = File(context.filesDir, ASSET_ROOT)
            val marker = File(targetDir, EXTRACTED_MARKER)
            if (!marker.exists()) {
                targetDir.deleteRecursively()
                targetDir.mkdirs()
                File(targetDir, "tokenizer").mkdirs()
                copyAsset(context, "$ASSET_ROOT/rl_agent_config.json", File(targetDir, "rl_agent_config.json"))
                copyAsset(context, "$ASSET_ROOT/laya.onnx", File(targetDir, "laya.onnx"))
                // The graph's weights: ONNX puts tensors over ~2GB in protobuf-equivalent size in
                // a sibling file and the .onnx graph references it by this exact bare filename, so
                // it must land next to laya.onnx, not renamed.
                copyAsset(context, "$ASSET_ROOT/laya.onnx.data", File(targetDir, "laya.onnx.data"))
                copyAsset(
                    context, "$ASSET_ROOT/tokenizer/tokenizer.json",
                    File(targetDir, "tokenizer/tokenizer.json")
                )
                copyAsset(
                    context, "$ASSET_ROOT/tokenizer/tokenizer_config.json",
                    File(targetDir, "tokenizer/tokenizer_config.json")
                )
                marker.writeText("ok")
            }
            return targetDir
        }

        private fun copyAsset(context: Context, assetPath: String, dest: File) {
            context.assets.open(assetPath).use { input ->
                dest.outputStream().use { output -> input.copyTo(output, bufferSize = 1 shl 20) }
            }
        }
    }
}
