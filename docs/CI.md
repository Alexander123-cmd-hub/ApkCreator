# CI/CD – automatische APK-Builds

Dieses Dokument beschreibt die Pipeline in `.github/workflows/build-apk.yml`:
welche Secrets nötig sind, wie ein Release ausgelöst wird und was bei den
typischen Fehlern zu tun ist.

---

## 1. Was die Pipeline wann tut

| Auslöser | Job | Ergebnis |
| --- | --- | --- |
| `push` auf `main` | `debug` | Debug-APK als Artefakt (30 Tage) |
| `pull_request` auf `main` | `debug` | Lint + Unit-Tests + Debug-APK + Reports |
| `push` eines Tags `v*` | `release` | Signierte Release-APK + GitHub Release |
| manuell (`workflow_dispatch`) | `debug` | Debug-APK als Artefakt |

Lint- und Testfehler brechen den Pull-Request-Build ab. Auf `main` und bei
manuellen Läufen wird nur gebaut – dort ist die Prüfung bereits im PR erfolgt.

---

## 2. Benötigte GitHub Secrets

Anzulegen unter **Settings → Secrets and variables → Actions → New repository secret**.
Die Namen müssen exakt so lauten:

| Secret | Inhalt |
| --- | --- |
| `KEYSTORE_BASE64` | Der komplette Keystore (`.jks`), Base64-kodiert, **ohne Zeilenumbrüche** |
| `KEYSTORE_PASSWORD` | Passwort des Keystores (`-storepass`) |
| `KEY_ALIAS` | Alias des Schlüssels im Keystore (z. B. `upload`) |
| `KEY_PASSWORD` | Passwort des Schlüssels (`-keypass`) |

Nur der Job `release` liest diese Secrets. Der Debug-Build kommt ohne aus.

> **Wichtig:** Sichere den Keystore zusätzlich an einem sicheren Ort
> (Passwort-Manager, verschlüsseltes Backup). Geht er verloren, kannst du
> bestehende Installationen nie wieder aktualisieren – ein neuer Keystore
> bedeutet eine neue App-Identität.

---

## 3. Keystore erzeugen und nach Base64 konvertieren

### Linux / macOS

```bash
# 1. Keystore erzeugen (fragt interaktiv nach Passwörtern und Namensangaben)
keytool -genkeypair -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload

# 2. Nach Base64 konvertieren (-w0 = keine Zeilenumbrüche, wichtig!)
base64 -w0 release.jks > release.jks.base64

# 3. Inhalt in die Zwischenablage (Linux, benötigt xclip)
xclip -selection clipboard < release.jks.base64

# 3b. Alternative für macOS
pbcopy < release.jks.base64
```

Auf macOS kennt `base64` kein `-w0`; dort erzeugt der Standardaufruf bereits
eine einzige Zeile:

```bash
base64 -i release.jks -o release.jks.base64
```

### Windows / PowerShell

```powershell
# 1. Keystore erzeugen
keytool -genkeypair -v `
  -keystore release.jks `
  -keyalg RSA -keysize 2048 -validity 10000 `
  -alias upload

# 2. Nach Base64 konvertieren (einzeilig)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) |
  Set-Content -NoNewline release.jks.base64

