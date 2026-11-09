package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizGenerator
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tout ce dont l'écran de configuration a besoin.
 *
 * @param settings les réglages en cours de choix.
 * @param availableGrades les grades réellement présents dans le jeu de
 *   données. On ne les écrit pas en dur : le sujet annonce des grades de 1 à 9
 *   mais `kanji_jouyou.json` ne contient que 1 à 6 et 8, proposer un grade 7
 *   vide serait un piège.
 * @param poolSize le nombre de caractères que ces réglages rendent
 *   interrogeables, affiché pour que l'utilisateur mesure l'étendue du quiz.
 */
data class QuizSetupUiState(
    val settings: QuizSettings = QuizSettings(),
    val availableGrades: List<Int> = emptyList(),
    val poolSize: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {

    /** Les types de question proposables pour le jeu de caractères choisi. */
    val questionTypes: List<QuizQuestionType>
        get() = QuizQuestionType.forCharacterSet(settings.characterSet)

    /** Le choix du grade n'a de sens que pour les kanji. */
    val showGrade: Boolean get() = !settings.characterSet.isKana

    /** Vrai si le quiz peut être lancé. */
    val canStart: Boolean get() = !isLoading && errorMessage == null && poolSize > 0
}

/**
 * Pilote l'écran de configuration d'un quiz.
 *
 * Le quiz n'est pas tiré ici : il le sera par l'écran de quiz à partir des
 * réglages transportés par la route de navigation. Ce ViewModel ne sert donc
 * qu'à présenter des choix cohérents, et à dire combien de caractères ils
 * concernent.
 */
class QuizSetupViewModel(private val dataSource: JapaneseDataSource) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizSetupUiState())
    val uiState: StateFlow<QuizSetupUiState> = _uiState.asStateFlow()

    private var generator: QuizGenerator? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val data = dataSource.load()
                generator = QuizGenerator(data)
                _uiState.value = _uiState.value.copy(
                    availableGrades = data.kanji.mapNotNull { it.grade }.distinct().sorted(),
                    isLoading = false,
                    errorMessage = null,
                )
                refreshPoolSize()
            } catch (cancellation: CancellationException) {
                // L'annulation doit remonter pour que la coroutine se termine.
                throw cancellation
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Impossible de charger les données.",
                )
            }
        }
    }

    /**
     * Change de jeu de caractères.
     *
     * Le type de question est réparé au besoin : « Caractère → romaji » ne veut
     * rien dire pour des kanji.
     */
    fun selectCharacterSet(set: CharacterSet) =
        update { it.withCharacterSet(set) }

    fun selectQuestionType(type: QuizQuestionType) =
        update { it.copy(questionType = type) }

    fun selectGrade(grade: Int) = update { it.copy(grade = grade) }

    fun setHintsEnabled(enabled: Boolean) = update { it.copy(hintsEnabled = enabled) }

    fun setSpeechEnabled(enabled: Boolean) = update { it.copy(speechEnabled = enabled) }

    private fun update(transform: (QuizSettings) -> QuizSettings) {
        _uiState.value = _uiState.value.copy(settings = transform(_uiState.value.settings))
        refreshPoolSize()
    }

    /** Recalcule le vivier, qui dépend de tous les réglages à la fois. */
    private fun refreshPoolSize() {
        val generator = generator ?: return
        _uiState.value = _uiState.value.copy(
            poolSize = generator.poolSize(_uiState.value.settings),
        )
    }

    companion object {

        fun factory(dataSource: JapaneseDataSource): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { QuizSetupViewModel(dataSource) }
            }
    }
}
