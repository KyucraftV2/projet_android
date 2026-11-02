package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts.rememberJapaneseSpeaker
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.CharacterScreen
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.CharacterViewModel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanaQuizTheme {
                val viewModel: CharacterViewModel = viewModel(
                    factory = CharacterViewModel.factory(applicationContext),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Le moteur de synthèse est fermé automatiquement quand l'écran
                // quitte la composition (voir rememberJapaneseSpeaker).
                val speaker = rememberJapaneseSpeaker()
                val speechStatus by speaker.status.collectAsStateWithLifecycle()

                CharacterScreen(
                    state = state,
                    speechStatus = speechStatus,
                    onModeSelected = viewModel::selectMode,
                    // On prononce le kana lui-même, ou pour un kanji sa lecture
                    // en hiragana : un idéogramme seul n'a pas de prononciation
                    // que le moteur saurait deviner.
                    onSpeak = { state.current?.let { speaker.speak(it.speechText) } },
                    onPrevious = viewModel::showPrevious,
                    onNext = viewModel::showNext,
                    toRomaji = viewModel::toRomaji,
                )
            }
        }
    }
}
