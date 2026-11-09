package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import kotlin.math.pow

/**
 * L'historique factice utilisé par les tests instrumentés.
 *
 * C'est le parcours plausible d'un apprenant : des hiragana hésitants, puis
 * maîtrisés, puis des kanji de plus en plus difficiles, avec une tentative
 * prématurée au grade 5 qui se solde par un échec. Il produit une courbe
 * croissante, donc un graphique représentatif de ce que l'écran doit montrer.
 *
 * Tous les résultats portent [ResultOrigin.SAMPLE] : c'est à cela que le test
 * de nettoyage les reconnaîtra, sans toucher aux quiz réellement joués.
 */
object SampleResults {

    /** Point de départ de l'historique : le 1er janvier 2026 à midi (UTC). */
    private const val FIRST_DAY = 1_767_268_800_000L

    private const val ONE_DAY = 86_400_000L

    /**
     * Le grade attendu au bout du parcours, calculé à la main.
     *
     * Valeur de référence : si la formule change, ce test doit le signaler.
     */
    const val EXPECTED_GRADE = 4.1272777814

    /** Le parcours complet, du plus ancien au plus récent. */
    val journey: List<QuizResult> = listOf(
        kana(correct = 5),
        kana(correct = 7),
        kana(correct = 9),
        kana(correct = 10),
        kanji(correct = 8, grade = 1, hints = true),
        kanji(correct = 9, grade = 1),
        kanji(correct = 7, grade = 2),
        kanji(correct = 8, grade = 3),
        kanji(correct = 10, grade = 3),
        // Une tentative trop ambitieuse : le grade doit encaisser sans s'effondrer.
        kanji(correct = 3, grade = 5),
        kanji(correct = 8, grade = 4),
        kanji(correct = 9, grade = 4),
        kanji(correct = 8, grade = 5),
        kanji(correct = 9, grade = 6),
        kanji(correct = 7, grade = 8),
        kanji(correct = 9, grade = 8),
    ).mapIndexed { index, result ->
        // Un quiz par jour, pour un historique qui ressemble à un vrai usage.
        result.copy(completedAt = FIRST_DAY + index * ONE_DAY)
    }

    /**
     * S'assure que l'historique factice est en base, sans le dupliquer.
     *
     * Les tests instrumentés sont rejouables : relancer celui qui remplit la
     * base ne doit pas y empiler le parcours une deuxième fois.
     */
    suspend fun ensurePresent(source: QuizResultSource): List<QuizResult> {
        val existing = source.results().filter { it.origin == ResultOrigin.SAMPLE }
        if (existing.size == journey.size) return existing

        source.deleteByOrigin(ResultOrigin.SAMPLE)
        source.saveAll(journey)
        return source.results().filter { it.origin == ResultOrigin.SAMPLE }
    }

    /**
     * Recalcule le grade à la main, sans appeler le code de production.
     *
     * Les constantes sont réécrites en dur exprès : vérifier le calcul en
     * appelant la fonction testée ne vérifierait rien du tout.
     */
    fun expectedGrade(results: List<QuizResult>): Double {
        val window = results.takeLast(10)
        var weighted = 0.0
        var weights = 0.0
        window.asReversed().forEachIndexed { age, result ->
            val weight = 0.85.pow(age)
            val rate = result.correctCount.toDouble() / result.questionCount
            val quality = ((rate - 0.25) / 0.75).coerceIn(0.0, 1.0)
            val difficulty = result.settings.effectiveGrade *
                if (result.settings.hintsEnabled) 0.85 else 1.0
            weighted += weight * difficulty * quality
            weights += weight
        }
        return weighted / weights
    }

    private fun kana(correct: Int) = QuizResult(
        completedAt = 0,
        settings = QuizSettings(
            characterSet = CharacterSet.HIRAGANA,
            questionType = QuizQuestionType.CHARACTER_TO_ROMAJI,
        ),
        questionCount = Quiz.QUESTION_COUNT,
        correctCount = correct,
        origin = ResultOrigin.SAMPLE,
    )

    private fun kanji(correct: Int, grade: Int, hints: Boolean = false) = QuizResult(
        completedAt = 0,
        settings = QuizSettings(
            characterSet = CharacterSet.KANJI,
            questionType = QuizQuestionType.CHARACTER_TO_MEANING,
            grade = grade,
            hintsEnabled = hints,
        ),
        questionCount = Quiz.QUESTION_COUNT,
        correctCount = correct,
        origin = ResultOrigin.SAMPLE,
    )
}
