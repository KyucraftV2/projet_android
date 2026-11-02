package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.dto

import kotlinx.serialization.Serializable

/** Une entrée du fichier `kana_data.json`. */
@Serializable
data class KanaDto(
    val char: String,
    val romaji: String,
)

/**
 * Structure du fichier `kana_data.json`, imbriquée sur trois niveaux :
 *
 * ```
 * type de kana  ->  section               ->  groupe   ->  kana
 * "hiragana"        "hiragana_alphabet"       "line_K"     [か, き, く, け, こ]
 * ```
 *
 * On la décode avec des `Map` génériques plutôt qu'avec des classes dédiées,
 * car les noms de section changent d'un syllabaire à l'autre
 * (`hiragana_alphabet` contre `katakana_alphabet`) : des classes imposeraient
 * de dupliquer la même structure deux fois.
 */
typealias KanaFileDto = Map<String, Map<String, Map<String, List<KanaDto>>>>
