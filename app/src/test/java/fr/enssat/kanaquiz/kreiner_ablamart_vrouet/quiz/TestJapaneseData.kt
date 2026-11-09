package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataParser
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.RomajiConverter
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.readRawResource

/**
 * Les vraies données de l'application, chargées une seule fois pour tous les
 * tests du générateur.
 *
 * On teste le tirage sur les fichiers réellement embarqués plutôt que sur un
 * jeu de données jouet : c'est là que se trouvent les cas pénibles (kanji sans
 * lecture *on*, romaji partagés par deux kana, grades manquants).
 */
internal object TestJapaneseData {

    val real: JapaneseData by lazy {
        val kana = JapaneseDataParser.parseKana(readRawResource("kana_data.json"))
        val kanji = JapaneseDataParser.parseKanji(readRawResource("kanji_jouyou.json"))
        JapaneseData(kana = kana, kanji = kanji, romaji = RomajiConverter(kana))
    }
}
