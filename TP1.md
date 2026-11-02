# Projet : TP 1

On commence le projet : KanaQuiz

On va d'abord s'intéresser à afficher et prononcer les caractères japonais.

Pour info, il y a 3 sortes de caractères japonais :
- Les caractères HIRAGANA : caractères phonétiques pour le japonais, ce ne sont pas des idéogrammes. 
Il faut apprendre à les reconnaître et les prononcer correctement. Ils ont une traduction directe en phonétique (romaji).
- Les caractères KATAKANA : caractères phonétiques pour les mots d'origine étrangère, ce ne sont pas des idéogrammes.
Il faut apprendre à les reconnaître et les prononcer correctement. Ils ont une traduction directe en phonétique (romaji).
- Les caractères KANJI : idéogrammes importés du chinois. Ils se prononcent avec un ou plusieurs Hiragana.


## Base de connaissances

On va utiliser 2 jeux de données :

Le fichier `kana_data.json` qui contient les HIRAGANA et les KATAKANA.

Le fichier `kanji-jouyou.json` qui contient les KANJI les plus utilisés.

Vous trouverez les fichiers sur le GitHub https://github.com/davidluzgouveia/kanji-data.
Pour pouvoir utiliser ces fichiers, il conviendra de donner les références du projet GitHub dans les crédits de votre projet.

## Créer le projet

Vous pourrez réutiliser (et améliorer) le code de ce TP pour le projet.

Pour partir sur de bonnes bases : créer un projet Android avec les logins du binôme pour être sûr que je puisse installer tous les projets sur le même téléphone.
Soit :

```
fr.enssat.<nomDuProjet>.<login1>_<login2>
```

## Le sujet du TP

### Afficher les caractères avec les informations utiles pour l'apprentissage.

HIRAGANA et KATAKANA : 
- le caractère en gros
- le romaji

KANJI :
- le caractère en gros
- la traduction du kanji en anglais
- la phonétique en HIRAGANA (voire en romaji)
- le grade du kanji (1 à 9) : le grade correspond au niveau d'apprentissage du kanji.

Il faut :
- définir un modèle de données
- charger les fichiers (soit des ressources de l'application, soit du réseau)
  - si vous optez pour le réseau, mettre les données en cache s'impose.
- parser les données (JSON) pour les convertir en objets.
- afficher les données dans une vue (Compose évidemment)
- ajouter un bouton pour passer au caractère suivant et précédent.

#### Définir un modèle de données

Une `data class` s'impose.

#### Charger les données des ressources

Pour ajouter un fichier JSON dans l'application, il faut : 
- créer un répertoire `raw` dans le dossier `res` de votre projet
- déposer le fichier dans ce répertoire

Pour récupérer le fichier dans l'application, utilisez :

```kotlin
fun loadJsonFromRaw(context: Context, @RawRes rawResId: Int): String {
    return context.resources.openRawResource(rawResId).bufferedReader().use {
        it.readText()
    }
}
```

#### Parsing des données

On pourra utiliser :
- Kotlinx Serialization : https://kotlinlang.org/docs/serialization.html
- Gson : https://google.github.io/gson/UserGuide.html
- Moshi : https://www.baeldung.com/java-json-moshi


### Vocaliser les caractères

Pour faciliter l'apprentissage, on va rajouter un bouton pour vocaliser les caractères.

Pour cela, on va utiliser l'API Text to Speech de Google :
https://developer.android.com/reference/android/speech/tts/package-summary

Cette API est disponible sur l'ensemble des téléphones (et émulateurs). Elle est moins puissante que la future génération d'API (https://developers.google.com/ml-kit/genai/text-to-speech/android?hl=fr). 
Mais cette dernière n'est disponible que sur un nombre limité de téléphones.

#### Démarrer l'API

Pour utiliser l'API TTS, il faut d'abord se connecter au service :

```kotlin
val tts = TextToSpeech(context, object : TextToSpeech.OnInitListener {
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TtS", "TTS initialized")
        }
    }
})
```
Tant que le log n'est pas affiché, les appels suivants seront sans effet.

#### Charger une langue

Il faut ensuite demander au service de charger le japonais :

```kotlin
val result = tts.setLanguage(Locale.JAPANESE)
if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
    Log.e("TtS", "Language not supported: $result")
}
```
Assurez-vous dans les paramètres de votre téléphone que les données de vocalisation du japonais sont bien chargées si vous avez des problèmes.


#### Demander une vocalisation

Relativement simple une fois qu'on a passé les étapes précédentes :

```kotlin
val result = tts.speak("これは日本語の文の一例です", TextToSpeech.QUEUE_FLUSH, null, null)
Log.d("TtS", "result: $result")
```

À noter qu'il est possible d'influer sur la vitesse de prononciation avec :

```kotlin
tts.setSpeechRate(0.5f) // to slow down
```

Sur Google Traduction, la première vocalisation est à vitesse normale, la seconde à vitesse réduite, la troisième à vitesse normale, la suivante à vitesse réduite, et ainsi de suite.

Pourquoi ne pas l'implémenter nous aussi ?

#### Fermer le service

Une fois que l'on n'a plus besoin du service, il convient de le fermer proprement :

```kotlin
tts.stop() // to stop any ongoing speech 
tts.shutdown() // to release the resource
```

