package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vérifie que les paramètres d'un quiz restent cohérents. */
class QuizSettingsTest {

    @Test
    fun `les parametres par defaut sont coherents`() {
        assertTrue(QuizSettings().isConsistent)
    }

    @Test
    fun `chaque jeu de caracteres propose au moins un type de question`() {
        for (set in CharacterSet.entries) {
            val types = QuizQuestionType.forCharacterSet(set)

            assertTrue("Aucun type pour ${set.label}", types.isNotEmpty())
            assertTrue(types.all { it.supports(set) })
        }
    }

    @Test
    fun `les kana proposent deux types de question et les kanji quatre`() {
        assertEquals(2, QuizQuestionType.forCharacterSet(CharacterSet.HIRAGANA).size)
        assertEquals(2, QuizQuestionType.forCharacterSet(CharacterSet.KATAKANA).size)
        assertEquals(4, QuizQuestionType.forCharacterSet(CharacterSet.KANJI).size)
    }

    @Test
    fun `changer de jeu de caracteres repare un type de question impossible`() {
        val kana = QuizSettings(
            characterSet = CharacterSet.HIRAGANA,
            questionType = QuizQuestionType.CHARACTER_TO_ROMAJI,
        )

        val kanji = kana.withCharacterSet(CharacterSet.KANJI)

        assertEquals(CharacterSet.KANJI, kanji.characterSet)
        assertTrue(kanji.isConsistent)
        assertTrue(QuizQuestionType.CHARACTER_TO_ROMAJI != kanji.questionType)
    }

    @Test
    fun `changer de syllabaire conserve le type de question`() {
        val hiragana = QuizSettings(
            characterSet = CharacterSet.HIRAGANA,
            questionType = QuizQuestionType.ROMAJI_TO_CHARACTER,
        )

        val katakana = hiragana.withCharacterSet(CharacterSet.KATAKANA)

        assertEquals(QuizQuestionType.ROMAJI_TO_CHARACTER, katakana.questionType)
    }

    @Test
    fun `le grade choisi est ignore pour les kana`() {
        val kana = QuizSettings(characterSet = CharacterSet.KATAKANA, grade = 6)
        val kanji = QuizSettings(
            characterSet = CharacterSet.KANJI,
            questionType = QuizQuestionType.CHARACTER_TO_MEANING,
            grade = 6,
        )

        assertEquals(QuizSettings.KANA_GRADE, kana.effectiveGrade)
        assertEquals(6, kanji.effectiveGrade)
    }

    @Test
    fun `un type de question de kanji est signale comme incoherent sur des kana`() {
        val settings = QuizSettings(
            characterSet = CharacterSet.HIRAGANA,
            questionType = QuizQuestionType.CHARACTER_TO_MEANING,
        )

        assertFalse(settings.isConsistent)
    }
}
