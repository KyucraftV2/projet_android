package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts

/**
 * Alterne vitesse normale et vitesse réduite d'une écoute à l'autre.
 *
 * C'est le comportement de Google Traduction, suggéré par le sujet : on écoute
 * une première fois à vitesse normale, et si on n'a pas bien saisi, un second
 * appui rejoue plus lentement.
 *
 * L'alternance est remise à zéro dès que le texte change : passer au caractère
 * suivant doit redonner une première écoute à vitesse normale, et non hériter
 * de la lenteur demandée pour le caractère précédent.
 *
 * Classe séparée du moteur de synthèse pour être testable sans Android.
 */
class SpeechRateAlternator(
    private val normalRate: Float = NORMAL_RATE,
    private val slowRate: Float = SLOW_RATE,
) {

    private var lastText: String? = null
    private var slowNext = false

    /** La vitesse à appliquer pour prononcer [text], et avance l'alternance. */
    fun nextRate(text: String): Float {
        slowNext = if (text == lastText) !slowNext else false
        lastText = text
        return if (slowNext) slowRate else normalRate
    }

    /** Oublie la dernière écoute : la prochaine repartira à vitesse normale. */
    fun reset() {
        lastText = null
        slowNext = false
    }

    companion object {
        const val NORMAL_RATE = 1.0f
        const val SLOW_RATE = 0.5f
    }
}
