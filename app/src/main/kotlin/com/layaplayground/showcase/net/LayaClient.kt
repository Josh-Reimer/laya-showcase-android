package com.layaplayground.showcase.net

import com.layaplayground.showcase.data.QuestionDraft
import com.layaplayground.showcase.data.QuestionType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChoiceAnswer(
    val selected: String,
    val confidence: Double,
    val probabilities: Map<String, Double>
)

data class ScoreAnswer(
    val score: Double,
    val confidence: Double,
    val probabilities: List<Double>
)

data class NoulAnswer(
    val probabilityTrue: Double
)

sealed class LayaAnswer {
    data class Choice(val value: ChoiceAnswer) : LayaAnswer()
    data class Score(val value: ScoreAnswer) : LayaAnswer()
    data class Noul(val value: NoulAnswer) : LayaAnswer()
}

data class LayaResult(
    val answers: Map<String, LayaAnswer>,
    val routingModel: String?,
    val routingReason: String?,
    val inputTokens: Int?,
    val clientLatencyMs: Long
)

class LayaRequestException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Thin client for a self-hosted `laya-serve` instance exposing the
 * Jev-compatible `POST /v1/systemone` wire protocol.
 */
class LayaClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun predict(baseUrl: String, stateBody: String, questions: List<QuestionDraft>): LayaResult {
        val url = baseUrl.trimEnd('/') + "/v1/systemone"

        val questionsJson = JSONObject()
        for (q in questions) {
            val qJson = JSONObject()
            qJson.put("type", q.type.wireName)
            qJson.put("instructions", q.instructions)
            when (q.type) {
                QuestionType.CHOICE -> {
                    val criteria = JSONObject()
                    for (opt in q.choiceOptions) criteria.put(opt.label, opt.description)
                    qJson.put("criteria", criteria)
                }
                QuestionType.SCORE -> {
                    val criteria = JSONArray()
                    for (level in q.scoreLevels) criteria.put(level)
                    qJson.put("criteria", criteria)
                }
                QuestionType.NOUL -> { /* no criteria needed */ }
            }
            questionsJson.put(q.id, qJson)
        }

        val body = JSONObject().apply {
            put("state", JSONObject().put("body", stateBody))
            put("questions", questionsJson)
        }

        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val startNanos = System.nanoTime()
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw LayaRequestException(
                "Couldn't reach Laya at $baseUrl. Is `laya-serve` running and reachable from this device?",
                e
            )
        }

        response.use { resp ->
            val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000
            val bodyStr = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw LayaRequestException("Laya server returned HTTP ${resp.code}: $bodyStr")
            }
            return parseResult(bodyStr, elapsedMs)
        }
    }

    private fun parseResult(raw: String, clientLatencyMs: Long): LayaResult {
        val json = try {
            JSONObject(raw)
        } catch (e: Exception) {
            throw LayaRequestException("Laya server returned an unexpected response.", e)
        }

        val answersJson = json.optJSONObject("answers") ?: JSONObject()
        val answers = mutableMapOf<String, LayaAnswer>()

        for (key in answersJson.keys()) {
            val a = answersJson.getJSONObject(key)
            when {
                a.has("choice") -> {
                    val probs = mutableMapOf<String, Double>()
                    a.optJSONObject("probabilities")?.let { p ->
                        for (k in p.keys()) probs[k] = p.getDouble(k)
                    }
                    answers[key] = LayaAnswer.Choice(
                        ChoiceAnswer(
                            selected = a.getString("choice"),
                            confidence = a.optDouble("confidence", Double.NaN),
                            probabilities = probs
                        )
                    )
                }
                a.has("score") -> {
                    val probs = mutableListOf<Double>()
                    a.optJSONArray("probabilities")?.let { arr ->
                        for (i in 0 until arr.length()) probs.add(arr.getDouble(i))
                    }
                    answers[key] = LayaAnswer.Score(
                        ScoreAnswer(
                            score = a.getDouble("score"),
                            confidence = a.optDouble("confidence", Double.NaN),
                            probabilities = probs
                        )
                    )
                }
                a.has("noul") -> {
                    answers[key] = LayaAnswer.Noul(NoulAnswer(probabilityTrue = a.getDouble("noul")))
                }
            }
        }

        val routing = json.optJSONObject("routing")
        val usage = json.optJSONObject("usage")

        return LayaResult(
            answers = answers,
            routingModel = routing?.optString("model"),
            routingReason = routing?.optString("reason"),
            inputTokens = usage?.optInt("input_tokens"),
            clientLatencyMs = clientLatencyMs
        )
    }
}
