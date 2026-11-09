package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.AnswerState
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestion
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSession
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts.SpeechStatus
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * Le quiz : la liste des résultats, la question, les quatre réponses.
 *
 * Un appui sur une réponse l'enregistre et passe immédiatement à la question
 * suivante, comme le décrit le sujet ; le retour à l'utilisateur se lit dans la
 * bande de résultats, qui se colore au fur et à mesure.
 *
 * @param onSpeak reçoit le texte à prononcer ; l'écran décide *quand*
 *   prononcer, le moteur vocal est branché plus haut.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    state: QuizUiState,
    speechStatus: SpeechStatus,
    onAnswer: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = state.session
    val question = state.question

    // Le japonais se prononce dès que la question s'affiche, et de nouveau si
    // le moteur vocal n'était pas encore prêt au moment de l'affichage.
    LaunchedEffect(question, speechStatus) {
        val toSpeak = question?.promptSpeech
        if (toSpeak != null && speechStatus is SpeechStatus.Ready) onSpeak(toSpeak)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            session == null -> "Quiz"
                            session.isFinished -> "Quiz terminé"
                            else -> "Question ${session.positionLabel}"
                        }
                    )
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            when {
                state.isLoading -> Centered(Modifier.weight(1f)) { CircularProgressIndicator() }

                state.errorMessage != null -> Centered(Modifier.weight(1f)) {
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                session == null -> Centered(Modifier.weight(1f)) {
                    Text("Aucune question à poser.")
                }

                else -> {
                    ResultsStrip(session)

                    if (question == null) {
                        QuizSummary(
                            session = session,
                            saveFailed = state.saveFailed,
                            onRestart = onRestart,
                            onBack = onBack,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        QuestionCard(
                            question = question,
                            canSpeak = speechStatus is SpeechStatus.Ready,
                            onSpeak = onSpeak,
                            modifier = Modifier.weight(1f),
                        )
                        AnswerButtons(question = question, onAnswer = onAnswer)
                    }
                }
            }
        }
    }
}

/**
 * La bande des résultats : une case par question.
 *
 * La couleur seule ne suffit pas pour qui ne la distingue pas, la bande porte
 * donc un résumé lisible par les lecteurs d'écran.
 */
@Composable
private fun ResultsStrip(session: QuizSession) {
    val results = session.results
    val description = "${results.count { it == AnswerState.CORRECT }} correctes, " +
        "${results.count { it == AnswerState.INCORRECT }} incorrectes, " +
        "${results.count { it == AnswerState.UNANSWERED }} sans réponse"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        results.forEachIndexed { index, result ->
            val isCurrent = index == session.currentIndex
            Surface(
                modifier = Modifier.weight(1f).height(if (isCurrent) 14.dp else 8.dp),
                shape = RoundedCornerShape(4.dp),
                color = when (result) {
                    AnswerState.CORRECT -> MaterialTheme.colorScheme.primary
                    AnswerState.INCORRECT -> MaterialTheme.colorScheme.error
                    AnswerState.UNANSWERED -> MaterialTheme.colorScheme.surfaceVariant
                },
                content = {},
            )
        }
    }
}

/** L'énoncé, son aide éventuelle et le bouton d'écoute. */
@Composable
private fun QuestionCard(
    question: QuizQuestion,
    canSpeak: Boolean,
    onSpeak: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = question.consigne,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = question.prompt,
            // Un caractère japonais se détaille (traits, clés) : il a besoin de
            // place, là où un mot latin reste lisible petit.
            fontSize = if (question.promptIsCharacter) 110.sp else 40.sp,
            fontWeight = if (question.promptIsCharacter) FontWeight.Normal else FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = if (question.promptIsCharacter) 120.sp else 48.sp,
        )
        if (question.hint != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Aide : ${question.hint}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
        if (question.promptSpeech != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { onSpeak(question.promptSpeech) },
                enabled = canSpeak,
            ) {
                Text("Réécouter")
            }
        }
    }
}

/** Les quatre réponses proposées. */
@Composable
private fun AnswerButtons(question: QuizQuestion, onAnswer: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        question.choices.forEachIndexed { index, choice ->
            OutlinedButton(
                onClick = { onAnswer(index) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = choice,
                    fontSize = if (question.answerIsCharacter) 32.sp else 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

/** Le bilan du quiz, avec la correction question par question. */
@Composable
private fun QuizSummary(
    session: QuizSession,
    saveFailed: Boolean,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = session.scoreLabel, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "bonnes réponses",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (saveFailed) {
                    Text(
                        text = "Ce résultat n'a pas pu être enregistré : " +
                            "il ne comptera pas dans votre progression.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            session.quiz.questions.forEachIndexed { index, question ->
                ReviewRow(
                    question = question,
                    chosenIndex = session.answers.getOrNull(index),
                )
                HorizontalDivider()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Terminer")
            }
            Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Text("Rejouer")
            }
        }
    }
}

/** Une ligne de correction : l'énoncé, la bonne réponse, et l'erreur s'il y en a une. */
@Composable
private fun ReviewRow(question: QuizQuestion, chosenIndex: Int?) {
    val correct = chosenIndex != null && question.isCorrect(chosenIndex)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (correct) "✓" else "✗",
            color = if (correct) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            },
            fontWeight = FontWeight.Bold,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${question.prompt} → ${question.correctAnswer}",
                style = MaterialTheme.typography.bodyLarge,
            )
            if (!correct) {
                val chosen = chosenIndex?.let { question.choices.getOrNull(it) }
                Text(
                    text = if (chosen == null) "Sans réponse" else "Votre réponse : $chosen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Centered(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

// ---------- Aperçus ----------

private val apercuQuestion = QuizQuestion(
    character = Kana("か", "ka", KanaType.HIRAGANA, "hiragana_alphabet", "line_K"),
    type = QuizQuestionType.CHARACTER_TO_ROMAJI,
    prompt = "か",
    hint = "Alphabet de base · ligne K",
    promptSpeech = "か",
    choices = listOf("ki", "ka", "sa", "na"),
    correctIndex = 1,
)

private val apercuQuiz = Quiz(
    settings = QuizSettings(),
    questions = List(Quiz.QUESTION_COUNT) { apercuQuestion },
)

@Preview(showBackground = true, name = "Question en cours")
@Composable
private fun QuizScreenPreview() {
    KanaQuizTheme {
        QuizScreen(
            state = QuizUiState(
                settings = QuizSettings(hintsEnabled = true),
                session = QuizSession(apercuQuiz).answer(1).answer(0),
                isLoading = false,
            ),
            speechStatus = SpeechStatus.Ready,
            onAnswer = {},
            onSpeak = {},
            onRestart = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Quiz terminé")
@Composable
private fun QuizScreenFinishedPreview() {
    KanaQuizTheme {
        var session = QuizSession(apercuQuiz)
        repeat(Quiz.QUESTION_COUNT) { session = session.answer(if (it % 3 == 0) 0 else 1) }
        QuizScreen(
            state = QuizUiState(
                settings = QuizSettings(characterSet = CharacterSet.HIRAGANA),
                session = session,
                isLoading = false,
            ),
            speechStatus = SpeechStatus.Ready,
            onAnswer = {},
            onSpeak = {},
            onRestart = {},
            onBack = {},
        )
    }
}
