# KanaQuiz — TP 1 et TP 2

Application Android d'apprentissage des caractères japonais : **hiragana**,
**katakana** et **kanji**. On parcourt les caractères un par un, on les écoute,
puis on se teste avec des **quiz** dont les résultats alimentent un **grade**
qui mesure la progression de l'apprenant.

- **Auteurs** : kreiner, ablamart, vrouet (ENSSAT)
- **`applicationId`** : `fr.enssat.kanaquiz.kreiner_ablamart_vrouet`
- **Sujets** : [`TP1.md`](TP1.md) (apprentissage) et [`TP2.md`](TP2.md) (quiz et statistiques)

## Lancer le projet

```bash
./gradlew :app:installDebug             # compile et installe sur l'appareil connecté
./gradlew :app:testDebugUnitTest        # 129 tests unitaires, sans émulateur
./gradlew :app:connectedDebugAndroidTest # 10 tests instrumentés, émulateur requis
```

Pour la vocalisation, les **données vocales japonaises** doivent être
installées sur l'appareil (Paramètres › Synthèse vocale). Si elles manquent,
l'application le signale explicitement au lieu de rester muette, et l'option
de vocalisation des quiz est désactivée avec la raison affichée.

## Ce que fait l'application

| Écran | Contenu |
|---|---|
| **Accueil** | le grade de l'utilisateur et son avancement vers le suivant, cliquable ; accès au quiz et à l'apprentissage ; crédits |
| **Apprendre les caractères** | onglets Hiragana / Katakana / Kanji, le caractère en grand, ses informations, la vocalisation |
| **Lancer un quiz** | choix du jeu de caractères, du sens de la question, de la difficulté, de l'aide et de la vocalisation |
| **Quiz** | la bande des résultats, la question, quatre réponses, puis le bilan corrigé |
| **Ma progression** | le grade, la courbe de son évolution et les compteurs d'assiduité |

### Apprentissage (TP 1)

| Onglet | Informations affichées |
|---|---|
| **Hiragana** / **Katakana** | le kana en 120 sp, son romaji, sa famille et sa ligne dans le tableau des kana |
| **Kanji** | l'idéogramme, sa traduction anglaise, ses lectures **ON** et **KUN** en hiragana *avec leur romaji*, son grade, son nombre de traits, son niveau JLPT et son rang de fréquence |

Les boutons **‹ Précédent** et **Suivant ›** parcourent la liste et se
désactivent aux extrémités. Le bouton **Écouter la prononciation** vocalise le
caractère, en **alternant vitesse normale et vitesse réduite** à chaque écoute
successive, comme Google Traduction.

### Quiz (TP 2)

Un quiz est une série de **dix questions** à **quatre réponses**, tirées au
hasard et indépendamment les unes des autres.

| Jeu de caractères | Sens de la question |
|---|---|
| Hiragana, Katakana | caractère → romaji, romaji → caractère |
| Kanji | kanji → signification, signification → kanji, kanji → prononciation, prononciation → kanji |

Deux options s'ajoutent aux réglages :

- **l'aide** affiche l'autre information connue du caractère — la signification
  quand on demande la prononciation et inversement, la place dans le tableau
  des kana pour un syllabaire ;
- **la vocalisation** prononce l'énoncé dès son affichage, avec un bouton pour
  réécouter plus lentement. Elle se tait quand l'énoncé *est* la réponse
  attendue, et quand la question porte justement sur la prononciation.

