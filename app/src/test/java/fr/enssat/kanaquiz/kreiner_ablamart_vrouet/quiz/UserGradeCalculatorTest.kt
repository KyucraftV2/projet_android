package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Vérifie la fonction de calcul du grade de l'apprenant.
 *
 * Les résultats sont fabriqués à la main : on teste la formule, pas la base de
 * données.
 */
class UserGradeCalculatorTest {

    private var horloge = 1_700_000_000_000L

    /** Un résultat de quiz, daté automatiquement dans l'ordre d'appel. */
    private fun result(
        correct: Int,
        grade: Int = 1,
        characterSet: CharacterSet = CharacterSet.HIRAGANA,
        hints: Boolean = false,
        speech: Boolean = false,
        questions: Int = Quiz.QUESTION_COUNT,
    ): QuizResult {
        horloge += 60_000
        val questionType = if (characterSet.isKana) {
            QuizQuestionType.CHARACTER_TO_ROMAJI
        } else {
            QuizQuestionType.CHARACTER_TO_MEANING
        }
        return QuizResult(
            completedAt = horloge,
            settings = QuizSettings(
                characterSet = characterSet,
                questionType = questionType,
                grade = grade,
                hintsEnabled = hints,
                speechEnabled = speech,
            ),
            questionCount = questions,
            correctCount = correct,
        )
    }

    private fun kanji(correct: Int, grade: Int, hints: Boolean = false, speech: Boolean = false) =
        result(correct, grade, CharacterSet.KANJI, hints, speech)

    // ---------- Cas limites ----------

    @Test
    fun `sans resultat il n y a pas de grade`() {
        assertNull(UserGradeCalculator.gradeOf(emptyList()))
        assertEquals(emptyList<GradePoint>(), UserGradeCalculator.progressionOf(emptyList()))
    }

    @Test
    fun `un sans faute sur des hiragana donne le grade 1`() {
        val grade = UserGradeCalculator.gradeOf(listOf(result(correct = 10)))!!

        // Le sujet donne le grade 1 aux syllabaires : on ne peut pas aller
        // au-dela en ne travaillant que les kana.
        assertEquals(1.0, grade.value, 0.0001)
        assertEquals(1, grade.level)
        assertEquals("Grade 1", grade.label)
        assertEquals(1, grade.resultCount)
    }

    @Test
    fun `un sans faute sur des kanji de grade 5 donne le grade 5`() {
        val grade = UserGradeCalculator.gradeOf(listOf(kanji(correct = 10, grade = 5)))!!

        assertEquals(5.0, grade.value, 0.0001)
        assertEquals("Grade 5", grade.label)
    }

    @Test
    fun `repondre au hasard ne rapporte aucun grade`() {
        // Quatre reponses proposees : 25 % s'obtiennent sans rien savoir.
        val grade = UserGradeCalculator.gradeOf(listOf(kanji(correct = 2, grade = 8)))!!

        assertEquals(0.0, grade.value, 0.05)
        assertEquals(0, grade.level)
        assertEquals("Débutant", grade.label)
    }

    @Test
    fun `faire pire que le hasard ne donne pas de grade negatif`() {
        val grade = UserGradeCalculator.gradeOf(listOf(kanji(correct = 0, grade = 8)))!!

        assertEquals(0.0, grade.value, 0.0001)
    }

    @Test
    fun `le grade ne depasse jamais le maximum`() {
        val parfait = List(20) { kanji(correct = 10, grade = UserGrade.MAX_LEVEL) }

        val grade = UserGradeCalculator.gradeOf(parfait)!!

        assertEquals(UserGrade.MAX_LEVEL.toDouble(), grade.value, 0.0001)
    }

    // ---------- Difficulté et aides ----------

    @Test
    fun `huit bonnes reponses sur dix valent les sept neuviemes du grade`() {
        // q = (0,8 - 0,25) / 0,75 = 0,7333
        val grade = UserGradeCalculator.gradeOf(listOf(kanji(correct = 8, grade = 6)))!!

        assertEquals(6 * (0.8 - 0.25) / 0.75, grade.value, 0.0001)
    }

