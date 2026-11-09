package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.JapaneseCharacter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Tout ce dont l'écran a besoin pour se dessiner.
 *
 * On stocke la liste et un index plutôt que le seul caractère courant : c'est
 * ce qui permet de savoir s'il reste un précédent ou un suivant, et d'afficher
 * la position dans la liste.
 */
data class CharacterUiState(
    val mode: CharacterSet = CharacterSet.HIRAGANA,
    val characters: List<JapaneseCharacter> = emptyList(),
    val index: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
) {
    /** Le caractère affiché, ou `null` tant que les données ne sont pas chargées. */
    val current: JapaneseCharacter? get() = characters.getOrNull(index)

    val hasPrevious: Boolean get() = index > 0

    val hasNext: Boolean get() = index < characters.size - 1

    /** Position dans la liste, par exemple `12 / 104`. */
    val positionLabel: String
        get() = if (characters.isEmpty()) "" else "${index + 1} / ${characters.size}"
}

/**
 * Pilote l'écran d'apprentissage : chargement des données, navigation d'un
 * caractère à l'autre et changement de syllabaire.
 *
 * L'état survit aux rotations de l'écran, et surtout les données ne sont
 * parsées qu'une fois : recharger le mégaoctet de kanji à chaque rotation
 * serait visible à l'œil nu.
 */
class CharacterViewModel(private val dataSource: JapaneseDataSource) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterUiState())
    val uiState: StateFlow<CharacterUiState> = _uiState.asStateFlow()

    private var data: JapaneseData? = null

    /**
     * Position mémorisée pour chaque mode.
     *
     * Sans cela, revenir aux hiragana après un détour par les kanji
     * ramènerait à `あ` et ferait perdre sa progression à l'utilisateur.
     */
    private val savedIndices = mutableMapOf<CharacterSet, Int>()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val loaded = dataSource.load()
                data = loaded
                val state = _uiState.value
                _uiState.value = state.copy(
                    characters = loaded.charactersOf(state.mode),
                    index = 0,
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (cancellation: CancellationException) {
                // L'annulation n'est pas une erreur : elle doit remonter pour que
                // la coroutine se termine normalement.
                throw cancellation
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Impossible de charger les données.",
                )
            }
        }
    }

    /** Passe à un autre syllabaire, en reprenant là où l'utilisateur s'était arrêté. */
    fun selectMode(mode: CharacterSet) {
        val loaded = data ?: return
        val state = _uiState.value
        if (state.mode == mode) return

        savedIndices[state.mode] = state.index
        val characters = loaded.charactersOf(mode)
        _uiState.value = state.copy(
            mode = mode,
            characters = characters,
            index = (savedIndices[mode] ?: 0).coerceIn(0, maxOf(characters.size - 1, 0)),
        )
    }

    /** Affiche le caractère suivant, sans effet si on est déjà au dernier. */
    fun showNext() = moveBy(1)

    /** Affiche le caractère précédent, sans effet si on est déjà au premier. */
    fun showPrevious() = moveBy(-1)

    private fun moveBy(delta: Int) {
        val state = _uiState.value
        val target = state.index + delta
        if (target !in state.characters.indices) return
        _uiState.value = state.copy(index = target)
    }

    /**
     * Transcrit des kana en romaji pour l'affichage.
     *
     * Renvoie une chaîne vide avant la fin du chargement, le temps que la
     * table de correspondance soit disponible.
     */
    fun toRomaji(kana: String): String = data?.romaji?.toRomaji(kana).orEmpty()

    companion object {

        /**
         * Fabrique le ViewModel autour de la source de données partagée.
         *
         * La source vient du conteneur de l'application et non d'un `Context`
         * retenu ici : le ViewModel vit plus longtemps que l'activité, garder
         * cette dernière provoquerait une fuite mémoire à chaque rotation, et
         * les données restent parsées une seule fois pour tout le processus.
         */
        fun factory(dataSource: JapaneseDataSource): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { CharacterViewModel(dataSource) }
            }
    }
}
