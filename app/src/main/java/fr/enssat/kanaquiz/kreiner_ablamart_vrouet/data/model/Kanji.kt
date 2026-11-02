package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model

/**
 * Un idéogramme importé du chinois, avec les informations utiles à son
 * apprentissage.
 *
 * @param character l'idéogramme, par exemple `一`.
 * @param meanings ses traductions en anglais, la première étant la principale.
 * @param readingsOn lectures *on'yomi*, dérivées du chinois.
 * @param readingsKun lectures *kun'yomi*, d'origine japonaise.
 *   Les deux listes gardent les marqueurs du KANJIDIC (voir [stripReadingMarkers]) :
 *   ils sont informatifs pour l'apprenant et retirés uniquement pour la vocalisation.
 * @param grade niveau scolaire d'apprentissage, 1 à 6 pour le primaire puis 8
 *   pour le collège. `null` si le kanji n'est pas classé.
 * @param strokes nombre de traits à tracer.
 * @param frequency rang de fréquence dans la presse japonaise (1 = le plus
 *   fréquent), `null` si le kanji n'est pas classé.
 * @param jlptLevel niveau du JLPT (test officiel de japonais), 5 = débutant.
 */
data class Kanji(
    override val character: String,
    val meanings: List<String>,
    val readingsOn: List<String>,
    val readingsKun: List<String>,
    val grade: Int?,
    val strokes: Int,
    val frequency: Int?,
    val jlptLevel: Int?,
) : JapaneseCharacter {

    /**
     * La lecture retenue pour la prononciation, en hiragana et sans marqueur.
     *
     * On privilégie une lecture *on'yomi*, celle qui sert dans les mots
     * composés, donc la plus utile à mémoriser avec le caractère.
     *
     * Le repli sur une lecture *kun'yomi* n'est pas théorique : les six
     * *kokuji* du jeu jōyō (込, 枠, 畑, 栃, 峠, 匂), créés au Japon, n'ont aucune
     * lecture d'origine chinoise.
     */
    val primaryReading: String? =
        (readingsOn + readingsKun)
            .asSequence()
            .map(::stripReadingMarkers)
            .firstOrNull { it.isNotEmpty() }

    /** La traduction principale, celle affichée sous le caractère. */
    val primaryMeaning: String? = meanings.firstOrNull()

    override val speechText: String get() = primaryReading ?: character

    /** Libellé lisible du [grade], à afficher dans l'interface. */
    val gradeLabel: String
        get() = when (grade) {
            null -> "Non classé"
            in 1..6 -> "Primaire, année $grade"
            7, 8 -> "Collège"
            9, 10 -> "Noms propres"
            else -> "Grade $grade"
        }
}
