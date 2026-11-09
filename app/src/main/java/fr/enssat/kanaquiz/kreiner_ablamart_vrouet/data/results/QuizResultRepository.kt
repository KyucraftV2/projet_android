package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * L'historique des quiz joués.
 *
 * L'interface existe pour que les ViewModels et le calcul du grade se testent
 * sur une fausse implémentation en mémoire, sans base de données ni émulateur.
 */
interface QuizResultSource {

    /** Les résultats, du plus ancien au plus récent, observés en continu. */
    fun observeResults(): Flow<List<QuizResult>>

    /** Les résultats, du plus ancien au plus récent. */
    suspend fun results(): List<QuizResult>

    /** Enregistre un résultat et renvoie son identifiant. */
    suspend fun save(result: QuizResult): Long

    /** Enregistre plusieurs résultats, par exemple un historique de test. */
    suspend fun saveAll(results: List<QuizResult>)

    /** Supprime les résultats de cette provenance et renvoie leur nombre. */
    suspend fun deleteByOrigin(origin: ResultOrigin): Int

    /** Le nombre de résultats enregistrés. */
    suspend fun count(): Int
}

/**
 * L'historique stocké en base avec Room.
 *
 * Le dépôt ne fait que traduire entre le modèle et les lignes de la table : les
 * requêtes sont dans [QuizResultDao], et aucune annotation Room ne remonte
 * jusqu'aux écrans.
 */
class QuizResultRepository(private val dao: QuizResultDao) : QuizResultSource {

    override fun observeResults(): Flow<List<QuizResult>> =
        dao.observeAll().map { entities -> entities.map { it.toModel() } }

    override suspend fun results(): List<QuizResult> =
        dao.findAll().map { it.toModel() }

    override suspend fun save(result: QuizResult): Long =
        dao.insert(result.toEntity())

    override suspend fun saveAll(results: List<QuizResult>) {
        dao.insertAll(results.map { it.toEntity() })
    }

    override suspend fun deleteByOrigin(origin: ResultOrigin): Int =
        dao.deleteByOrigin(origin)

    override suspend fun count(): Int = dao.count()
}
