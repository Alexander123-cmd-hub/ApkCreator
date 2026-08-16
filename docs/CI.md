# Signierte Releases einrichten

Die normale Debug-APK entsteht **ohne jede Einrichtung** — einfach Dateien
hochladen, fertig. Dieses Dokument brauchst du nur, wenn du zusätzlich
signierte Release-APKs willst.

**Wozu der Aufwand?**

| | Debug | Release |
| --- | --- | --- |
| Größe | ~9 MB | ~0,8 MB |
| Weitergabe an andere | möglich, aber unsauber | dafür gemacht |
| Play Store | nein | ja |
| Updates ohne Deinstallieren | nur bei gleicher Debug-Signatur | ja |

Der Kern ist ein **Signaturschlüssel** (Keystore). Er beweist, dass ein Update
wirklich von dir stammt. Einmal erzeugt, gilt er für alle künftigen Versionen.

---

## 1. Was die Pipeline wann tut

| Auslöser | Job | Ergebnis |
| --- | --- | --- |
| `push` auf `main` | `build` | Debug-APK als Artefakt (30 Tage) |
| `pull_request` auf `main` | `build` | Lint + Unit-Tests + Debug-APK + Reports |
| manueller Start | `build` | Debug **oder** Release, mit eingetippten Angaben |
| `push` eines Tags `v*` | `release` | Signierte APK + GitHub Release |

Lint- und Testfehler brechen den Pull-Request-Build ab. Auf `main` wird nur
gebaut — dort ist die Prüfung bereits im PR erfolgt.

---

## 2. Die vier Secrets

Anzulegen unter **Settings → Secrets and variables → Actions → New repository
secret**. Die Namen müssen exakt so lauten:

| Secret | Inhalt |
| --- | --- |
| `KEYSTORE_BASE64` | Der Keystore (`.jks`), Base64-kodiert, **ohne Zeilenumbrüche** |
| `KEYSTORE_PASSWORD` | Passwort des Keystores (`-storepass`) |
| `KEY_ALIAS` | Alias des Schlüssels, z. B. `upload` |
| `KEY_PASSWORD` | Passwort des Schlüssels (`-keypass`) |

Nur der Release-Job liest sie. Debug-Builds kommen ohne aus.

> **Sichere den Keystore zusätzlich** an einem sicheren Ort (Passwort-Manager,
> verschlüsseltes Backup). Geht er verloren, kannst du bestehende
> Installationen nie wieder aktualisieren — ein neuer Keystore bedeutet für
> Android eine neue App-Identität.

---

## 3. Keystore erzeugen und kodieren

### Linux / macOS

```bash
# 1. Keystore erzeugen (fragt nach Passwörtern und Namensangaben)
keytool -genkeypair -v \
  -keystore release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias upload

# 2. Nach Base64 (-w0 = keine Zeilenumbrüche, wichtig!)
base64 -w0 release.jks > release.jks.base64

# 3. In die Zwischenablage
xclip -selection clipboard < release.jks.base64   # Linux
pbcopy < release.jks.base64                       # macOS
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

# 2. Nach Base64 (einzeilig)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.jks")) |
  Set-Content -NoNewline release.jks.base64

# 3. In die Zwischenablage
Get-Content -Raw release.jks.base64 | Set-Clipboard
```

### Mit installierter GitHub CLI

```bash
gh secret set KEYSTORE_BASE64 < release.jks.base64
gh secret set KEY_ALIAS --body 'upload'
gh secret set KEYSTORE_PASSWORD   # fragt interaktiv, bleibt aus der History
gh secret set KEY_PASSWORD
```

Danach `release.jks.base64` löschen. Der Keystore selbst gehört **nicht** ins
Repository — `.gitignore` blockt `*.jks`, `*.keystore` und `*.p12` bereits.

---

## 4. Release auslösen

### Über die Weboberfläche (auch am Handy)

1. **Releases** → **Draft a new release**
2. *Choose a tag* → `v1.0.0` eintippen → **Create new tag: v1.0.0 on publish**

   Nur die Version eintippen — der Text „on publish" gehört zum Button, nicht
   in das Feld. Erlaubt sind Buchstaben, Ziffern und `.` `-` `_`, keine
   Leerzeichen.
3. **Publish release**

### Über die Kommandozeile

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Was dann passiert

1. Keystore wird aus `KEYSTORE_BASE64` nach `$RUNNER_TEMP/release.jks`
   entschlüsselt — außerhalb des Workspace.
2. `versionName` wird aus dem Tag abgeleitet: `v1.0.0` → `1.0.0`.
3. `versionCode` wird auf die Lauf-Nummer des Workflows gesetzt.
4. Gradle baut mit R8-Shrinking und signiert.
5. Ein Prüfschritt bricht ab, falls die APK versehentlich das Debug-Zertifikat
   trägt.
6. APK und `mapping.txt` werden als Artefakte gesichert.
7. Das GitHub Release wird angelegt, die APK angehängt, die Release Notes
   entstehen aus den Commits seit dem vorherigen Tag.
8. Der Keystore wird gelöscht — auch wenn der Build fehlschlägt.

**Vorabversionen:** Enthält der Tag einen Bindestrich (`v1.1.0-rc1`), wird das
Release automatisch als *Pre-release* markiert.

**Tag zurücknehmen:**

