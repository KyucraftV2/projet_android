package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.AnswerState
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.TestJapaneseData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.random.Random

/** Vérifie le déroulement d'un quiz, du tirage au score final. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = TestJapaneseData.real

    private class FakeDataSource(
        private val data: JapaneseData? = null,
        private val failure: Exception? = null,
    ) : JapaneseDataSource {
        override suspend fun load(): JapaneseData = failure?.let { throw it } ?: data!!
    }

    private fun viewModel(
        settings: QuizSettings = QuizSettings(),
        source: JapaneseDataSource = FakeDataSource(data),
    ) = QuizViewModel(settings, source, Random(1234))

    @Test
    fun `le quiz est tire une fois les donnees chargees`() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(Quiz.QUESTION_COUNT, state.session?.quiz?.size)
        assertEquals(Quiz.CHOICE_COUNT, state.question?.choices?.size)
        assertEquals(List(10) { AnswerState.UNANSWERED }, state.session?.results)
    }

    @Test
    fun `repondre juste passe a la question suivante`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val premiere = vm.uiState.value.question!!

        vm.answer(premiere.correctIndex)

        val state = vm.uiState.value
        assertEquals(AnswerState.CORRECT, state.session!!.results.first())
        assertEquals("2 / 10", state.session.positionLabel)
        assertNotEquals(premiere, state.question)
    }

    @Test
    fun `repondre faux est enregistre comme incorrect`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val question = vm.uiState.value.question!!

        // N'importe quelle réponse qui n'est pas la bonne.
        vm.answer((question.correctIndex + 1) % question.choices.size)

        assertEquals(AnswerState.INCORRECT, vm.uiState.value.session!!.results.first())
    }

    @Test
    fun `un sans faute donne un score plein et termine le quiz`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        repeat(Quiz.QUESTION_COUNT) {
            vm.answer(vm.uiState.value.question!!.correctIndex)
        }

        val state = vm.uiState.value
        assertTrue(state.isFinished)
        assertNull(state.question)
        assertEquals("10 / 10", state.session!!.scoreLabel)
    }

    @Test
    fun `repondre apres la fin du quiz n ajoute pas de resultat`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        repeat(Quiz.QUESTION_COUNT) { vm.answer(0) }

        vm.answer(0)
        vm.answer(1)

        val session = vm.uiState.value.session!!
        assertEquals(Quiz.QUESTION_COUNT, session.answeredCount)
        assertTrue(session.results.none { it == AnswerState.UNANSWERED })
    }

    @Test
    fun `rejouer tire un nouveau quiz vierge`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        val premierQuiz = vm.uiState.value.session!!.quiz
        vm.answer(0)

        vm.restart()
        advanceUntilIdle()

        val session = vm.uiState.value.session!!
        assertEquals(0, session.answeredCount)
        assertEquals(0, session.currentIndex)
        assertNotEquals(premierQuiz, session.quiz)
    }

    @Test
    fun `les reglages choisis sont ceux du quiz tire`() = runTest {
        val settings = QuizSettings(
            characterSet = CharacterSet.KANJI,
            questionType = QuizQuestionType.CHARACTER_TO_READING,
            grade = 2,
            hintsEnabled = true,
        )
        val vm = viewModel(settings)
        advanceUntilIdle()

        val quiz = vm.uiState.value.session!!.quiz
        assertEquals(settings, quiz.settings)
        assertEquals(QuizQuestionType.CHARACTER_TO_READING, quiz.questions.first().type)
        assertTrue(quiz.questions.all { it.hint != null })
        // L'aide ne doit pas servir deux fois : la consigne reste celle du type.
        assertEquals("Comment se prononce ce kanji ?", quiz.questions.first().consigne)
    }

    @Test
    fun `un echec de chargement est signale a l utilisateur`() = runTest {
        val vm = viewModel(source = FakeDataSource(failure = IOException("Fichier illisible")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fichier illisible", state.errorMessage)
        assertNull(state.session)
    }
}
