package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

/**
 * D'où vient un résultat enregistré.
 *
 * Le sujet demande un test instrumenté qui remplit la base de faux résultats,
 * puis un autre qui nettoie « en n'enlevant que les résultats factices » : il
 * faut donc pouvoir les distinguer de ceux d'un vrai utilisateur, sans quoi le
 * nettoyage effacerait sa progression.
 */
enum class ResultOrigin {
    /** Quiz réellement joué par l'utilisateur. */
    USER,

    /** Résultat fabriqué par un test, à nettoyer ensuite. */
    SAMPLE,
}

/**
 * Le résultat d'un quiz terminé.
 *
 * Immuable et sans dépendance à Room ni à Android : c'est sur ces objets que se
 * calcule le grade de l'utilisateur, en test unitaire JVM.
 *
 * @param id l'identifiant en base, `0` tant que le résultat n'est pas enregistré.
 * @param completedAt date de fin, en millisecondes depuis l'époque Unix. On
 *   garde un `Long` plutôt qu'un type de `java.time`, indisponible sans
 *   désucrage sous Android 8.
 * @param settings les réglages du quiz joué, qui disent sur quoi l'utilisateur
 *   a été interrogé et à quelle difficulté.
 * @param questionCount le nombre de questions posées (dix, sauf vivier trop
 *   petit).
 * @param correctCount le nombre de bonnes réponses.
 */
data class QuizResult(
    val id: Long = 0,
    val completedAt: Long,
    val settings: QuizSettings,
    val questionCount: Int,
    val correctCount: Int,
    val origin: ResultOrigin = ResultOrigin.USER,
) {

    /** La part de bonnes réponses, entre 0 et 1. */
    val successRate: Double
        get() = if (questionCount == 0) 0.0 else correctCount.toDouble() / questionCount

    /** Le score tel qu'on l'affiche, par exemple `7 / 10`. */
    val scoreLabel: String get() = "$correctCount / $questionCount"

    companion object {

        /** Le résultat d'une session jouée, prêt à être enregistré. */
        fun of(
            session: QuizSession,
            completedAt: Long,
            origin: ResultOrigin = ResultOrigin.USER,
        ): QuizResult = QuizResult(
            completedAt = completedAt,
            settings = session.quiz.settings,
            questionCount = session.quiz.size,
            correctCount = session.correctCount,
            origin = origin,
        )
    }
}
