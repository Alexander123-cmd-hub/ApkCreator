# ApkCreator

**Aus einer Webseite wird eine echte Android-App — ohne Android Studio, ohne
Programmierkenntnisse, ohne Installation. Alles läuft im Browser, auch auf dem
Handy.**

Du lädst deine Dateien hoch, trägst einen Namen ein und startest den Build.
Ein paar Minuten später liegt eine fertige `.apk` zum Download bereit, die du
installieren oder weitergeben kannst.

---

## Was du brauchst

Nur einen GitHub-Account. Wirklich sonst nichts — kein PC, kein Java, kein
Android Studio. Die Umwandlung passiert auf GitHubs Servern.

---

## In 4 Schritten zur eigenen App

### 1. Repository kopieren

Klicke oben rechts auf **Fork**. Du bekommst eine eigene Kopie, in der du alles
ändern darfst.

### 2. Deine Dateien hochladen

Öffne den Ordner **`webapp/`** und ersetze den Beispielinhalt durch deine
eigenen Dateien.

Direkt auf GitHub: Ordner öffnen → **Add file** → **Upload files** → Dateien
auswählen → **Commit changes**. Das geht auch am Handy.

Wichtig ist nur: Es muss eine **`index.html`** geben. Alles andere — CSS,
JavaScript, Bilder, Schriften, Unterordner — kommt einfach mit.

### 3. Namen und Icon festlegen

Bearbeite **`apkcreator.json`** (Stift-Symbol antippen):

```json
{
  "appName": "Meine App",
  "packageId": "de.meinname.meineapp",
  "versionName": "1.0.0"
}
```

Für ein eigenes Icon lädst du eine quadratische PNG-Datei als
**`branding/icon.png`** hoch (mindestens 512 × 512 Pixel). Ohne eigenes Icon
wird ein neutrales Standard-Icon verwendet.

### 4. APK bauen lassen

Sobald du etwas hochlädst, startet der Build automatisch. Du findest das
Ergebnis unter **Actions** → oberster Eintrag → ganz unten bei **Artifacts**.

> **Hinweis:** GitHub packt Artefakte immer in eine ZIP-Datei. Einmal entpacken,
> dann die `.apk` antippen und installieren. Beim ersten Mal fragt Android nach
> der Erlaubnis „Unbekannte Apps installieren" — das ist normal bei Apps, die
> nicht aus dem Play Store kommen.

---

## Was kann rein — und was nicht?

ApkCreator verpackt **Web-Inhalte**. Das ist mehr, als es klingt: Alles, was in
einem Browser läuft, läuft auch hier.

| Funktioniert | Funktioniert **nicht** |
| --- | --- |
| HTML, CSS, JavaScript | Python-, Java- oder C-Programme |
| Bilder, Videos, Schriften, Icons | Fertige `.exe`- oder `.jar`-Dateien |
| Fertige Web-Apps (React, Vue, Svelte …) | Programme, die eine Desktop-Oberfläche brauchen |
| Spiele mit Canvas oder WebGL | |
| `localStorage`, `fetch`, ES-Module | |

**Bei React, Vue oder Angular:** Lade nicht den Quellcode hoch, sondern das
Ergebnis von `npm run build` — also den Inhalt des Ordners `dist/` oder
`build/`. Achte darauf, dass dein Build-Tool **relative Pfade** erzeugt
(bei Vite z. B. `base: './'` in der `vite.config.js`).

Deine Dateien werden übrigens nicht über `file://` geladen, sondern über einen
eingebauten Mini-Server. Dadurch funktionieren ES-Module, `fetch()` und
`localStorage` genauso wie im echten Browser — anders als bei vielen einfachen
WebView-Wrappern.

---

## Alle Einstellungen

Alles in `apkcreator.json`. Weglassen ist erlaubt — dann gilt der Standardwert.

