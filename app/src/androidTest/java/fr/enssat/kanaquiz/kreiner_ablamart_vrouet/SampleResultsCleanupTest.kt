package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Nettoie la base de l'appareil des résultats factices laissés par
 * [SampleResultsTest].
 *
 * Classe séparée pour pouvoir être lancée seule, une fois la progression
 * peuplée observée dans l'application. Le nettoyage ne touche qu'aux résultats
 * marqués [ResultOrigin.SAMPLE] : les quiz réellement joués sur l'appareil
 * doivent survivre, ce que le test vérifie explicitement.
 */
@RunWith(AndroidJUnit4::class)
class SampleResultsCleanupTest {

    private val source: QuizResultSource
        get() = ApplicationProvider.getApplicationContext<Context>().appContainer.quizResults

    @Test
    fun nettoyer_la_base_n_enleve_que_les_resultats_factices() = runBlocking {
        // On part d'une base peuplée, que le test ait déjà tourné ou non.
        SampleResults.ensurePresent(source)
        val avant = source.results()
        val vraisQuiz = avant.filter { it.origin == ResultOrigin.USER }
        val faux = avant.filter { it.origin == ResultOrigin.SAMPLE }
        assertTrue("La base doit contenir les faux resultats", faux.isNotEmpty())

        val supprimes = source.deleteByOrigin(ResultOrigin.SAMPLE)

        val apres = source.results()
        assertEquals(faux.size, supprimes)
        assertEquals(emptyList<Any>(), apres.filter { it.origin == ResultOrigin.SAMPLE })
        // La progression réelle de l'utilisateur est intacte, à l'identique.
        assertEquals(vraisQuiz, apres)
    }
}
