package com.layaplayground.showcase.engine

data class ChoiceAnswer(
    val selected: String,
    val confidence: Double,
    val probabilities: Map<String, Double>
)

data class ScoreAnswer(
    val score: Double,
    val confidence: Double,
    val legend: Map<String, String>,
    val probabilities: Map<String, Double>
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
    val inputTokens: Int?,
    val inferenceLatencyMs: Long
)
