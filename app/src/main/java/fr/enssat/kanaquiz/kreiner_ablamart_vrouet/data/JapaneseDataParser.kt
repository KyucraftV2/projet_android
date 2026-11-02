package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.dto.KanaFileDto
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.dto.KanjiDto
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import kotlinx.serialization.json.Json

/**
 * Convertit le contenu brut des fichiers JSON en objets du modèle.
 *
 * Volontairement sans dépendance à Android : la lecture des fichiers est le
 * travail de [JapaneseDataRepository], ce qui permet de tester tout le parsing
 * en test unitaire JVM, sans émulateur.
 */
object JapaneseDataParser {

    private val json = Json {
        // Le fichier kanji contient des champs WaniKani (wk_*) dont on ne se sert pas.
        ignoreUnknownKeys = true
    }

    /**
     * Parse `kanji_jouyou.json`.
     *
     * Les kanji sont triés par grade puis par fréquence, c'est-à-dire du plus
     * simple au plus rare : la navigation « suivant / précédent » suit alors
     * l'ordre dans lequel on les apprend réellement.
     */
    fun parseKanji(raw: String): List<Kanji> {
        val entries: Map<String, KanjiDto> = json.decodeFromString(raw)
        return entries
            .map { (character, dto) ->
                Kanji(
                    character = character,
                    meanings = dto.meanings,
                    readingsOn = dto.readingsOn,
                    readingsKun = dto.readingsKun,
                    grade = dto.grade,
                    strokes = dto.strokes,
                    frequency = dto.freq,
                    // jlpt_new est la nomenclature actuelle (N5..N1), jlpt_old
                    // l'ancienne : on prend la première disponible.
                    jlptLevel = dto.jlptNew ?: dto.jlptOld,
                )
            }
            .sortedWith(
                compareBy(
                    { it.grade ?: Int.MAX_VALUE },
                    { it.frequency ?: Int.MAX_VALUE },
                    { it.character },
                )
            )
    }

    /**
     * Parse `kana_data.json`.
     *
     * L'ordre du fichier est conservé (あ, い, う, え, お, か...), c'est l'ordre
     * du tableau des kana donc celui dans lequel on les apprend. Une section
     * dont le type est inconnu est ignorée plutôt que de faire échouer tout le
     * chargement.
     */
    fun parseKana(raw: String): List<Kana> {
        val file: KanaFileDto = json.decodeFromString(raw)
        return buildList {
            for ((typeKey, sections) in file) {
                val type = KanaType.fromKey(typeKey) ?: continue
                for ((section, groups) in sections) {
                    for ((group, kanas) in groups) {
                        for (dto in kanas) {
                            add(
                                Kana(
                                    character = dto.char,
                                    romaji = dto.romaji,
                                    type = type,
                                    section = section,
                                    group = group,
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
