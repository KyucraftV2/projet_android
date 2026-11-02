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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.JapaneseCharacter
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.KanaType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.stripReadingMarkers
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * L'écran d'apprentissage : un caractère en grand, ses informations, et de quoi
 * naviguer dans la liste.
 *
 * Le composable ne reçoit que des données et des lambdas, aucun ViewModel : il
 * reste ainsi affichable dans les aperçus d'Android Studio et ne contient
 * aucune logique.
 *
 * @param toRomaji transcrit des kana en romaji ; passé en paramètre plutôt
 *   qu'appelé directement, pour que les aperçus puissent fournir un faux.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(
    state: CharacterUiState,
    onModeSelected: (StudyMode) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    toRomaji: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("KanaQuiz") }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            PrimaryTabRow(selectedTabIndex = state.mode.ordinal) {
                StudyMode.entries.forEach { mode ->
                    Tab(
                        selected = mode == state.mode,
                        onClick = { onModeSelected(mode) },
                        text = { Text(mode.label) },
                    )
                }
            }

            val content = Modifier.weight(1f).fillMaxWidth()
            when {
                state.isLoading -> CenteredMessage(content) { CircularProgressIndicator() }

                state.errorMessage != null -> CenteredMessage(content) {
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> {
                    val character = state.current
                    if (character == null) {
                        CenteredMessage(content) { Text("Aucun caractère à afficher.") }
                    } else {
                        CharacterDetails(
                            character = character,
                            toRomaji = toRomaji,
                            modifier = content,
                        )
                    }
                    NavigationBar(
                        positionLabel = state.positionLabel,
                        hasPrevious = state.hasPrevious,
                        hasNext = state.hasNext,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )
                }
            }
        }
    }
}

/** Centre un contenu court (chargement, erreur) dans l'espace disponible. */
@Composable
private fun CenteredMessage(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** Le caractère en grand, suivi des informations propres à son type. */
@Composable
private fun CharacterDetails(
    character: JapaneseCharacter,
    toRomaji: (String) -> String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        // Centré quand la fiche est courte (un kana), défilant quand elle est
        // longue (un kanji à nombreuses lectures).
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = character.character,
            // Taille fixe plutôt qu'un style du thème : le sujet demande le
            // caractère « en gros », et displayLarge plafonne à 57.sp.
            fontSize = 120.sp,
            lineHeight = 136.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // Le `when` est exhaustif sans `else` grâce à la sealed interface :
        // ajouter un type de caractère ferait échouer la compilation ici.
        when (character) {
            is Kana -> KanaInformation(character)
            is Kanji -> KanjiInformation(character, toRomaji)
        }
    }
}

/** Pour un kana : le romaji suffit, sa prononciation est invariable. */
@Composable
private fun KanaInformation(kana: Kana) {
    Text(
        text = kana.romaji,
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.height(12.dp))

    Text(
        text = "${kana.type.label} · ${kana.familyLabel()} (${kana.groupLabel()})",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** Pour un kanji : traduction, lectures, et niveau d'apprentissage. */
@Composable
private fun KanjiInformation(kanji: Kanji, toRomaji: (String) -> String) {
    kanji.primaryMeaning?.let { meaning ->
        Text(
            text = meaning,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }

    val otherMeanings = kanji.meanings.drop(1)
    if (otherMeanings.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = otherMeanings.joinToString(", "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }

    Spacer(Modifier.height(20.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    ReadingBlock("Lecture ON (origine chinoise)", kanji.readingsOn, toRomaji)
    ReadingBlock("Lecture KUN (origine japonaise)", kanji.readingsKun, toRomaji)

    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    InfoRow("Grade", kanji.gradeLabel)
    InfoRow("Traits", kanji.strokes.toString())
    kanji.jlptLevel?.let { InfoRow("JLPT", "N$it") }
    kanji.frequency?.let { InfoRow("Fréquence", "n°$it des kanji les plus courants") }
}

/**
 * Une famille de lectures, chacune suivie de sa transcription en romaji.
 *
 * La lecture garde ses marqueurs (`ひと.つ`), informatifs pour l'apprenant,
 * mais le romaji est calculé sur la lecture nettoyée.
 */
@Composable
private fun ReadingBlock(
    title: String,
    readings: List<String>,
    toRomaji: (String) -> String,
) {
    if (readings.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = readings.joinToString("   ·   ") { reading ->
                val kana = stripReadingMarkers(reading)
                if (kana.isEmpty()) reading else "$reading (${toRomaji(kana)})"
            },
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

/** Une ligne « libellé / valeur » du tableau d'informations. */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.6f),
        )
    }
}

/** Les boutons précédent / suivant, et la position dans la liste. */
@Composable
private fun NavigationBar(
    positionLabel: String,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPrevious, enabled = hasPrevious) {
                Text("‹ Précédent")
            }
            Text(
                text = positionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(onClick = onNext, enabled = hasNext) {
                Text("Suivant ›")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Aperçus
// ---------------------------------------------------------------------------

private val apercuKana = Kana(
    character = "か",
    romaji = "ka",
    type = KanaType.HIRAGANA,
    section = "hiragana_alphabet",
    group = "line_K",
)

private val apercuKanji = Kanji(
    character = "一",
    meanings = listOf("One", "One Radical (no.1)"),
    readingsOn = listOf("いち", "いつ"),
    readingsKun = listOf("ひと-", "ひと.つ"),
    grade = 1,
    strokes = 1,
    frequency = 2,
    jlptLevel = 5,
)

/** Faux convertisseur : les aperçus n'ont pas accès aux ressources. */
private val apercuRomaji: (String) -> String = { "romaji" }

@Preview(showBackground = true, name = "Kana")
@Composable
private fun CharacterScreenKanaPreview() {
    KanaQuizTheme {
        CharacterScreen(
            state = CharacterUiState(
                mode = StudyMode.HIRAGANA,
                characters = listOf(apercuKana),
                index = 0,
                isLoading = false,
            ),
            onModeSelected = {},
            onPrevious = {},
            onNext = {},
            toRomaji = apercuRomaji,
        )
    }
}

@Preview(showBackground = true, name = "Kanji")
@Composable
private fun CharacterScreenKanjiPreview() {
    KanaQuizTheme {
        CharacterScreen(
            state = CharacterUiState(
                mode = StudyMode.KANJI,
                characters = listOf(apercuKanji, apercuKanji),
                index = 0,
                isLoading = false,
            ),
            onModeSelected = {},
            onPrevious = {},
            onNext = {},
            toRomaji = apercuRomaji,
        )
    }
}

@Preview(showBackground = true, name = "Chargement")
@Composable
private fun CharacterScreenLoadingPreview() {
    KanaQuizTheme {
        CharacterScreen(
            state = CharacterUiState(isLoading = true),
            onModeSelected = {},
            onPrevious = {},
            onNext = {},
            toRomaji = apercuRomaji,
        )
    }
}
