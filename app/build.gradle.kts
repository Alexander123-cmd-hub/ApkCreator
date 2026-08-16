import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ===========================================================================
// 1. Konfiguration einlesen
//
// Reihenfolge: Umgebungsvariable (von der CI gesetzt) schlaegt apkcreator.json,
// und apkcreator.json schlaegt den eingebauten Standardwert. So kann man die
// Datei bearbeiten ODER beim manuellen Start des Workflows Werte eintippen,
// ohne dass sich beides in die Quere kommt.
// ===========================================================================

fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)

val configFile = rootProject.file("apkcreator.json")

@Suppress("UNCHECKED_CAST")
val appConfig: Map<String, Any?> = if (configFile.isFile) {
    try {
        JsonSlurper().parse(configFile) as? Map<String, Any?> ?: emptyMap()
    } catch (error: Exception) {
        // Kaputtes JSON darf den Build nicht sprengen - es gelten dann die
        // Standardwerte, und der Hinweis steht gut sichtbar im Log.
        logger.warn("[apkcreator] apkcreator.json ist fehlerhaft (${error.message}) - Standardwerte werden verwendet.")
        emptyMap()
    }
} else {
    emptyMap()
}

fun setting(envName: String, jsonKey: String, fallback: String): String =
    env(envName) ?: appConfig[jsonKey]?.toString()?.takeIf(String::isNotBlank) ?: fallback

val appName = setting("APP_NAME", "appName", "Meine App")
val appPackageId = setting("PACKAGE_ID", "packageId", "de.meinefirma.meineapp")
val appVersionName = setting("VERSION_NAME", "versionName", "1.0.0")
val appVersionCode = setting("VERSION_CODE", "versionCode", "1").toIntOrNull() ?: 1
val appStartUrl = setting("START_URL", "startUrl", "index.html")
val appIconBackground = setting("ICON_BACKGROUND_COLOR", "iconBackgroundColor", "#2E6A4F")
val appOrientation = setting("ORIENTATION", "orientation", "unspecified")
val openLinksExternally = setting("OPEN_EXTERNAL_LINKS", "openExternalLinksInBrowser", "true").toBoolean()

// Der Ordner, in den Nutzer ihre Web-App legen. Existiert er nicht, startet die
// App mit einem Hinweisbildschirm statt zu crashen.
val webAppDir = rootProject.file("webapp")
val customIconFile = rootProject.file("branding/icon.png")

// ===========================================================================
// 2. Signing (unveraendert): alle Werte kommen aus der Umgebung, nichts im Repo.
// ===========================================================================

val keystoreFile = env("KEYSTORE_PATH")?.let(::file)?.takeIf(File::isFile)
val keystorePasswordEnv = env("KEYSTORE_PASSWORD")
val keyAliasEnv = env("KEY_ALIAS")
val keyPasswordEnv = env("KEY_PASSWORD")

val hasReleaseSigning =
    keystoreFile != null &&
        keystorePasswordEnv != null &&
        keyAliasEnv != null &&
        keyPasswordEnv != null

// ===========================================================================
// 3. Branding: Launcher-Icon aus branding/icon.png erzeugen
//
// Die Icon-Ressourcen werden immer generiert - so gibt es genau eine Quelle
// und keine Konflikte mit Dateien unter src/main/res.
// ===========================================================================

val brandingResDir = layout.buildDirectory.dir("generated/branding/res")

