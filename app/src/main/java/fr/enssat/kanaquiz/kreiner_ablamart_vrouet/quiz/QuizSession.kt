package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

/** L'état d'une question dans la liste des résultats affichée pendant le quiz. */
enum class AnswerState {
    /** Pas encore répondu. */
    UNANSWERED,

    /** Réponse correcte. */
    CORRECT,

    /** Réponse incorrecte. */
    INCORRECT,
}

/**
 * La progression de l'utilisateur dans un quiz.
 *
 * Immuable : [answer] renvoie une nouvelle session. L'écran n'a ainsi qu'un
 * seul état à observer, et toute la logique de correction se teste sans
 * interface graphique.
 *
 * @param quiz le quiz joué, inchangé du début à la fin.
 * @param answers l'indice de la réponse choisie pour chaque question, `null`
 *   tant qu'elle n'a pas été répondue.
 * @param currentIndex la question affichée.
 */
data class QuizSession(
    val quiz: Quiz,
    val answers: List<Int?> = List(quiz.size) { null },
    val currentIndex: Int = 0,
) {

    /** La question affichée, `null` quand le quiz est terminé. */
    val current: QuizQuestion? get() = quiz.questions.getOrNull(currentIndex)

    /** L'état de chacune des questions, dans l'ordre du quiz. */
    val results: List<AnswerState>
        get() = quiz.questions.mapIndexed { index, question ->
            when (val choice = answers.getOrNull(index)) {
                null -> AnswerState.UNANSWERED
                else -> if (question.isCorrect(choice)) AnswerState.CORRECT else AnswerState.INCORRECT
            }
        }

    /** Le nombre de bonnes réponses données jusqu'ici. */
    val correctCount: Int get() = results.count { it == AnswerState.CORRECT }

    /** Le nombre de questions déjà répondues. */
    val answeredCount: Int get() = answers.count { it != null }

    /** Vrai quand toutes les questions ont été répondues. */
    val isFinished: Boolean get() = answeredCount == quiz.size

    /** Position dans le quiz, par exemple `3 / 10`. */
    val positionLabel: String
        get() = if (quiz.size == 0) "" else "${minOf(currentIndex + 1, quiz.size)} / ${quiz.size}"

    /** Le score final, par exemple `7 / 10`. */
    val scoreLabel: String get() = "$correctCount / ${quiz.size}"

    /**
     * Enregistre la réponse [choiceIndex] à la question courante et passe à la
     * suivante.
     *
     * Sans effet si le quiz est terminé, si la question a déjà été répondue ou
     * si l'indice ne correspond à aucune réponse proposée : un double clic sur
     * une réponse ne doit pas faire sauter une question.
     */
    fun answer(choiceIndex: Int): QuizSession {
        val question = current ?: return this
        if (answers[currentIndex] != null) return this
        if (choiceIndex !in question.choices.indices) return this

        return copy(
            answers = answers.toMutableList().also { it[currentIndex] = choiceIndex },
            currentIndex = currentIndex + 1,
        )
    }
}
