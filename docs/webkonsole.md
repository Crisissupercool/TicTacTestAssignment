# TicTacTest im Browser – Web-Konsole (Auftrag 1)

```
Browser ──WebSocket──► ttyd ──Pseudo-Terminal──► java -jar tictactest-X.Y.Z.jar
                       (ein Prozess pro Verbindung)
```

## Werkzeug: ttyd

| Kriterium | ttyd | GoTTY | websocketd |
| --- | --- | --- | --- |
| echtes Terminal (PTY) | ja | ja | nein, nur Textzeilen |
| Browser-Terminal enthalten (xterm.js) | ja | ja | nein, eigene HTML/JS-Seite nötig |
| ANSI-Farben des Spiels | funktionieren | funktionieren | müssten selbst umgesetzt werden |
| Pflege, Installation | aktiv, statisches Binary | wenig gepflegt | aktiv |

**Wahl: ttyd.** Das Spiel gibt ANSI-Farben aus und liest Zeilen von der Tastatur. Mit einem echten Terminal läuft das Jar unverändert. Bei websocketd wäre zusätzlicher Code nötig.

Wie es funktioniert: Pro Browser-Verbindung startet ttyd einen neuen Prozess (hier die JVM), hängt ihn an ein Pseudo-Terminal und leitet Ein- und Ausgabe über einen WebSocket zum Terminal im Browser (xterm.js).

## Lokal starten – nur aus Release-Assets

```bash
# Assets eines Releases herunterladen (nicht lokal bauen)
gh release download v1.2.0 --repo Crisissupercool/TicTacTestAssignment

# Web-Konsole: ttyd startet das Jar und sonst nichts
ttyd -W -i lo -p 7681 -m 5 java -jar tictactest-1.2.0.jar
```

ttyd läuft unter Linux, macOS, WSL und im DevContainer (`apk add ttyd` bzw. Download von github.com/tsl0922/ttyd), unter Windows als `ttyd.win32.exe`. Danach `http://localhost:7681` öffnen.

| Option | Wirkung |
| --- | --- |
| `-W` | **nötig zum Tippen**: ohne diese Option ist das Terminal nur lesbar |
| `-i lo` | lauscht nur auf localhost |
| `-p 7681` | Port |
| `-m 5` | höchstens 5 gleichzeitige Verbindungen |
| *kein* `-a` | URL-Parameter dürfen den Befehl nicht erweitern |

## Verhalten (aus dem Code abgeleitet, beim Ausprobieren bestätigen)

| Frage | Antwort |
| --- | --- |
| Spiel zu Ende? | `main()` endet, der Prozess ist weg, ttyd schliesst die Verbindung. |
| Ungültige Eingabe? | `Integer.parseInt` wirft bei «abc» eine `NumberFormatException`, ein besetztes oder ungültiges Feld eine `IllegalStateException`. Das Spiel bricht mit Stacktrace ab. |
| Neues Spiel? | Seite neu laden: neue Verbindung, neuer Prozess, neue Partie. |
| Zwei Tabs? | Zwei Verbindungen, **zwei Java-Prozesse** (`ps aux | grep java`). |
| 1 GB RAM? | Jede JVM braucht je nach Einstellung 50–150 MB. Deshalb `-Xmx64m` und `-m 5`, sonst reichen wenige Besucher, um den Server in den Swap zu treiben. |

## Sicherheit

Die URL ist für alle erreichbar, die sie kennen. Startet die Web-Konsole eine **Shell** (`bash`, `sh`, `login`), bekommt jeder Besucher eine Kommandozeile auf dem Server: beliebigen Code ausführen, Daten lesen, Schadsoftware starten. Läuft das als root, gehört dem Angreifer die ganze Instanz. Zusätzlich könnte er über den Metadaten-Dienst die AWS-Rechte der Instanz abgreifen und damit weiter ins Konto.

Was wir dagegen tun:

