# Infrastruktur und Deployment (Auftrag 2 und 3)

```
                  Release (Tag v*)  ──►  GitHub-Release: Jar + Frontend-Zip
                                              │ Deploy-Workflow (nur geänderte Teile)
        ┌─────────────────────────────────────┴──────────────────────┐
        ▼ Frontend: aws s3 sync                                      ▼ Backend: aws ssm send-command
   S3-Bucket (Static Website)                                    EC2-Instanz (Elastic IP)
   config.js ← OpenTofu                                          nginx :80 ─► ttyd :7681 ─► java -jar
```

| Ebene | Verantwortlich | Wann |
| --- | --- | --- |
| Infrastruktur (Instanz, IP, Bucket, Dienst) | OpenTofu, Ordner [`infra/`](../infra) | lokal mit `tofu apply`, selten |
| Applikation (Jar, Frontend) | Deploy-Workflow / `tictactest-deploy` | bei jedem Release |

## Aufbau von `infra/`

| Datei | Inhalt |
| --- | --- |
| `versions.tf`, `variables.tf` | Provider, Region, Instanztyp, ttyd-Version und -Prüfsumme |
| `main.tf` | Instanz (`LabInstanceProfile` über `data`), Security Group, Elastic IP, S3-Bucket mit Website, `config.js` |
| `outputs.tf` | Adressen und IDs |
| `user_data.sh.tftpl`, `files/` | Einrichtung beim ersten Start: Corretto 25, nginx, ttyd (fest + SHA-256), systemd-Unit, `tictactest-deploy` |

Instanz, Dienst und Deploy-Skript:

- **Dienst:** `tictactest-console.service` läuft als Benutzer `tictactest`, startet beim Booten (`enable`), startet nach Absturz neu (`Restart=always`) und startet nur das Jar `/opt/tictactest/current.jar`.
- **Version nach Neustart:** `current.jar` ist ein Symlink auf `tictactest-X.Y.Z.jar` und `version.txt` bleibt auf der Platte. Nach einem Neustart läuft dasselbe Jar, ohne dass jemand etwas tut.
- **Elastic IP:** feste Adresse, überlebt Stopp und Start der Session.
- **ttyd reproduzierbar:** feste Version 1.7.7, Download mit `sha256sum -c`.

## Entscheidung: Wie kommt das Jar auf die Instanz?

**Die Instanz lädt es selbst vom GitHub-Release** (`tictactest-deploy 1.2.0`, ausgelöst per SSM).

| Variante | Für | Gegen |
| --- | --- | --- |
| **Direkt vom GitHub-Release** (gewählt) | kein zweiter Speicher, kein Kopieren, genau das geprüfte Release-Asset, Rollback = andere Version | Repository muss öffentlich sein |
| Privater S3-Bucket | geht auch bei privatem Repository, Zugriff über Instance Profile | zusätzlicher Bucket, zusätzlicher Kopierschritt |
| Per SSH von aussen | einfach | SSH-Zugang und Schlüssel nötig, in der Pipeline ein weiteres Secret |

Bei einem **privaten Repository** müsste die Instanz einen Token zum Herunterladen haben (ein Secret auf der Instanz). Dann wäre S3 mit Instance Profile besser, weil keine Zugangsdaten auf die Instanz müssen.

**Warum nicht das Jar in `user_data`?** `user_data` läuft nur beim ersten Start. Stünde die Version dort:

- Ein neuer Release ändert `user_data`. Mit `user_data_replace_on_change = true` ersetzt `tofu apply` die **Instanz**: Ausfall, Zustand weg. Das widerspricht der Anforderung, dass ein Release kein `apply` braucht.
- Ohne dieses Flag würde die Änderung nur im State stehen und auf der laufenden Instanz **nichts bewirken** (cloud-init läuft nicht erneut). Das wäre eine unbemerkte Abweichung.

Darum steht die Version nicht in `user_data`. Wir setzen `user_data_replace_on_change = true` trotzdem, aber nur für Änderungen an der *Einrichtung* (Teil 4, Experiment 2): Die Instanz wird neu erstellt, damit die geänderte Einrichtung auch wirklich läuft.

