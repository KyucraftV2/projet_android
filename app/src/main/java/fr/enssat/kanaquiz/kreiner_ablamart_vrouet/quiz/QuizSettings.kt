package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet

/**
 * Les paramètres choisis par l'utilisateur avant de lancer un quiz.
 *
 * @param characterSet le jeu de caractères interrogé.
 * @param questionType le sens de la question, qui doit être compatible avec
 *   [characterSet] : [withCharacterSet] garantit cette cohérence.
 * @param grade la difficulté demandée : les kanji sont tirés parmi ceux de grade
 *   inférieur ou égal à `g`, donc appris jusqu'à cette année scolaire (voir
 *   `QuizGenerator.matchesGrade`). Ignoré pour les kana, tous de grade 1.
 * @param hintsEnabled affiche une aide avec la question (la signification quand
 *   on demande la prononciation, par exemple).
 * @param speechEnabled autorise la vocalisation de l'énoncé.
 */
data class QuizSettings(
    val characterSet: CharacterSet = CharacterSet.HIRAGANA,
    val questionType: QuizQuestionType = QuizQuestionType.CHARACTER_TO_ROMAJI,
    val grade: Int = KANA_GRADE,
    val hintsEnabled: Boolean = false,
    val speechEnabled: Boolean = false,
) {

    /**
     * Le grade réellement appliqué au tirage.
     *
     * Les quiz de kana valent toujours 1 : il n'y a pas de niveau scolaire pour
     * les syllabaires, c'est le premier apprentissage.
     */
    val effectiveGrade: Int get() = if (characterSet.isKana) KANA_GRADE else grade

    /** Vrai si [questionType] a un sens pour [characterSet]. */
    val isConsistent: Boolean get() = questionType.supports(characterSet)

    /**
     * Change de jeu de caractères en réparant le type de question.
     *
     * Passer des hiragana aux kanji rend « Caractère → romaji » impossible : on
     * retombe alors sur le premier type valable plutôt que de laisser des
     * paramètres incohérents atteindre le générateur.
     */
    fun withCharacterSet(set: CharacterSet): QuizSettings {
        if (set == characterSet) return this
        val type = questionType.takeIf { it.supports(set) }
            ?: QuizQuestionType.forCharacterSet(set).first()
        return copy(characterSet = set, questionType = type)
    }

    companion object {

        /** Le grade des quiz de kana, et le plus facile des quiz de kanji. */
        const val KANA_GRADE = 1
    }
}
