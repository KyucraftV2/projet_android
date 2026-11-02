package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.stripReadingMarkers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Vérifie la transcription kana vers romaji. */
class RomajiConverterTest {

    private val kana = JapaneseDataParser.parseKana(readRawResource("kana_data.json"))
    private val converter = RomajiConverter(kana)

    @Test
    fun `transcrit les syllabes simples`() {
        assertEquals("ichi", converter.toRomaji("いち"))
        assertEquals("nihon", converter.toRomaji("にほん"))
        assertEquals("hito", converter.toRomaji("ひと"))
    }

    @Test
    fun `transcrit les combinaisons yoon comme une seule syllabe`() {
        // きゃ doit sortir "kya" et non "ki" suivi d'un ゃ non reconnu.
        assertEquals("kya", converter.toRomaji("きゃ"))
        assertEquals("kyou", converter.toRomaji("きょう"))
        assertEquals("jouyou", converter.toRomaji("じょうよう"))
    }

    @Test
    fun `le sokuon redouble la consonne suivante`() {
        assertEquals("gakkou", converter.toRomaji("がっこう"))
        assertEquals("kitte", converter.toRomaji("きって"))
    }

    @Test
    fun `le chounpu allonge la voyelle precedente`() {
        assertEquals("raamen", converter.toRomaji("ラーメン"))
    }

    @Test
    fun `transcrit aussi les katakana`() {
        assertEquals("terebi", converter.toRomaji("テレビ"))
        assertEquals("amerika", converter.toRomaji("アメリカ"))
    }

    @Test
    fun `un caractere non kana est recopie tel quel`() {
        assertEquals("a-i", converter.toRomaji("あ-い"))
        assertEquals("a i", converter.toRomaji("あ い"))
    }

    @Test
    fun `une chaine vide reste vide`() {
        assertEquals("", converter.toRomaji(""))
    }

    @Test
    fun `toutes les lectures des kanji jouyou sont transcriptibles`() {
        val kanji = JapaneseDataParser.parseKanji(readRawResource("kanji_jouyou.json"))

        // Aucune lecture ne doit laisser passer un kana non transcrit : si le
        // résultat contient encore un caractère japonais, la table est incomplète.
        val nonTranscrites = kanji
            .flatMap { it.readingsOn + it.readingsKun }
            .map(::stripReadingMarkers)
            .filter { it.isNotEmpty() }
            .distinct()
            .filter { lecture ->
                converter.toRomaji(lecture).any { it.code in 0x3040..0x30FF }
            }

        assertEquals(emptyList<String>(), nonTranscrites)
    }

    @Test
    fun `la table couvre les 208 kana du fichier`() {
        val nonTranscrits = kana.filter { entry ->
            converter.toRomaji(entry.character) != entry.romaji
        }

        assertTrue("kana mal transcrits : $nonTranscrits", nonTranscrits.isEmpty())
    }
}
