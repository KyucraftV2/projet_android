package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

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

    /** La progression du grade de l'utilisateur. */
    @Serializable
    data object Progress : Destination
}
