package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import kotlinx.serialization.Serializable

/**
 * Les écrans de l'application, utilisés comme destinations de navigation.
 *
 * On passe par les routes *type-safe* de Navigation Compose : une destination
 * est un objet sérialisable plutôt qu'une chaîne `"home"`. Le compilateur
 * vérifie ainsi qu'on ne navigue pas vers un écran inexistant, et les
 * paramètres d'un écran (les réglages d'un quiz, à venir) voyagent en typé.
 */
sealed interface Destination {

    /** L'accueil : le grade de l'utilisateur et l'accès aux autres écrans. */
    @Serializable
    data object Home : Destination

    /** L'écran d'apprentissage du TP 1 : les caractères un par un. */
    @Serializable
    data object Learn : Destination

    /** Le choix des paramètres avant de lancer un quiz. */
    @Serializable
    data object QuizSetup : Destination

    /**
     * Un quiz en cours, dont les réglages voyagent dans la route.
     *
     * Les paramètres sont étalés en champs simples plutôt que de transporter un
     * [QuizSettings] : Navigation Compose sait sérialiser les énumérations et
     * les booléens sans type de navigation sur mesure. Avantage sur un état
     * partagé entre les deux écrans, les réglages survivent à la mort du
     * processus : Android peut tuer l'application pendant un quiz, l'utilisateur
     * la retrouve sur le même quiz.
     */
    @Serializable
    data class Quiz(
        val characterSet: CharacterSet,
        val questionType: QuizQuestionType,
        val grade: Int,
        val hintsEnabled: Boolean,
        val speechEnabled: Boolean,
    ) : Destination {

        /** Les réglages reconstruits depuis la route. */
        fun toSettings(): QuizSettings = QuizSettings(
            characterSet = characterSet,
            questionType = questionType,
            grade = grade,
            hintsEnabled = hintsEnabled,
            speechEnabled = speechEnabled,
        )

        companion object {

            /** La route menant au quiz décrit par [settings]. */
            fun of(settings: QuizSettings) = Quiz(
                characterSet = settings.characterSet,
                questionType = settings.questionType,
                grade = settings.effectiveGrade,
                hintsEnabled = settings.hintsEnabled,
                speechEnabled = settings.speechEnabled,
            )
        }
    }

    /** La progression du grade de l'utilisateur. */
    @Serializable
    data object Progress : Destination
}
