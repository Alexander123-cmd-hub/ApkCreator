# ApkCreator Studio

Die grafische Oberfläche: Dateien auswählen, Angaben eintragen, fertige APK
herunterladen. Kein Terminal, keine Dateien im Repository bearbeiten.

Die Seite ist reines HTML und JavaScript und läuft vollständig im Browser.
Es gibt keinen Server, der deine Dateien sieht — sie gehen direkt von deinem
Gerät an GitHub.

---

## 1. Einmalig einrichten

### GitHub Pages aktivieren

**Settings → Pages** → bei *Source* **Deploy from a branch** wählen →
Branch **`main`**, Ordner **`/docs`** → **Save**.

Nach ein bis zwei Minuten erreichbar unter:

```
https://DEIN-BENUTZERNAME.github.io/ApkCreator/
```

### Zugriffs-Token erstellen

Das Studio muss in deinem Namen Dateien hochladen und den Build starten.
Dafür braucht es ein Token.

1. [Fine-grained Token anlegen](https://github.com/settings/personal-access-tokens/new)
2. *Repository access* → **Only select repositories** → dieses Repository
3. *Permissions → Repository permissions*:

   | Berechtigung | Wert | Wofür |
   | --- | --- | --- |
   | **Contents** | Read and write | Dateien hochladen, Version taggen |
   | **Actions** | Read and write | Build verfolgen, APK herunterladen |

4. **Generate token**, kopieren, im Studio einfügen, **Verbinden**

Das Token wird im `localStorage` deines Browsers abgelegt und ausschließlich
an `api.github.com` gesendet. Mit **Token löschen** entfernst du es wieder.

> **Tipp:** Setze beim Anlegen ein Ablaufdatum. Läuft das Token ab, meldet
> das Studio beim Verbinden `401` — dann einfach ein neues erstellen.

---

## 2. Benutzen

| Schritt | Was passiert |
| --- | --- |
| **Dateien wählen** | Deine `index.html` und alles, was dazugehört. Wählst du einen Ordner, wird die oberste Ebene automatisch entfernt. |
| **Angaben eintragen** | Name, Paket-ID, Version, Ausrichtung, Icon-Farbe, Icon |
| **App bauen** | Alles wird hochgeladen, der Build startet, die APK lädt herunter |

Die Paket-ID wird schon beim Tippen geprüft — ein ungültiger Wert wird rot
markiert, bevor irgendetwas hochgeladen wird.

### Zum Ausprobieren oder zum Weitergeben

| | Ausprobieren | Weitergeben |
| --- | --- | --- |
| Größe | ~9 MB | ~0,8 MB |
| Signatur | automatisch | dein Schlüssel |
| Play Store | nein | ja |
| Voraussetzung | keine | Secrets eingerichtet ([docs/CI.md](CI.md)) |

Bei **Weitergeben** erzeugt das Studio zusätzlich einen Versions-Tag und ein
GitHub Release. Existiert die Version schon, sagt es das und bricht ab —
erhöhe dann die Versionsnummer.

---

## 3. Was im Hintergrund passiert

Nichts Magisches — dieselben Schritte, die du auch von Hand machen könntest:

1. Deine Dateien werden als Git-Blobs hochgeladen
2. Ein neuer Commit ersetzt den Inhalt von `webapp/` und aktualisiert
   `apkcreator.json`. Alte Dateien, die du nicht erneut hochgeladen hast,
   werden entfernt — sonst blieben Reste der vorherigen App in der APK.
3. Der Push löst den Workflow aus
4. Das Studio wartet auf das Ergebnis, lädt das Artefakt und packt die
   APK direkt im Browser aus

Alles landet als normaler Commit in deiner Historie — du kannst also jederzeit
nachsehen oder zurückgehen.

---

## Wenn etwas nicht klappt

| Meldung | Ursache | Lösung |
| --- | --- | --- |
| `401` beim Verbinden | Token falsch oder abgelaufen | Neues Token erstellen |
| `404` beim Verbinden | Benutzer/Repository falsch, oder das Token hat keinen Zugriff auf genau dieses Repository | Beides prüfen |
| `403` beim Hochladen | Token fehlt *Contents: Read and write* | Berechtigungen nachtragen |
| „Es wurde kein Build gestartet" | Actions im Repository deaktiviert | Settings → Actions → *Allow all actions* |
| „Nichts geändert" | Dateien und Einstellungen sind identisch zum Repository | Etwas ändern oder Version erhöhen |
| „Die Version gibt es schon" | Der Tag existiert bereits | Versionsnummer erhöhen |
| Build fehlgeschlagen | Fehler im Projekt | Link „Build auf GitHub öffnen" zeigt den Grund im Klartext |

Die Seite funktioniert in aktuellen Versionen von Chrome, Firefox, Edge und
Safari — auch mobil. Das Auspacken der APK im Browser nutzt `DecompressionStream`;
in sehr alten Browsern schlägt das fehl, dann führt der Link „Build auf GitHub
öffnen" zum Artefakt.
