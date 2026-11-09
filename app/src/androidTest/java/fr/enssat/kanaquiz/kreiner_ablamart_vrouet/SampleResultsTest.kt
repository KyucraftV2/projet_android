package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGradeCalculator
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.ProgressScreen
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.ProgressTestTags
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.ProgressUiState
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.ProgressViewModel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Remplit la base de l'application de résultats factices et vérifie, comme le
 * demande le sujet, que le grade est correctement calculé et que le graphique
 * de progression s'affiche.
 *
 * Ces tests écrivent dans la **vraie** base de l'appareil, marquée
 * [ResultOrigin.SAMPLE] : on peut donc lancer l'application juste après pour
 * regarder la progression peuplée. [SampleResultsCleanupTest] fait le ménage.
 *
 * Chaque test s'assure lui-même que l'historique est présent : JUnit ne
 * garantit pas l'ordre des méthodes, et relancer un test ne doit pas empiler
 * deux fois le même parcours.
 */
@RunWith(AndroidJUnit4::class)
class SampleResultsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val source: QuizResultSource
        get() = ApplicationProvider.getApplicationContext<Context>().appContainer.quizResults

    /** L'état de la page de progression, calculé par le vrai ViewModel. */
    private fun progressState(): ProgressUiState = runBlocking {
        SampleResults.ensurePresent(source)
        val viewModel = ProgressViewModel(source)
        withTimeout(10_000) { viewModel.uiState.first { !it.isLoading } }
    }

    @Test
    fun generer_de_faux_resultats_remplit_la_base() = runBlocking {
        val enBase = SampleResults.ensurePresent(source)

        assertEquals(SampleResults.journey.size, enBase.size)
        assertTrue(enBase.all { it.origin == ResultOrigin.SAMPLE })
        assertTrue("Les identifiants doivent venir de la base", enBase.all { it.id > 0 })
        // Les résultats ressortent dans l'ordre chronologique.
        assertEquals(
            SampleResults.journey.map { it.completedAt },
            enBase.map { it.completedAt },
        )
    }

    @Test
    fun le_calcul_du_grade_est_correct() = runBlocking {
        val enBase = SampleResults.ensurePresent(source)

        val grade = UserGradeCalculator.gradeOf(enBase)!!

        // Valeur recalculée à la main, sans appeler le code de production.
        assertEquals(SampleResults.expectedGrade(enBase), grade.value, 0.000001)
        // Et valeur de référence figée, pour qu'un changement de formule se voie.
        assertEquals(SampleResults.EXPECTED_GRADE, grade.value, 0.000001)
        assertEquals(4, grade.level)
        assertEquals("Grade 4", grade.label)
        assertEquals(UserGradeCalculator.WINDOW_SIZE, grade.resultCount)
    }

    @Test
    fun le_graphique_de_progression_s_affiche() {
        val state = progressState()

        composeRule.setContent {
            KanaQuizTheme {
                ProgressScreen(state = state, onStartQuiz = {}, onBack = {})
            }
        }

        // Le grade lu en base est bien celui qui s'affiche en grand.
        composeRule.onNodeWithTag(ProgressTestTags.GRADE_VALUE)
            .assertTextEquals(state.grade!!.level.toString())

        // Le graphique est dessiné, avec un point par quiz enregistré.
        composeRule.onNodeWithTag(ProgressTestTags.CHART)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription(
            "Courbe du grade sur ${state.progression.size} quiz",
            substring = true,
        ).assertExists()
        composeRule.onNodeWithText("Évolution de votre grade").assertExists()

        // L'invitation du premier démarrage n'a plus lieu d'être.
        composeRule.onNodeWithTag(ProgressTestTags.EMPTY).assertDoesNotExist()
    }

    @Test
    fun sans_resultat_le_graphique_laisse_place_a_une_invitation() {
        composeRule.setContent {
            KanaQuizTheme {
                ProgressScreen(
                    state = ProgressUiState(isLoading = false),
                    onStartQuiz = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag(ProgressTestTags.EMPTY).assertIsDisplayed()
        composeRule.onNodeWithTag(ProgressTestTags.CHART).assertDoesNotExist()
    }
}
