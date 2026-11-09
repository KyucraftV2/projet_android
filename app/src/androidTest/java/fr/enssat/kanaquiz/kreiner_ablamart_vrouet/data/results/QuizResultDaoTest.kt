package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.SampleResults
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGradeCalculator
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Vérifie la base de données sur une base **en mémoire**, donc isolée : ces
 * tests n'écrivent jamais dans la base de l'application installée.
 *
 * Le remplissage de la vraie base, lui, est l'objet de [SampleResultsTest].
 */
@RunWith(AndroidJUnit4::class)
class QuizResultDaoTest {

    private lateinit var database: KanaQuizDatabase
    private lateinit var source: QuizResultSource

    @Before
    fun ouvreLaBase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KanaQuizDatabase::class.java).build()
        source = QuizResultRepository(database.quizResultDao())
    }

    @After
    fun fermeLaBase() {
        database.close()
    }

    @Test
    fun les_reglages_d_un_quiz_survivent_a_l_aller_retour_en_base() = runBlocking {
        val settings = QuizSettings(
            characterSet = CharacterSet.KANJI,
            questionType = QuizQuestionType.READING_TO_CHARACTER,
            grade = 6,
            hintsEnabled = true,
            speechEnabled = true,
        )
        val result = QuizResult(
            completedAt = 1_767_268_800_000L,
            settings = settings,
            questionCount = Quiz.QUESTION_COUNT,
            correctCount = 7,
        )

        val id = source.save(result)
        val relu = source.results().single()

        assertTrue("L'identifiant doit etre genere", id > 0)
        assertEquals(id, relu.id)
        assertEquals(settings, relu.settings)
        assertEquals(result.completedAt, relu.completedAt)
        assertEquals(7, relu.correctCount)
        assertEquals(ResultOrigin.USER, relu.origin)
    }

    @Test
    fun les_resultats_sont_relus_du_plus_ancien_au_plus_recent() = runBlocking {
        // Insérés en désordre : c'est la requête qui doit remettre en ordre.
        source.saveAll(SampleResults.journey.reversed())

        val dates = source.results().map { it.completedAt }

        assertEquals(SampleResults.journey.size, dates.size)
        assertEquals(dates.sorted(), dates)
    }

    @Test
    fun dix_sans_faute_de_grade_quatre_donnent_exactement_le_grade_quatre() = runBlocking {
        val parfait = List(UserGradeCalculator.WINDOW_SIZE) {
            QuizResult(
                completedAt = 1_767_268_800_000L + it * 86_400_000L,
                settings = QuizSettings(
                    characterSet = CharacterSet.KANJI,
                    questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                    grade = 4,
                ),
                questionCount = Quiz.QUESTION_COUNT,
                correctCount = Quiz.QUESTION_COUNT,
            )
        }
        source.saveAll(parfait)

        val grade = UserGradeCalculator.gradeOf(source.results())!!

        // Dix fois le même quiz réussi : la moyenne pondérée vaut le grade du
        // quiz, quels que soient les poids d'ancienneté.
        assertEquals(4.0, grade.value, 0.000001)
        assertEquals(4, grade.level)
    }

    @Test
    fun le_nettoyage_n_enleve_que_les_faux_resultats() = runBlocking {
        val vraiQuiz = QuizResult(
            completedAt = 1_800_000_000_000L,
            settings = QuizSettings(),
            questionCount = Quiz.QUESTION_COUNT,
            correctCount = 9,
            origin = ResultOrigin.USER,
        )
        source.saveAll(SampleResults.journey)
        source.save(vraiQuiz)

        val supprimes = source.deleteByOrigin(ResultOrigin.SAMPLE)

        val restants = source.results()
        assertEquals(SampleResults.journey.size, supprimes)
        assertEquals(1, restants.size)
        assertEquals(ResultOrigin.USER, restants.single().origin)
        assertEquals(9, restants.single().correctCount)
    }
}
