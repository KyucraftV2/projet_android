package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.quiz

import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.JapaneseData
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.JapaneseCharacter
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kana
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.Kanji
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.familyLabel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.groupLabel
import fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data.model.stripReadingMarkers
import kotlin.random.Random

/**
 * Fabrique des quiz à partir des données d'apprentissage.
 *
 * Volontairement sans dépendance à Android et avec un [Random] injectable :
 * tout le tirage se teste en test unitaire JVM, de façon reproductible en
 * fixant la graine.
 */
class QuizGenerator(private val data: JapaneseData) {

    /**
     * Un caractère candidat, avec les textes que la question lui associe.
     *
     * Les textes sont calculés une fois pour toutes : ils servent autant à
     * fabriquer la question qu'à choisir des distracteurs crédibles.
     *
     * @param infos *toutes* les informations acceptables pour ce caractère et ce
     *   type de question (ses significations, ou toutes ses lectures), et pas
     *   seulement celle retenue comme réponse : c'est ce qui permet d'écarter un
     *   distracteur qui serait en réalité une seconde bonne réponse.
     */
    private data class Candidate(
        val character: JapaneseCharacter,
        val prompt: String,
        val answer: String,
        val infos: Set<String>,
    )

    /**
     * Tire un quiz respectant [settings].
     *
     * Les dix caractères sont tirés sans remise : réviser deux fois le même
     * caractère dans une série de dix n'aurait pas d'intérêt. Si le vivier est
     * plus petit que dix (grade très élevé), le quiz est simplement plus court
     * plutôt que de répéter des questions.
     */
    fun generate(settings: QuizSettings, random: Random = Random.Default): Quiz {
        require(settings.isConsistent) {
            "Le type de question ${settings.questionType} ne s'applique pas aux " +
                "${settings.characterSet.label}"
        }

        val candidates = candidates(settings)
        val subjects = candidates.shuffled(random)
            // Deux questions au même énoncé (deux kanji traduits par « One »)
            // dérouteraient l'utilisateur sans rien lui apprendre.
            .distinctBy { it.prompt }
            .take(Quiz.QUESTION_COUNT)

        return Quiz(
            settings = settings,
            questions = subjects.map { buildQuestion(it, candidates, settings, random) },
        )
    }

    /** Les caractères éligibles, c'est-à-dire ceux dont on peut faire une question. */
    private fun candidates(settings: QuizSettings): List<Candidate> {
        val type = settings.questionType
        return data.charactersOf(settings.characterSet)
            .asSequence()
            .filter { matchesGrade(it, settings.effectiveGrade) }
            .mapNotNull { character ->
                // Un caractère sans l'information demandée (kanji sans lecture
                // exploitable) est écarté : il ne ferait pas une question honnête.
                val infos = infoTexts(character, type).filter { it.isNotBlank() }
                val info = infos.firstOrNull() ?: return@mapNotNull null
                val glyph = character.character.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                Candidate(
                    character = character,
                    prompt = if (type.promptIsCharacter) glyph else info,
                    answer = if (type.promptIsCharacter) info else glyph,
                    infos = infos.toSet(),
                )
            }
            .toList()
    }

    /**
     * Applique la règle de difficulté.
     *
     * Pour un grade `g`, on tire parmi les kanji de grade **inférieur ou égal**
     * à `g`, c'est-à-dire ceux qu'un élève a appris jusqu'à cette année : un
     * quiz de grade 1 ne contient que les 80 kanji de CP, un quiz de grade 8
     * peut tomber sur n'importe quel kanji jōyō.
     *
     * Le sujet écrit « supérieur ou égal », ce que nous considérons comme une
     * coquille : avec cette lecture, le grade 1 — censé être le plus facile —
     * tirerait dans les 2136 kanji dont 1130 de collège, et le grade le plus
     * élevé serait le plus simple. La difficulté ne serait pas croissante et le
     * grade de l'utilisateur, calculé plus loin sur ses résultats, n'aurait plus
     * de sens.
     *
     * Les kanji non classés n'ont pas de difficulté connue, ils restent donc en
     * dehors des quiz.
     */
    private fun matchesGrade(character: JapaneseCharacter, grade: Int): Boolean =
        when (character) {
            is Kana -> true
            is Kanji -> character.grade != null && character.grade <= grade
        }

