package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import kotlin.math.floor
import kotlin.math.pow

/**
 * Le grade d'un apprenant, mesuré sur ses derniers quiz.
 *
 * Le grade reprend l'échelle des kanji : « grade 3 » signifie que l'utilisateur
 * réussit, sans aide, des quiz portant sur les kanji appris jusqu'à la
 * troisième année du primaire. Un apprenant qui ne travaille que les kana
 * plafonne donc à 1, puisque le sujet donne le grade 1 aux syllabaires : pour
 * monter, il faut affronter des quiz plus difficiles, et c'est exactement
 * l'incitation que l'on cherche.
 *
 * @param value le grade continu, entre 0 et [MAX_LEVEL].
 * @param resultCount le nombre de quiz réellement pris en compte, c'est-à-dire
 *   la taille de la fenêtre glissante remplie.
 */
data class UserGrade(
    val value: Double,
    val resultCount: Int,
) {

    /** Le grade affiché, partie entière du grade continu. */
    val level: Int get() = floor(value).toInt()

    /** L'avancement vers le grade suivant, entre 0 et 1, pour la barre de progression. */
    val progressToNextLevel: Float get() = (value - level).toFloat().coerceIn(0f, 1f)

    /** Le libellé affiché, par exemple `Grade 3`. */
    val label: String get() = if (level == 0) "Débutant" else "Grade $level"

    companion object {

        /**
         * Le grade le plus élevé atteignable.
         *
         * C'est le grade maximal des kanji du jeu de données : `kanji_jouyou.json`
         * s'arrête au collège, codé 8. Un test vérifie que la constante colle
         * toujours aux données embarquées.
         */
        const val MAX_LEVEL = 8
    }
}

/** Un point de la courbe de progression : le grade juste après un quiz. */
data class GradePoint(
    /** Date du quiz qui a produit ce point, en millisecondes. */
    val completedAt: Long,

    /** Le grade atteint à ce moment-là. */
    val value: Double,
)

/**
 * Calcule le grade d'un apprenant à partir de ses résultats.
 *
 * ## La formule
 *
 * ```
 *          Σ  wᵢ · gᵢ · qᵢ
 * grade =  ────────────────      sur les [WINDOW_SIZE] derniers quiz
 *             Σ  wᵢ
 * ```
 *
 * - `gᵢ` est la **difficulté** du quiz : son grade (1 pour les kana, le grade
 *   demandé pour les kanji), diminué de [HINT_PENALTY] quand l'utilisateur a
 *   activé les aides. Réussir avec la signification sous les yeux ne prouve pas
 *   la même maîtrise ; en revanche la vocalisation ne pénalise rien, car le
 *   générateur se tait dès qu'entendre l'énoncé donnerait la réponse : c'est un
 *   confort d'écoute, pas une aide à la réponse.
 * - `qᵢ` est la **qualité** du quiz, le taux de bonnes réponses ramené à
 *   l'échelle « au-dessus du hasard ». Avec quatre réponses proposées, 25 %
 *   s'obtiennent en répondant n'importe quoi : `q = (taux − ¼) / (¾)`, borné à
 *   0. Un quiz au hasard ne rapporte donc rien, et 10/10 vaut exactement le
 *   grade du quiz.
 * - `wᵢ` est le **poids d'ancienneté**, [RECENCY_DECAY] élevé au nombre de quiz
 *   joués depuis : le quiz le plus récent compte pleinement, le dixième en
 *   arrière environ un quart. Les progrès se voient donc vite, sans qu'un seul
 *   mauvais quiz n'efface le niveau atteint.
 *
 * ## Pourquoi une fenêtre glissante
 *
 * Le sujet l'impose, et pour une bonne raison : une moyenne sur tout
 * l'historique ferait traîner indéfiniment les débuts de l'apprenant et
 * rendrait la progression de plus en plus lente à bouger, jusqu'à devenir
 * décourageante. La fenêtre de dix quiz, soit une centaine de questions, est
 * assez large pour que le grade ne saute pas à chaque partie, et assez courte
 * pour récompenser un progrès réel en quelques quiz.
 *
 * Le grade peut aussi **redescendre** : c'est un niveau courant, pas un
 * trophée. Mais grâce à la décote d'ancienneté, une mauvaise série se rattrape
 * en deux ou trois bons quiz.
 */
object UserGradeCalculator {

    /** Le nombre de quiz récents pris en compte. */
    const val WINDOW_SIZE = 10

    /** Facteur d'oubli d'un quiz à l'autre, du plus récent au plus ancien. */
    const val RECENCY_DECAY = 0.85

    /** Ce que coûte l'activation des aides sur la difficulté d'un quiz. */
    const val HINT_PENALTY = 0.15

    /** Le taux de bonnes réponses obtenu en répondant au hasard. */
    const val CHANCE_RATE = 1.0 / Quiz.CHOICE_COUNT

    /**
     * Le grade de l'utilisateur, ou `null` s'il n'a encore terminé aucun quiz.
     *
     * [results] est attendu dans l'ordre chronologique, du plus ancien au plus
     * récent, comme le renvoie la base de données.
     */
    fun gradeOf(results: List<QuizResult>): UserGrade? {
        val window = results.takeLast(WINDOW_SIZE)
        if (window.isEmpty()) return null

        var weightedSum = 0.0
        var weightSum = 0.0
        // L'indice 0 est le quiz le plus récent de la fenêtre.
        for ((age, result) in window.asReversed().withIndex()) {
            val weight = RECENCY_DECAY.pow(age)
            weightedSum += weight * difficultyOf(result) * qualityOf(result)
            weightSum += weight
        }

        return UserGrade(
            value = (weightedSum / weightSum).coerceIn(0.0, UserGrade.MAX_LEVEL.toDouble()),
            resultCount = window.size,
        )
    }

    /**
     * La courbe de progression : le grade tel qu'il était après chaque quiz.
     *
     * Chaque point est recalculé avec la même fonction que le grade affiché, la
     * courbe se termine donc exactement sur le grade courant.
     */
    fun progressionOf(results: List<QuizResult>): List<GradePoint> =
        results.indices.mapNotNull { index ->
            val grade = gradeOf(results.subList(0, index + 1)) ?: return@mapNotNull null
            GradePoint(completedAt = results[index].completedAt, value = grade.value)
        }

    /** La difficulté d'un quiz : son grade, amputé si l'utilisateur s'est fait aider. */
    private fun difficultyOf(result: QuizResult): Double {
        val assistance = if (result.settings.hintsEnabled) 1.0 - HINT_PENALTY else 1.0
        return result.settings.effectiveGrade * assistance
    }

    /**
     * La qualité d'un quiz : le taux de bonnes réponses au-dessus du hasard,
     * entre 0 et 1.
     */
    private fun qualityOf(result: QuizResult): Double =
        ((result.successRate - CHANCE_RATE) / (1.0 - CHANCE_RATE)).coerceIn(0.0, 1.0)
}
