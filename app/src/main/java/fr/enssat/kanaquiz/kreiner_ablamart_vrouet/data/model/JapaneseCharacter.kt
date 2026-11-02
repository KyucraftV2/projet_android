package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model

/**
 * Un caractère japonais que l'on peut afficher et faire prononcer.
 *
 * Les deux implémentations ([Kana] et [Kanji]) n'exposent pas les mêmes
 * informations d'apprentissage, mais l'interface a besoin de les manipuler de
 * façon uniforme pour naviguer d'un caractère au suivant.
 */
sealed interface JapaneseCharacter {

    /** Le glyphe affiché en gros. */
    val character: String

    /**
     * Le texte envoyé au moteur Text-to-Speech.
     *
     * Pour un kana c'est le caractère lui-même ; pour un kanji c'est une de ses
     * lectures en hiragana, car un idéogramme isolé n'a pas de prononciation
     * unique que le moteur saurait deviner.
     */
    val speechText: String
}

/**
 * Retire les marqueurs de notation du KANJIDIC présents dans les lectures.
 *
 * Dans `kanji-jouyou.json` les lectures sont annotées :
 * - `ひと.つ` : le point sépare la lecture du kanji de l'okurigana qui le suit,
 *   seule la partie avant le point se prononce pour le caractère seul ;
 * - `ひと-` : le tiret marque un préfixe ou un suffixe ;
 * - `!ひと` : le point d'exclamation est un marqueur WaniKani.
 */
fun stripReadingMarkers(reading: String): String =
    reading.substringBefore('.').replace("-", "").replace("!", "").trim()
