package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana

/**
 * Transcrit du texte en kana vers le romaji.
 *
 * Sert à afficher la prononciation d'un kanji en alphabet latin en plus des
 * hiragana, comme le demande le sujet. La table de correspondance n'est pas
 * écrite en dur : elle est construite à partir du `kana_data.json` déjà chargé,
 * qui contient précisément les 208 couples kana/romaji dont on a besoin.
 *
 * Limites assumées : les voyelles longues sont doublées plutôt que notées avec
 * un macron (`きょう` donne `kyou` et non `kyō`), ce qui est la convention
 * *wāpuro rōmaji*, celle que l'on tape au clavier.
 */
class RomajiConverter(kanaTable: List<Kana>) {

    private val table: Map<String, String> = buildMap {
        for (kana in kanaTable) {
            if (kana.character !in this) put(kana.character, kana.romaji)
        }
        // Seule combinaison apparaissant dans les lectures des kanji jōyō mais
        // absente du jeu de données fourni.
        if ("ぢゅ" !in this) put("ぢゅ", "ju")
    }

    /** La plus longue séquence reconnue dans la table (les yōon font 2 caractères). */
    private val maxTokenLength: Int = table.keys.maxOfOrNull { it.length } ?: 1

    /**
     * Transcrit [text] en romaji.
     *
     * Un caractère qui n'est pas un kana (ponctuation, lettre latine) est
     * recopié tel quel, pour ne jamais perdre d'information.
     */
    fun toRomaji(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val current = text[i]
            when {
                // Sokuon : っ redouble la consonne de la syllabe qui suit (がっこう -> gakkou).
                current == 'っ' || current == 'ッ' -> {
                    nextToken(text, i + 1)?.let { out.append(it.first()) }
                    i++
                }

                // Chōonpu : ー allonge la voyelle précédente (ラーメン -> raamen).
                current == 'ー' -> {
                    out.lastOrNull()?.let { out.append(it) }
                    i++
                }

                else -> {
                    val match = longestMatch(text, i)
                    if (match != null) {
                        out.append(match.second)
                        i += match.first
                    } else {
                        out.append(current)
                        i++
                    }
                }
            }
        }
        return out.toString()
    }

    /** Le romaji de la syllabe commençant à [index], sans le consommer. */
    private fun nextToken(text: String, index: Int): String? =
        longestMatch(text, index)?.second

    /**
     * Cherche à [index] la plus longue séquence présente dans la table.
     *
     * On teste du plus long au plus court pour que `きゃ` soit reconnu comme
     * une syllabe entière et non comme `き` suivi d'un `ゃ` orphelin.
     *
     * @return la longueur consommée et le romaji correspondant, ou `null`.
     */
    private fun longestMatch(text: String, index: Int): Pair<Int, String>? {
        if (index >= text.length) return null
        val maxLength = minOf(maxTokenLength, text.length - index)
        for (length in maxLength downTo 1) {
            val romaji = table[text.substring(index, index + length)]
            if (romaji != null) return length to romaji
        }
        return null
    }
}
