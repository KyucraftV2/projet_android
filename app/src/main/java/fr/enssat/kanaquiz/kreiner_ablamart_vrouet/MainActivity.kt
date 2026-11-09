package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.KanaQuizApp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * L'unique activité de l'application.
 *
 * Elle ne fait que poser le thème et le graphe de navigation : tous les écrans
 * sont des composables, ce qui évite de multiplier les activités et laisse la
 * navigation entièrement du côté de Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanaQuizTheme {
                KanaQuizApp()
            }
        }
    }
}
