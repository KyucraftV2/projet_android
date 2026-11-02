package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.data

/**
 * Lit un fichier de `res/raw` depuis le classpath de test.
 *
 * `app/build.gradle.kts` ajoute `src/main/res/raw` aux ressources des tests
 * unitaires : les tests valident ainsi exactement les fichiers embarqués dans
 * l'application, sans avoir à dupliquer le mégaoctet de `kanji_jouyou.json`.
 */
internal fun readRawResource(fileName: String): String {
    val stream = RawResourcesMarker::class.java.getResourceAsStream("/$fileName")
        ?: error("Fichier $fileName introuvable dans le classpath de test")
    return stream.bufferedReader().use { it.readText() }
}

private class RawResourcesMarker
