package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizGenerator
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestion
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSession
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Ce que l'écran de quiz affiche.
 *
 * @param session le quiz et les réponses déjà données, `null` pendant le
 *   tirage.
 * @param saveFailed vrai si le résultat n'a pas pu être enregistré ; le bilan
 *   le signale alors, plutôt que de faire croire à une progression comptée.
 */
data class QuizUiState(
    val settings: QuizSettings,
    val session: QuizSession? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val saveFailed: Boolean = false,
) {

    /** La question en cours, `null` si le quiz est terminé ou pas encore prêt. */
    val question: QuizQuestion? get() = session?.current

    /** Vrai quand les dix questions ont été répondues. */
    val isFinished: Boolean get() = session?.isFinished == true
}

/**
 * Pilote un quiz : tirage, enregistrement des réponses, rejouabilité.
 *
 * Le quiz est tiré une seule fois et conservé par le ViewModel : il survit
 * ainsi aux rotations d'écran, l'utilisateur ne perd pas sa série. En revanche,
 * si Android tue le processus en cours de quiz, un nouveau quiz est tiré au
 * retour — conserver les dix questions dans l'état sauvegardé obligerait à
 * sérialiser tout le modèle pour un cas où la série est de toute façon perdue.
 *
 * @param resultSource l'historique dans lequel le résultat est enregistré dès
 *   la dernière question répondue, puisque c'est lui qui nourrit le grade de
 *   l'utilisateur.
 * @param random injectable pour que les tests fixent le tirage.
 * @param now injectable pour que les tests maîtrisent la date du résultat.
 */
class QuizViewModel(
    private val settings: QuizSettings,
    private val dataSource: JapaneseDataSource,
    private val resultSource: QuizResultSource,
    private val random: Random = Random.Default,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuizUiState(settings = settings))
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    private var generator: QuizGenerator? = null

    init {
        draw()
    }

    /** Tire un quiz, en chargeant les données si c'est la première fois. */
    private fun draw() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val quizGenerator = generator
                    ?: QuizGenerator(dataSource.load()).also { generator = it }
                _uiState.value = _uiState.value.copy(
                    session = QuizSession(quizGenerator.generate(settings, random)),
                    isLoading = false,
                    saveFailed = false,
                )
            } catch (cancellation: CancellationException) {
                // L'annulation doit remonter pour que la coroutine se termine.
                throw cancellation
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Impossible de préparer le quiz.",
                )
            }
        }
    }

    /**
     * Enregistre la réponse choisie et passe à la question suivante.
     *
     * [QuizSession] ignore d'elle-même un second appui sur la même question :
     * un double clic ne doit pas faire sauter une question.
     */
    fun answer(choiceIndex: Int) {
        val session = _uiState.value.session ?: return
        val answered = session.answer(choiceIndex)
        _uiState.value = _uiState.value.copy(session = answered)

        // Le résultat est enregistré au moment exact où le quiz se termine, et
        // une seule fois : l'état précédent n'était pas encore terminé.
        if (answered.isFinished && !session.isFinished) {
            save(QuizResult.of(answered, completedAt = now()))
        }
    }

    /**
     * Enregistre le résultat, sans faire échouer le bilan en cas de problème.
     *
     * Un quiz joué reste un quiz joué : si l'écriture échoue, on le signale
     * dans l'état plutôt que de remplacer le score par un message d'erreur.
     */
    private fun save(result: QuizResult) {
        viewModelScope.launch {
            try {
                resultSource.save(result)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(saveFailed = true)
                Log.e(TAG, "Echec de l'enregistrement du resultat", error)
            }
        }
    }

    /** Rejoue un quiz neuf avec les mêmes réglages. */
    fun restart() = draw()

    companion object {

        private const val TAG = "KanaQuizQuiz"

        fun factory(
            settings: QuizSettings,
            dataSource: JapaneseDataSource,
            resultSource: QuizResultSource,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { QuizViewModel(settings, dataSource, resultSource) }
        }
    }
}
