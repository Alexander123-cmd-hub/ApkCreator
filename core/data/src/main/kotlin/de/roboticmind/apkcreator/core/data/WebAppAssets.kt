package de.roboticmind.apkcreator.core.data

import android.content.res.AssetManager
import java.io.IOException

/**
 * Lesender Blick auf einen Verzeichnisbaum.
 *
 * Die Abstraktion existiert, weil [AssetManager] final ist und sich in
 * Unit-Tests weder erzeugen noch ableiten laesst. Ueber diese Schnittstelle
 * ist die Logik ohne Geraet oder Emulator pruefbar.
 */
fun interface AssetTree {

    /**
     * Namen der direkten Eintraege unterhalb von [path].
     *
     * Leer, wenn [path] eine Datei ist oder nicht existiert - genau das
     * Verhalten von [AssetManager.list].
     */
    fun entries(path: String): List<String>
}

/** Verbindet die Schnittstelle mit den echten Android-Assets. */
fun AssetManager.asAssetTree(): AssetTree = AssetTree { path ->
    try {
        list(path).orEmpty().toList()
    } catch (_: IOException) {
        emptyList()
    }
}

/**
 * Prueft, ob eine Web-App mit in die APK gepackt wurde.
 *
 * Ohne diese Pruefung wuerde eine APK ohne Inhalt nur eine leere weisse
 * Flaeche zeigen; stattdessen kann die App einen erklaerenden Bildschirm
 * anzeigen.
 */
object WebAppAssets {

    /**
     * @param startUrl Pfad der Startseite relativ zum Assets-Wurzelverzeichnis,
     *   z. B. `index.html` oder `unterordner/start.html`.
     */
    fun hasEntryPoint(tree: AssetTree, startUrl: String): Boolean {
        val normalised = startUrl.trim().trim('/')
        if (normalised.isEmpty()) return false

        val parent = normalised.substringBeforeLast('/', missingDelimiterValue = "")
        val fileName = normalised.substringAfterLast('/')
        return fileName in tree.entries(parent)
    }

    /**
     * Zaehlt die mitgelieferten Dateien - nur fuer die Anzeige gedacht.
     */
    fun countFiles(tree: AssetTree, path: String = ""): Int {
        val entries = tree.entries(path)
        if (entries.isEmpty()) {
            // Ein Blatt ist eine Datei - ausser wir stehen auf der leeren Wurzel.
            return if (path.isEmpty()) 0 else 1
        }
        return entries.sumOf { entry ->
            countFiles(tree, if (path.isEmpty()) entry else "$path/$entry")
        }
    }
}
