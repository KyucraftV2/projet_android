package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** Où en est le moteur de synthèse vocale. */
sealed interface SpeechStatus {

    /** Le service se connecte ; les demandes de vocalisation seraient sans effet. */
    data object Initializing : SpeechStatus

    /** Le japonais est chargé, on peut prononcer. */
    data object Ready : SpeechStatus

    /** Impossible de prononcer, avec la raison à montrer à l'utilisateur. */
    data class Unavailable(val reason: String) : SpeechStatus
}

/**
 * Prononce du texte japonais via l'API Text-to-Speech d'Android.
 *
 * Le service se connecte de façon asynchrone : tant que [status] ne vaut pas
 * [SpeechStatus.Ready], les appels à [speak] sont ignorés plutôt que perdus
 * silencieusement, et l'interface peut désactiver son bouton.
 *
 * @param context n'importe quel contexte ; seul le contexte applicatif est
 *   conservé, pour ne pas retenir l'activité après sa destruction.
 */
class JapaneseSpeaker(context: Context) {

    private val _status = MutableStateFlow<SpeechStatus>(SpeechStatus.Initializing)
    val status: StateFlow<SpeechStatus> = _status.asStateFlow()

    private val alternator = SpeechRateAlternator()

    // Le callback d'initialisation est posté sur le thread principal, donc il
    // ne peut pas s'exécuter avant la fin de ce constructeur.
    private val engine = TextToSpeech(context.applicationContext, ::onEngineInit)

    private fun onEngineInit(initStatus: Int) {
        if (initStatus != TextToSpeech.SUCCESS) {
            Log.e(TAG, "Le service de synthèse vocale n'a pas démarré (status=$initStatus)")
            _status.value = SpeechStatus.Unavailable(
                "La synthèse vocale n'est pas disponible sur cet appareil."
            )
            return
        }

        _status.value = when (engine.setLanguage(Locale.JAPANESE)) {
            TextToSpeech.LANG_MISSING_DATA -> {
                Log.e(TAG, "Données vocales japonaises absentes")
                SpeechStatus.Unavailable(
                    "Les données vocales japonaises ne sont pas installées. " +
                        "Ajoutez-les dans Paramètres › Synthèse vocale."
                )
            }

            TextToSpeech.LANG_NOT_SUPPORTED -> {
                Log.e(TAG, "Japonais non pris en charge par le moteur")
                SpeechStatus.Unavailable(
                    "Le moteur de synthèse vocale de cet appareil ne gère pas le japonais."
                )
            }

            else -> {
                Log.d(TAG, "Synthèse vocale japonaise prête")
                SpeechStatus.Ready
            }
        }
    }

    /**
     * Prononce [text], en alternant vitesse normale et vitesse lente à chaque
     * écoute successive du même texte.
     *
     * [TextToSpeech.QUEUE_FLUSH] interrompt la lecture en cours : réappuyer
     * doit relancer immédiatement, pas attendre la fin de la précédente.
     */
    fun speak(text: String) {
        if (text.isBlank() || _status.value !is SpeechStatus.Ready) return

        val rate = alternator.nextRate(text)
        engine.setSpeechRate(rate)
        Log.d(TAG, "Vocalisation de \"$text\" a la vitesse $rate")

        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "Échec de la vocalisation de \"$text\"")
        }
    }

    /** Libère le service, à appeler quand l'écran disparaît. */
    fun shutdown() {
        engine.stop()
        engine.shutdown()
        alternator.reset()
    }

    private companion object {
        const val TAG = "KanaQuizTts"
    }
}

/**
 * Crée un [JapaneseSpeaker] lié à la durée de vie du composable.
 *
 * Le `DisposableEffect` garantit la fermeture du service quand l'écran quitte
 * la composition : sans cela la connexion au moteur de synthèse resterait
 * ouverte, ce que le sujet demande explicitement d'éviter.
 */
@Composable
fun rememberJapaneseSpeaker(): JapaneseSpeaker {
    val context = LocalContext.current
    val speaker = remember(context) { JapaneseSpeaker(context) }

    DisposableEffect(speaker) {
        onDispose { speaker.shutdown() }
    }

    return speaker
}