## Security Group

| Regel | Begründung |
| --- | --- |
| Eingehend TCP 80 aus `0.0.0.0/0` | nginx: Web-Konsole (WebSocket) und `/version.txt`, vom Browser aller Besucher |
| Eingehend sonst nichts, **kein SSH (22)** | Befehle auf der Instanz laufen über SSM, eine offene Admin-Schnittstelle weniger |
| Ausgehend alles | Paketquellen, GitHub (ttyd, Jar), SSM |

Der Port 7681 von ttyd ist bewusst **nicht** offen: ttyd lauscht nur auf localhost, nginx ist der einzige Eingang.

## Frontend und Konfiguration

OpenTofu schreibt `config.js` mit der Adresse der Elastic IP in den Bucket (`aws_s3_object.config`). Das Deployment:

- Das Release-Archiv enthält kein `config.js`.
- `aws s3 sync … --delete --exclude config.js` überschreibt die Datei nicht und löscht sie auch nicht, obwohl `--delete` sonst alles entfernt, was nicht im Archiv steht.

Würde das Deployment `config.js` löschen, wüsste das Frontend die Adresse nicht mehr: iframe leer, Version «unbekannt». Der Workflow prüft deshalb nach dem Deployment, dass `config.js` noch ausgeliefert wird.

**Nie in diesen öffentlichen Bucket:** Zugangsdaten, Access Keys, `terraform.tfstate`, Logs, Spielprotokolle, irgendetwas Vertrauliches. Alles darin ist für das Internet lesbar.

## Outputs

| Output | Zweck | vertraulich? |
| --- | --- | --- |
| `play_url` | Spielen | nein, öffentlich |
| `console_url`, `version_url` | Web-Konsole, Versionsabfrage | nein, öffentlich erreichbar |
| `bucket_name`, `instance_id` | Deployment | nicht geheim, aber nicht zum Veröffentlichen gedacht (Angriffsfläche); kein Zugriff ohne Credentials |
| *(State-Datei)* | alle Details | **ja**, nie ins Repository |

Zugangsdaten sind keine Outputs und kommen nirgends im Code vor.

## Deploy von Hand (Vorlage für die Pipeline)

```bash
# 0. Learner-Lab-Credentials aus "AWS Details" als Umgebungsvariablen setzen
export AWS_ACCESS_KEY_ID=…  AWS_SECRET_ACCESS_KEY=…  AWS_SESSION_TOKEN=…

# 1. Infrastruktur
cd infra && tofu init && tofu apply
tofu output

# 2. Release A (Tag setzen, Workflow "Release" abwarten)
git tag v0.1.0 && git push origin v0.1.0

# 3. Backend: Instanz holt das Jar und startet den Dienst neu (SSM statt SSH)
aws ssm send-command --instance-ids "$(tofu output -raw instance_id)" \
  --document-name AWS-RunShellScript \
  --parameters 'commands=["/usr/local/bin/tictactest-deploy 0.1.0"]'

# 4. Frontend: Archiv aus dem Release holen und synchronisieren
gh release download v0.1.0 --pattern 'tictactest-frontend-*.zip' --dir dl
unzip -q dl/tictactest-frontend-*.zip -d site
aws s3 sync site "s3://$(tofu output -raw bucket_name)" --delete --exclude config.js

# 5. prüfen
curl "$(tofu output -raw version_url)"        # → 0.1.0
curl "$(tofu output -raw play_url)/version.txt"
```

Release B: gleich mit `v0.2.0` (Schritt 2 bis 5), **ohne** `tofu apply`. Danach `tofu plan` → *No changes*, weil die Version nirgends in der OpenTofu-Konfiguration steht.

| | SSM | SSH |
| --- | --- | --- |
| Pipeline | nur AWS-Credentials nötig, kein offener Port | Schlüssel als Secret, Port 22 offen oder Runner-IPs freigeben |
| Ausgabe/Fehler | Befehlsausgabe über die API abrufbar | direkt im Log |
| Voraussetzung | SSM-Agent und passende Rechte im Instance Profile (im Learner Lab vorhanden) | Schlüsselpaar, erreichbare Adresse |

