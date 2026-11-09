package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizResult
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizSettings
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin

/**
 * Un résultat de quiz tel qu'il est stocké en base.
 *
 * La table est volontairement « plate » : les réglages du quiz sont étalés en
 * colonnes plutôt que sérialisés en JSON, pour pouvoir plus tard filtrer la
 * progression par type de caractère ou par difficulté en SQL.
 *
 * L'entité est distincte du modèle [QuizResult] : le calcul du grade et les
 * tests unitaires travaillent sur le modèle, sans rien savoir de Room.
 */
@Entity(tableName = "quiz_results")
data class QuizResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Date de fin du quiz, en millisecondes depuis l'époque Unix. */
    @ColumnInfo(name = "completed_at")
    val completedAt: Long,

    @ColumnInfo(name = "character_set")
    val characterSet: CharacterSet,

    @ColumnInfo(name = "question_type")
    val questionType: QuizQuestionType,

    /** La difficulté du quiz : les kanji allaient jusqu'à ce grade. */
    val grade: Int,

    @ColumnInfo(name = "hints_enabled")
    val hintsEnabled: Boolean,

    @ColumnInfo(name = "speech_enabled")
    val speechEnabled: Boolean,

    @ColumnInfo(name = "question_count")
    val questionCount: Int,

    @ColumnInfo(name = "correct_count")
    val correctCount: Int,

    /** Distingue les faux résultats des tests de ceux de l'utilisateur. */
    val origin: ResultOrigin,
)

/** Convertit une ligne de la base vers le modèle. */
internal fun QuizResultEntity.toModel(): QuizResult = QuizResult(
    id = id,
    completedAt = completedAt,
    settings = QuizSettings(
        characterSet = characterSet,
        questionType = questionType,
        grade = grade,
        hintsEnabled = hintsEnabled,
        speechEnabled = speechEnabled,
    ),
    questionCount = questionCount,
    correctCount = correctCount,
    origin = origin,
)

/**
 * Convertit un résultat vers la ligne à écrire.
 *
 * L'identifiant `0` est laissé tel quel : Room le remplace par la valeur
 * auto-incrémentée.
 */
internal fun QuizResult.toEntity(): QuizResultEntity = QuizResultEntity(
    id = id,
    completedAt = completedAt,
    characterSet = settings.characterSet,
    questionType = settings.questionType,
    grade = settings.effectiveGrade,
    hintsEnabled = settings.hintsEnabled,
    speechEnabled = settings.speechEnabled,
    questionCount = questionCount,
    correctCount = correctCount,
    origin = origin,
)
