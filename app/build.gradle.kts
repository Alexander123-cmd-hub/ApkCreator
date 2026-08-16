import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Liest eine Umgebungsvariable und liefert `null`, wenn sie fehlt oder leer ist.
 * Damit unterscheidet sich "nicht gesetzt" nicht von "leer gesetzt" - beides
 * fuehrt zum lokalen Fallback-Verhalten.
 */
fun env(name: String): String? = System.getenv(name)?.takeIf(String::isNotBlank)

// --- Signing: alle Werte kommen ausschliesslich aus der Umgebung, nichts steht im Repo. ---
val keystoreFile = env("KEYSTORE_PATH")?.let(::file)?.takeIf(File::isFile)
val keystorePasswordEnv = env("KEYSTORE_PASSWORD")
val keyAliasEnv = env("KEY_ALIAS")
val keyPasswordEnv = env("KEY_PASSWORD")

// Nur wenn wirklich alle vier Angaben vorliegen, wird echtes Release-Signing aktiviert.
// Fehlt auch nur eine, faellt der Release-Build auf die Debug-Signatur zurueck,
// statt den Build abzubrechen - so bleibt ein lokales `assembleRelease` moeglich.
val hasReleaseSigning =
    keystoreFile != null &&
        keystorePasswordEnv != null &&
        keyAliasEnv != null &&
        keyPasswordEnv != null

// versionCode/versionName lassen sich per Umgebungsvariable ueberschreiben,
// damit die CI z. B. die Run-Number oder den Tag-Namen einsetzen kann.
// Ohne Variable gelten die hier definierten Werte.
val defaultVersionCode = 1
val defaultVersionName = "1.0.0"

android {
    namespace = "de.roboticmind.apkcreator"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.roboticmind.apkcreator"
        minSdk = 26
        targetSdk = 35

        versionCode = env("VERSION_CODE")?.toIntOrNull() ?: defaultVersionCode
        versionName = env("VERSION_NAME") ?: defaultVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Der Block wird nur angelegt, wenn die Zugangsdaten vorhanden sind.
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
            // Der abweichende App-Name der Debug-Variante kommt aus
            // src/debug/res/values/strings.xml und ueberschreibt dort app_name.
        }

        release {
            // R8: Code schrumpfen/obfuskieren und ungenutzte Ressourcen entfernen.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                // Bewusster Fallback: die APK ist dann NICHT verteilbar,
                // laesst sich aber lokal bauen und installieren.
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
    }

    lint {
        // Warnungen sollen den Build nicht abbrechen, echte Fehler schon.
        warningsAsErrors = false
        abortOnError = true
        // Reports landen als Artefakt in der CI.
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

// Sichtbarer Hinweis im Build-Log, ohne irgendeinen Geheimniswert auszugeben.
if (!hasReleaseSigning) {
    logger.lifecycle(
        "[signing] Keine vollstaendigen Release-Keystore-Variablen gefunden - " +
            "Release-Build wird mit der Debug-Signatur signiert.",
    )
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