    @Test
    fun `un quiz reussi avec les aides vaut moins qu un quiz sans aide`() {
        val sansAide = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 4)))!!
        val avecAide = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 4, hints = true)))!!

        assertEquals(4.0, sansAide.value, 0.0001)
        assertEquals(4.0 * (1 - UserGradeCalculator.HINT_PENALTY), avecAide.value, 0.0001)
        assertTrue(avecAide.value < sansAide.value)
    }

    @Test
    fun `la vocalisation ne change rien au grade`() {
        // Le generateur se tait des qu'entendre l'enonce donnerait la reponse :
        // la vocalisation est un confort d'ecoute, pas une aide.
        val muet = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 4)))!!
        val parle = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 4, speech = true)))!!

        assertEquals(muet.value, parle.value, 0.0001)
    }

    @Test
    fun `le grade est une moyenne des quiz de la fenetre`() {
        // Deux quiz identiques de grades differents : le grade tombe entre les
        // deux, plus pres du plus recent.
        val grade = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 2), kanji(10, grade = 6)))!!

        assertTrue("${grade.value}", grade.value > 4.0)
        assertTrue("${grade.value}", grade.value < 6.0)
    }

    // ---------- Fenêtre glissante et ancienneté ----------

    @Test
    fun `les quiz hors de la fenetre sont ignores`() {
        val vieuxEchecs = List(UserGradeCalculator.WINDOW_SIZE) { kanji(correct = 0, grade = 8) }
        val reussitesRecentes = List(UserGradeCalculator.WINDOW_SIZE) { kanji(10, grade = 4) }

        val grade = UserGradeCalculator.gradeOf(vieuxEchecs + reussitesRecentes)!!

        assertEquals(4.0, grade.value, 0.0001)
        assertEquals(UserGradeCalculator.WINDOW_SIZE, grade.resultCount)
    }

    @Test
    fun `le quiz le plus recent compte plus que le plus ancien`() {
        val progres = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 2), kanji(10, grade = 8)))!!
        val rechute = UserGradeCalculator.gradeOf(listOf(kanji(10, grade = 8), kanji(10, grade = 2)))!!

        assertTrue("$progres vs $rechute", progres.value > rechute.value)
    }

    @Test
    fun `une mauvaise serie se rattrape en quelques quiz`() {
        val mauvaiseSerie = List(5) { kanji(correct = 3, grade = 5) }
        val creux = UserGradeCalculator.gradeOf(mauvaiseSerie)!!

        val remontee = UserGradeCalculator.gradeOf(mauvaiseSerie + List(3) { kanji(10, grade = 5) })!!

        assertTrue("$creux puis $remontee", remontee.value > creux.value + 1.5)
    }

    // ---------- Courbe de progression ----------

    @Test
    fun `la courbe a un point par quiz dans l ordre chronologique`() {
        val results = listOf(kanji(10, 1), kanji(10, 3), kanji(10, 5))

        val courbe = UserGradeCalculator.progressionOf(results)

        assertEquals(3, courbe.size)
        assertEquals(results.map { it.completedAt }, courbe.map { it.completedAt })
        assertEquals(courbe.map { it.completedAt }.sorted(), courbe.map { it.completedAt })
    }

    @Test
    fun `le dernier point de la courbe est le grade affiche`() {
        val results = List(15) { kanji(correct = 7 + it % 4, grade = 1 + it % 6) }

        val courbe = UserGradeCalculator.progressionOf(results)
        val grade = UserGradeCalculator.gradeOf(results)!!

        assertEquals(grade.value, courbe.last().value, 0.0001)
    }

    @Test
    fun `une progression reelle donne une courbe croissante`() {
        // L'utilisateur reussit des quiz de plus en plus difficiles.
        val results = (1..UserGrade.MAX_LEVEL).map { kanji(10, grade = it) }

        val valeurs = UserGradeCalculator.progressionOf(results).map { it.value }

        assertEquals(valeurs.sorted(), valeurs)
        assertTrue("${valeurs.first()} puis ${valeurs.last()}", valeurs.last() > valeurs.first() + 3)
    }

    // ---------- Affichage ----------

    @Test
    fun `le grade se decompose en niveau et avancement`() {
        val grade = UserGrade(value = 2.4, resultCount = 10)

        assertEquals(2, grade.level)
        assertEquals(0.4f, grade.progressToNextLevel, 0.0001f)
        assertEquals("Grade 2", grade.label)
    }

    @Test
    fun `le grade maximum correspond au grade le plus eleve des donnees`() {
        val maxDonnees = TestJapaneseData.real.kanji.mapNotNull { it.grade }.max()

        assertEquals(maxDonnees, UserGrade.MAX_LEVEL)
    }
}
