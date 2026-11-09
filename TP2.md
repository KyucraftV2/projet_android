# Projet : TP 2

On continue sur le projet KanaQuiz

En se basant sur le précédent TP, on va rajouter les quiz.

## Qu'est-ce qu'un quiz ?

### Type de quiz

On a des quiz sur les Hiragana, les Katakana et les Kanji.

Sur les Hiragana et les Katakana, on pourra avoir des questions sur :

- On donne le caractère et on doit trouver le romaji
- On donne le romaji et on doit trouver le caractère

Sur les Kanji, on pourra avoir des questions sur :

- On donne le caractère et on doit trouver la signification
- On donne la signification et on doit trouver le caractère
- On donne le caractère et on doit trouver comment le prononcer
- On donne la prononciation et on doit trouver le caractère

On pourra avoir des quiz avec des aides.

- Ajouter la signification du caractère quand on demande la prononciation par exemple.

Un quiz est une série de 10 questions sur des caractères japonais tirés au hasard.
L'utilisateur doit trouver la bonne réponse parmi les 4 proposées.

### Grade du quiz

Les quiz sur les Hiragana et les Katakana ont un grade de 1.
Les quiz sur les Kanji ont un grade qui dépend de la difficulté :
Pour un grade `g`, les 10 kanji du quiz sont tirés au hasard dans la liste des kanji
avec un grade supérieur ou égal à `g`.

## Générer un quiz

On va générer des quiz en fonction des paramètres suivants :

- Type de caractère
- Type de question
- Aide disponible ou pas
- Text-to-speech ou pas

Chaque quiz est tiré aléatoirement indépendamment les uns des autres.

## Afficher le quiz

On a besoin d'une interface pour que l'utilisateur puisse sélectionner les
paramètres du quiz.

On a aussi besoin d'une interface pour le quiz à proprement parler avec :

- Les réponses (Correcte, Incorrecte, Pas encore répondu)
- La question
- Les réponses proposées

L'utilisateur clique sur l'une des réponses,
on met à jour la liste des résultats et on passe à la question suivante.

## Statistiques de l'utilisateur

Afin de gamifier l'application, l'utilisateur va se voir attribuer un `grade` en fonction de ses résultats.

Pour cela, on a besoin de stocker les résultats de ses quiz en base de données.

Son grade est calculé en fonction de ces résultats.

À vous de trouver et d'expliquer dans votre choix une fonction de calcul du `grade` de l'utilisateur apprenant
le japonais.

La fonction devra prendre en compte une fenêtre glissante de résultats de l'utilisateur.
On demande ainsi d'afficher un graphique de progression du `grade` de l'utilisateur suffisamment motivant
pour que l'utilisateur puisse voir sa progression et avoir envie de s'améliorer.

### Base de données des résultats

Vous stockez les résultats dans une base de données en utilisant la librairie Room.

### Calcul et affichage du grade de l'utilisateur

Le grade de l'utilisateur devra apparaître sur la page d'accueil de l'application.
En cliquant dessus, on accède à la page de progression de l'utilisateur.

### Test des statistiques

Pour des raisons de testabilité, il est demandé de créer un test instrumenté (`androidTest`) pour générer des résultats factices dans
la base de données et vérifier que :

- Le calcul du grade est correct ;
- Le graphique de progression s'affiche correctement.

On n'oubliera pas de créer un autre test pour nettoyer la base de données (en n'enlevant que les résultats factices).
