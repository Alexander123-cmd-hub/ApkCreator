# webapp/

**Hier kommt deine App hinein.** Alles in diesem Ordner landet in der fertigen
APK.

## Das Wichtigste

Es muss eine Datei **`index.html`** geben — sie ist die Startseite. Alles
andere (CSS, JavaScript, Bilder, Schriften, Unterordner) nimmst du einfach mit
dazu.

## Hochladen ohne Programme

**Add file** → **Upload files** → Dateien auswählen → **Commit changes**.
Funktioniert auch auf dem Handy. Ganze Ordner kannst du am PC per Drag-and-drop
ablegen.

Die drei Beispieldateien hier (`index.html`, `style.css`, `app.js`) darfst du
löschen, sobald du eigene hochlädst.

## Pfade: der häufigste Stolperstein

Verwende **relative** Pfade:

```html
<!-- richtig -->
<link rel="stylesheet" href="style.css">
<img src="bilder/logo.png">

<!-- führt zu einer weißen Seite -->
<link rel="stylesheet" href="/style.css">
<img src="/bilder/logo.png">
```

Der führende Schrägstrich zeigt auf die Wurzel des Servers, nicht auf deinen
Ordner — die Datei wird dann nicht gefunden.

## Fertige Web-Apps (React, Vue, Svelte, Angular)

Lade **nicht** den Quellcode hoch, sondern das Ergebnis von `npm run build` —
also den Inhalt von `dist/` bzw. `build/`.

Damit die Pfade stimmen, stelle dein Build-Tool auf relative Pfade um:

| Werkzeug | Einstellung |
| --- | --- |
| Vite | `base: './'` in `vite.config.js` |
| Create React App | `"homepage": "."` in `package.json` |
| Angular | `ng build --base-href ./` |

## Was funktioniert

Deine Dateien werden über einen eingebauten Mini-Server ausgeliefert, nicht
über `file://`. Deshalb laufen ES-Module, `fetch()`, `localStorage` und
Service Worker genauso wie im Browser.

Für Inhalte aus dem Internet (Schriften, APIs) ist die nötige Berechtigung
bereits gesetzt.

## Andere Startseite

Heißt deine Startdatei anders oder liegt sie in einem Unterordner, trage den
Pfad in `apkcreator.json` ein:

```json
{ "startUrl": "app/start.html" }
```
