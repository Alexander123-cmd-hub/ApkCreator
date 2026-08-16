# Icon

Lege hier eine Datei namens **`icon.png`** ab — sie wird beim Bauen
automatisch zum Launcher-Icon deiner App.

## Anforderungen

| Punkt | Empfehlung |
| --- | --- |
| Dateiname | exakt `icon.png` (Kleinschreibung) |
| Format | PNG |
| Größe | mindestens 512 × 512 Pixel, quadratisch |
| Hintergrund | darf transparent sein |

## Gut zu wissen

Android schneidet Launcher-Icons je nach Gerät rund, quadratisch oder als
Tropfen zu. Der Build rückt dein Bild deshalb automatisch etwas ein, damit
vom Motiv nichts abgeschnitten wird.

Die Fläche hinter dem Motiv füllt die Farbe aus `iconBackgroundColor` in
`apkcreator.json`. Wähle sie passend zu deinem Icon — bei einem Icon mit
transparentem Rand sieht man sie rundherum.

**Ohne `icon.png` hier** wird ein neutrales Standard-Icon verwendet. Die App
lässt sich also auch ohne eigenes Icon bauen.
