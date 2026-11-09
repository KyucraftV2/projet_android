package fr.enssat.kanaquiz.kreiner_ablamart_vrouet

import android.app.Application
import android.content.Context
import androidx.room.Room
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataRepository
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseDataSource
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.KanaQuizDatabase
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultRepository
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results.QuizResultSource

/**
 * Les dépendances partagées par toute l'application.
 *
 * Elles vivent aussi longtemps que le processus : les données d'apprentissage
 * ne sont donc parsées qu'une fois, même si l'utilisateur entre et sort dix
 * fois de l'écran d'apprentissage. Sans ce conteneur, chaque écran créerait son
 * propre dépôt et re-lirait le mégaoctet de `kanji_jouyou.json`.
 *
 * Le conteneur porte aussi la base de données des résultats de quiz.
 */
class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    /** La source des caractères japonais, chargée paresseusement. */
    val japaneseData: JapaneseDataSource by lazy {
        JapaneseDataRepository(applicationContext)
    }

    /**
     * La base de données locale.
     *
     * Ouverte paresseusement : lancer l'application sans jouer de quiz ne doit
     * pas coûter l'ouverture d'un fichier SQLite.
     */
    private val database: KanaQuizDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KanaQuizDatabase::class.java,
            KanaQuizDatabase.NAME,
        ).build()
    }

    /** L'historique des quiz joués. */
    val quizResults: QuizResultSource by lazy {
        QuizResultRepository(database.quizResultDao())
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
