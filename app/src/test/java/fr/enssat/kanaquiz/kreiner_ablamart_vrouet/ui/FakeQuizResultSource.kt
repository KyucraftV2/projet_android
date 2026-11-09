package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * Historique des quiz en mémoire, pour tester les ViewModels sans base de
 * données ni émulateur.
 *
 * Les résultats sont portés par un `MutableStateFlow` : un enregistrement est
 * donc immédiatement visible par les écrans qui observent l'historique, comme
 * le fait Room.
 *
 * @param failing fait échouer les écritures, pour tester la gestion d'erreur.
 * @param failingReads fait échouer la lecture de l'historique.
 */
internal class FakeQuizResultSource(
    initial: List<QuizResult> = emptyList(),
    private val failing: Boolean = false,
    private val failingReads: Boolean = false,
) : QuizResultSource {

    private val state = MutableStateFlow(initial)

    /** Les résultats enregistrés, du plus ancien au plus récent. */
    val saved: List<QuizResult> get() = state.value

    override fun observeResults(): Flow<List<QuizResult>> =
        if (failingReads) flow { throw IOException("Base illisible") } else state

    override suspend fun results(): List<QuizResult> = state.value

    override suspend fun save(result: QuizResult): Long {
        if (failing) throw IOException("Base indisponible")
        state.value = state.value + result
        return state.value.size.toLong()
    }

    override suspend fun saveAll(results: List<QuizResult>) {
        results.forEach { save(it) }
    }

    override suspend fun deleteByOrigin(origin: ResultOrigin): Int {
        val before = state.value.size
        state.value = state.value.filterNot { it.origin == origin }
        return before - state.value.size
    }

    override suspend fun count(): Int = state.value.size
}
