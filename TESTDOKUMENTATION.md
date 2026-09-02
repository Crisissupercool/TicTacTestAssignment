# Testdokumentation – TicTacToe (M450)

## 1. Setup

Das Projekt wird mit Gradle gebaut. JUnit 5 (Jupiter) und AssertJ sind in
[`build.gradle`](build.gradle) als Test-Abhängigkeiten eingebunden:

```groovy
dependencies {
	testImplementation 'org.assertj:assertj-core:3.27.7'
	testImplementation 'org.junit.jupiter:junit-jupiter:6.1.3'
	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}

test {
	useJUnitPlatform()
	testLogging {
		events 'passed', 'skipped', 'failed'
	}
}
```

Tests ausführen:

```bash
./gradlew test
```

Der HTML-Report liegt danach unter `build/reports/tests/test/index.html`.

## 2. Dummy-Tests

In [`DummyTest.java`](src/test/java/ch/bbw/m450/tictactoe/DummyTest.java) wird nur geprüft,
dass beide Frameworks korrekt eingebunden sind – es wird kein Produktivcode getestet.

| Test | Framework | Inhalt |
| --- | --- | --- |
| `GIVEN_junitOnClasspath_WHEN_plainAssertionsEvaluated_THEN_theyHold` | JUnit 5 | `assertFalse(...)`, `assertTrue(...)`, `assertEquals(2, 1 + 1)` |
| `GIVEN_assertJOnClasspath_WHEN_fluentAssertionsEvaluated_THEN_theyHold` | AssertJ | `assertThat(false).isFalse()`, `assertThat("tic-tac-toe").startsWith("tic")...` |

> Hinweis: Die Aufgabenstellung nennt als Beispiel `assertFalse(true)`. Dieser Ausdruck
> würde absichtlich fehlschlagen. Damit der Build grün bleibt (siehe Screenshot), sind die
> Dummy-Assertions so formuliert, dass sie erfüllt sind – die Aussage "das Framework läuft"
> bleibt dieselbe.

## 3. Die 5 Tests nach dem GIVEN-WHEN-THEN-Pattern

Alle Tests befinden sich in
[`TicTacToeMainTest.java`](src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java).
`E` steht in den Board-Arrays für ein leeres Feld (`null`).

Das GIVEN_WHEN_THEN-Pattern ist dreifach umgesetzt:

1. im **Methodennamen** – `GIVEN_<Ausgangslage>_WHEN_<Aktion>_THEN_<Erwartung>`
2. im **`@DisplayName`** – derselbe Satz ausgeschrieben, so erscheint er im Test-Report
3. im **Methodenrumpf** – die drei Abschnitte sind mit `// GIVEN`, `// WHEN`, `// THEN` kommentiert

Das Spielfeld ist als eindimensionales Array der Länge 9 organisiert:

```
 0 | 1 | 2
---+---+---
 3 | 4 | 5
---+---+---
 6 | 7 | 8
```

---

### Test 1 – `GIVEN_middleRowOfCrosses_WHEN_isWinForCross_THEN_returnsTrue`

| | |
| --- | --- |
| **GIVEN** | ein Board, auf dem CROSS die komplette mittlere Reihe (Felder 3, 4, 5) besetzt: `{CIRCLE, CIRCLE, E, CROSS, CROSS, CROSS, E, E, E}` |
| **WHEN** | `TicTacToeMain.isWin(board, CROSS)` aufgerufen wird |
| **THEN** | ist das Resultat `true` – die Reihe wird als Sieg erkannt |

**Getestete Anforderung:** `isWin` erkennt horizontale Gewinnlinien.

---

### Test 2 – `GIVEN_antiDiagonalOfCircles_WHEN_isWinForCircle_THEN_returnsTrue`

| | |
| --- | --- |
| **GIVEN** | ein Board, auf dem CIRCLE die Felder 2, 4 und 6 besetzt: `{CROSS, CROSS, CIRCLE, E, CIRCLE, E, CIRCLE, E, E}` |
| **WHEN** | `TicTacToeMain.isWin(board, CIRCLE)` aufgerufen wird |
| **THEN** | ist das Resultat `true` – auch die Diagonale wird als Sieg erkannt |

