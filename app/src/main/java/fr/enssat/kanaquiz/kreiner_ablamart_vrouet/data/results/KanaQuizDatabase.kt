package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.results

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.QuizQuestionType
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz.ResultOrigin

/**
 * Traduit les énumérations du modèle en texte stockable.
 *
 * On enregistre le *nom* de la constante et non son ordinal : insérer un
 * nouveau type de question au milieu de l'énumération ne doit pas réécrire le
 * sens des résultats déjà enregistrés.
 */
class QuizEnumConverters {

    @TypeConverter
    fun fromCharacterSet(value: CharacterSet): String = value.name

    @TypeConverter
    fun toCharacterSet(value: String): CharacterSet = CharacterSet.valueOf(value)

    @TypeConverter
    fun fromQuestionType(value: QuizQuestionType): String = value.name

    @TypeConverter
    fun toQuestionType(value: String): QuizQuestionType = QuizQuestionType.valueOf(value)

    @TypeConverter
    fun fromOrigin(value: ResultOrigin): String = value.name

    @TypeConverter
    fun toOrigin(value: String): ResultOrigin = ResultOrigin.valueOf(value)
}

/**
 * La base de données locale de l'application.
 *
 * Elle ne contient que les résultats de quiz : les caractères japonais, eux,
 * sont figés dans `res/raw` et n'ont rien à faire dans une base modifiable.
 *
 * `exportSchema = false` car il n'y a qu'une version : aucune migration à
 * tester pour l'instant. Il faudra l'activer le jour où le schéma évoluera.
 */
@Database(entities = [QuizResultEntity::class], version = 1, exportSchema = false)
@TypeConverters(QuizEnumConverters::class)
abstract class KanaQuizDatabase : RoomDatabase() {

    abstract fun quizResultDao(): QuizResultDao

    companion object {

        /** Le nom du fichier de base de données sur l'appareil. */
        const val NAME = "kanaquiz.db"
    }
}
