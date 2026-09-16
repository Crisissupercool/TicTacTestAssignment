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

## 3. Die 5 Basis-Tests nach dem GIVEN-WHEN-THEN-Pattern

Diese Tests befinden sich in
[`TicTacToeMainTest.java`](src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java).
Die Boards werden über die Helper-Klasse `BoardFixtures` gebaut; dort steht `E` für ein leeres Feld (`null`).

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

### Helper und Fixtures

Die Tests bauen ihre Boards nicht mehr von Hand, sondern über die Helper-Klasse
[`BoardFixtures.java`](src/test/java/ch/bbw/m450/tictactoe/BoardFixtures.java):

| Helper | Zweck |
| --- | --- |
| `emptyBoard()` | leeres Board der Länge 9 |
| `boardOf(Stone...)` | Board aus genau 9 Feldern (prüft die Länge) |
| `place(board, color, positions...)` | Kopie des Boards mit gesetzten Steinen – das Original bleibt unverändert |
| `middleRowOfCrosses()` | benanntes Fixture-Board für Test 1 |
| `antiDiagonalOfCircles()` | benanntes Fixture-Board für Test 2 |
| `mixedTopRow()` | benanntes Fixture-Board für Test 3 |
| `assertNobodyWins(board)` | Assertion-Helper: prüft `isWin` für beide Farben und meldet im Fehlerfall das Board |

Dazu kommen die JUnit-Fixtures in
[`TicTacToeMainTest.java`](src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java):

* `@BeforeEach setUp()` erzeugt vor **jedem** Test zwei frische `GreedyPlayer`-Instanzen
  (`xPlayer`, `oPlayer`) und leitet `System.out` in einen `ByteArrayOutputStream` um.
  So bleiben die Tests unabhängig voneinander und die Spielausgabe verschmutzt den
  Test-Report nicht – sie kann stattdessen mitgeprüft werden.
* `@AfterEach tearDown()` stellt den ursprünglichen `System.out` wieder her (Teardown).

---

### Test 1 – `GIVEN_middleRowOfCrosses_WHEN_isWinForCross_THEN_returnsTrue`

| | |
| --- | --- |
| **GIVEN** | ein Board, auf dem CROSS die komplette mittlere Reihe (Felder 3, 4, 5) besetzt – aufgebaut vom Helper `BoardFixtures.middleRowOfCrosses()` |
| **WHEN** | `TicTacToeMain.isWin(board, CROSS)` aufgerufen wird |
| **THEN** | ist das Resultat `true` – die Reihe wird als Sieg erkannt |

**Getestete Anforderung:** `isWin` erkennt horizontale Gewinnlinien.

---

### Test 2 – `GIVEN_antiDiagonalOfCircles_WHEN_isWinForCircle_THEN_returnsTrue`

| | |
| --- | --- |
| **GIVEN** | ein Board, auf dem CIRCLE die Felder 2, 4 und 6 besetzt – aufgebaut vom Helper `BoardFixtures.antiDiagonalOfCircles()` |
| **WHEN** | `TicTacToeMain.isWin(board, CIRCLE)` aufgerufen wird |
| **THEN** | ist das Resultat `true` – auch die Diagonale wird als Sieg erkannt |

**Getestete Anforderung:** `isWin` erkennt diagonale Gewinnlinien und funktioniert für beide Farben.

---

### Test 3 – `GIVEN_mixedLineAndEmptyBoard_WHEN_isWinForBothColours_THEN_returnsFalse`

| | |
| --- | --- |
| **GIVEN** | ein Board mit gemischter oberer Reihe (`BoardFixtures.mixedTopRow()`) sowie ein komplett leeres Board (`BoardFixtures.emptyBoard()`) |
| **WHEN** | der Assertion-Helper `assertNobodyWins(board)` für beide Boards `isWin` mit beiden Farben aufruft |
| **THEN** | liefern alle vier Aufrufe `false` |

**Getestete Anforderung:** Negativfall – `isWin` meldet keinen Sieg, wenn keiner vorliegt.
Insbesondere darf ein leeres Board (lauter `null`) nicht fälschlicherweise als Sieg gelten.

---

### Test 4 – `GIVEN_singlePlayerInstance_WHEN_playStartedForBothColours_THEN_throwsIllegalArgumentException`

