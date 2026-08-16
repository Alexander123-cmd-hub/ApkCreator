# ApkCreator

Android-App (Kotlin, Jetpack Compose, Material 3) mit Multi-Modul-Aufbau und
vollautomatischen APK-Builds über GitHub Actions.

## Modulstruktur

| Modul | Typ | Inhalt |
| --- | --- | --- |
| `:app` | `com.android.application` | Einstiegspunkt, UI-Screens, Signing- und Release-Konfiguration |
| `:core:designsystem` | `com.android.library` | Material-3-Theme, Farb- und Typografie-Definitionen |
| `:core:data` | `com.android.library` | Datenmodelle und Repository (kotlinx.serialization) |

## Toolchain

| Komponente | Version |
| --- | --- |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Gradle Wrapper | 8.11.1 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |
| JVM-Target | 17 |

Abhängigkeiten werden zentral im Version Catalog `gradle/libs.versions.toml`
gepflegt.

## Lokal bauen

```bash
# Android-SDK-Pfad einmalig eintragen (Datei ist gitignored)
echo "sdk.dir=$ANDROID_HOME" > local.properties

./gradlew assembleDebug                          # Debug-APK
./gradlew lintDebug testDebugUnitTest             # Lint + Unit-Tests
./gradlew assembleRelease                         # Release-APK (R8 + Shrinking)
```

Ohne gesetzte Keystore-Variablen wird die Release-APK mit der Debug-Signatur
signiert – der Build läuft also auch ohne Zugangsdaten durch.

## CI/CD

Push auf `main` und Pull Requests erzeugen eine Debug-APK, Tags nach Muster
`v*` eine signierte Release-APK samt GitHub Release.

Alle Details – benötigte Secrets, Keystore-Erzeugung, Release-Ablauf und
Troubleshooting – stehen in **[docs/CI.md](docs/CI.md)**.
