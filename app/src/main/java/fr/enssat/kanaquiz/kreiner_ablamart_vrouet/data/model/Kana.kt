package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model

/** Les deux syllabaires japonais. */
enum class KanaType(val label: String) {
    /** Syllabaire de base, pour les mots japonais. */
    HIRAGANA("Hiragana"),

    /** Syllabaire pour les mots d'origine étrangère. */
    KATAKANA("Katakana"),
    ;

    companion object {
        /** Retrouve le type depuis la clé du fichier JSON (`"hiragana"` / `"katakana"`). */
        fun fromKey(key: String): KanaType? =
            entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}

/**
 * Un caractère phonétique (hiragana ou katakana).
 *
 * Un kana se prononce toujours de la même façon, donc le [romaji] suffit à
 * l'apprentissage : pas besoin de lectures multiples comme pour un kanji.
 *
 * @param character le kana, par exemple `か`.
 * @param romaji sa transcription phonétique latine, par exemple `ka`.
 * @param type hiragana ou katakana.
 * @param section la famille dans le syllabaire : alphabet de base,
 *   dakuten/handakuten (consonnes modifiées) ou combinaisons yōon.
 * @param group la ligne du tableau des kana, par exemple `line_K`.
 */
data class Kana(
    override val character: String,
    val romaji: String,
    val type: KanaType,
    val section: String,
    val group: String,
) : JapaneseCharacter {

    override val speechText: String get() = character
}
