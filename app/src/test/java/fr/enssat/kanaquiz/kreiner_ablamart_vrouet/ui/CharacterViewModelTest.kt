package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.RomajiConverter
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * Vérifie la navigation et le changement de syllabaire, sur un faux jeu de
 * données réduit : deux hiragana, deux katakana et un kanji.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CharacterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun kana(character: String, romaji: String, type: KanaType) = Kana(
        character = character,
        romaji = romaji,
        type = type,
        section = "alphabet",
        group = "voyelles",
    )

    private val hiragana = listOf(
        kana("あ", "a", KanaType.HIRAGANA),
        kana("い", "i", KanaType.HIRAGANA),
    )
    private val katakana = listOf(
        kana("ア", "a", KanaType.KATAKANA),
        kana("イ", "i", KanaType.KATAKANA),
    )
    private val kanji = listOf(
        Kanji(
            character = "一",
            meanings = listOf("One"),
            readingsOn = listOf("いち"),
            readingsKun = listOf("ひと-"),
            grade = 1,
            strokes = 1,
            frequency = 2,
            jlptLevel = 5,
        )
    )

    private val fakeData = JapaneseData(
        kana = hiragana + katakana,
        kanji = kanji,
        romaji = RomajiConverter(hiragana + katakana),
    )

    private class FakeDataSource(
        private val data: JapaneseData? = null,
        private val failure: Exception? = null,
    ) : JapaneseDataSource {
        override suspend fun load(): JapaneseData = failure?.let { throw it } ?: data!!
    }

    private fun viewModel(source: JapaneseDataSource = FakeDataSource(fakeData)) =
        CharacterViewModel(source)

    @Test
    fun `l etat initial est en chargement`() = runTest {
        val vm = viewModel()

        // La coroutine de chargement n'a pas encore tourné.
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.current)
    }

    @Test
    fun `apres chargement on affiche le premier hiragana`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals(CharacterSet.HIRAGANA, state.mode)
        assertEquals("あ", state.current?.character)
        assertEquals("1 / 2", state.positionLabel)
    }

    @Test
    fun `suivant avance d un caractere`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showNext()

        assertEquals("い", vm.uiState.value.current?.character)
        assertEquals("2 / 2", vm.uiState.value.positionLabel)
    }

    @Test
    fun `suivant est sans effet sur le dernier caractere`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.showNext()

        assertFalse(vm.uiState.value.hasNext)
        vm.showNext()

        assertEquals(1, vm.uiState.value.index)
    }

    @Test
    fun `precedent est sans effet sur le premier caractere`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.hasPrevious)
        vm.showPrevious()

        assertEquals(0, vm.uiState.value.index)
    }

    @Test
    fun `precedent revient en arriere`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.showNext()

        vm.showPrevious()

        assertEquals("あ", vm.uiState.value.current?.character)
    }

    @Test
    fun `changer de mode change la liste affichee`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.selectMode(CharacterSet.KATAKANA)

        assertEquals(CharacterSet.KATAKANA, vm.uiState.value.mode)
        assertEquals("ア", vm.uiState.value.current?.character)

        vm.selectMode(CharacterSet.KANJI)

        assertEquals("一", vm.uiState.value.current?.character)
        assertEquals("1 / 1", vm.uiState.value.positionLabel)
    }

    @Test
    fun `revenir a un mode restitue la position precedente`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        vm.showNext() // hiragana : on est sur い
        vm.selectMode(CharacterSet.KATAKANA)
        vm.selectMode(CharacterSet.HIRAGANA)

        assertEquals("い", vm.uiState.value.current?.character)
    }

    @Test
    fun `la position restituee est bornee a la taille de la nouvelle liste`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()

        // On va en position 2 en hiragana, puis sur les kanji qui n'ont qu'un
        // seul élément : l'index doit être ramené dans les bornes.
        vm.showNext()
        vm.selectMode(CharacterSet.KANJI)
        vm.selectMode(CharacterSet.HIRAGANA)
        vm.selectMode(CharacterSet.KANJI)

        assertEquals(0, vm.uiState.value.index)
        assertEquals("一", vm.uiState.value.current?.character)
    }

    @Test
    fun `selectionner le mode courant ne fait rien`() = runTest {
        val vm = viewModel()
        advanceUntilIdle()
        vm.showNext()

        vm.selectMode(CharacterSet.HIRAGANA)

        assertEquals(1, vm.uiState.value.index)
    }

    @Test
    fun `un echec de chargement remonte un message et arrete le chargement`() = runTest {
        val vm = viewModel(FakeDataSource(failure = IOException("fichier illisible")))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("fichier illisible", state.errorMessage)
        assertNull(state.current)
    }

    @Test
    fun `la navigation est sans effet avant la fin du chargement`() = runTest {
        val vm = viewModel()

        vm.showNext()
        vm.selectMode(CharacterSet.KANJI)

        assertEquals(0, vm.uiState.value.index)
        assertEquals(CharacterSet.HIRAGANA, vm.uiState.value.mode)
    }

    @Test
    fun `la transcription romaji passe par les donnees chargees`() = runTest {
        val vm = viewModel()

        // Avant chargement : pas de table, donc chaîne vide plutôt qu'un plantage.
        assertEquals("", vm.toRomaji("あい"))

        advanceUntilIdle()

        assertEquals("ai", vm.toRomaji("あい"))
    }
}
