package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Vérifie l'alternance normal / lent demandée par le sujet, calquée sur le
 * comportement de Google Traduction.
 */
class SpeechRateAlternatorTest {

    private val normal = SpeechRateAlternator.NORMAL_RATE
    private val lent = SpeechRateAlternator.SLOW_RATE

    @Test
    fun `la premiere ecoute est a vitesse normale`() {
        val alternator = SpeechRateAlternator()

        assertEquals(normal, alternator.nextRate("あ"), 0f)
    }

    @Test
    fun `les ecoutes successives du meme texte alternent`() {
        val alternator = SpeechRateAlternator()

        val vitesses = List(5) { alternator.nextRate("あ") }

        assertEquals(listOf(normal, lent, normal, lent, normal), vitesses)
    }

    @Test
    fun `changer de texte repart a vitesse normale`() {
        val alternator = SpeechRateAlternator()

        alternator.nextRate("あ")               // normale
        assertEquals(lent, alternator.nextRate("あ"), 0f)

        // Passer au caractère suivant ne doit pas hériter de la lenteur.
        assertEquals(normal, alternator.nextRate("い"), 0f)
        assertEquals(lent, alternator.nextRate("い"), 0f)
    }

    @Test
    fun `revenir a un texte deja ecoute repart a vitesse normale`() {
        val alternator = SpeechRateAlternator()

        alternator.nextRate("あ")
        alternator.nextRate("い")

        assertEquals(normal, alternator.nextRate("あ"), 0f)
    }

    @Test
    fun `reset remet l alternance a zero`() {
        val alternator = SpeechRateAlternator()

        alternator.nextRate("あ")
        alternator.reset()

        assertEquals(normal, alternator.nextRate("あ"), 0f)
    }

    @Test
    fun `la vitesse lente est bien plus lente que la normale`() {
        assertEquals(1.0f, SpeechRateAlternator.NORMAL_RATE, 0f)
        assertEquals(0.5f, SpeechRateAlternator.SLOW_RATE, 0f)
    }
}