# 3. Inhalt in die Zwischenablage
Get-Content -Raw release.jks.base64 | Set-Clipboard
```

Danach den Inhalt von `release.jks.base64` als Wert für `KEYSTORE_BASE64`
einfügen und die lokalen Dateien `release.jks.base64` **löschen**.
Der Keystore selbst gehört nicht ins Repository – `.gitignore` blockt
`*.jks`, `*.keystore` und `*.p12` bereits.

---

## 4. Release auslösen

```bash
# Tag setzen und pushen – das startet den Release-Job
git tag v1.0.0
git push origin v1.0.0
```

Was dann passiert:

1. Der Keystore wird aus `KEYSTORE_BASE64` nach `$RUNNER_TEMP/release.jks`
   entschlüsselt (außerhalb des Workspace).
2. `versionName` wird aus dem Tag abgeleitet: `v1.0.0` → `1.0.0`.
3. `versionCode` wird auf die Lauf-Nummer des Workflows gesetzt.
4. `./gradlew assembleRelease` baut mit R8-Shrinking und signiert.
5. Ein Prüfschritt bricht ab, falls die APK versehentlich mit dem
   Debug-Zertifikat signiert wurde.
6. APK und R8-`mapping.txt` werden als Artefakte hochgeladen.
7. Ein GitHub Release wird angelegt, die APK angehängt, die Release Notes
   aus den Commits seit dem vorherigen Tag erzeugt.
8. Der Keystore wird vom Runner gelöscht – auch wenn der Build fehlschlägt.

**Vorabversionen:** Enthält der Tag einen Bindestrich (z. B. `v1.1.0-rc1`),
wird das Release automatisch als *Pre-release* markiert.

**Tag zurücknehmen** (falls etwas schiefging):

```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

Das zugehörige GitHub Release muss zusätzlich manuell gelöscht werden.

---

## 5. Lokal testen, ohne zu pushen

Der Release-Build lässt sich vollständig lokal nachstellen. Ohne gesetzte
Variablen fällt er bewusst auf die Debug-Signatur zurück und bricht **nicht** ab:

```bash
# Variante A: ohne Keystore – prüft nur, ob R8/Shrinking durchläuft
./gradlew assembleRelease
# Erwartete Log-Zeile:
# [signing] Keine vollstaendigen Release-Keystore-Variablen gefunden - ...
```

```bash
# Variante B: mit echtem Keystore – prüft die komplette Signierung
export KEYSTORE_PATH="$HOME/keys/release.jks"   # absoluter Pfad
export KEYSTORE_PASSWORD="…"
export KEY_ALIAS="upload"
export KEY_PASSWORD="…"
export VERSION_CODE="42"        # optional, überschreibt build.gradle.kts
export VERSION_NAME="1.0.0"     # optional

./gradlew assembleRelease
```

Ergebnis prüfen:

```bash
# Signatur anzeigen – hier darf NICHT "CN=Android Debug" stehen
"$ANDROID_HOME"/build-tools/35.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk

# versionCode / versionName aus der fertigen APK auslesen
"$ANDROID_HOME"/build-tools/35.0.0/aapt2 dump badging \
  app/build/outputs/apk/release/app-release.apk | head -1
```

Das Gegenstück zum PR-Job:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

---

## 6. Troubleshooting

### `gradle-wrapper.jar` fehlt