Gewählt: SSM.

## Robustheit (Teil 4) – Ergebnisse eintragen

| Experiment | Erwartung | Ergebnis |
| --- | --- | --- |
| Session beenden, neu starten | Elastic IP bleibt, nginx und Dienst starten beim Booten, `current.jar` zeigt auf dieselbe Version | ☐ |
| Einrichtung ändern (z. B. `Environment=` in der Unit) | `tofu plan` zeigt *must be replaced* für die Instanz (`user_data` geändert). Die Elastic IP bleibt, wird neu verknüpft. Das Jar fehlt danach, der Dienst startet nicht, bis neu deployt wird | ☐ |
| `tofu destroy` + `tofu apply` | neue Elastic IP, neuer Bucket-Name, neue Instanz-ID. Version ist `none`, bis neu deployt wird | ☐ |

## Secrets-Konzept (Teil 1 Auftrag 3)

**Was braucht die Pipeline?** AWS-Zugang für SSM und S3: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` aus dem Learner Lab («AWS Details»). Gültig nur für die laufende Session (wenige Stunden). Das Jar holt die Instanz selbst aus dem öffentlichen Release, das Frontend-Archiv der Workflow mit dem automatischen `GITHUB_TOKEN`. Dafür gibt es keine eigenen Zugangsdaten.

| | GitHub **Secrets** | GitHub **Variables** |
| --- | --- | --- |
| Inhalt | verschlüsselt, nach dem Speichern nicht mehr lesbar, in Logs maskiert | im Klartext sichtbar |
| bei uns | `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN` | `AWS_REGION`, `INSTANCE_ID`, `S3_BUCKET`, `VERSION_URL`, `PLAY_URL` |

Variablen setzt man aus den OpenTofu-Outputs:

```bash
gh variable set INSTANCE_ID --body "$(tofu output -raw instance_id)"
gh variable set S3_BUCKET   --body "$(tofu output -raw bucket_name)"
gh variable set VERSION_URL --body "$(tofu output -raw version_url)"
gh variable set PLAY_URL    --body "$(tofu output -raw play_url)"
gh variable set AWS_REGION  --body us-east-1
# Secrets: aus den AWS Details; gh fragt nach dem Wert, nichts landet im Shell-Verlauf
gh secret set AWS_ACCESS_KEY_ID && gh secret set AWS_SECRET_ACCESS_KEY && gh secret set AWS_SESSION_TOKEN
```

**Abgelaufene Credentials:** Der erste Schritt jedes Jobs ruft `aws sts get-caller-identity` auf. Schlägt das fehl, bricht der Workflow sofort mit einer klaren Meldung ab (`::error::AWS-Credentials fehlen oder sind abgelaufen …`), statt später mit einem kryptischen Fehler. Danach: neue Credentials als Secrets speichern und den Workflow **Deploy** manuell mit der Version starten (Actions → Deploy → Run workflow). Die Release-Assets existieren ja schon, es wird nichts neu gebaut.

**Wer sieht oder nutzt die Secrets?** Nur Personen mit Schreibrecht im Repository, und nur Workflows, die sie ausdrücklich verwenden. Der Deploy-Workflow läuft nur nach einem Release-Tag oder per `workflow_dispatch`, nie bei Pull Requests. Workflows aus **Forks** und Pull Requests von Forks erhalten keine Secrets. In **Logs** werden Secrets als `***` maskiert; abgeleitete Werte (z. B. Base64) nicht, deshalb nie ausgeben und kein `set -x` in Schritten mit Secrets. Eingaben (Version) werden vor der Verwendung in der Shell geprüft (`^[0-9]+\.[0-9]+\.[0-9]+$`), sonst wäre Befehlseinschleusung über `workflow_dispatch` möglich.

**Schaden bei einem Leak:** Im Learner Lab begrenzt (Zugang läuft nach Stunden ab, nur das Lab-Konto). In einem echten Konto: fremder Zugriff auf Instanzen, Bucket und Rechnung (Krypto-Mining!). Reaktion: Credentials sofort deaktivieren und rotieren, in CloudTrail prüfen, was geschehen ist, Secret im Repository ersetzen.

**Im Unternehmen:** **OIDC** zwischen GitHub Actions und AWS. Der Workflow erhält von GitHub ein kurzlebiges Identitäts-Token, tauscht es bei AWS gegen eine zeitlich begrenzte Rolle (`sts:AssumeRoleWithWebIdentity`), die nur für dieses Repository, diesen Branch oder dieses Environment gilt. Es gibt dann **keine gespeicherten Schlüssel** mehr, die gestohlen werden oder ablaufen könnten. Im Learner Lab geht das nicht, weil keine eigenen IAM-Rollen erstellt werden dürfen.

## Änderungen erkennen (Teil 2)

Der Workflow vergleicht den **aktuellen Tag mit dem vorherigen Release-Tag** (`git describe --tags --abbrev=0 --match 'v*' <tag>^` und `git diff --name-only`).

| Pfade | gilt als |
| --- | --- |
| `src/main/`, `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew*` | **Backend**: verändert das Jar (auch Build-Dateien) |
| `frontend/` | **Frontend** |
| `src/test/`, `docs/`, `infra/`, `.github/`, `scripts/` | **weder noch**: Tests und Doku ändern kein Artefakt; Infrastruktur gehört zu `tofu apply`; geänderte Workflows liefern nichts aus |

- **Warum der Tag-Vergleich, nicht der letzte Commit?** Zwischen zwei Releases liegen viele Commits. Verglichen mit dem letzten Commit würde nur dessen Änderung erkannt, und ein früherer Commit mit Backend-Änderung ginge verloren.
- **Erster Release:** Es gibt keinen Vorgänger, also werden Backend und Frontend deployt.
- **Warum hilft der `paths`-Filter nicht?** Er vergleicht die Dateien **eines Pushes**. Ein Tag-Push enthält keine Dateiänderungen im Sinne «seit dem letzten Release», und ein Filter kann nicht zwei Tags vergleichen.
- Gezieltes Deployment (`workflow_dispatch`) deployt immer beides.

## Backend und Frontend deployen (Teil 3, 4)

- **Backend:** `aws ssm send-command` ruft `tictactest-deploy <Version>` auf: Jar vom Release laden, **Probestart** (ein kaputtes Jar ersetzt die laufende Version nicht, das Deployment schlägt fehl), Symlink umlegen, `version.txt` schreiben, Dienst neu starten. Danach fragt der Workflow `version.txt` bis zu 2 Minuten lang ab und schlägt fehl, wenn nicht die erwartete Version erscheint. Die Instanz wird nicht neu erstellt.
- **Frontend:** Archiv aus dem Release, `aws s3 sync --delete --exclude config.js`, danach Prüfung von `version.txt` und `config.js` über die öffentliche Adresse.
- **Keine parallelen Deployments:** `concurrency: group: deploy, cancel-in-progress: false`. Ein zweites Deployment wartet.
- **Spieler während des Neustarts?** Ihre Partie bricht ab (ihr Prozess wird beendet), sie müssen die Seite neu laden. Verhinderbar (nur beschreiben): vor dem Neustart warten, bis keine Verbindung mehr offen ist (Draining), oder zwei Instanzen hinter einem Load Balancer abwechselnd aktualisieren (Rolling/Blue-Green).
- **Welche Version nach einem Neustart?** Die, auf die `current.jar` zeigt und die `version.txt` nennt. Beides liegt auf der Platte der Instanz und überlebt Stopp/Start.

## Gezielt eine Version deployen (Teil 5)

Actions → **Deploy** → *Run workflow* → Version (z. B. `1.2.0`). Backend und Frontend dieser Version werden deployt.

| Test | Vorgehen | Ergebnis |
| --- | --- | --- |
| Rollback | v1.3.0 läuft → Deploy mit `1.2.0` | ☐ Frontend und Backend zeigen 1.2.0 |
| Kaputter Release | Tag mit unbrauchbarem Jar | ☐ Probestart meldet den Fehler, Workflow rot, die laufende Version bleibt, Dauer bis wieder alles läuft: sofort (nichts wurde umgestellt) |
| Nach `tofu destroy` + `apply` | die 4 Variablen aus den neuen Outputs setzen (`INSTANCE_ID`, `S3_BUCKET`, `VERSION_URL`, `PLAY_URL`), Secrets erneuern, Deploy mit der aktuellen Version | ☐ |

## Nachweis (Teil 6)

| Release | Erwartung | Workflow-Lauf |
| --- | --- | --- |
| nur Frontend geändert | Frontend neu, Backend-Version unverändert | ☐ Link |
| nur Backend geändert | Backend-Version neu, Frontend nicht deployt | ☐ Link |
| beides | beide deployt | ☐ Link |
| Push auf `main` ohne Tag | nichts deployt | ☐ Link |

## Reflexion und Promotion (Teil 7)

Weg einer Änderung:

```
Backend:   Commit → PR (CI: Tests) → main → Tag vX → Release (Jar) → Deploy → SSM → Probestart → Dienst → Versionsprüfung
Frontend:  Commit → PR → main → Tag vX → Release (Zip) → Deploy → s3 sync → Prüfung von version.txt und config.js
```

**Noch manuell:** `tofu apply`, Learner-Lab-Credentials erneuern, Tags setzen, Variablen nach `destroy` aktualisieren.

**Mehrere Umgebungen (dev/staging/prod):** Dieselbe OpenTofu-Konfiguration als Modul mit Variablen. **Gleich** bleiben Ressourcen, Einrichtung und Dienst. **Unterschiedlich** sind Name/Präfix, Instanzgrösse, Adressen, Credentials und State (je Umgebung ein eigener State oder Workspace).

**Promotion:**

```
Tag v1.4.0 ─► dev (automatisch) ─► Integrationstest ─► staging (automatisch) ─► Smoke-Test ─► prod (Freigabe)
                       dasselbe Artefakt wird weitergereicht, nichts wird neu gebaut
