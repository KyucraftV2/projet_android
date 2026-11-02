package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.stripReadingMarkers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vérifie que les deux fichiers JSON embarqués se parsent intégralement et que
 * le modèle en sort cohérent.
 */
class JapaneseDataParserTest {

    private val kanji by lazy {
        JapaneseDataParser.parseKanji(readRawResource("kanji_jouyou.json"))
    }
    private val kana by lazy {
        JapaneseDataParser.parseKana(readRawResource("kana_data.json"))
    }

    // ---------- Kanji ----------

    @Test
    fun `les 2136 kanji jouyou sont parses`() {
        assertEquals(2136, kanji.size)
    }

    @Test
    fun `chaque kanji a un caractere une traduction et une lecture`() {
        val sansCaractere = kanji.filter { it.character.isBlank() }
        val sansTraduction = kanji.filter { it.meanings.isEmpty() }
        val sansLecture = kanji.filter { it.primaryReading.isNullOrBlank() }

        assertEquals(emptyList<Any>(), sansCaractere)
        assertEquals(emptyList<Any>(), sansTraduction)
        assertEquals(emptyList<Any>(), sansLecture)
    }

    @Test
    fun `le premier kanji est correctement decode`() {
        val ichi = kanji.first { it.character == "一" }

        assertEquals(1, ichi.grade)
        assertEquals(1, ichi.strokes)
        assertEquals(2, ichi.frequency)
        assertEquals("One", ichi.primaryMeaning)
        assertEquals(listOf("いち", "いつ"), ichi.readingsOn)
        assertTrue("ひと-" in ichi.readingsKun)
        // La lecture on'yomi est privilégiée pour la prononciation.
        assertEquals("いち", ichi.primaryReading)
    }

    @Test
    fun `les kanji sont tries du plus simple au plus rare`() {
        val grades = kanji.mapNotNull { it.grade }

        assertEquals(grades.sorted(), grades)
        // Le premier kanji est de grade 1 et très fréquent.
        assertEquals(1, kanji.first().grade)
        assertTrue((kanji.first().frequency ?: Int.MAX_VALUE) < 100)
    }

    @Test
    fun `les grades restent dans la plage attendue`() {
        val grades = kanji.mapNotNull { it.grade }.distinct().sorted()

        // Le jeu jōyō couvre le primaire (1 à 6) et le collège (8) : pas de 7 ni de 9.
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 8), grades)
    }

    @Test
    fun `les champs optionnels absents du JSON donnent null et non une erreur`() {
        // 97 kanji n'ont pas de rang de fréquence, 172 pas de niveau JLPT actuel.
        assertTrue(kanji.any { it.frequency == null })
        assertTrue(kanji.any { it.jlptLevel == null })
        // Mais personne ne perd son grade ni ses traits.
        assertTrue(kanji.all { it.grade != null && it.strokes > 0 })
    }

    @Test
    fun `le libelle de grade est lisible`() {
        assertEquals("Primaire, année 1", kanji.first { it.grade == 1 }.gradeLabel)
        assertEquals("Collège", kanji.first { it.grade == 8 }.gradeLabel)
    }

    // ---------- Kana ----------

    @Test
    fun `les 208 kana sont parses dans les deux syllabaires`() {
        assertEquals(208, kana.size)
        assertEquals(104, kana.count { it.type == KanaType.HIRAGANA })
        assertEquals(104, kana.count { it.type == KanaType.KATAKANA })
    }

    @Test
    fun `l ordre du tableau des kana est conserve`() {
        val voyelles = kana.filter { it.type == KanaType.HIRAGANA }.take(5)

        assertEquals(listOf("あ", "い", "う", "え", "お"), voyelles.map { it.character })
        assertEquals(listOf("a", "i", "u", "e", "o"), voyelles.map { it.romaji })
    }

    @Test
    fun `les trois familles de kana sont presentes`() {
        val sections = kana.filter { it.type == KanaType.HIRAGANA }.map { it.section }.distinct()

        assertEquals(
            listOf("hiragana_alphabet", "dakuten_et_handakuten", "yoon_combinaisons"),
            sections,
        )
    }

    @Test
    fun `aucun kana n est hors des plages Unicode japonaises`() {
        // Garde-fou : le fichier fourni contenait un "я" cyrillique à la place
        // du hiragana "や". On vérifie que plus aucun intrus ne passe.
        val intrus = kana.filter { entry ->
            entry.character.any { it.code !in 0x3040..0x30FF }
        }

        assertEquals(emptyList<Any>(), intrus)
    }

    @Test
    fun `le ya hiragana est bien un ya japonais`() {
        val ya = kana.first { it.type == KanaType.HIRAGANA && it.romaji == "ya" }

        assertEquals("や", ya.character)
        assertEquals(0x3084, ya.character.single().code)
    }

    @Test
    fun `les combinaisons yoon font deux caracteres`() {
        val kya = kana.first { it.type == KanaType.HIRAGANA && it.romaji == "kya" }

        assertEquals("きゃ", kya.character)
        assertEquals("yoon_combinaisons", kya.section)
    }

    @Test
    fun `un type de kana inconnu est ignore sans faire echouer le parsing`() {
        val resultat = JapaneseDataParser.parseKana(
            """
            {
              "hiragana": { "alphabet": { "voyelles": [ { "char": "あ", "romaji": "a" } ] } },
              "klingon":  { "alphabet": { "voyelles": [ { "char": "x", "romaji": "x" } ] } }
            }
            """.trimIndent()
        )

        assertEquals(1, resultat.size)
        assertEquals("あ", resultat.single().character)
    }

    // ---------- Marqueurs de lecture ----------

    @Test
    fun `les marqueurs du KANJIDIC sont retires`() {
        assertEquals("ひと", stripReadingMarkers("ひと.つ"))
        assertEquals("ひと", stripReadingMarkers("ひと-"))
        assertEquals("ひと", stripReadingMarkers("!ひと"))
        assertEquals("ひと", stripReadingMarkers("^ひと"))
        assertEquals("あた", stripReadingMarkers("あた.り"))
        assertEquals("いち", stripReadingMarkers("いち"))
    }

    @Test
    fun `le texte vocalise d un kanji est sa lecture on'yomi`() {
        val ichi = kanji.first { it.character == "一" }

        assertEquals("いち", ichi.speechText)
        assertNotNull(ichi.primaryReading)
    }

    @Test
    fun `un kokuji sans lecture on se rabat sur sa lecture kun`() {
        // 峠 (col de montagne) est un caractère créé au Japon : il n'a aucune
        // lecture d'origine chinoise, le repli doit donc fonctionner.
        val touge = kanji.first { it.character == "峠" }

        assertEquals(emptyList<String>(), touge.readingsOn)
        assertEquals("とうげ", touge.speechText)
    }
}