    /**
     * Toutes les informations du côté « information » de la question : romaji,
     * significations ou lectures selon le type.
     *
     * La première est celle qui sert de réponse (la signification principale,
     * la lecture *on* privilégiée), les suivantes restent des réponses
     * acceptables qu'aucun distracteur ne doit proposer. Liste vide si le
     * caractère ne peut pas répondre à ce type de question.
     */
    private fun infoTexts(character: JapaneseCharacter, type: QuizQuestionType): List<String> =
        when (type) {
            QuizQuestionType.CHARACTER_TO_ROMAJI,
            QuizQuestionType.ROMAJI_TO_CHARACTER,
                -> listOfNotNull((character as? Kana)?.romaji)

            QuizQuestionType.CHARACTER_TO_MEANING,
            QuizQuestionType.MEANING_TO_CHARACTER,
                -> (character as? Kanji)?.meanings.orEmpty()

            QuizQuestionType.CHARACTER_TO_READING,
            QuizQuestionType.READING_TO_CHARACTER,
                -> (character as? Kanji)
                    ?.let { it.readingsOn + it.readingsKun }
                    .orEmpty()
                    .map(::stripReadingMarkers)
        }

    /**
     * Compose la question : la bonne réponse, trois distracteurs et le mélange.
     *
     * Les distracteurs sont tirés dans le même vivier que le sujet, donc du
     * même syllabaire ou du même niveau de kanji : un quiz de grade 5 dont les
     * mauvaises réponses viendraient du grade 1 serait trop facile.
     */
    private fun buildQuestion(
        subject: Candidate,
        candidates: List<Candidate>,
        settings: QuizSettings,
        random: Random,
    ): QuizQuestion {
        val wrongAnswers = candidates.shuffled(random)
            .asSequence()
            .filter { isWrongAnswer(subject, it, settings.questionType) }
            .map { it.answer }
            .distinct()
            .take(Quiz.CHOICE_COUNT - 1)
            .toList()

        val choices = (wrongAnswers + subject.answer).shuffled(random)
        return QuizQuestion(
            character = subject.character,
            prompt = subject.prompt,
            promptIsCharacter = settings.questionType.promptIsCharacter,
            hint = if (settings.hintsEnabled) hint(subject.character, settings.questionType) else null,
            promptSpeech = promptSpeech(subject.character, settings),
            choices = choices,
            correctIndex = choices.indexOf(subject.answer),
        )
    }

    /**
     * Vrai si [other] peut servir de mauvaise réponse à la question de [subject].
     *
     * Un distracteur ne doit jamais être une réponse juste déguisée. Dans le
     * sens « caractère → information », on écarte donc toute information que le
     * sujet accepte aussi (une de ses autres significations). Dans le sens
     * inverse, on écarte tout caractère qui accepte l'énoncé (un autre kanji qui
     * se lit lui aussi `はん`).
     */
    private fun isWrongAnswer(
        subject: Candidate,
        other: Candidate,
        type: QuizQuestionType,
    ): Boolean {
        if (other.character.character == subject.character.character) return false
        return if (type.promptIsCharacter) {
            other.answer !in subject.infos
        } else {
            subject.prompt !in other.infos
        }
    }

    /**
     * L'aide proposée, c'est-à-dire l'autre information connue du caractère.
     *
     * Quand on demande la prononciation on donne la signification et
     * inversement, comme suggéré par le sujet. Pour un kana, la seule autre
     * information utile est sa place dans le tableau des kana : savoir qu'on
     * est sur la ligne K réduit le champ sans donner la réponse.
     */
    private fun hint(character: JapaneseCharacter, type: QuizQuestionType): String? =
        when (type) {
            QuizQuestionType.CHARACTER_TO_ROMAJI,
            QuizQuestionType.ROMAJI_TO_CHARACTER,
                -> (character as? Kana)?.let { "${it.familyLabel()} · ${it.groupLabel()}" }

            QuizQuestionType.CHARACTER_TO_MEANING,
            QuizQuestionType.MEANING_TO_CHARACTER,
                -> (character as? Kanji)?.primaryReading

            QuizQuestionType.CHARACTER_TO_READING,
            QuizQuestionType.READING_TO_CHARACTER,
                -> (character as? Kanji)?.primaryMeaning
        }

    /**
     * Le texte vocalisé pendant la question, ou `null` s'il faut se taire.
     *
     * On ne prononce que l'énoncé, et seulement quand c'est un caractère : dans
     * le sens inverse, le caractère est la réponse. On se tait aussi quand la
     * question porte sur la prononciation, l'entendre répondrait à la place de
     * l'utilisateur. L'écran de quiz reste libre de vocaliser le caractère
     * après la réponse, où cela devient une correction.
     */
    private fun promptSpeech(character: JapaneseCharacter, settings: QuizSettings): String? {
        val type = settings.questionType
        val reveals = type == QuizQuestionType.CHARACTER_TO_READING
        val speak = settings.speechEnabled && type.promptIsCharacter && !reveals
        return character.speechText.takeIf { speak && it.isNotBlank() }
    }
}
