package de.roboticmind.apkcreator

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import de.roboticmind.apkcreator.core.data.WebAppAssets
import de.roboticmind.apkcreator.core.data.asAssetTree
import de.roboticmind.apkcreator.core.designsystem.ApkCreatorTheme
import de.roboticmind.apkcreator.ui.SetupScreen

/**
 * Zeigt die mitgelieferte Web-App an.
 *
 * Die Dateien aus dem Ordner `webapp/` landen beim Bauen in den Assets und
 * werden hier ueber [WebViewAssetLoader] unter einer https-Adresse
 * ausgeliefert. Das ist wichtiger, als es klingt: unter `file://` blockieren
 * moderne WebViews ES-Module, `fetch()` und `localStorage`. Ueber den
 * Asset-Loader funktioniert all das wie auf einem echten Webserver.
 */
class MainActivity : ComponentActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (!WebAppAssets.hasEntryPoint(assets.asAssetTree(), BuildConfig.START_URL)) {
            // Noch keine Web-App hochgeladen - erklaeren statt weisse Seite zeigen.
            setContent {
                ApkCreatorTheme {
                    SetupScreen(startUrl = BuildConfig.START_URL)
                }
            }
            return
        }

        setContentView(createWebView())
        registerBackNavigation()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        return WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                // Zugriff aufs Dateisystem bleibt aus - die Inhalte kommen
                // ueber den Asset-Loader, nicht ueber file://.
                allowFileAccess = false
                allowContentAccess = false
                mediaPlaybackRequiresUserGesture = false
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webViewClient = object : WebViewClientCompat() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest,
                ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    val url = request.url
                    val isLocal = url.host == ASSET_HOST
                    if (isLocal || !BuildConfig.OPEN_EXTERNAL_LINKS) {
                        return false
                    }
                    // Externe Links im Browser oeffnen, damit die App nicht
                    // versehentlich zum Vollbild-Browser wird.
                    return openInBrowser(url)
                }
            }

            loadUrl("https://$ASSET_HOST/assets/${BuildConfig.START_URL}")
            this@MainActivity.webView = this
        }
    }

    private fun openInBrowser(url: Uri): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, url))
        true
    } catch (_: ActivityNotFoundException) {
        // Kein Browser vorhanden: lieber im WebView oeffnen als gar nichts tun.
        false
    }

    private fun registerBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val view = webView
                    if (view != null && view.canGoBack()) {
                        view.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    private companion object {
        /** Von WebViewAssetLoader vorgegebene, reservierte Domain. */
        const val ASSET_HOST = "appassets.androidplatform.net"
    }
}