```bash
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

Das zugehörige GitHub Release muss zusätzlich manuell gelöscht werden.

---

## 5. Lokal testen, ohne zu pushen

Ohne gesetzte Variablen fällt der Release-Build bewusst auf die Debug-Signatur
zurück und bricht **nicht** ab:

```bash
# Variante A: ohne Keystore – prüft nur, ob R8/Shrinking durchläuft
./gradlew assembleRelease
# Erwartete Log-Zeile:
# [apkcreator] Kein Release-Keystore gesetzt - ...
```

```bash
# Variante B: mit echtem Keystore – prüft die komplette Signierung
export KEYSTORE_PATH="$HOME/keys/release.jks"   # absoluter Pfad
export KEYSTORE_PASSWORD="…"
export KEY_ALIAS="upload"
export KEY_PASSWORD="…"

./gradlew assembleRelease
```

Ergebnis prüfen:

```bash
# Signatur – hier darf NICHT "CN=Android Debug" stehen
"$ANDROID_HOME"/build-tools/35.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-release.apk

# Name, Paket-ID und Version aus der fertigen APK
"$ANDROID_HOME"/build-tools/35.0.0/aapt2 dump badging \
  app/build/outputs/apk/release/app-release.apk | grep -E "^package|application-label"
```

Das Gegenstück zum PR-Job:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

---

## 6. Troubleshooting

### `gradle-wrapper.jar` fehlt

**Symptom:** `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`

**Ursache:** `gradle/wrapper/gradle-wrapper.jar` fehlt im Repository — meist,
weil eine `.gitignore`-Regel wie `*.jar` sie ausschließt.

```bash
git ls-files gradle/wrapper/     # muss BEIDE Dateien zeigen
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
git add -f gradle/wrapper/gradle-wrapper.jar
```

Die `.gitignore` dieses Projekts enthält dafür bereits eine Ausnahme
(`!gradle/wrapper/gradle-wrapper.jar`).

---

### Falsche JDK-Version

**Symptom:** `Unsupported class file major version 6x` oder
`Android Gradle plugin requires Java 17 to run.`

**Ursache:** AGP 8.7.3 verlangt JDK 17; die Module kompilieren mit
`jvmTarget = 17`.

Im Workflow steht die Version zentral in `env.JAVA_VERSION`. Lokal:

```bash
java -version          # muss 17 oder neuer melden
export JAVA_HOME=/pfad/zu/jdk-17
```

JDK 21 funktioniert lokal ebenfalls, weil `sourceCompatibility` und
`jvmTarget` explizit auf 17 stehen.

---

### Keystore-Passwort passt nicht

**Symptom:** `keystore password was incorrect` oder `Cannot recover key`

1. **Store- und Key-Passwort verwechselt.** `KEYSTORE_PASSWORD` ist
   `-storepass`, `KEY_PASSWORD` ist `-keypass` — bei `keytool` zwei
   verschiedene Werte, auch wenn sie oft gleich gesetzt werden.
2. **Alias falsch.** Vorhandene Aliase auflisten:

   ```bash
   keytool -list -v -keystore release.jks | grep "Alias name"
   ```

3. **Zeilenumbrüche im Base64-Secret.** Ohne `-w0` enthält das Secret Umbrüche
   und die dekodierte Datei ist beschädigt:

   ```bash
   base64 -w0 release.jks | md5sum   # mit dem Secret-Inhalt vergleichen
   ```

4. **Leerzeichen beim Einfügen** ins Secret-Feld — führendes oder
   abschließendes Whitespace zerstört den Wert.

Der Schritt „Signatur prüfen" fängt den Fall ab, dass der Build trotz gesetzter
Secrets still auf die Debug-Signatur zurückfällt.

---

### Out of Memory auf dem Runner

**Symptom:** `Java heap space`, `GC overhead limit exceeded`, oder Exit-Code
137 / `The runner has received a shutdown signal`.

Aktuelle Einstellung in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx3g -XX:MaxMetaspaceSize=1g -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

Wenn es klemmt, in dieser Reihenfolge:

1. Heap erhöhen (auf GitHub-Runnern sind 4–5 GB gefahrlos möglich):

   ```properties
   org.gradle.jvmargs=-Xmx5g -XX:MaxMetaspaceSize=1g
   ```

2. Dem Kotlin-Compiler-Daemon eigenen Speicher geben:

   ```properties
   kotlin.daemon.jvmargs=-Xmx2g
   ```

3. Parallelität reduzieren: `org.gradle.parallel=false`
4. Als letzte Stufe: `./gradlew assembleRelease --no-daemon`

Faustregel: `-Xmx` plus `kotlin.daemon.jvmargs` sollten zusammen deutlich unter
dem RAM des Runners bleiben (ubuntu-latest: 16 GB), sonst greift der
OOM-Killer.

---

### Weitere Stolpersteine

| Symptom | Ursache | Lösung |
| --- | --- | --- |
| `SDK location not found` | `local.properties` fehlt lokal | `sdk.dir=/pfad/zum/android-sdk` eintragen (gitignored; auf dem Runner nicht nötig) |
| `Artifact name is not valid` | Branchname enthält `/` | Der Workflow ersetzt `/` bereits durch `-` |
| `Resource not accessible by integration` | Job ohne Schreibrechte | `permissions: contents: write` im Job `release` prüfen |
| App zeigt Hinweisbildschirm | `webapp/index.html` fehlt | Datei hochladen, Kleinschreibung beachten |
| Weiße Seite in der App | absolute Pfade wie `/app.js` | auf relative Pfade umstellen |
| `apkcreator.json` wirkt nicht | JSON-Syntaxfehler | Build-Log zeigt `[apkcreator] apkcreator.json ist fehlerhaft`; Kommas und Anführungszeichen prüfen |