| | |
| --- | --- |
| **GIVEN** | eine einzige `GreedyPlayer`-Instanz – das Fixture `xPlayer` aus `@BeforeEach` |
| **WHEN** | `TicTacToeMain.play(xPlayer, xPlayer)` mit dieser Instanz für beide Farben aufgerufen wird |
| **THEN** | wird eine `IllegalArgumentException` mit der Meldung `"players must differ"` geworfen |

**Getestete Anforderung:** Vorbedingung der Methode `play` – die beiden Spieler müssen verschieden sein.

---

### Test 5 – `GIVEN_twoGreedyPlayers_WHEN_fullGameIsPlayed_THEN_crossWins`

| | |
| --- | --- |
| **GIVEN** | die beiden Fixtures `xPlayer` und `oPlayer` – zwei verschiedene `GreedyPlayer`-Instanzen, die immer das am weitesten oben-links liegende freie Feld belegen |
| **WHEN** | mit `TicTacToeMain.play(xPlayer, oPlayer)` eine komplette Partie gespielt wird |
| **THEN** | ist der Rückgabewert `Stone.CROSS`, und die aufgezeichnete Konsolenausgabe enthält `"...and the winner is: CROSS"` |

**Begründung:** Beide Spieler wählen immer das kleinste freie Feld. Der Spielverlauf ist
deshalb deterministisch: X=0, O=1, X=2, O=3, X=4, O=5, X=6. Nach dem 7. Zug hält CROSS
die Felder 2, 4 und 6 – die Gegendiagonale – und gewinnt.

**Getestete Anforderung:** Integrationstest der kompletten Spielschleife inkl. abwechselnder
Züge, Siegerkennung und Rückgabewert.

---

## 4. Parameterized Tests

Die vier Tests in
[`TicTacToeMainTest.java`](src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java)
prüfen mit je **einem** Testrumpf viele Board-Konstellationen. Der `name`-Parameter von
`@ParameterizedTest` schreibt das GIVEN_WHEN_THEN-Muster auch für jeden einzelnen Fall
in den Report.

| Test | Quelle | Fälle | Inhalt |
| --- | --- | --- | --- |
| `GIVEN_everyWinningLine_WHEN_isWinIsCalled_THEN_onlyTheOwnerWins` | `@MethodSource("allWinningLines")` | 16 | alle 8 Gewinnlinien (3 Reihen, 3 Spalten, 2 Diagonalen) × beide Farben; geprüft wird zusätzlich, dass der Gegner **nicht** gewinnt |
| `GIVEN_tableOfBoards_WHEN_isWinIsCalled_THEN_expectedWinnerIsReported` | `@CsvSource` | 12 | Tabelle aus Board + erwartetem Gewinner, inkl. leerem Board, gemischter Reihe, "zwei in einer Reihe" und einem vollen Unentschieden-Board |
| `GIVEN_onlyTwoStonesPerLine_WHEN_isWinIsCalled_THEN_nobodyWins` | `@EnumSource(Stone.class)` | 2 | für jede Farbe: 4 Steine auf dem Board, aber nie drei in einer Linie |
| `GIVEN_scriptedGames_WHEN_fullGameIsPlayed_THEN_expectedWinnerIsReturned` | `@MethodSource("scriptedGames")` | 3 | komplette Partien über `play(...)`: Sieg für CROSS, Sieg für CIRCLE und ein Unentschieden (`null`) |

### Board-Notation im `@CsvSource`

Damit die Tabelle lesbar bleibt, werden die Boards als 9-Zeichen-String geschrieben und vom
Helper `BoardFixtures.parse(...)` eingelesen: `X` = CROSS, `O` = CIRCLE, `-` = freies Feld.
Der erwartete Gewinner steht als `X`, `O` oder `-` (niemand) in der zweiten Spalte:

```java
@CsvSource({
        "XXX------, X", // obere Reihe
        "--X-X-X--, X", // Gegendiagonale
        "---------, -", // leeres Board
        "XOXXOOOXX, -"  // volles Board, niemand hat eine Linie
})
```

### Zusätzliche Helper für die Parameterized Tests

| Helper | Zweck |
| --- | --- |
| `BoardFixtures.WINNING_LINES` | alle 8 Gewinnlinien als `int[][]` – die Datenquelle für `allWinningLines()` |
| `BoardFixtures.lineOf(color, line)` | Board, auf dem eine Farbe genau diese Linie besetzt |
| `BoardFixtures.parse("XOX------")` | Board aus der kompakten Notation |
| [`ScriptedPlayer`](src/test/java/ch/bbw/m450/tictactoe/ScriptedPlayer.java) | Spieler, der eine fest vorgegebene Zugfolge abspielt – damit lässt sich `play(...)` in eine exakt bekannte Spielsituation steuern |

