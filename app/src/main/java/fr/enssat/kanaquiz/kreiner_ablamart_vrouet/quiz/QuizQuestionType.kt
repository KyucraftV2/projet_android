package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet

/**
 * Ce qu'une question donne et ce qu'elle demande.
 *
 * Chaque type ne vaut que pour certains jeux de caractères : demander la
 * signification d'un hiragana n'a pas de sens, un kana n'en a pas.
 *
 * @param label le libellé affiché dans l'écran de configuration.
 * @param characterSets les jeux de caractères auxquels le type s'applique.
 * @param consigne la question posée en français, affichée au-dessus de l'énoncé.
 * @param promptIsCharacter vrai si l'énoncé est le caractère japonais et la
 *   réponse une information (romaji, signification, lecture) ; faux dans le
 *   sens inverse. C'est ce qui décide quel côté s'affiche en gros à l'écran.
 */
enum class QuizQuestionType(
    val label: String,
    val consigne: String,
    val characterSets: Set<CharacterSet>,
    val promptIsCharacter: Boolean,
) {
    /** `か` → `ka` */
    CHARACTER_TO_ROMAJI(
        label = "Caractère → romaji",
        consigne = "Quel est le romaji de ce caractère ?",
        characterSets = setOf(CharacterSet.HIRAGANA, CharacterSet.KATAKANA),
        promptIsCharacter = true,
    ),

    /** `ka` → `か` */
    ROMAJI_TO_CHARACTER(
        label = "Romaji → caractère",
        consigne = "Quel caractère se lit ainsi ?",
        characterSets = setOf(CharacterSet.HIRAGANA, CharacterSet.KATAKANA),
        promptIsCharacter = false,
    ),

    /** `一` → `One` */
    CHARACTER_TO_MEANING(
        label = "Kanji → signification",
        consigne = "Que signifie ce kanji ?",
        characterSets = setOf(CharacterSet.KANJI),
        promptIsCharacter = true,
    ),

    /** `One` → `一` */
    MEANING_TO_CHARACTER(
        label = "Signification → kanji",
        consigne = "Quel kanji a cette signification ?",
        characterSets = setOf(CharacterSet.KANJI),
        promptIsCharacter = false,
    ),

    /** `一` → `いち` */
    CHARACTER_TO_READING(
        label = "Kanji → prononciation",
        consigne = "Comment se prononce ce kanji ?",
        characterSets = setOf(CharacterSet.KANJI),
        promptIsCharacter = true,
    ),

    /** `いち` → `一` */
    READING_TO_CHARACTER(
        label = "Prononciation → kanji",
        consigne = "Quel kanji se prononce ainsi ?",
        characterSets = setOf(CharacterSet.KANJI),
        promptIsCharacter = false,
    ),
    ;

    /** Vrai si ce sont les réponses proposées qui sont des caractères japonais. */
    val answerIsCharacter: Boolean get() = !promptIsCharacter

    /** Vrai si le type de question a un sens pour [set]. */
    fun supports(set: CharacterSet): Boolean = set in characterSets

    companion object {

        /** Les types de question proposables pour [set], dans l'ordre de l'énoncé. */
        fun forCharacterSet(set: CharacterSet): List<QuizQuestionType> =
            entries.filter { it.supports(set) }
    }
}