**Getestete Anforderung:** `isWin` erkennt diagonale Gewinnlinien und funktioniert für beide Farben.

---

### Test 3 – `GIVEN_mixedLineAndEmptyBoard_WHEN_isWinForBothColours_THEN_returnsFalse`

| | |
| --- | --- |
| **GIVEN** | ein Board mit gemischter oberer Reihe `{CROSS, CIRCLE, CROSS, E, E, E, E, E, E}` sowie ein komplett leeres Board |
| **WHEN** | `isWin` für beide Boards und beide Farben aufgerufen wird |
| **THEN** | liefern alle vier Aufrufe `false` |

**Getestete Anforderung:** Negativfall – `isWin` meldet keinen Sieg, wenn keiner vorliegt.
Insbesondere darf ein leeres Board (lauter `null`) nicht fälschlicherweise als Sieg gelten.

---

### Test 4 – `GIVEN_singlePlayerInstance_WHEN_playStartedForBothColours_THEN_throwsIllegalArgumentException`

| | |
| --- | --- |
| **GIVEN** | eine einzige `GreedyPlayer`-Instanz |
| **WHEN** | `TicTacToeMain.play(player, player)` mit dieser Instanz für beide Farben aufgerufen wird |
| **THEN** | wird eine `IllegalArgumentException` mit der Meldung `"players must differ"` geworfen |

**Getestete Anforderung:** Vorbedingung der Methode `play` – die beiden Spieler müssen verschieden sein.

---

### Test 5 – `GIVEN_twoGreedyPlayers_WHEN_fullGameIsPlayed_THEN_crossWins`

| | |
| --- | --- |
| **GIVEN** | zwei verschiedene `GreedyPlayer`-Instanzen, die immer das am weitesten oben-links liegende freie Feld belegen |
| **WHEN** | mit `TicTacToeMain.play(xPlayer, oPlayer)` eine komplette Partie gespielt wird |
| **THEN** | ist der Rückgabewert `Stone.CROSS` |

**Begründung:** Beide Spieler wählen immer das kleinste freie Feld. Der Spielverlauf ist
deshalb deterministisch: X=0, O=1, X=2, O=3, X=4, O=5, X=6. Nach dem 7. Zug hält CROSS
die Felder 2, 4 und 6 – die Gegendiagonale – und gewinnt.

**Getestete Anforderung:** Integrationstest der kompletten Spielschleife inkl. abwechselnder
Züge, Siegerkennung und Rückgabewert.

---

## 4. Link zum Test-Code auf GitHub

Repository: <https://github.com/Crisissupercool/TicTacTestAssignment>

| Datei | Link |
| --- | --- |
| Die 5 TicTacToe-Tests | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java> |
| Dummy-Tests (JUnit + AssertJ) | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/DummyTest.java> |
| Build-Konfiguration | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/build.gradle> |

## 5. Screenshot: alle Tests erfolgreich

![Alle Tests erfolgreich](docs/screenshot-tests.png)

Erzeugt mit `./gradlew test`. Konsolenausgabe des Laufs:

```
> Task :compileTestJava
> Task :testClasses

> Task :test

DummyTest > GIVEN JUnit 5 on the test classpath WHEN plain assertions are evaluated THEN they hold PASSED

DummyTest > GIVEN AssertJ on the test classpath WHEN fluent assertions are evaluated THEN they hold PASSED

TicTacToeMainTest > GIVEN two greedy players WHEN a full game is played THEN CROSS wins PASSED

TicTacToeMainTest > GIVEN a board with a middle row of crosses WHEN isWin is called for CROSS THEN it returns true PASSED

TicTacToeMainTest > GIVEN a board with circles on the anti-diagonal WHEN isWin is called for CIRCLE THEN it returns true PASSED

TicTacToeMainTest > GIVEN one single player instance WHEN play is started with it for both colours THEN an IllegalArgumentException is thrown PASSED

TicTacToeMainTest > GIVEN a mixed line and an empty board WHEN isWin is called for both colours THEN it returns false PASSED

BUILD SUCCESSFUL in 2s
3 actionable tasks: 3 executed
```

**7 Tests, 0 Fehler, 0 übersprungen.**
