package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.AnswerState
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.TestJapaneseData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    /** Date de fin fixe, pour que le résultat enregistré soit vérifiable. */
    private val FIN = 1_700_000_000_000L

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = TestJapaneseData.real

    private class FakeDataSource(
        private val data: JapaneseData? = null,
        private val failure: Exception? = null,
    ) : JapaneseDataSource {
        override suspend fun load(): JapaneseData = failure?.let { throw it } ?: data!!
    }

    /** Historique en mémoire, qui note ce qu'on lui demande d'enregistrer. */
    private class FakeResultSource(private val failing: Boolean = false) : QuizResultSource {
        val saved = mutableListOf<QuizResult>()

        override fun observeResults(): Flow<List<QuizResult>> = flowOf(saved.toList())

        override suspend fun results(): List<QuizResult> = saved.toList()

        override suspend fun save(result: QuizResult): Long {
            if (failing) throw IOException("Base indisponible")
            saved += result
            return saved.size.toLong()
        }

        override suspend fun saveAll(results: List<QuizResult>) {
            results.forEach { save(it) }
        }

        override suspend fun deleteByOrigin(origin: ResultOrigin): Int {
            val before = saved.size
            saved.removeAll { it.origin == origin }
            return before - saved.size
        }

        override suspend fun count(): Int = saved.size
    }

    private val results = FakeResultSource()

    private fun viewModel(
        settings: QuizSettings = QuizSettings(),
        source: JapaneseDataSource = FakeDataSource(data),
        resultSource: QuizResultSource = results,
    ) = QuizViewModel(settings, source, resultSource, Random(1234), now = { FIN })

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
    fun `terminer un quiz enregistre le resultat une seule fois`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        repeat(Quiz.QUESTION_COUNT) { vm.answer(vm.uiState.value.question!!.correctIndex) }
        advanceUntilIdle()
        // Des appuis supplementaires ne doivent pas enregistrer deux fois.
        vm.answer(0)
        advanceUntilIdle()

        assertEquals(1, results.saved.size)
        val result = results.saved.single()
        assertEquals(FIN, result.completedAt)
        assertEquals(Quiz.QUESTION_COUNT, result.questionCount)
        assertEquals(Quiz.QUESTION_COUNT, result.correctCount)
        assertEquals(QuizSettings(), result.settings)
        assertEquals(ResultOrigin.USER, result.origin)
        assertFalse(vm.uiState.value.saveFailed)
    }

    @Test
    fun `un quiz abandonne n est pas enregistre`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        repeat(Quiz.QUESTION_COUNT - 1) { vm.answer(0) }
        advanceUntilIdle()

        assertTrue(results.saved.isEmpty())
    }

    @Test
    fun `le score enregistre est celui des bonnes reponses`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // Une bonne reponse sur deux.
        repeat(Quiz.QUESTION_COUNT) { index ->
            val question = vm.uiState.value.question!!
            val choice = if (index % 2 == 0) {
                question.correctIndex
            } else {
                (question.correctIndex + 1) % question.choices.size
            }
            vm.answer(choice)
        }
        advanceUntilIdle()

        assertEquals(5, results.saved.single().correctCount)
        assertEquals(0.5, results.saved.single().successRate, 0.0001)
    }

    @Test
    fun `rejouer puis terminer enregistre un second resultat`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        repeat(Quiz.QUESTION_COUNT) { vm.answer(0) }
        advanceUntilIdle()

        vm.restart()
        advanceUntilIdle()
        repeat(Quiz.QUESTION_COUNT) { vm.answer(0) }
        advanceUntilIdle()

        assertEquals(2, results.saved.size)
    }

    @Test
    fun `un echec d enregistrement est signale sans effacer le score`() = runTest {
        val vm = viewModel(resultSource = FakeResultSource(failing = true))
        advanceUntilIdle()

        repeat(Quiz.QUESTION_COUNT) { vm.answer(vm.uiState.value.question!!.correctIndex) }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.saveFailed)
        assertTrue(state.isFinished)
        assertEquals("10 / 10", state.session!!.scoreLabel)
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
