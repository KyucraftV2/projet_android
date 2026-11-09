package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.GradePoint
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGrade
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGradeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * La progression de l'utilisateur, telle qu'elle s'affiche.
 *
 * @param grade le grade courant, `null` si aucun quiz n'a encore été terminé.
 * @param progression le grade après chacun des quiz joués, pour le graphique.
 * @param totalQuizCount le nombre total de quiz terminés, au-delà de la
 *   fenêtre glissante : c'est un compteur d'assiduité, pas un niveau.
 * @param bestValue le meilleur grade jamais atteint, affiché comme un record à
 *   battre.
 */
data class ProgressUiState(
    val grade: UserGrade? = null,
    val progression: List<GradePoint> = emptyList(),
    val totalQuizCount: Int = 0,
    val bestValue: Double? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {

    val hasResults: Boolean get() = progression.isNotEmpty()
}

/**
 * Observe l'historique des quiz et en déduit le grade et sa courbe.
 *
 * Le même ViewModel sert à l'accueil, qui n'affiche que le grade, et à la page
 * de progression : les deux écrans lisent la même source et resteraient de
 * toute façon d'accord, autant ne calculer le grade qu'à un seul endroit.
 *
 * L'état vient d'un `Flow` observé sur la base : terminer un quiz met à jour
 * l'accueil sans rien rafraîchir à la main.
 */
class ProgressViewModel(resultSource: QuizResultSource) : ViewModel() {

    val uiState: StateFlow<ProgressUiState> = resultSource.observeResults()
        .map { results ->
            val progression = UserGradeCalculator.progressionOf(results)
            ProgressUiState(
                grade = UserGradeCalculator.gradeOf(results),
                progression = progression,
                totalQuizCount = results.size,
                bestValue = progression.maxOfOrNull { it.value },
                isLoading = false,
            )
        }
        .catch { error ->
            emit(
                ProgressUiState(
                    isLoading = false,
                    errorMessage = error.message ?: "Impossible de lire vos résultats.",
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            // On garde l'état quelques secondes après la disparition de l'écran :
            // une rotation ne doit pas relancer une requête sur la base.
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProgressUiState(),
        )

    companion object {

        fun factory(resultSource: QuizResultSource): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ProgressViewModel(resultSource) }
            }
    }
}
