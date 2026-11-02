# KanaQuiz — TP 1

Application Android d'apprentissage des caractères japonais : **hiragana**,
**katakana** et **kanji**. Chaque caractère est affiché en grand avec les
informations utiles à son apprentissage, et peut être prononcé par la synthèse
vocale.

- **Auteurs** : kreiner, ablamart, vrouet (ENSSAT)
- **`applicationId`** : `fr.enssat.kanaquiz.kreiner_ablamart_vrouet`
- **Sujet** : voir [`TP1.md`](TP1.md)

## Lancer le projet

```bash
./gradlew :app:installDebug     # compile et installe sur l'appareil connecté
./gradlew :app:testDebugUnitTest # lance les 45 tests unitaires
```

Pour la vocalisation, les **données vocales japonaises** doivent être
installées sur l'appareil (Paramètres › Synthèse vocale). Si elles manquent,
l'application le signale explicitement sous le bouton d'écoute au lieu de
rester muette.

## Ce que fait l'application

| Écran | Contenu |
|---|---|
| Onglet **Hiragana** / **Katakana** | le kana en 120 sp, son romaji, sa famille et sa ligne dans le tableau des kana |
| Onglet **Kanji** | l'idéogramme, sa traduction anglaise, ses lectures **ON** et **KUN** en hiragana *avec leur romaji*, son grade, son nombre de traits, son niveau JLPT et son rang de fréquence |

Les boutons **‹ Précédent** et **Suivant ›** parcourent la liste et se
désactivent aux extrémités. Le bouton **Écouter la prononciation** vocalise le
caractère, en **alternant vitesse normale et vitesse réduite** à chaque écoute
successive, comme Google Traduction.

## Architecture

```
data/
  dto/          KanjiDto, KanaDto          collés au JSON (@SerialName)
  model/        JapaneseCharacter          sealed interface
                Kana, Kanji                modèle métier
  JapaneseDataParser                       parsing pur, sans Android
  RomajiConverter                          transcription kana → romaji
  JapaneseDataRepository                   lecture de res/raw, cache
tts/
  JapaneseSpeaker                          API Text-to-Speech
  SpeechRateAlternator                     alternance normal / lent
ui/
  CharacterViewModel                       état, navigation, modes
  CharacterScreen                          Compose, sans ViewModel
  AboutDialog                              crédits
```

Quelques partis pris :

- **Le parsing ne dépend pas d'Android.** `JapaneseDataParser` ne reçoit qu'une
  `String` ; seul `JapaneseDataRepository` touche au `Context`. C'est ce qui
  permet de tester le décodage des 2 136 kanji en test unitaire JVM, sans
  émulateur.
- **`JapaneseDataSource` est une interface**, pour que le `ViewModel` soit
  testable avec un faux jeu de données.
- **`CharacterScreen` ne reçoit aucun `ViewModel`**, seulement des données et
  des lambdas : les `@Preview` fonctionnent, et le composable n'a pas de logique.
- **`JapaneseCharacter` est une `sealed interface`** : le `when` de l'affichage
  est exhaustif sans `else`, donc ajouter un type de caractère casse la
  compilation à l'endroit exact qu'il faut compléter.
- **Les données sont embarquées dans `res/raw`** plutôt que téléchargées :
  l'application fonctionne hors ligne, sans cache réseau à gérer. Le fichier
  kanji pèse 1 Mo, il est donc lu et parsé sur `Dispatchers.IO` et mis en cache
  pour ne pas être relu à chaque rotation.
- **Les kanji sont triés par grade puis par fréquence**, c'est-à-dire dans
  l'ordre où on les apprend, et non dans l'ordre arbitraire du fichier.
- **La table de romaji n'est pas écrite en dur** : elle est construite à partir
  des 208 couples kana/romaji de `kana_data.json`.
- **Un kanji est prononcé via sa lecture en hiragana**, pas via l'idéogramme
  seul, qui n'a pas de prononciation que le moteur saurait deviner.

## Tests

45 tests unitaires (`./gradlew :app:testDebugUnitTest`) :

| Classe | Couvre |
|---|---|
| `JapaneseDataParserTest` | décodage intégral des 2 136 kanji et des 208 kana, champs `null`, tri, marqueurs du KANJIDIC |
| `RomajiConverterTest` | yōon, sokuon, chōonpu, katakana, et transcription de **toutes** les lectures jōyō |
| `CharacterViewModelTest` | navigation bornée, changement de mode, position mémorisée, erreur de chargement |
| `SpeechRateAlternatorTest` | alternance normal / lent et sa remise à zéro |

`app/build.gradle.kts` ajoute `src/main/res/raw` au classpath des tests : les
tests valident exactement les fichiers embarqués dans l'APK, sans dupliquer le
mégaoctet de `kanji_jouyou.json`.

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
