package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.GradePoint
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGrade
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.UserGradeCalculator
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui.theme.KanaQuizTheme
import kotlin.math.ceil

/**
 * Les identifiants de test de l'écran de progression.
 *
 * Le sujet demande un test instrumenté vérifiant que le graphique s'affiche :
 * il a besoin de pouvoir le désigner sans dépendre d'un texte traduisible.
 */
object ProgressTestTags {
    const val CHART = "progression_graphique"
    const val GRADE_VALUE = "progression_grade"
    const val EMPTY = "progression_vide"
}

/**
 * La page de progression : le grade courant et sa courbe.
 *
 * @param onStartQuiz permet de lancer un quiz directement depuis l'écran vide,
 *   le grade ne pouvant pas exister avant le premier quiz terminé.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onStartQuiz: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ma progression") },
                navigationIcon = { BackButton(onBack) },
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
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                state.errorMessage != null -> Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                )

                !state.hasResults -> EmptyProgress(onStartQuiz)

                else -> {
                    GradeSummary(state)
                    ChartCard(state)
                    Statistics(state)
                }
            }
        }
    }
}

/** Ce qu'on affiche avant le premier quiz terminé. */
@Composable
private fun EmptyProgress(onStartQuiz: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(ProgressTestTags.EMPTY),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Votre grade apparaîtra ici dès votre premier quiz terminé.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Il est calculé sur vos " +
                "${UserGradeCalculator.WINDOW_SIZE} derniers quiz : la difficulté " +
                "choisie compte autant que vos bonnes réponses.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onStartQuiz) { Text("Lancer un quiz") }
    }
}

/** Le grade courant, en grand, avec l'avancement vers le suivant. */
@Composable
private fun GradeSummary(state: ProgressUiState) {
    val grade = state.grade ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = grade.level.toString(),
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag(ProgressTestTags.GRADE_VALUE),
                )
                Column {
                    Text(text = grade.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Grade exact : %.2f".format(grade.value),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { grade.progressToNextLevel },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (grade.level >= UserGrade.MAX_LEVEL) {
                    "Vous êtes au grade le plus élevé : les 2136 kanji jōyō sont à votre portée."
                } else {
                    "%d %% du chemin vers le grade %d.".format(
                        (grade.progressToNextLevel * 100).toInt(),
                        grade.level + 1,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/** La courbe, avec son titre et sa légende. */
@Composable
private fun ChartCard(state: ProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Évolution de votre grade", style = MaterialTheme.typography.titleMedium)
            GradeChart(
                points = state.progression,
                best = state.bestValue ?: 0.0,
                modifier = Modifier.fillMaxWidth().height(200.dp),
            )
            Text(
                text = "Un point par quiz terminé, du plus ancien au plus récent. " +
                    "Le trait pointillé marque votre record.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Dessine la courbe du grade.
 *
 * L'axe vertical s'arrête juste au-dessus du record plutôt qu'au grade 8 : la
 * courbe d'un débutant resterait sinon écrasée en bas de l'écran, où aucun
 * progrès ne se verrait. L'axe horizontal compte les quiz et non le temps, pour
 * qu'une semaine sans jouer n'aplatisse pas la progression.
 */
@Composable
private fun GradeChart(points: List<GradePoint>, best: Double, modifier: Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = MaterialTheme.colorScheme.primary
    val areaColor = lineColor.copy(alpha = 0.18f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val recordColor = MaterialTheme.colorScheme.tertiary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val topGrade = ceil(best + 0.5).toInt().coerceIn(2, UserGrade.MAX_LEVEL)
    val description = "Courbe du grade sur ${points.size} quiz, de " +
        "%.1f à %.1f, record %.1f".format(points.first().value, points.last().value, best)

    Canvas(
        modifier = modifier
            .testTag(ProgressTestTags.CHART)
            .semantics { contentDescription = description }
    ) {
        val gutter = 24.dp.toPx()
        val margin = 6.dp.toPx()
        val plotTop = margin
        val plotBottom = size.height - margin
        val plotLeft = gutter
        val plotRight = size.width

        fun x(index: Int): Float = when {
            points.size == 1 -> (plotLeft + plotRight) / 2f
            else -> plotLeft + (plotRight - plotLeft) * index / (points.size - 1)
        }

        fun y(value: Double): Float =
            plotBottom - (plotBottom - plotTop) * (value / topGrade).toFloat()

        // Une ligne par grade entier, avec son étiquette à gauche.
        for (grade in 0..topGrade) {
            val lineY = y(grade.toDouble())
            drawLine(
                color = gridColor,
                start = Offset(plotLeft, lineY),
                end = Offset(plotRight, lineY),
                strokeWidth = 1.dp.toPx(),
            )
            val layout = textMeasurer.measure(AnnotatedString(grade.toString()), labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(0f, lineY - layout.size.height / 2f),
            )
        }

        // L'aire sous la courbe donne du corps à la progression.
        val area = Path().apply {
            moveTo(x(0), plotBottom)
            points.forEachIndexed { index, point -> lineTo(x(index), y(point.value)) }
            lineTo(x(points.lastIndex), plotBottom)
            close()
        }
        drawPath(area, color = areaColor)

        if (points.size > 1) {
            val line = Path().apply {
                moveTo(x(0), y(points.first().value))
                points.forEachIndexed { index, point -> lineTo(x(index), y(point.value)) }
            }
            drawPath(line, color = lineColor, style = Stroke(width = 3.dp.toPx()))
        }

        // Le record, à battre.
        if (best > 0.0) {
            drawLine(
                color = recordColor,
                start = Offset(plotLeft, y(best)),
                end = Offset(plotRight, y(best)),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8.dp.toPx(), 6.dp.toPx())
                ),
            )
        }

        // Le dernier point, c'est-à-dire le grade affiché sur l'accueil.
        drawCircle(
            color = lineColor,
            radius = 5.dp.toPx(),
            center = Offset(x(points.lastIndex), y(points.last().value)),
        )
    }
}

/** Les compteurs d'assiduité, sous la courbe. */
@Composable
private fun Statistics(state: ProgressUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatRow("Quiz terminés", state.totalQuizCount.toString())
            StatRow(
                label = "Quiz comptés dans le grade",
                value = "${state.grade?.resultCount ?: 0} sur " +
                    "${UserGradeCalculator.WINDOW_SIZE}",
            )
            StatRow(
                label = "Meilleur grade atteint",
                value = state.bestValue?.let { "%.2f".format(it) } ?: "–",
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ---------- Aperçus ----------

/** Une progression plausible : des débuts hésitants puis des kanji de plus en plus durs. */
private val apercuCourbe = listOf(
    0.33, 0.48, 0.63, 0.75, 0.71, 0.75, 0.85, 1.13, 1.49, 1.28, 1.62, 2.00, 2.36, 2.94,
).mapIndexed { index, value -> GradePoint(completedAt = index * 86_400_000L, value = value) }

@Preview(showBackground = true, name = "Progression")
@Composable
private fun ProgressScreenPreview() {
    KanaQuizTheme {
        ProgressScreen(
            state = ProgressUiState(
                grade = UserGrade(value = 2.94, resultCount = 10),
                progression = apercuCourbe,
                totalQuizCount = apercuCourbe.size,
                bestValue = apercuCourbe.maxOf { it.value },
                isLoading = false,
            ),
            onStartQuiz = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "Progression vide")
@Composable
private fun ProgressScreenEmptyPreview() {
    KanaQuizTheme {
        ProgressScreen(
            state = ProgressUiState(isLoading = false),
            onStartQuiz = {},
            onBack = {},
        )
    }
}
