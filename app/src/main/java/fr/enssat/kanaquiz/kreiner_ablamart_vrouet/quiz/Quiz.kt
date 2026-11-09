package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.JapaneseCharacter

/**
 * Une question : un énoncé, quatre réponses proposées, une seule correcte.
 *
 * @param character le caractère sur lequel porte la question, gardé pour
 *   pouvoir le vocaliser ou l'afficher une fois la réponse donnée.
 * @param type le sens de la question : il porte la consigne à afficher, et dira
 *   plus tard sur quoi l'utilisateur a été interrogé dans ses résultats.
 * @param prompt le texte de l'énoncé (le caractère, ou l'information demandée
 *   dans le sens inverse).
 * @param hint l'aide à afficher, `null` si l'utilisateur a désactivé les aides.
 * @param promptSpeech le texte à faire prononcer pendant la question, `null` si
 *   la vocalisation est désactivée ou si elle donnerait la réponse.
 * @param choices les réponses proposées, déjà mélangées, toutes différentes.
 * @param correctIndex la position de la bonne réponse dans [choices].
 */
data class QuizQuestion(
    val character: JapaneseCharacter,
    val type: QuizQuestionType,
    val prompt: String,
    val hint: String?,
    val promptSpeech: String?,
    val choices: List<String>,
    val correctIndex: Int,
) {

    /** Le texte de la bonne réponse. */
    val correctAnswer: String get() = choices[correctIndex]

    /** La question posée en français, affichée au-dessus de l'énoncé. */
    val consigne: String get() = type.consigne

    /** Vrai si [prompt] est le caractère japonais, à afficher en très grand. */
    val promptIsCharacter: Boolean get() = type.promptIsCharacter

    /** Vrai si les réponses proposées sont des caractères japonais. */
    val answerIsCharacter: Boolean get() = type.answerIsCharacter

    /** Vrai si la réponse à la position [choiceIndex] est la bonne. */
    fun isCorrect(choiceIndex: Int): Boolean = choiceIndex == correctIndex
}

/**
 * Une série de questions tirées au hasard pour un jeu de paramètres donné.
 *
 * Le quiz est immuable : la progression de l'utilisateur est portée par
 * [QuizSession], ce qui permet de rejouer exactement le même quiz.
 */
data class Quiz(
    val settings: QuizSettings,
    val questions: List<QuizQuestion>,
) {

    val size: Int get() = questions.size

    companion object {

        /** Un quiz compte dix questions, comme demandé dans le sujet. */
        const val QUESTION_COUNT = 10

        /** Quatre réponses proposées par question. */
        const val CHOICE_COUNT = 4
    }
}
