package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.gradeLabel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.Quiz
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.tts.SpeechStatus
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * Le choix des paramètres d'un quiz : caractères, sens de la question,
 * difficulté, aide et vocalisation.
 *
 * Comme les autres écrans, le composable ne reçoit que des données et des
 * lambdas, pour rester affichable dans les aperçus.
 *
 * @param speechStatus état du moteur de synthèse vocale : inutile de proposer
 *   la vocalisation si l'appareil ne sait pas prononcer le japonais.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizSetupScreen(
    state: QuizSetupUiState,
    speechStatus: SpeechStatus,
    onCharacterSetSelected: (CharacterSet) -> Unit,
    onQuestionTypeSelected: (QuizQuestionType) -> Unit,
    onGradeSelected: (Int) -> Unit,
    onHintsEnabledChange: (Boolean) -> Unit,
    onSpeechEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Lancer un quiz") },
                navigationIcon = { BackButton(onBack) },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onStart,
                        enabled = state.canStart,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Commencer le quiz")
                    }
                    Text(
                        text = quizSummary(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Centered(innerPadding) { CircularProgressIndicator() }

            state.errorMessage != null -> Centered(innerPadding) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Section("Caractères") {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        CharacterSet.entries.forEachIndexed { index, set ->
                            SegmentedButton(
                                selected = set == state.settings.characterSet,
                                onClick = { onCharacterSetSelected(set) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = CharacterSet.entries.size,
                                ),
                                label = { Text(set.label) },
                            )
                        }
                    }
                }

                Section("Question") {
                    Column {
                        state.questionTypes.forEach { type ->
                            ChoiceRow(
                                label = type.label,
                                selected = type == state.settings.questionType,
                                onSelect = { onQuestionTypeSelected(type) },
                            )
                        }
                    }
                }

                if (state.showGrade) {
                    Section("Difficulté") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.availableGrades.forEach { grade ->
                                    FilterChip(
                                        selected = grade == state.settings.grade,
                                        onClick = { onGradeSelected(grade) },
                                        label = { Text("Grade $grade") },
                                    )
                                }
                            }
                            Text(
                                text = "Kanji appris jusqu'à ce niveau : " +
                                    gradeLabel(state.settings.grade).lowercase() + ".",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Section("Options") {
                    Column {
                        SwitchRow(
                            label = "Afficher une aide",
                            description = "La signification quand on demande la " +
                                "prononciation, la place dans le tableau pour un kana.",
                            checked = state.settings.hintsEnabled,
                            onCheckedChange = onHintsEnabledChange,
                        )
                        SwitchRow(
                            label = "Prononcer la question",
                            description = speechDescription(speechStatus),
                            checked = state.settings.speechEnabled,
                            enabled = speechStatus !is SpeechStatus.Unavailable,
                            onCheckedChange = onSpeechEnabledChange,
                        )
                    }
                }
            }
        }
    }
}

/** Résumé affiché sous le bouton : ce que le quiz contiendra réellement. */
private fun quizSummary(state: QuizSetupUiState): String = when {
    state.isLoading -> "Chargement des caractères…"
    state.errorMessage != null -> "Quiz indisponible."
    state.poolSize == 0 -> "Aucun caractère ne correspond à ces réglages."
    else -> {
        val questions = minOf(Quiz.QUESTION_COUNT, state.poolSize)
        "$questions questions tirées parmi ${state.poolSize} caractères."
    }
}

/** Explique l'état du moteur vocal, qui conditionne l'option. */
private fun speechDescription(status: SpeechStatus): String = when (status) {
    is SpeechStatus.Unavailable -> status.reason
    SpeechStatus.Initializing -> "Démarrage de la synthèse vocale…"
    SpeechStatus.Ready ->
        "L'énoncé est prononcé, sauf quand l'entendre donnerait la réponse."
}

/** Un groupe de réglages, avec son titre. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        content()
        HorizontalDivider()
    }
}

/**
 * Une ligne de choix exclusif.
 *
 * Le `Modifier.selectable` porte le clic et le rôle d'accessibilité sur toute
 * la ligne : viser le petit bouton radio serait pénible.
 */
@Composable
private fun ChoiceRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Une option à bascule, avec son explication. */
@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked && enabled, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun Centered(
    padding: androidx.compose.foundation.layout.PaddingValues,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

@Preview(showBackground = true, name = "Réglages kana")
@Composable
private fun QuizSetupKanaPreview() {
    KanaQuizTheme {
        QuizSetupScreen(
            state = QuizSetupUiState(
                settings = QuizSettings(hintsEnabled = true),
                availableGrades = listOf(1, 2, 3, 4, 5, 6, 8),
                poolSize = 104,
                isLoading = false,
            ),
            speechStatus = SpeechStatus.Ready,
            onCharacterSetSelected = {},
            onQuestionTypeSelected = {},
            onGradeSelected = {},
            onHintsEnabledChange = {},
            onSpeechEnabledChange = {},
            onStart = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Réglages kanji sans synthèse vocale")
@Composable
private fun QuizSetupKanjiPreview() {
    KanaQuizTheme {
        QuizSetupScreen(
            state = QuizSetupUiState(
                settings = QuizSettings(
                    characterSet = CharacterSet.KANJI,
                    questionType = QuizQuestionType.CHARACTER_TO_READING,
                    grade = 3,
                ),
                availableGrades = listOf(1, 2, 3, 4, 5, 6, 8),
                poolSize = 440,
                isLoading = false,
            ),
            speechStatus = SpeechStatus.Unavailable(
                "Les données vocales japonaises ne sont pas installées."
            ),
            onCharacterSetSelected = {},
            onQuestionTypeSelected = {},
            onGradeSelected = {},
            onHintsEnabledChange = {},
            onSpeechEnabledChange = {},
            onStart = {},
            onBack = {},
        )
    }
}