- ttyd startet **nur** `java -jar …`, nie eine Shell, und kein `-a`.
- Der Dienst läuft als eigener Benutzer `tictactest`, nicht als root (`NoNewPrivileges`, `ProtectSystem=strict`).
- ttyd lauscht nur auf `127.0.0.1`, von aussen erreichbar ist nur nginx auf Port 80.
- IMDSv2 ist Pflicht, die Security Group lässt nur Port 80 zu.
- `-m 5` und `-Xmx64m` begrenzen den Ressourcenverbrauch.
- Bekannte Grenze: Es gibt keine Anmeldung und kein HTTPS. Für ein öffentliches Spiel ohne Daten ist das vertretbar, für etwas Vertrauliches nicht.

## Versionsabfrage

`GET http://<Instanz>/version.txt` liefert die laufende Version als Text.

Die Versionsnummer kommt **aus dem Release-Tag** (`v1.2.0`): Der Release-Workflow gibt sie beim Bauen mit (`-PreleaseVersion=1.2.0`), sie steckt im Dateinamen `tictactest-1.2.0.jar` und im Manifest (`Implementation-Version`). Das Deployment lädt genau diese Datei und schreibt die Version nach `version.txt`, die nginx ausliefert. Nichts wird von Hand gepflegt. Das Jar selbst bleibt unverändert.

Lokal ersetzt `python3 scripts/serve_version.py 1.2.0` den nginx (liefert `/version.txt` auf Port 8081, mit CORS-Header).

## Frontend

Ordner [`frontend/`](../frontend): `index.html`, `style.css`, `app.js`, `config.js`, `version.txt`. Plain HTML/CSS/JS, kein Framework, kein Build.

**Konfiguration statt fester URL:** `index.html` lädt zuerst `config.js`, die `window.TICTACTEST_CONFIG` setzt (`consoleUrl`, `versionUrl`). `app.js` liest nur diese Werte.

| Umgebung | Woher `config.js` |
| --- | --- |
| lokal | Datei im Repository (`localhost:7681`) |
| Cloud | OpenTofu schreibt sie in den Bucket (Adresse der Elastic IP) |

Das Release-Archiv enthält **kein** `config.js`.

Lokal mit zwei Servern testen:

```bash
ttyd -W -i lo -p 7681 java -jar tictactest-1.2.0.jar     # Web-Konsole
python3 scripts/serve_version.py 1.2.0                   # Version (Port 8081)
cd frontend && python3 -m http.server 8000               # Frontend → http://localhost:8000
```

Wird die Backend-Version nicht angezeigt, steht der Grund in der Entwickler-Konsole des Browsers (Tab «Konsole»/«Netzwerk»). Typisch ist ein CORS-Fehler, wenn die Version von einer anderen Origin ohne `Access-Control-Allow-Origin` kommt.

## Release

`git tag v1.2.0 && git push origin v1.2.0` startet [`release.yml`](../.github/workflows/release.yml): Tests, `tictactest-1.2.0.jar` und `tictactest-frontend-1.2.0.zip` werden als Assets an den GitHub-Release gehängt.

| Datei | Im Release? | Begründung |
| --- | --- | --- |
| Jar, Frontend-Archiv | ja | entstehen aus dem Code und ändern sich mit ihm |
| Start-Skript, systemd-Unit, nginx-Konfiguration | **nein, Infrastruktur** | beschreiben, *wie und wo* gestartet wird, nicht *was*. Sie hängen von der Umgebung ab, ändern sich unabhängig vom Code und werden von OpenTofu (`user_data`) eingerichtet |
| `config.js` | nein, Infrastruktur | gehört zur Umgebung |

**Warum das Frontend als Asset, obwohl es nur ein paar Dateien sind?** Es ist dann unveränderlich und versioniert (nachvollziehbar, was wann live war), das Deployment holt dasselbe geprüfte Artefakt statt neu zu bauen, ein Rollback ist «Version X nochmal deployen», und dieselbe Datei lässt sich später in weitere Umgebungen weiterreichen.

## Zu bestätigen beim Ausprobieren

- [ ] Jar aus dem Release läuft in ttyd, Partie im Browser gespielt
- [ ] Verhalten bei Spielende, ungültiger Eingabe und zwei Tabs wie oben beschrieben
- [ ] Frontend lokal: Terminal eingebettet, beide Versionen sichtbar
- [ ] Alles nur aus Release-Assets gestartet
