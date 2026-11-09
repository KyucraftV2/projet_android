package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin
import kotlinx.coroutines.flow.Flow

/** Les accès à la table des résultats de quiz. */
@Dao
interface QuizResultDao {

    /** Enregistre un résultat et renvoie son identifiant. */
    @Insert
    suspend fun insert(result: QuizResultEntity): Long

    /** Enregistre plusieurs résultats d'un coup, dans une seule transaction. */
    @Insert
    suspend fun insertAll(results: List<QuizResultEntity>): List<Long>

    /**
     * Observe tout l'historique, du plus ancien au plus récent.
     *
     * Un `Flow` plutôt qu'une liste : la page de progression se redessine
     * d'elle-même dès qu'un quiz est terminé, sans avoir à la rafraîchir à la
     * main. L'ordre chronologique est celui dont le calcul du grade a besoin.
     */
    @Query("SELECT * FROM quiz_results ORDER BY completed_at ASC, id ASC")
    fun observeAll(): Flow<List<QuizResultEntity>>

    /** L'historique complet, pour les tests et les calculs ponctuels. */
    @Query("SELECT * FROM quiz_results ORDER BY completed_at ASC, id ASC")
    suspend fun findAll(): List<QuizResultEntity>

    /** Le nombre de résultats enregistrés. */
    @Query("SELECT COUNT(*) FROM quiz_results")
    suspend fun count(): Int

    /**
     * Supprime les résultats d'une provenance donnée et renvoie leur nombre.
     *
     * C'est ce qui permet au test de nettoyage de n'effacer que les faux
     * résultats, en laissant intacte la progression d'un vrai utilisateur.
     */
    @Query("DELETE FROM quiz_results WHERE origin = :origin")
    suspend fun deleteByOrigin(origin: ResultOrigin): Int
}