```

- **Dasselbe Artefakt oder neu bauen?** Dasselbe weiterreichen. Was getestet wurde, ist dann genau das, was in Produktion läuft. Ein Neubau könnte (andere Abhängigkeiten, anderer Zeitpunkt) ein anderes Ergebnis liefern.
- **Auslöser:** Tag → dev automatisch; staging nach erfolgreichem Test in dev; prod per manueller Freigabe.
- **Prüfung vor dem nächsten Schritt:** Smoke-Test gegen die Versionsabfrage plus ein Ende-zu-Ende-Test, der eine Partie im Terminal durchspielt (z. B. mit Playwright).
- **Wer darf nach prod?** GitHub **Environments** mit *Required reviewers*, außerdem getrennte Secrets pro Environment.
- **Zusammenspiel mit der Änderungserkennung:** Umgebungen hinken unterschiedlich weit hinterher. Der Vergleich «vorheriger Tag» stimmt dann für prod nicht mehr, weil prod womöglich noch zwei Releases zurückliegt. Besser wäre, **die tatsächlich in der Zielumgebung laufende Version** (aus `version.txt`) als Vergleichsbasis zu nehmen.

## Bekannte Grenzen

- Die Pipeline und die Infrastruktur sind bisher nicht gegen ein echtes AWS-Konto getestet. Die Änderungserkennung und der Probestart wurden lokal geprüft.
- Das Repository muss öffentlich bleiben (Jar-Download ohne Zugangsdaten).
- Der Bucket ist öffentlich lesbar. Verbietet das Konto öffentliche Buckets (Block Public Access auf Kontoebene), scheitert `tofu apply` an der Bucket-Policy.
- Kein HTTPS: Der S3-Websitebetrieb bietet es nicht, und eine HTTPS-Seite dürfte kein `ws://` einbetten.
