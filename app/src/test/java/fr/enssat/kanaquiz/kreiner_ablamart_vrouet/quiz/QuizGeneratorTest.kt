package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.CharacterSet
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.stripReadingMarkers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Vérifie le tirage des quiz sur les données réellement embarquées.
 *
 * La graine du générateur aléatoire est fixée dans chaque test : un quiz tiré
 * au hasard qui échoue une fois sur cent serait intestable.
 */
class QuizGeneratorTest {

    private val data = TestJapaneseData.real
    private val generator = QuizGenerator(data)

    private val kanjiByCharacter: Map<String, Kanji> = data.kanji.associateBy { it.character }

    /** Tous les couples (jeu de caractères, type de question) valides. */
    private val allSettings: List<QuizSettings> = CharacterSet.entries.flatMap { set ->
        QuizQuestionType.forCharacterSet(set).map { type ->
            QuizSettings(characterSet = set, questionType = type)
        }
    }

    private fun generate(settings: QuizSettings, seed: Long = 42L) =
        generator.generate(settings, Random(seed))

    // ---------- Forme du quiz ----------

    @Test
    fun `chaque quiz compte dix questions a quatre reponses`() {
        for (settings in allSettings) {
            val quiz = generate(settings)
            val description = "${settings.characterSet.label} / ${settings.questionType.label}"

            assertEquals(description, Quiz.QUESTION_COUNT, quiz.size)
            for (question in quiz.questions) {
                assertEquals(description, Quiz.CHOICE_COUNT, question.choices.size)
                assertEquals(
                    "$description : réponses en double dans ${question.choices}",
                    question.choices.size,
                    question.choices.distinct().size,
                )
                assertTrue(description, question.correctIndex in question.choices.indices)
                assertTrue(description, question.choices.none { it.isBlank() })
                assertTrue(description, question.prompt.isNotBlank())
            }
        }
    }

    @Test
    fun `les dix questions portent sur dix caracteres differents`() {
        for (settings in allSettings) {
            val quiz = generate(settings)
            val characters = quiz.questions.map { it.character.character }

            assertEquals(settings.questionType.label, 10, characters.distinct().size)
        }
    }

    @Test
    fun `l enonce est le caractere ou l information selon le sens de la question`() {
        val direct = generate(QuizSettings(questionType = QuizQuestionType.CHARACTER_TO_ROMAJI))
        val reverse = generate(QuizSettings(questionType = QuizQuestionType.ROMAJI_TO_CHARACTER))

        assertTrue(direct.questions.all { it.prompt == it.character.character })
        assertTrue(direct.questions.all { it.promptIsCharacter })
        assertTrue(reverse.questions.all { it.prompt != it.character.character })
        assertTrue(reverse.questions.all { it.answerIsCharacter })
    }

    // ---------- Justesse des réponses ----------

    @Test
    fun `la bonne reponse d un quiz de hiragana est le romaji du caractere`() {
        val quiz = generate(QuizSettings(questionType = QuizQuestionType.CHARACTER_TO_ROMAJI))

        for (question in quiz.questions) {
            val kana = question.character as Kana

            assertEquals(CharacterSet.HIRAGANA.kanaType, kana.type)
            assertEquals(kana.romaji, question.correctAnswer)
        }
    }