| Schlüssel | Bedeutung | Standard |
| --- | --- | --- |
| `appName` | Name unter dem Icon | `Meine App` |
| `packageId` | Eindeutige Kennung, z. B. `de.name.app`. Kleinbuchstaben, mindestens ein Punkt, keine Umlaute | `de.meinefirma.meineapp` |
| `versionName` | Sichtbare Version, z. B. `1.0.0` | `1.0.0` |
| `versionCode` | Interne Nummer, muss bei jedem Update steigen | `1` |
| `startUrl` | Startseite innerhalb von `webapp/` | `index.html` |
| `iconBackgroundColor` | Farbe hinter dem Icon, als Hex | `#2E6A4F` |
| `orientation` | `unspecified`, `portrait` oder `landscape` | `unspecified` |
| `openExternalLinksInBrowser` | Externe Links im Browser statt in der App öffnen | `true` |

> **Die `packageId` ist die Identität deiner App.** Änderst du sie später, gilt
> das für Android als völlig neue App — ein Update ist dann nicht mehr möglich,
> die alte muss deinstalliert werden. Überlege sie dir also einmal in Ruhe.

### Ohne Dateien bearbeiten

Du kannst Name, Paket-ID und Version auch direkt beim Start eintippen:
**Actions** → **APK bauen** → **Run workflow**. Was du dort einträgst, gilt nur
für diesen einen Build. Leer gelassene Felder nehmen die Werte aus
`apkcreator.json`.

---

## Zwei Arten von APK

| | Debug | Release |
| --- | --- | --- |
| Wofür | schnell ausprobieren | weitergeben, veröffentlichen |
| Größe | ~9 MB | ~0,8 MB |
| Signatur | automatisch | dein eigener Schlüssel |
| Play Store | nein | ja |
| Einrichtung nötig | nein | einmalig, ~5 Minuten |

Die Debug-Variante entsteht bei jedem Upload automatisch. Sie trägt den Zusatz
„Debug" im Namen und lässt sich parallel zur Release-Version installieren.

Für die kleinere, signierte Release-Variante brauchst du einmalig einen
Signaturschlüssel. Wie das geht, steht in **[docs/CI.md](docs/CI.md)** —
inklusive fertiger Befehle zum Kopieren.

---

## Neue Version veröffentlichen

Wenn der Signaturschlüssel eingerichtet ist:

1. **Releases** → **Draft a new release**
2. Bei *Choose a tag* eine Version eintippen, z. B. `v1.0.1`, dann
   **Create new tag on publish**
3. **Publish release**

Nach wenigen Minuten hängt die fertige, signierte APK direkt am Release — ohne
ZIP, ein Tap zum Download. Die Release Notes entstehen automatisch aus deinen
Änderungen.

---

## Wenn etwas nicht klappt

| Problem | Ursache und Lösung |
| --- | --- |
| App zeigt „Noch keine App hochgeladen" | Es fehlt `webapp/index.html`. Auf exakte Kleinschreibung achten. |
| Weiße Seite nach dem Start | Meist absolute Pfade wie `/assets/app.js`. Auf relative Pfade umstellen (`assets/app.js`). |
| Build ist rot | **Actions** → fehlgeschlagenen Lauf öffnen → roter Schritt zeigt den Grund im Klartext. |
| Icon sieht abgeschnitten aus | Motiv mittig platzieren und rundherum Luft lassen — Android beschneidet Icons je nach Gerät. |
| Update lässt sich nicht installieren | Die `packageId` wurde geändert. Alte App deinstallieren oder alte ID zurücksetzen. |

Ausführlicher — inklusive Signatur-, Speicher- und JDK-Problemen — in
**[docs/CI.md](docs/CI.md)**.

---

## Wie es aufgebaut ist

Für alle, die es genauer wissen wollen:

| Modul | Inhalt |
| --- | --- |
| `:app` | Android-App, WebView-Container, Build- und Signaturlogik |
| `:core:designsystem` | Material-3-Theme |
| `:core:data` | Erkennung der mitgelieferten Web-App |

| Baustein | Version |
| --- | --- |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.0.21 |
| Gradle | 8.11.1 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 (Android 8.0) |

Lokal bauen, falls du doch einen Rechner hast:

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew assembleDebug
```

---

## Lizenz

[MIT](LICENSE) — nutze es privat oder kommerziell, ganz wie du magst.

Die Apps, die du damit baust, gehören selbstverständlich dir. ApkCreator
verlangt keine Nennung und baut nichts in deine App ein.
