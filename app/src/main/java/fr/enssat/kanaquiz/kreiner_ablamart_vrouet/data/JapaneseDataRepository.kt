package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

import android.content.Context
import androidx.annotation.RawRes
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.R
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Les données d'apprentissage, une fois chargées et prêtes à l'emploi. */
data class JapaneseData(
    val kana: List<Kana>,
    val kanji: List<Kanji>,
    val romaji: RomajiConverter,
)

/**
 * Source des données d'apprentissage.
 *
 * L'interface existe pour que le `ViewModel` puisse être testé avec un faux
 * jeu de données, sans `Context` Android ni émulateur.
 */
interface JapaneseDataSource {

    /** Renvoie les données, en les chargeant si nécessaire. */
    suspend fun load(): JapaneseData
}

/**
 * Charge les données depuis les ressources `res/raw` de l'application.
 *
 * Les fichiers sont embarqués plutôt que téléchargés : l'application
 * fonctionne hors ligne et il n'y a pas de cache réseau à gérer.
 *
 * Le chargement est `suspend` et renvoyé sur [Dispatchers.IO] : lire et parser
 * le million d'octets de `kanji_jouyou.json` prend trop de temps pour le faire
 * sur le thread principal, cela gèlerait l'affichage.
 */
class JapaneseDataRepository(private val context: Context) : JapaneseDataSource {

    private val mutex = Mutex()
    private var cached: JapaneseData? = null

    /**
     * Renvoie les données, en ne les parsant qu'une seule fois.
     *
     * Le mutex évite que deux appels concurrents (par exemple deux écrans qui
     * s'ouvrent en même temps) ne fassent le travail deux fois.
     */
    override suspend fun load(): JapaneseData = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) {
            val kana = JapaneseDataParser.parseKana(readRawText(R.raw.kana_data))
            val kanji = JapaneseDataParser.parseKanji(readRawText(R.raw.kanji_jouyou))
            JapaneseData(kana = kana, kanji = kanji, romaji = RomajiConverter(kana))
        }.also { cached = it }
    }

    /** Lit un fichier de `res/raw` en texte, comme indiqué dans le sujet du TP. */
    private fun readRawText(@RawRes rawResId: Int): String =
        context.resources.openRawResource(rawResId).bufferedReader().use { it.readText() }
}
