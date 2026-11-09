package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.TestJapaneseData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Vérifie l'écran de configuration sur les vraies données : ce sont elles qui
 * déterminent les grades proposés et la taille des viviers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizSetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val data = TestJapaneseData.real

    private class FakeDataSource(
        private val data: JapaneseData? = null,
        private val failure: Exception? = null,
    ) : JapaneseDataSource {
        override suspend fun load(): JapaneseData = failure?.let { throw it } ?: data!!
    }

    private fun viewModel(source: JapaneseDataSource = FakeDataSource(data)) =
        QuizSetupViewModel(source)

    @Test
    fun `l etat initial est en chargement`() = runTest {
        val vm = viewModel()

        assertTrue(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.canStart)
    }

    @Test
    fun `les grades proposes sont ceux reellement presents dans le jeu de donnees`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // Le sujet annonce des grades de 1 à 9, mais kanji_jouyou.json s'arrête
        // au grade 8 et ne contient aucun grade 7.
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 8), vm.uiState.value.availableGrades)
    }

    @Test
    fun `le vivier par defaut est celui des hiragana`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        val hiragana = data.kana.count { it.type == KanaType.HIRAGANA }

        assertEquals(CharacterSet.HIRAGANA, state.settings.characterSet)
        assertEquals(hiragana, state.poolSize)
        assertFalse(state.showGrade)
        assertTrue(state.canStart)
    }

    @Test
    fun `choisir les kanji repare le type de question et affiche la difficulte`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectCharacterSet(CharacterSet.KANJI)

        val state = vm.uiState.value
        assertEquals(CharacterSet.KANJI, state.settings.characterSet)
        assertTrue(state.settings.isConsistent)
        assertTrue(state.showGrade)
        assertEquals(4, state.questionTypes.size)
    }

    @Test
    fun `le vivier grandit avec le grade demande`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.selectCharacterSet(CharacterSet.KANJI)

        vm.selectGrade(1)
        val premiereAnnee = vm.uiState.value.poolSize
        vm.selectGrade(6)
        val finDuPrimaire = vm.uiState.value.poolSize
        vm.selectGrade(8)
        val tout = vm.uiState.value.poolSize

        assertEquals(80, premiereAnnee)
        assertTrue("$premiereAnnee puis $finDuPrimaire", finDuPrimaire > premiereAnnee)
        assertTrue("$finDuPrimaire puis $tout", tout > finDuPrimaire)
        assertEquals(data.kanji.count { it.grade != null }, tout)
    }

    @Test
    fun `changer de syllabaire conserve le type de question`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectQuestionType(QuizQuestionType.ROMAJI_TO_CHARACTER)
        vm.selectCharacterSet(CharacterSet.KATAKANA)

        val state = vm.uiState.value
        assertEquals(QuizQuestionType.ROMAJI_TO_CHARACTER, state.settings.questionType)
        assertEquals(data.kana.count { it.type == KanaType.KATAKANA }, state.poolSize)
    }

    @Test
    fun `les options d aide et de vocalisation sont memorisees`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setHintsEnabled(true)
        vm.setSpeechEnabled(true)

        assertTrue(vm.uiState.value.settings.hintsEnabled)
        assertTrue(vm.uiState.value.settings.speechEnabled)
    }

    @Test
    fun `un echec de chargement empeche de lancer un quiz`() = runTest {
        val vm = viewModel(FakeDataSource(failure = IOException("Fichier illisible")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Fichier illisible", state.errorMessage)
        assertFalse(state.canStart)
    }
}
