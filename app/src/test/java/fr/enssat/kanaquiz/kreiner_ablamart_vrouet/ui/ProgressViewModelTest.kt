package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGradeCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Vérifie l'état de la page de progression, et donc du grade affiché sur
 * l'accueil, qui lit le même ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var horloge = 1_700_000_000_000L

    private fun result(correct: Int, grade: Int): QuizResult {
        horloge += 60_000
        return QuizResult(
            completedAt = horloge,
            settings = QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                grade = grade,
            ),
            questionCount = Quiz.QUESTION_COUNT,
            correctCount = correct,
        )
    }

    @Test
    fun `sans resultat l etat est vide mais charge`() = runTest {
        val vm = ProgressViewModel(FakeQuizResultSource())
        // stateIn(WhileSubscribed) n'emet rien sans collecteur : sans cet
        // abonnement, l'etat resterait sur sa valeur initiale.
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.hasResults)
        assertNull(state.grade)
        assertNull(state.bestValue)
        assertEquals(0, state.totalQuizCount)
    }

    @Test
    fun `le grade et la courbe suivent l historique`() = runTest {
        val results = listOf(result(10, 1), result(10, 3), result(10, 5))
        val vm = ProgressViewModel(FakeQuizResultSource(results))
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.hasResults)
        assertEquals(3, state.totalQuizCount)
        assertEquals(3, state.progression.size)
        assertEquals(UserGradeCalculator.gradeOf(results)!!.value, state.grade!!.value, 0.0001)
        // La courbe se termine sur le grade affiché.
        assertEquals(state.grade!!.value, state.progression.last().value, 0.0001)
    }

    @Test
    fun `le record est le meilleur point de la courbe et non le dernier`() = runTest {
        // Une bonne serie, puis une rechute : le record doit rester en memoire.
        val results = List(4) { result(10, 6) } + List(4) { result(3, 1) }
        val vm = ProgressViewModel(FakeQuizResultSource(results))
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(state.progression.maxOf { it.value }, state.bestValue!!, 0.0001)
        assertTrue("${state.bestValue} vs ${state.grade!!.value}", state.bestValue!! > state.grade!!.value)
    }

    @Test
    fun `terminer un quiz met a jour le grade sans rafraichissement`() = runTest {
        val source = FakeQuizResultSource()
        val vm = ProgressViewModel(source)
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()
        assertNull(vm.uiState.value.grade)

        source.save(result(10, 4))
        advanceUntilIdle()

        assertEquals(4.0, vm.uiState.value.grade!!.value, 0.0001)
        assertEquals(1, vm.uiState.value.totalQuizCount)
    }

    @Test
    fun `une base illisible est signalee sans faire planter l ecran`() = runTest {
        val vm = ProgressViewModel(FakeQuizResultSource(failingReads = true))
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Base illisible", state.errorMessage)
        assertFalse(state.hasResults)
    }

    @Test
    fun `seuls les dix derniers quiz comptent dans le grade`() = runTest {
        val results = List(15) { result(10, 2) }
        val vm = ProgressViewModel(FakeQuizResultSource(results))
        backgroundScope.launch { vm.uiState.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(15, state.totalQuizCount)
        assertEquals(UserGradeCalculator.WINDOW_SIZE, state.grade!!.resultCount)
        // La courbe garde en revanche tout l'historique.
        assertEquals(15, state.progression.size)
    }
}
