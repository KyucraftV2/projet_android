package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.app.Application
import android.content.Context
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataRepository
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource

/**
 * Les dépendances partagées par toute l'application.
 *
 * Elles vivent aussi longtemps que le processus : les données d'apprentissage
 * ne sont donc parsées qu'une fois, même si l'utilisateur entre et sort dix
 * fois de l'écran d'apprentissage. Sans ce conteneur, chaque écran créerait son
 * propre dépôt et re-lirait le mégaoctet de `kanji_jouyou.json`.
 *
 * C'est aussi ici que viendra la base de données des résultats de quiz.
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    /** La source des caractères japonais, chargée paresseusement. */
    val japaneseData: JapaneseDataSource by lazy {
        JapaneseDataRepository(applicationContext)
    }
}

/**
 * L'application, qui porte le conteneur de dépendances.
 *
 * Déclarée dans le manifeste ; on évite ainsi un singleton global mutable et
 * les tests instrumentés peuvent atteindre le conteneur depuis le contexte.
 */
class KanaQuizApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Le conteneur de l'application à laquelle appartient ce contexte. */
val Context.appContainer: AppContainer
    get() = (applicationContext as KanaQuizApplication).container