**Symptom:**
`Error: Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

**Ursache:** Die Datei `gradle/wrapper/gradle-wrapper.jar` ist nicht im
Repository – meist, weil eine `.gitignore`-Regel wie `*.jar` sie ausschließt.

**Prüfen:**

```bash
git ls-files gradle/wrapper/
# Muss gradle-wrapper.jar UND gradle-wrapper.properties zeigen
git check-ignore -v gradle/wrapper/gradle-wrapper.jar
# Gibt nichts aus, wenn die Datei korrekt nicht ignoriert wird
```

**Beheben:**

```bash
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
git add -f gradle/wrapper/gradle-wrapper.jar
```

Die `.gitignore` dieses Projekts enthält dafür bereits eine
Ausnahme-Regel (`!gradle/wrapper/gradle-wrapper.jar`).

---

### Falsche JDK-Version

**Symptom:**
`Unsupported class file major version 6x` oder
`Android Gradle plugin requires Java 17 to run. You are currently using Java 11.`

**Ursache:** AGP 8.7.3 verlangt JDK 17; die Module kompilieren mit
`jvmTarget = 17`.

**Beheben:** Im Workflow steht die Version zentral in `env.JAVA_VERSION`.
Lokal:

```bash
java -version          # muss 17 (oder neuer) melden
export JAVA_HOME=/pfad/zu/jdk-17
```

JDK 21 funktioniert lokal ebenfalls, weil `sourceCompatibility`/`jvmTarget`
explizit auf 17 stehen. Der Runner nutzt bewusst 17, um exakt der von AGP
unterstützten Kombination zu entsprechen.

---

### Keystore-Passwort passt nicht

**Symptom:**
`Failed to read key upload from store … keystore password was incorrect`
oder `Cannot recover key`

**Ursachen und Prüfung:**

1. **Store- und Key-Passwort verwechselt.** `KEYSTORE_PASSWORD` ist
   `-storepass`, `KEY_PASSWORD` ist `-keypass`. Bei `keytool` sind das zwei
   verschiedene Werte, auch wenn sie oft identisch gesetzt werden.
2. **Alias falsch.** Vorhandene Aliase auflisten:

   ```bash
   keytool -list -v -keystore release.jks | grep "Alias name"
   ```

3. **Zeilenumbrüche im Base64-Secret.** Wurde `base64` ohne `-w0` benutzt,
   enthält das Secret Umbrüche und die dekodierte Datei ist beschädigt.
   Lokal gegenprüfen:

   ```bash
   base64 -w0 release.jks | md5sum      # mit dem Secret-Inhalt vergleichen
   ```

4. **Leerzeichen beim Einfügen.** Beim Kopieren in das GitHub-Secret-Feld
   darf kein führendes/abschließendes Whitespace mitkommen.

Der Workflow-Schritt „Signatur prüfen" fängt den Fall ab, dass der Build
trotz gesetzter Secrets still auf die Debug-Signatur zurückfällt.

---

### Out of Memory beim Gradle-Build auf dem Runner

**Symptom:**
`Java heap space`, `GC overhead limit exceeded`, oder der Runner beendet den
Job mit `The runner has received a shutdown signal` / Exit-Code 137.

**Ursache:** Gradle-Daemon und Kotlin-Compiler teilen sich den Speicher des
Runners (ubuntu-latest: 16 GB RAM, 4 vCPU).

**Aktuelle Einstellung** in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

**Wenn es trotzdem klemmt, in dieser Reihenfolge:**

1. Heap erhöhen – auf GitHub-Runnern sind 4–5 GB gefahrlos möglich:

   ```properties
   org.gradle.jvmargs=-Xmx5g -XX:MaxMetaspaceSize=1g
   ```

2. Dem Kotlin-Compiler-Daemon eigenen Speicher geben:

   ```properties
   kotlin.daemon.jvmargs=-Xmx2g
   ```

3. Parallelität reduzieren, wenn mehrere Module gleichzeitig kompilieren:

   ```properties
   org.gradle.parallel=false
   ```

4. Als letzte Stufe den Daemon im CI-Lauf abschalten:

   ```bash
   ./gradlew assembleRelease --no-daemon
   ```

Faustregel: `-Xmx` plus `kotlin.daemon.jvmargs` sollten zusammen deutlich
unter dem RAM des Runners bleiben, sonst greift der OOM-Killer (Exit 137).

---

### Weitere Stolpersteine

| Symptom | Ursache | Lösung |
| --- | --- | --- |
| `SDK location not found` | `local.properties` fehlt lokal | `sdk.dir=/pfad/zum/android-sdk` eintragen (Datei ist gitignored; auf dem Runner nicht nötig, dort greift `ANDROID_HOME`) |
| `Artifact name is not valid` | Branchname enthält `/` | Der Workflow ersetzt `/` bereits durch `-` |
| `Resource not accessible by integration` beim Release | Job hat keine Schreibrechte | `permissions: contents: write` im Job `release` prüfen |
| `SerializationException` nur im Release-Build | R8 hat Serializer entfernt | keep-Regeln in `core/data/consumer-rules.pro` prüfen |
