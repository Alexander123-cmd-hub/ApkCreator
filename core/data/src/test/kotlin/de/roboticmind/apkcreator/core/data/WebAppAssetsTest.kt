package de.roboticmind.apkcreator.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebAppAssetsTest {

    /** Baut aus einer Liste von Dateipfaden denselben Baum, den Android liefert. */
    private fun treeOf(vararg files: String) = AssetTree { path ->
        val prefix = if (path.isEmpty()) "" else "$path/"
        files
            .filter { it.startsWith(prefix) && it != path }
            .map { it.removePrefix(prefix).substringBefore('/') }
            .distinct()
    }

    @Test
    fun `entry point is detected when the file exists`() {
        assertTrue(WebAppAssets.hasEntryPoint(treeOf("index.html", "style.css"), "index.html"))
    }

    @Test
    fun `missing entry point is reported instead of throwing`() {
        assertFalse(WebAppAssets.hasEntryPoint(treeOf("style.css"), "index.html"))
    }

    @Test
    fun `a start page inside a folder is found`() {
        val tree = treeOf("app/start.html", "app/app.js")

        assertTrue(WebAppAssets.hasEntryPoint(tree, "app/start.html"))
        assertFalse(WebAppAssets.hasEntryPoint(tree, "index.html"))
    }

    @Test
    fun `leading slashes in the configured start url are tolerated`() {
        // Eine fuehrende "/" ist ein naheliegender Tippfehler in apkcreator.json
        // und darf nicht dazu fuehren, dass die App den Hinweisbildschirm zeigt.
        assertTrue(WebAppAssets.hasEntryPoint(treeOf("index.html"), "/index.html"))
    }

    @Test
    fun `a blank start url never matches`() {
        assertFalse(WebAppAssets.hasEntryPoint(treeOf("index.html"), "   "))
    }

    @Test
    fun `files are counted across nested folders`() {
        val tree = treeOf("index.html", "css/style.css", "img/logo/mark.png")

        assertEquals(3, WebAppAssets.countFiles(tree))
    }

    @Test
    fun `an empty asset tree counts as zero files`() {
        assertEquals(0, WebAppAssets.countFiles(treeOf()))
    }
}
