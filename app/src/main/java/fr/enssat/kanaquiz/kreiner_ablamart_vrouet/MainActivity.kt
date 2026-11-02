package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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

                CharacterScreen(
                    state = state,
                    onModeSelected = viewModel::selectMode,
                    onPrevious = viewModel::showPrevious,
                    onNext = viewModel::showNext,
                    toRomaji = viewModel::toRomaji,
                )
            }
        }
    }
}
