package com.layaplayground.showcase.data

/** The three typed decision primitives Laya supports. */
enum class QuestionType(val wireName: String, val label: String) {
    CHOICE("choice", "Choice"),
    SCORE("score", "Score"),
    NOUL("noul", "Noul (yes/no)")
}

data class CriterionOption(
    val label: String,
    val description: String
)

/** Editable question the user is building to send to Laya. */
data class QuestionDraft(
    val id: String,
    val type: QuestionType,
    val instructions: String,
    // CHOICE: label -> description. SCORE: ordered level descriptions. NOUL: unused.
    val choiceOptions: List<CriterionOption> = emptyList(),
    val scoreLevels: List<String> = emptyList()
)

/** Ready-made scenarios so the app is useful on first launch, no typing required. */
object Presets {

    data class Preset(
        val title: String,
        val stateBody: String,
        val questions: List<QuestionDraft>
    )

    val all = listOf(
        Preset(
            title = "Billing refund ticket",
            stateBody = "We were billed twice this month for the Pro plan. Please refund the duplicate charge, this is the second time it's happened.",
            questions = listOf(
                QuestionDraft(
                    id = "department",
                    type = QuestionType.CHOICE,
                    instructions = "Which team should handle this?",
                    choiceOptions = listOf(
                        CriterionOption("billing", "invoices, payments, refunds"),
                        CriterionOption("technical", "bugs, outages, system errors"),
                        CriterionOption("other", "everything else")
                    )
                ),
                QuestionDraft(
                    id = "urgency",
                    type = QuestionType.SCORE,
                    instructions = "How urgent is this request?",
                    scoreLevels = listOf("not urgent", "soon", "critical deadline or blocking issue")
                ),
                QuestionDraft(
                    id = "churn_risk",
                    type = QuestionType.NOUL,
                    instructions = "Does the user threaten to cancel or leave?"
                )
            )
        ),
        Preset(
            title = "Angry support message",
            stateBody = "This is absolutely ridiculous. Your app crashed and I lost two hours of work. If this happens again I'm switching to a competitor and telling everyone I know.",
            questions = listOf(
                QuestionDraft(
                    id = "sentiment",
                    type = QuestionType.CHOICE,
                    instructions = "What is the user's emotional tone?",
                    choiceOptions = listOf(
                        CriterionOption("angry", "frustrated, upset, hostile"),
                        CriterionOption("neutral", "matter-of-fact, calm"),
                        CriterionOption("positive", "happy, satisfied, appreciative")
                    )
                ),
                QuestionDraft(
                    id = "churn_risk",
                    type = QuestionType.NOUL,
                    instructions = "Does the user threaten to cancel or leave?"
                ),
                QuestionDraft(
                    id = "escalate",
                    type = QuestionType.NOUL,
                    instructions = "Should this be escalated to a human agent immediately?"
                )
            )
        ),
        Preset(
            title = "Product feedback",
            stateBody = "The new dashboard redesign is a lot cleaner, but the export button moved and now I can't find it. Otherwise pretty happy with the update.",
            questions = listOf(
                QuestionDraft(
                    id = "overall_sentiment",
                    type = QuestionType.SCORE,
                    instructions = "How positive is this feedback overall?",
                    scoreLevels = listOf("negative", "mixed", "positive")
                ),
                QuestionDraft(
                    id = "contains_bug_report",
                    type = QuestionType.NOUL,
                    instructions = "Does this describe a usability problem or bug?"
                )
            )
        )
    )
}
