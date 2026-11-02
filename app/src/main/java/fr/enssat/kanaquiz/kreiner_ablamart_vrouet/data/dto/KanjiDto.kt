package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Une entrée du fichier `kanji_jouyou.json`.
 *
 * Le fichier est un objet JSON dont les clés sont les idéogrammes eux-mêmes :
 * le caractère n'apparaît donc pas dans cette classe, il vient de la clé.
 *
 * Tous les champs ont une valeur par défaut, et les champs `wk_*` (données
 * WaniKani, inutiles ici) sont simplement ignorés au décodage.
 */
@Serializable
data class KanjiDto(
    val strokes: Int = 0,
    val grade: Int? = null,
    /** Rang de fréquence dans la presse, `null` si non classé. */
    val freq: Int? = null,
    @SerialName("jlpt_new") val jlptNew: Int? = null,
    @SerialName("jlpt_old") val jlptOld: Int? = null,
    val meanings: List<String> = emptyList(),
    @SerialName("readings_on") val readingsOn: List<String> = emptyList(),
    @SerialName("readings_kun") val readingsKun: List<String> = emptyList(),
)
