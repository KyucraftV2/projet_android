package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * La page d'accueil : le grade de l'utilisateur et les deux activités de
 * l'application.
 *
 * Le sujet demande que le grade apparaisse ici et qu'un appui dessus mène à la
 * page de progression : c'est donc la carte principale de l'écran.
 *
 * @param grade le grade courant de l'utilisateur, `null` s'il n'a pas encore
 *   terminé de quiz.
 * @param onOpenProgress appelé quand on appuie sur le grade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    grade: Int?,
    onLearn: () -> Unit,
    onStartQuiz: () -> Unit,
    onOpenProgress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // État purement local : l'ouverture des crédits ne concerne personne d'autre.
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("KanaQuiz") },
                actions = {
                    TextButton(onClick = { showAbout = true }) { Text("À propos") }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "かなクイズ",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Apprenez les hiragana, les katakana et les kanji, puis " +
                    "mesurez vos progrès.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GradeCard(grade = grade, onClick = onOpenProgress)

            ActivityCard(
                title = "Lancer un quiz",
                description = "Dix questions, quatre réponses proposées. " +
                    "Vous choisissez le type de caractère, le sens de la " +
                    "question et la difficulté.",
                onClick = onStartQuiz,
            )

            ActivityCard(
                title = "Apprendre les caractères",
                description = "Parcourez les caractères un par un, avec leur " +
                    "prononciation, leurs lectures et leur signification.",
                onClick = onLearn,
            )
        }
    }
}

/**
 * Le grade de l'utilisateur, mis en avant et cliquable.
 *
 * Tant qu'aucun quiz n'a été terminé, on affiche une invitation plutôt qu'un
 * grade à zéro : un apprenant qui démarre n'a pas « échoué ».
 */
@Composable
private fun GradeCard(grade: Int?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = grade?.toString() ?: "–",
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Votre grade",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (grade == null) {
                        "Terminez un quiz pour obtenir votre premier grade."
                    } else {
                        "Appuyez pour voir votre progression."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/** Une activité proposée depuis l'accueil. */
@Composable
private fun ActivityCard(title: String, description: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, name = "Accueil sans résultat")
@Composable
private fun HomeScreenEmptyPreview() {
    KanaQuizTheme {
        HomeScreen(grade = null, onLearn = {}, onStartQuiz = {}, onOpenProgress = {})
    }
}

@Preview(showBackground = true, name = "Accueil avec grade")
@Composable
private fun HomeScreenGradedPreview() {
    KanaQuizTheme {
        HomeScreen(grade = 4, onLearn = {}, onStartQuiz = {}, onOpenProgress = {})
    }
}