val generateBranding by tasks.registering {
    description = "Erzeugt Launcher-Icon-Ressourcen aus branding/icon.png."

    // Damit Gradle weiss, wann die Aufgabe erneut laufen muss.
    // files() statt file(): beide Dateien sind optional, und eine fehlende
    // Datei darf die Eingabepruefung nicht scheitern lassen.
    inputs.files(project.files(configFile, customIconFile))
        .withPropertyName("brandingSources")
    inputs.property("iconBackground", appIconBackground)
    outputs.dir(brandingResDir)

    doLast {
        val resDir = brandingResDir.get().asFile
        resDir.deleteRecursively()

        val hasCustomIcon = customIconFile.isFile
        val foreground = if (hasCustomIcon) "@drawable/branding_foreground" else "@drawable/ic_launcher_default"

        // Hintergrundfarbe des adaptiven Icons.
        resDir.resolve("values").mkdirs()
        resDir.resolve("values/branding.xml").writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="ic_launcher_background">$appIconBackground</color>
            </resources>
            """.trimIndent() + "\n",
        )

        if (hasCustomIcon) {
            // Das Nutzer-PNG wird als Bitmap-Ressource abgelegt und fuer das
            // adaptive Icon eingerueckt: Android beschneidet die aeusseren ~25 %
            // je nach Geraeteform, der Inset haelt das Motiv in der sicheren Zone.
            resDir.resolve("drawable-nodpi").mkdirs()
            customIconFile.copyTo(resDir.resolve("drawable-nodpi/branding_icon.png"), overwrite = true)

            resDir.resolve("drawable").mkdirs()
            resDir.resolve("drawable/branding_foreground.xml").writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <inset xmlns:android="http://schemas.android.com/apk/res/android"
                    android:drawable="@drawable/branding_icon"
                    android:inset="17%" />
                """.trimIndent() + "\n",
            )
        }

        resDir.resolve("mipmap-anydpi-v26").mkdirs()
        listOf("ic_launcher", "ic_launcher_round").forEach { name ->
            resDir.resolve("mipmap-anydpi-v26/$name.xml").writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
                    <background android:drawable="@color/ic_launcher_background" />
                    <foreground android:drawable="$foreground" />
                    <monochrome android:drawable="$foreground" />
                </adaptive-icon>
                """.trimIndent() + "\n",
            )
        }

        logger.lifecycle(
            if (hasCustomIcon) {
                "[apkcreator] Launcher-Icon aus branding/icon.png erzeugt."
            } else {
                "[apkcreator] Kein branding/icon.png gefunden - Standard-Icon wird verwendet."
            },
        )
    }
}

android {
    namespace = "de.roboticmind.apkcreator"
    compileSdk = 35

    defaultConfig {
        applicationId = appPackageId
        minSdk = 26
        targetSdk = 35

        versionCode = appVersionCode
        versionName = appVersionName

        // Werte, die die App zur Laufzeit braucht.
        buildConfigField("String", "START_URL", "\"$appStartUrl\"")
        buildConfigField("boolean", "OPEN_EXTERNAL_LINKS", "$openLinksExternally")

        // Der App-Name und die Bildschirmausrichtung landen ueber Platzhalter
        // im Manifest.
        manifestPlaceholders["appLabel"] = appName
        manifestPlaceholders["screenOrientation"] = appOrientation
    }

    sourceSets {
        getByName("main") {
            // Die hochgeladene Web-App wird direkt als Asset eingebunden -
            // kein Kopierschritt, kein Zwischenordner im Repository.
            if (webAppDir.isDirectory) {
                assets.srcDir(webAppDir)
            }
            res.srcDir(brandingResDir)
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
            manifestPlaceholders["appLabel"] = "$appName Debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // Bewusster Fallback: die APK laesst sich installieren, ist aber
                // nicht fuer den Play Store geeignet. Der Build bricht nicht ab.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        // Die Anleitung im Ordner webapp/ richtet sich an Menschen auf GitHub
        // und hat in der fertigen APK nichts verloren.
        ignoreAssetsPatterns += "README.md"
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        htmlReport = true
        xmlReport = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Die generierten Icon-Ressourcen muessen bereitstehen, bevor irgendein
// Variantentask sie liest. preBuild haengt jedem Variantenbau voran - damit
// ist die Reihenfolge fuer alle Konsumenten (Ressourcen zusammenfuehren,
// Pfade aufloesen, Lint) auf einen Schlag korrekt.
tasks.named("preBuild") {
    dependsOn(generateBranding)
}

// Uebersicht im Build-Log - hilft beim Nachvollziehen, was gebaut wurde.
// Es werden ausschliesslich unkritische Werte ausgegeben, keine Secrets.
gradle.projectsEvaluated {
    val webAppFiles = if (webAppDir.isDirectory) {
        webAppDir.walkTopDown().count { it.isFile }
    } else {
        0
    }
    logger.lifecycle(
        """
        [apkcreator] App-Name:   $appName
        [apkcreator] Paket-ID:   $appPackageId
        [apkcreator] Version:    $appVersionName ($appVersionCode)
        [apkcreator] Startseite: $appStartUrl
        [apkcreator] Dateien in webapp/: $webAppFiles
        """.trimIndent(),
    )
    if (!hasReleaseSigning) {
        logger.lifecycle(
            "[apkcreator] Kein Release-Keystore gesetzt - Release-Builds werden mit der Debug-Signatur signiert.",
        )
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