    @Test
    fun `la bonne reponse d un quiz de katakana est le caractere attendu`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KATAKANA,
                questionType = QuizQuestionType.ROMAJI_TO_CHARACTER,
            )
        )

        for (question in quiz.questions) {
            val kana = question.character as Kana

            assertEquals(CharacterSet.KATAKANA.kanaType, kana.type)
            assertEquals(kana.romaji, question.prompt)
            assertEquals(kana.character, question.correctAnswer)
        }
    }

    @Test
    fun `la bonne reponse d un quiz de kanji est sa signification ou sa lecture`() {
        val meanings = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
            )
        )
        val readings = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_READING,
            )
        )

        for (question in meanings.questions) {
            val kanji = question.character as Kanji
            assertEquals(kanji.primaryMeaning, question.correctAnswer)
        }
        for (question in readings.questions) {
            val kanji = question.character as Kanji
            assertEquals(kanji.primaryReading, question.correctAnswer)
            // La lecture proposée est nettoyée des marqueurs du KANJIDIC.
            assertTrue(question.correctAnswer.none { it in ".-!^" })
        }
    }

    @Test
    fun `aucun distracteur ne constitue une seconde bonne reponse`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.MEANING_TO_CHARACTER,
            )
        )

        for (question in quiz.questions) {
            val wrong = question.choices.filterIndexed { index, _ -> index != question.correctIndex }
            val meanings = wrong.map { kanjiByCharacter.getValue(it).primaryMeaning }

            assertTrue(
                "« ${question.prompt} » a plusieurs réponses justes : $meanings",
                meanings.none { it == question.prompt },
            )
        }
    }

    @Test
    fun `aucun distracteur n est une autre signification du kanji interroge`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
            )
        )

        for (question in quiz.questions) {
            val kanji = question.character as Kanji
            val wrong = question.choices.filterIndexed { index, _ -> index != question.correctIndex }

            assertTrue(
                "${kanji.character} accepte aussi $wrong (${kanji.meanings})",
                wrong.none { it in kanji.meanings },
            )
        }
    }

    @Test
    fun `aucun distracteur ne se lit comme la prononciation demandee`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.READING_TO_CHARACTER,
            )
        )

        for (question in quiz.questions) {
            val wrong = question.choices.filterIndexed { index, _ -> index != question.correctIndex }

            for (character in wrong) {
                val kanji = kanjiByCharacter.getValue(character)
                val readings = (kanji.readingsOn + kanji.readingsKun).map(::stripReadingMarkers)

                assertTrue(
                    "$character se lit aussi « ${question.prompt} »",
                    question.prompt !in readings,
                )
            }
        }
    }

    // ---------- Difficulté ----------

    @Test
    fun `un quiz de grade donne ne tire que des kanji appris jusque la`() {
        for (grade in 1..6) {
            val quiz = generate(
                QuizSettings(
                    characterSet = CharacterSet.KANJI,
                    questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                    grade = grade,
                ),
                seed = grade.toLong(),
            )

            for (question in quiz.questions) {
                val kanji = question.character as Kanji
                assertTrue("Grade ${kanji.grade} tiré pour un quiz de grade $grade", kanji.grade!! <= grade)
            }
        }
    }

    @Test
    fun `les distracteurs respectent aussi le grade demande`() {
        val grade = 5
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.MEANING_TO_CHARACTER,
                grade = grade,
            )
        )

        val grades = quiz.questions
            .flatMap { it.choices }
            .map { kanjiByCharacter.getValue(it).grade }

        assertTrue("Grades proposés : ${grades.distinct()}", grades.all { it != null && it <= grade })
    }

    @Test
    fun `la difficulte est croissante avec le grade`() {
        val pools = (1..8).map { grade ->
            // On recense le vivier à travers les caractères tirés sur dix graines.
            (1..10L).flatMap { seed ->
                generate(
                    QuizSettings(
                        characterSet = CharacterSet.KANJI,
                        questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                        grade = grade,
                    ),
                    seed = seed,
                ).questions.map { (it.character as Kanji).grade!! }
            }.toSet()
        }

        for ((index, grades) in pools.withIndex()) {
            val grade = index + 1
            assertTrue("Grade $grade a tiré $grades", grades.all { it <= grade })
        }
        // Le vivier du grade 8 contient des kanji que celui du grade 1 n'a pas.
        assertTrue(pools.last().size > pools.first().size)
    }

    @Test
    fun `le grade le plus facile reste jouable malgre son petit vivier`() {
        // Les kanji de première année ne sont que 80 : c'est le plus petit vivier.
        assertEquals(80, data.kanji.count { it.grade == 1 })

        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_READING,
                grade = 1,
            )
        )

        assertEquals(Quiz.QUESTION_COUNT, quiz.size)
        assertTrue(quiz.questions.all { it.choices.size == Quiz.CHOICE_COUNT })
        assertTrue(quiz.questions.all { (it.character as Kanji).grade == 1 })
    }

    @Test
    fun `le grade le plus eleve ouvre tout le jeu jouyou`() {
        // Le fichier jōyō ne contient que les grades 1 à 6 et 8.
        val maxGrade = data.kanji.mapNotNull { it.grade }.max()
        assertEquals(8, maxGrade)

        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                grade = maxGrade,
            )
        )

        val grades = quiz.questions.map { (it.character as Kanji).grade }

        assertEquals(Quiz.QUESTION_COUNT, quiz.size)
        assertTrue("Grades tirés : $grades", grades.distinct().size > 1)
    }

    // ---------- Aléatoire reproductible ----------

    @Test
    fun `deux tirages de meme graine donnent le meme quiz`() {
        val settings = QuizSettings()

        assertEquals(generate(settings, seed = 7L), generate(settings, seed = 7L))
    }

    @Test
    fun `deux tirages de graines differentes donnent des quiz differents`() {
        val settings = QuizSettings()

        assertNotEquals(generate(settings, seed = 7L), generate(settings, seed = 8L))
    }

    // ---------- Aides ----------

    @Test
    fun `aucune aide n est fournie quand elles sont desactivees`() {
        for (settings in allSettings) {
            val quiz = generate(settings)

            assertTrue(settings.questionType.label, quiz.questions.all { it.hint == null })
        }
    }

    @Test
    fun `l aide donne la signification quand on demande la prononciation`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_READING,
                hintsEnabled = true,
            )
        )

        for (question in quiz.questions) {
            assertEquals((question.character as Kanji).primaryMeaning, question.hint)
        }
    }

    @Test
    fun `l aide donne la prononciation quand on demande la signification`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                hintsEnabled = true,
            )
        )

        for (question in quiz.questions) {
            assertEquals((question.character as Kanji).primaryReading, question.hint)
        }
    }

    @Test
    fun `l aide d un quiz de kana situe le caractere dans le tableau`() {
        val quiz = generate(
            QuizSettings(
                questionType = QuizQuestionType.CHARACTER_TO_ROMAJI,
                hintsEnabled = true,
            )
        )

        for (question in quiz.questions) {
            val hint = question.hint
            assertTrue("Aide manquante pour ${question.prompt}", !hint.isNullOrBlank())
            // L'aide ne doit pas contenir la réponse attendue.
            assertTrue(hint!!.lowercase() != question.correctAnswer.lowercase())
        }
    }

    // ---------- Vocalisation ----------

    @Test
    fun `rien n est vocalise quand la synthese vocale est desactivee`() {
        for (settings in allSettings) {
            val quiz = generate(settings)

            assertTrue(settings.questionType.label, quiz.questions.all { it.promptSpeech == null })
        }
    }

    @Test
    fun `la vocalisation prononce le caractere de l enonce`() {
        val quiz = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_MEANING,
                speechEnabled = true,
            )
        )

        for (question in quiz.questions) {
            assertEquals(question.character.speechText, question.promptSpeech)
        }
    }

    @Test
    fun `la vocalisation se tait quand elle donnerait la reponse`() {
        val reversed = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.MEANING_TO_CHARACTER,
                speechEnabled = true,
            )
        )
        val pronunciation = generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_READING,
                speechEnabled = true,
            )
        )

        // Le caractère est la réponse : le prononcer la révélerait.
        assertTrue(reversed.questions.all { it.promptSpeech == null })
        // La question porte sur la prononciation elle-même.
        assertTrue(pronunciation.questions.all { it.promptSpeech == null })
    }

    // ---------- Garde-fou ----------

    @Test(expected = IllegalArgumentException::class)
    fun `un type de question incompatible est refuse`() {
        generate(
            QuizSettings(
                characterSet = CharacterSet.KANJI,
                questionType = QuizQuestionType.CHARACTER_TO_ROMAJI,
            )
        )
    }
}
