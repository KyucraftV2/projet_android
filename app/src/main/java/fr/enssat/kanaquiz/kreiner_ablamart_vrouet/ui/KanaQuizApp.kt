package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.appContainer
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts.rememberJapaneseSpeaker

/**
 * L'application entière : la navigation entre les écrans.
 *
 * Le moteur de synthèse vocale est créé ici, au-dessus du graphe de
 * navigation, et non dans chaque écran : l'apprentissage et le quiz s'en
 * servent tous les deux, et ouvrir une nouvelle connexion au service à chaque
 * changement d'écran serait du gaspillage. Il reste fermé automatiquement quand
 * l'application quitte la composition.
 */
@Composable
fun KanaQuizApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val speaker = rememberJapaneseSpeaker()
    val speechStatus by speaker.status.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Destination.Home,
        modifier = modifier,
    ) {
        composable<Destination.Home> {
            val container = LocalContext.current.appContainer
            val viewModel: ProgressViewModel = viewModel(
                factory = ProgressViewModel.factory(container.quizResults),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            HomeScreen(
                grade = state.grade,
                onLearn = { navController.navigate(Destination.Learn) },
                onStartQuiz = { navController.navigate(Destination.QuizSetup) },
                onOpenProgress = { navController.navigate(Destination.Progress) },
            )
        }

        composable<Destination.Learn> {
            // Le ViewModel est créé à l'entrée dans l'écran : le mégaoctet de
            // kanji n'est pas parsé tant que l'utilisateur reste sur l'accueil.
            // La source, elle, est celle du conteneur applicatif, donc partagée.
            val container = LocalContext.current.appContainer
            val viewModel: CharacterViewModel = viewModel(
                factory = CharacterViewModel.factory(container.japaneseData),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            CharacterScreen(
                state = state,
                speechStatus = speechStatus,
                onModeSelected = viewModel::selectMode,
                onBack = { navController.popBackStack() },
                // On prononce le kana lui-même, ou pour un kanji sa lecture en
                // hiragana : un idéogramme seul n'a pas de prononciation que le
                // moteur saurait deviner.
                onSpeak = { state.current?.let { speaker.speak(it.speechText) } },
                onPrevious = viewModel::showPrevious,
                onNext = viewModel::showNext,
                toRomaji = viewModel::toRomaji,
            )
        }

        composable<Destination.QuizSetup> {
            val container = LocalContext.current.appContainer
            val viewModel: QuizSetupViewModel = viewModel(
                factory = QuizSetupViewModel.factory(container.japaneseData),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            QuizSetupScreen(
                state = state,
                speechStatus = speechStatus,
                onCharacterSetSelected = viewModel::selectCharacterSet,
                onQuestionTypeSelected = viewModel::selectQuestionType,
                onGradeSelected = viewModel::selectGrade,
                onHintsEnabledChange = viewModel::setHintsEnabled,
                onSpeechEnabledChange = viewModel::setSpeechEnabled,
                onStart = { navController.navigate(Destination.Quiz.of(state.settings)) },
                onBack = { navController.popBackStack() },
            )
        }

        composable<Destination.Quiz> { entry ->
            // Les réglages sont relus depuis la route, donc rejoués tels quels
            // après une rotation ou un retour en arrière.
            val settings = entry.toRoute<Destination.Quiz>().toSettings()
            val container = LocalContext.current.appContainer
            val viewModel: QuizViewModel = viewModel(
                factory = QuizViewModel.factory(
                    settings = settings,
                    dataSource = container.japaneseData,
                    resultSource = container.quizResults,
                ),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            QuizScreen(
                state = state,
                speechStatus = speechStatus,
                onAnswer = viewModel::answer,
                onSpeak = speaker::speak,
                onRestart = viewModel::restart,
                onBack = { navController.popBackStack() },
            )
        }

        composable<Destination.Progress> {
            val container = LocalContext.current.appContainer
            val viewModel: ProgressViewModel = viewModel(
                factory = ProgressViewModel.factory(container.quizResults),
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            ProgressScreen(
                state = state,
                // Depuis l'écran vide, on enchaîne directement sur un quiz : le
                // grade ne peut pas exister avant le premier quiz terminé.
                onStartQuiz = { navController.navigate(Destination.QuizSetup) },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
