# DevContainer – Versionierung und Deployment

Image: `ghcr.io/crisissupercool/tictactestassignment-devcontainer` (GitHub Container Registry)

## Versionierungs-Konzept

Das Image folgt **SemVer** (`MAJOR.MINOR.PATCH`). Leitfrage: *Muss das Projekt angepasst
werden, damit es im neuen Container noch baut?*

| Stelle | Wann | Beispiel |
| --- | --- | --- |
| MAJOR | nicht abwärtskompatibel | JDK 25 → 26, Alpine → Debian |
| MINOR | neue Funktionalität, kompatibel | Gradle-Update, zusätzliches Tool |
| PATCH | keine funktionale Änderung | Rebuild wegen Sicherheitsupdates |

## Freigabeprozess

Eine Freigabe ist das Setzen eines Git-Tags:

```bash
git tag devcontainer-v1.1.0
git push origin devcontainer-v1.1.0
```

Das löst [`devcontainer.yml`](.github/workflows/devcontainer.yml) aus. Der Workflow baut das
Image, pusht es als `1.1.0` und `latest` nach GHCR und öffnet anschliessend automatisch
einen Pull Request, der die Version in `.devcontainer/devcontainer.json` anhebt.

**Wie unfreigegebene Container verhindert werden:** Der Workflow reagiert ausschliesslich
auf `push: tags: ['devcontainer-v*']`. Ein Push auf einen Branch baut und pusht gar nichts.
In GHCR existieren dadurch nur Images, für die jemand bewusst einen Tag gesetzt hat.

## Verwendung

**CI** – [`main.yml`](.github/workflows/main.yml) führt den Build im Container aus:

```yaml
    container:
      image: ghcr.io/crisissupercool/tictactestassignment-devcontainer:latest
```

`latest` zeigt immer auf die neueste freigegebene Version, also nutzt die CI sie automatisch.
JDK und Gradle kommen aus dem Image, `setup-java` und `setup-gradle` entfallen.

**Lokal** – `.devcontainer/devcontainer.json` referenziert eine feste Version, die der
Bump-PR aktuell hält:

```json
"image": "ghcr.io/crisissupercool/tictactestassignment-devcontainer:1.0.0"
```

In VS Code *Dev Containers: Reopen in Container*. Nach einem `git pull` mit neuer Version
*Rebuild Container* – VS Code lädt das neue Image dann selbst.

## Erstinbetriebnahme

`devcontainer.json` zeigt auf ein Image, das es in GHCR noch nicht gibt. Die erste Freigabe
muss darum von Hand angestossen werden:

```bash
git tag devcontainer-v1.0.0 && git push origin devcontainer-v1.0.0
```

Danach unter *Packages → Package settings* die Sichtbarkeit auf **Public** stellen (oder das
Repo mit Leserecht verknüpfen), sonst kann die CI das Image nicht ziehen.

## Anpassung am Projekt

`gradle/gradle-daemon-jvm.properties` enthielt `toolchainVendor=AZUL`. Gradle akzeptiert
dadurch nur ein Azul-JDK, lädt es über foojay herunter – und dieses Archiv entpackt auf
Alpine (musl) fehlerhaft. Die Zeile ist entfernt; Gradle nutzt jetzt das JDK aus dem
Container. Der Build auf Windows funktioniert unverändert.
