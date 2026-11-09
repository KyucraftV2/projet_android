package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vérifie le suivi des réponses pendant un quiz.
 *
 * Le quiz utilisé est écrit à la main : on teste ici la mécanique des
 * résultats, pas le tirage (voir [QuizGeneratorTest]).
 */
class QuizSessionTest {

    /** Trois questions dont la bonne réponse est toujours en première position. */
    private val quiz = Quiz(
        settings = QuizSettings(),
        questions = listOf("a", "i", "u").map { romaji ->
            QuizQuestion(
                character = Kana(
                    character = romaji,
                    romaji = romaji,
                    type = KanaType.HIRAGANA,
                    section = "hiragana_alphabet",
                    group = "voyelles",
                ),
                type = QuizQuestionType.CHARACTER_TO_ROMAJI,
                prompt = romaji,
                hint = null,
                promptSpeech = null,
                choices = listOf(romaji, "x", "y", "z"),
                correctIndex = 0,
            )
        },
    )

    private val session = QuizSession(quiz)

    @Test
    fun `une session neuve commence a la premiere question sans resultat`() {
        assertEquals(quiz.questions.first(), session.current)
        assertEquals("1 / 3", session.positionLabel)
        assertEquals(List(3) { AnswerState.UNANSWERED }, session.results)
        assertEquals(0, session.answeredCount)
        assertFalse(session.isFinished)
    }

    @Test
    fun `repondre juste marque la question correcte et passe a la suivante`() {
        val next = session.answer(0)

        assertEquals(AnswerState.CORRECT, next.results.first())
        assertEquals(AnswerState.UNANSWERED, next.results[1])
        assertEquals(quiz.questions[1], next.current)
        assertEquals("2 / 3", next.positionLabel)
        assertEquals(1, next.correctCount)
    }

    @Test
    fun `repondre faux marque la question incorrecte`() {
        val next = session.answer(2)

        assertEquals(AnswerState.INCORRECT, next.results.first())
        assertEquals(0, next.correctCount)
        assertEquals(1, next.answeredCount)
    }

    @Test
    fun `le quiz est termine quand toutes les questions sont repondues`() {
        val finished = session.answer(0).answer(1).answer(0)

        assertTrue(finished.isFinished)
        assertNull(finished.current)
        assertEquals("2 / 3", finished.scoreLabel)
        assertEquals(
            listOf(AnswerState.CORRECT, AnswerState.INCORRECT, AnswerState.CORRECT),
            finished.results,
        )
        // La position reste bornée au nombre de questions.
        assertEquals("3 / 3", finished.positionLabel)
    }

    @Test
    fun `repondre apres la fin du quiz ne change rien`() {
        val finished = session.answer(0).answer(0).answer(0)

        assertEquals(finished, finished.answer(0))
    }

    @Test
    fun `un indice de reponse inexistant est ignore`() {
        assertEquals(session, session.answer(-1))
        assertEquals(session, session.answer(Quiz.CHOICE_COUNT))
    }

    @Test
    fun `la session d un quiz vide est immediatement terminee`() {
        val empty = QuizSession(Quiz(QuizSettings(), emptyList()))

        assertTrue(empty.isFinished)
        assertNull(empty.current)
        assertEquals("", empty.positionLabel)
        assertEquals(emptyList<AnswerState>(), empty.results)
    }
}
