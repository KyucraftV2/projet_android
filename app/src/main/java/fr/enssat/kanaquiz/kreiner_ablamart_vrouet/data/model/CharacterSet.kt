package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model

/**
 * Un des trois jeux de caractères que l'application fait travailler.
 *
 * Sert à la fois à l'écran d'apprentissage (quel syllabaire on feuillette) et
 * au quiz (sur quoi porte la série de questions) : les deux écrans manipulent
 * la même notion, elle est donc déclarée une seule fois dans le modèle.
 *
 * @param label le nom affiché à l'utilisateur.
 * @param kanaType le syllabaire correspondant, `null` pour les kanji qui ne
 *   sont pas des kana. Permet de filtrer les données sans `when` supplémentaire.
 */
enum class CharacterSet(val label: String, val kanaType: KanaType?) {
    HIRAGANA("Hiragana", KanaType.HIRAGANA),
    KATAKANA("Katakana", KanaType.KATAKANA),
    KANJI("Kanji", null),
    ;

    /** Vrai si le jeu est un syllabaire, faux pour les kanji. */
    val isKana: Boolean get() = kanaType != null
}
