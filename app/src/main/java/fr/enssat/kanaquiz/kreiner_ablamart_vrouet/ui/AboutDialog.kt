package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme

/**
 * Les crédits de l'application.
 *
 * Le sujet impose de citer le projet GitHub dont proviennent les données ;
 * on remonte aussi aux sources d'origine (KANJIDIC et consorts) et on indique
 * la licence, puisque le jeu de données est redistribué dans l'APK.
 */
@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("À propos de KanaQuiz") },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text(
                    text = "Application d'apprentissage des caractères japonais : " +
                        "hiragana, katakana et kanji.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                SectionTitle("Auteurs")
                Text(
                    text = "Projet réalisé à l'ENSSAT par kreiner, ablamart et vrouet.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                SectionTitle("Données des kanji")
                Text(
                    text = "Fichier kanji-jouyou.json (2 136 kanji jōyō), issu du projet " +
                        "kanji-data de David Gouveia, distribué sous licence MIT.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                LinkText("github.com/davidluzgouveia/kanji-data")

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Ce jeu de données compile lui-même KANJIDIC (Electronic " +
                        "Dictionary Research and Development Group), les ressources JLPT " +
                        "de Jonathan Waller et le contenu de WaniKani.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                LinkText("edrdg.org/wiki/index.php/KANJIDIC_Project")

                SectionTitle("Données des kana")
                Text(
                    text = "Fichier kana_data.json (208 hiragana et katakana), fourni avec " +
                        "le sujet du TP.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                SectionTitle("Vocalisation")
                Text(
                    text = "Synthèse vocale assurée par l'API Text-to-Speech d'Android.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(14.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(2.dp))
}

/** Un lien ouvert dans le navigateur du téléphone. */
@Composable
private fun LinkText(url: String) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = url,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable { uriHandler.openUri("https://$url") },
    )
}

@Preview
@Composable
private fun AboutDialogPreview() {
    KanaQuizTheme {
        AboutDialog(onDismiss = {})
    }
}
