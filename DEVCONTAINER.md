# DevContainer – Versionierung und Deployment

Image: `ghcr.io/crisissupercool/tictactestassignment-devcontainer` (GitHub Container Registry)

## Der DevContainer

[`.devcontainer/Dockerfile`](.devcontainer/Dockerfile) basiert auf `eclipse-temurin:25-jdk-alpine`
und enthält Java 25, Gradle 9.7.0 sowie einen Benutzer `vscode` mit **UID:GID 1000:1000**.
JUnit kommt über die Gradle-Abhängigkeiten. [`devcontainer.json`](.devcontainer/devcontainer.json)
aktiviert das Java Extension Pack und die Gradle-Extension für VS Code.

## Ablauf

```
Push auf main mit geändertem .devcontainer/Dockerfile
        │
        ▼
  Workflow "DevContainer"
        ├─ nächste Version bestimmen  (letzter Tag + 1 Patch)
        ├─ Image bauen, als :X.Y.Z und :latest nach GHCR pushen
        ├─ Git-Tag devcontainer-vX.Y.Z setzen
        └─ Pull Request erstellen, der devcontainer.json und
           alle CI-Workflows auf :X.Y.Z hebt
        │
        ▼
  PR mergen → CI und lokale Umgebungen nutzen das neue Image
```

## Versionierungs-Konzept

**SemVer** (`MAJOR.MINOR.PATCH`). Leitfrage: *Muss das Projekt angepasst werden, damit es im
neuen Container noch baut?*

| Stelle | Wann | Beispiel |
| --- | --- | --- |
| MAJOR | nicht abwärtskompatibel | JDK 25 → 26, Alpine → Debian |
| MINOR | neue Funktionalität, kompatibel | Gradle-Update, zusätzliches Tool |
| PATCH | keine funktionale Änderung | Rebuild wegen Sicherheitsupdates |

Die **Patch-Stelle zählt der Workflow automatisch hoch**, ausgehend vom höchsten
vorhandenen Tag `devcontainer-v*`. Ein MAJOR- oder MINOR-Sprung wird gesetzt, indem man
vor dem Push von Hand einen Tag anlegt:

```bash
git tag devcontainer-v2.0.0 && git push origin devcontainer-v2.0.0
```

Der nächste automatische Build macht daraus `2.0.1`.

**Wie unfreigegebene Container verhindert werden:** Gebaut und gepusht wird ausschliesslich
aus `main`. Feature-Branches und Pull Requests erzeugen kein Image. In GHCR liegt damit nur,
was den Review nach `main` überstanden hat. Zusätzlich referenzieren CI und
`devcontainer.json` eine **feste Version**, nie `latest` – ein neues Image wird also erst
verwendet, wenn der Bump-PR gemergt ist.

## Verwendung

**CI** – [`main.yml`](.github/workflows/main.yml) führt den Build im Container aus:

```yaml
    container:
      image: ghcr.io/crisissupercool/tictactestassignment-devcontainer:1.0.0
```

JDK und Gradle kommen aus dem Image, `setup-java` und `setup-gradle` entfallen. Die Version
hält der Bump-PR aktuell.

**Lokal** – in VS Code *Dev Containers: Reopen in Container*. Nach einem `git pull` mit
neuer Version *Rebuild Container*; VS Code lädt das neue Image dann selbst.

## Warum der Path-Filter

Der Workflow reagiert nur auf Änderungen an `.devcontainer/Dockerfile`. Das ist nicht
kosmetisch: Der Bump-PR ändert `devcontainer.json` und die Workflows. Würde der Workflow
auch darauf reagieren, löste sein Merge sofort den nächsten Build samt nächstem PR aus –
eine Endlosschleife.

## Anpassung am Projekt

`gradle/gradle-daemon-jvm.properties` enthielt `toolchainVendor=AZUL`. Gradle akzeptiert
dadurch nur ein Azul-JDK, lädt es über foojay herunter – und dieses Archiv entpackt auf
Alpine (musl) fehlerhaft. Die Zeile ist entfernt; Gradle nutzt jetzt das JDK aus dem
Container. Der Build auf Windows funktioniert unverändert.