## 5. Link zum Test-Code auf GitHub

Repository: <https://github.com/Crisissupercool/TicTacTestAssignment>

| Datei | Link |
| --- | --- |
| TicTacToe-Tests (inkl. Parameterized Tests) | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/TicTacToeMainTest.java> |
| Helper / Fixtures | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/BoardFixtures.java> |
| Scripted-Player (Helper) | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/ScriptedPlayer.java> |
| Dummy-Tests (JUnit + AssertJ) | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/src/test/java/ch/bbw/m450/tictactoe/DummyTest.java> |
| Build-Konfiguration | <https://github.com/Crisissupercool/TicTacTestAssignment/blob/main/build.gradle> |

## 6. Screenshot: alle Tests erfolgreich

![Alle Tests erfolgreich](docs/screenshot-tests.png)

Erzeugt mit `./gradlew test`. Konsolenausgabe des Laufs (gekürzt – die 16 Fälle der
Gewinnlinien und die 12 Fälle der Board-Tabelle sind hier nur je zweimal aufgeführt):

```
> Task :compileTestJava
> Task :testClasses

> Task :test

DummyTest > GIVEN JUnit 5 on the test classpath WHEN plain assertions are evaluated THEN they hold PASSED

DummyTest > GIVEN AssertJ on the test classpath WHEN fluent assertions are evaluated THEN they hold PASSED

TicTacToeMainTest > GIVEN a board with a middle row of crosses WHEN isWin is called for CROSS THEN it returns true PASSED

TicTacToeMainTest > GIVEN a board with circles on the anti-diagonal WHEN isWin is called for CIRCLE THEN it returns true PASSED

TicTacToeMainTest > GIVEN a mixed line and an empty board WHEN isWin is called for both colours THEN it returns false PASSED

TicTacToeMainTest > GIVEN one single player instance WHEN play is started with it for both colours THEN an IllegalArgumentException is thrown PASSED

TicTacToeMainTest > GIVEN two greedy players WHEN a full game is played THEN CROSS wins PASSED

TicTacToeMainTest > GIVEN each of the 8 winning lines for each colour WHEN isWin is called THEN only the owner wins > GIVEN CROSS on the line [0, 1, 2] WHEN isWin is called THEN it returns true PASSED

TicTacToeMainTest > GIVEN each of the 8 winning lines for each colour WHEN isWin is called THEN only the owner wins > GIVEN CIRCLE on the line [2, 4, 6] WHEN isWin is called THEN it returns true PASSED

TicTacToeMainTest > GIVEN a table of board constellations WHEN isWin is called THEN the expected winner is reported > GIVEN the board "XXX------" WHEN isWin is called THEN the winner is "X" PASSED

TicTacToeMainTest > GIVEN a table of board constellations WHEN isWin is called THEN the expected winner is reported > GIVEN the board "XOXXOOOXX" WHEN isWin is called THEN the winner is "-" PASSED

TicTacToeMainTest > GIVEN a colour holding only two stones per line WHEN isWin is called THEN nobody wins > GIVEN CROSS with two stones per line WHEN isWin is called THEN it returns false PASSED

TicTacToeMainTest > GIVEN a colour holding only two stones per line WHEN isWin is called THEN nobody wins > GIVEN CIRCLE with two stones per line WHEN isWin is called THEN it returns false PASSED

TicTacToeMainTest > GIVEN scripted game constellations WHEN a full game is played THEN the expected winner is returned > GIVEN CROSS completes the top row WHEN the game is played THEN the winner is CROSS PASSED

TicTacToeMainTest > GIVEN scripted game constellations WHEN a full game is played THEN the expected winner is returned > GIVEN CIRCLE completes the middle row WHEN the game is played THEN the winner is CIRCLE PASSED

TicTacToeMainTest > GIVEN scripted game constellations WHEN a full game is played THEN the expected winner is returned > GIVEN both fill the board without a line WHEN the game is played THEN the winner is null PASSED

BUILD SUCCESSFUL in 2s
3 actionable tasks: 2 executed, 1 up-to-date
```

**40 Tests, 0 Fehler, 0 übersprungen** – 2 Dummy-Tests, 5 klassische Tests und
33 Fälle aus den 4 Parameterized Tests (16 + 12 + 2 + 3).