La difficulté d'un quiz de kanji est son **grade**, et les grades proposés sont
lus dans le jeu de données plutôt qu'écrits en dur (voir
[Écarts au sujet](#écarts-au-sujet)).

**Aucun distracteur n'est une seconde bonne réponse.** Le générateur écarte
tout caractère dont l'une des informations — et pas seulement la principale —
répondrait aussi à la question : un kanji dont *une autre* signification est
celle du kanji interrogé, ou qui se lit lui aussi `はん` quand c'est la lecture
demandée. Les distracteurs sont par ailleurs tirés dans le même vivier que le
sujet, donc du même syllabaire ou du même niveau de kanji : un quiz de grade 5
dont les mauvaises réponses viendraient du grade 1 serait trop facile.

## Le grade de l'apprenant

Le grade reprend **l'échelle des kanji** : « grade 3 » signifie que
l'utilisateur réussit, sans aide, des quiz portant sur les kanji appris jusqu'à
la troisième année du primaire. Qui ne travaille que les kana plafonne donc
à 1, puisque le sujet donne le grade 1 aux syllabaires : **pour monter, il faut
affronter des quiz plus difficiles**, et c'est exactement l'incitation
recherchée.

```
         Σ wᵢ · gᵢ · qᵢ
grade = ───────────────     sur les 10 derniers quiz
            Σ wᵢ
```

| Terme | Définition | Pourquoi |
|---|---|---|
| `gᵢ` | le grade du quiz, **moins 15 %** si les aides étaient activées | réussir avec la signification sous les yeux ne prouve pas la même maîtrise. La vocalisation, elle, ne pénalise rien : le générateur se tait dès qu'entendre l'énoncé donnerait la réponse, c'est un confort d'écoute et non une aide |
| `qᵢ` | `(taux − ¼) / ¾`, borné à 0 | avec quatre réponses proposées, **25 % s'obtiennent au hasard** : un quiz répondu n'importe comment ne rapporte rien, et 10/10 vaut exactement le grade du quiz |
| `wᵢ` | `0,85` puissance le nombre de quiz joués depuis | le dernier quiz compte pleinement, le dixième en arrière environ un quart : les progrès se voient vite |

**Pourquoi une fenêtre glissante.** Une moyenne sur tout l'historique ferait
traîner indéfiniment les débuts de l'apprenant et deviendrait de plus en plus
lente à bouger, donc décourageante — précisément l'inverse d'une
gamification. Dix quiz, soit une centaine de questions, c'est assez large pour
que le grade ne saute pas à chaque partie et assez court pour récompenser un
progrès réel en deux ou trois quiz.

**Le grade peut redescendre** : c'est un niveau courant, pas un trophée. La
décote d'ancienneté garantit cependant qu'une mauvaise série se rattrape vite,
ce qu'un test vérifie explicitement.

Exemple de parcours, tel que le produit la fonction :

```
 4. hiragana maîtrisés   10/10 grade 1  →  0,75   Débutant
 8. kanji                 8/10 grade 3  →  1,13   Grade 1
 9. kanji                10/10 grade 3  →  1,49   Grade 1
10. tentative trop dure   3/10 grade 5  →  1,28   Grade 1
12. kanji                 9/10 grade 4  →  2,00   Grade 2
16. kanji                 9/10 grade 8  →  4,13   Grade 4
```

### La courbe de progression

`progressionOf` recalcule le grade après chaque quiz **avec la même fonction**
que le grade affiché : la courbe se termine donc exactement sur le grade de
l'accueil. Elle est dessinée au `Canvas`, sans bibliothèque de graphiques :

- **l'axe vertical s'arrête juste au-dessus du record**, et non au grade 8 :
  sinon la courbe d'un débutant resterait écrasée en bas de l'écran, là où
  aucun progrès ne se verrait ;
- **l'axe horizontal compte les quiz et non le temps**, pour qu'une semaine
  sans jouer n'aplatisse pas la progression ;
- le **record** reste marqué d'un trait pointillé même après une rechute : il
  devient l'objectif à rebattre ;
- le graphique porte une description pour les lecteurs d'écran, une courbe
  seule n'étant pas lisible.

## Architecture

```
KanaQuizApplication                        AppContainer : dépendances partagées
data/
  dto/          KanjiDto, KanaDto          collés au JSON (@SerialName)
  model/        JapaneseCharacter          sealed interface
                Kana, Kanji, CharacterSet  modèle métier
  JapaneseDataParser                       parsing pur, sans Android
  RomajiConverter                          transcription kana → romaji
  JapaneseDataRepository                   lecture de res/raw, cache
  results/      KanaQuizDatabase           Room : base des résultats
                QuizResultEntity, Dao      table plate + convertisseurs
                QuizResultRepository       QuizResultSource ↔ modèle
quiz/                                      domaine pur, testable en JVM
  QuizQuestionType, QuizSettings           ce qu'on demande et comment
  Quiz, QuizGenerator                      tirage des questions
  QuizSession                              progression dans un quiz
  QuizResult                               résultat enregistrable
  UserGrade, UserGradeCalculator           grade et courbe
tts/
  JapaneseSpeaker                          API Text-to-Speech
  SpeechRateAlternator                     alternance normal / lent
ui/
  KanaQuizApp, Destinations                graphe de navigation type-safe
  HomeScreen                               accueil et grade
  CharacterScreen / ViewModel              apprentissage (TP 1)
  QuizSetupScreen / ViewModel              réglages d'un quiz
  QuizScreen / QuizViewModel               déroulement d'un quiz
  ProgressScreen / ViewModel               grade et graphique
  AboutDialog                              crédits
```

Quelques partis pris :

- **Le domaine du quiz ne dépend ni d'Android ni de Room.** Le paquet `quiz`
  n'utilise que du Kotlin : tirage, correction et calcul du grade se testent
  donc intégralement en JVM, avec un `Random` injectable pour que les tirages
  soient reproductibles.
- **Le parsing ne dépend pas d'Android.** `JapaneseDataParser` ne reçoit qu'une
  `String` ; seul `JapaneseDataRepository` touche au `Context`. C'est ce qui
  permet de tester le décodage des 2 136 kanji sans émulateur.
- **Les sources sont des interfaces** (`JapaneseDataSource`,
  `QuizResultSource`) : les ViewModels se testent sur de faux dépôts en
  mémoire.
- **Les composables ne reçoivent aucun `ViewModel`**, seulement des données et
  des lambdas : les `@Preview` fonctionnent et les écrans n'ont pas de logique.
- **Les réglages d'un quiz voyagent dans la route de navigation**, étalés en
  champs simples que Navigation Compose sérialise sans type sur mesure. Ils
  survivent ainsi à la mort du processus, ce qu'un état partagé entre écrans ne
  permettrait pas.
- **Un seul `AppContainer`** porte la source de données et la base : sans lui,
  chaque entrée dans un écran recréerait son dépôt et re-parserait le
  mégaoctet de `kanji_jouyou.json`.
- **Le moteur vocal est créé au-dessus du graphe de navigation** : apprentissage
  et quiz le partagent, au lieu d'ouvrir une connexion au service à chaque
  changement d'écran.
- **La table des résultats est plate** : les réglages du quiz sont étalés en
  colonnes plutôt que sérialisés, pour pouvoir filtrer la progression en SQL.
  Les énumérations y sont stockées **par leur nom et non leur ordinal**, afin
  qu'insérer une valeur au milieu d'une énumération ne réécrive pas le sens des
  résultats déjà enregistrés.
- **Chaque résultat porte sa provenance** (`USER` ou `SAMPLE`) : c'est ce qui
  permet au test de nettoyage de n'effacer que les faux résultats.
- **Le résultat est enregistré à l'instant précis où la dernière question est
  répondue**, une seule fois ; un quiz abandonné n'est pas enregistré, et un
  échec d'écriture est signalé sous le score plutôt que silencieux.
- **Les données sont embarquées dans `res/raw`** plutôt que téléchargées :
  l'application fonctionne hors ligne. Le fichier kanji pèse 1 Mo, il est donc
  lu et parsé sur `Dispatchers.IO` et mis en cache.
- **Les kanji sont triés par grade puis par fréquence**, c'est-à-dire dans
  l'ordre où on les apprend, et non dans l'ordre arbitraire du fichier.
- **La table de romaji n'est pas écrite en dur** : elle est construite à partir
  des 208 couples kana/romaji de `kana_data.json`.
- **Un kanji est prononcé via sa lecture en hiragana**, pas via l'idéogramme
  seul, qui n'a pas de prononciation que le moteur saurait deviner.

## Tests

### 129 tests unitaires — `./gradlew :app:testDebugUnitTest`

| Classe | Couvre |
|---|---|
| `JapaneseDataParserTest` | décodage intégral des 2 136 kanji et des 208 kana, champs `null`, tri, marqueurs du KANJIDIC |
| `RomajiConverterTest` | yōon, sokuon, chōonpu, katakana, et transcription de **toutes** les lectures jōyō |
| `QuizGeneratorTest` | forme des quiz, justesse des réponses, distracteurs jamais justes, respect du grade, reproductibilité, aides, vocalisation qui ne trahit pas la réponse |
| `QuizSessionTest` | suivi des réponses, score, double appui, quiz vide |
| `QuizSettingsTest` | cohérence des réglages et réparation du type de question |
| `UserGradeCalculatorTest` | formule du grade, seuil du hasard, pénalité d'aide, fenêtre glissante, rattrapage, croissance de la courbe |
| `CharacterViewModelTest` | navigation bornée, changement de mode, position mémorisée, erreur de chargement |
| `QuizSetupViewModelTest` | grades disponibles, taille des viviers, cohérence des choix |
| `QuizViewModelTest` | déroulement d'un quiz, enregistrement unique du résultat, quiz abandonné, échec d'écriture |
| `ProgressViewModelTest` | grade et courbe depuis l'historique, record conservé après une rechute, mise à jour sans rafraîchissement |

`app/build.gradle.kts` ajoute `src/main/res/raw` au classpath des tests : les
tests valident exactement les fichiers embarqués dans l'APK, sans dupliquer le
mégaoctet de `kanji_jouyou.json`.

### 10 tests instrumentés — `./gradlew :app:connectedDebugAndroidTest`

| Classe | Couvre |
|---|---|
| `QuizResultDaoTest` | base **en mémoire** : aller-retour des réglages, ordre chronologique, nettoyage qui épargne les vrais quiz |
| `SampleResultsTest` | remplit la **vraie** base de résultats factices, vérifie le grade calculé et l'affichage du graphique |
| `SampleResultsCleanupTest` | supprime les résultats factices **sans toucher** à ceux de l'utilisateur |

La vérification du grade ne se contente pas de rejouer la fonction testée, ce
qui ne prouverait rien : la valeur attendue est **recalculée dans le test**,
constantes réécrites en dur, puis comparée à une valeur de référence figée
(`4,1272777814`) pour qu'un changement de formule se voie. Un second contrôle,
exact celui-là, vérifie que dix sans-faute de grade 4 donnent **exactement
4,00**, quels que soient les poids d'ancienneté.

Les tests instrumentés sont **rejouables** : chacun s'assure que l'historique
factice est présent sans le dupliquer, JUnit ne garantissant pas l'ordre des
méthodes.

## Écarts au sujet

- **La difficulté est `grade ≤ g`, pas `grade ≥ g`.** Le sujet écrit que les
  kanji d'un quiz de grade `g` sont tirés parmi ceux de grade *supérieur ou
  égal* à `g` ; nous y voyons une coquille. Avec cette lecture, le grade 1 —
  censé être le plus facile — tirerait dans les 2 136 kanji dont 1 130 de
  collège, et le grade le plus élevé serait le plus simple : la difficulté ne
  serait pas croissante et le grade de l'utilisateur n'aurait plus de sens.
  Nous tirons donc parmi les kanji **appris jusqu'à l'année `g`**, soit 80
  caractères au grade 1 et 2 136 au grade 8.
- **Les grades proposés sont 1 à 6 et 8.** Le sujet annonce des grades de 1
  à 9, mais `kanji_jouyou.json` ne contient ni grade 7 ni grade 9. La liste est
  donc construite à partir du jeu de données, pour ne jamais proposer un
  niveau vide.

## Corrections apportées aux jeux de données

- `kana_data.json` contenait un **`я` cyrillique (U+044F)** à la place du
  hiragana **`や` (U+3084)**. Corrigé, avec un test de non-régression qui
  vérifie qu'aucun caractère ne sort des plages Unicode japonaises.
- `kanji-jouyou.json` a été renommé **`kanji_jouyou.json`** : les noms de
  ressources Android interdisent le tiret.
- La combinaison `ぢゅ`, présente dans les lectures jōyō, manque au
  `kana_data.json` fourni ; elle est ajoutée en secours dans `RomajiConverter`.

## Crédits

Les données des kanji proviennent du projet **kanji-data** de David Gouveia,
sous licence MIT : <https://github.com/davidluzgouveia/kanji-data>

Ce jeu de données compile lui-même :

- **KANJIDIC** — Electronic Dictionary Research and Development Group :
  <http://www.edrdg.org/wiki/index.php/KANJIDIC_Project>
- les ressources **JLPT** de Jonathan Waller : <http://www.tanos.co.uk/jlpt/>
- le contenu **WaniKani** : <https://docs.api.wanikani.com/>

Le fichier `kana_data.json` a été fourni avec le sujet du TP.

Ces crédits sont également accessibles dans l'application, via « À propos ».
