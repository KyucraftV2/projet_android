package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model

/**
 * Rend lisibles les clés techniques du fichier `kana_data.json`.
 *
 * Le fichier nomme ses sections `hiragana_alphabet` ou `yoon_combinaisons` :
 * tel quel à l'écran, ce n'est pas présentable.
 */
internal fun Kana.familyLabel(): String = when {
    section.endsWith("_alphabet") -> "Alphabet de base"
    section == "dakuten_et_handakuten" -> "Dakuten / handakuten"
    section == "yoon_combinaisons" -> "Combinaison yōon"
    else -> section
}

/** Rend lisible la ligne du tableau des kana (`line_K` donne « ligne K »). */
internal fun Kana.groupLabel(): String = when {
    group == "voyelles" -> "voyelles"
    group.startsWith("line_") ->
        "ligne " + group.removePrefix("line_").replace("_and_", " / ")
    group.startsWith("base_") -> "base " + group.removePrefix("base_")
    group.startsWith("dakuten_") -> "dakuten " + group.removePrefix("dakuten_")
    group.startsWith("handakuten_") -> "handakuten " + group.removePrefix("handakuten_")
    else -> group
}
