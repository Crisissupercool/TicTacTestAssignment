package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.BoardFixtures.WINNING_LINES;
import static ch.bbw.m450.tictactoe.BoardFixtures.antiDiagonalOfCircles;
import static ch.bbw.m450.tictactoe.BoardFixtures.assertNobodyWins;
import static ch.bbw.m450.tictactoe.BoardFixtures.emptyBoard;
import static ch.bbw.m450.tictactoe.BoardFixtures.lineOf;
import static ch.bbw.m450.tictactoe.BoardFixtures.middleRowOfCrosses;
import static ch.bbw.m450.tictactoe.BoardFixtures.mixedTopRow;
import static ch.bbw.m450.tictactoe.BoardFixtures.parse;
import static ch.bbw.m450.tictactoe.BoardFixtures.place;
import static ch.bbw.m450.tictactoe.TicTacToeMain.isWin;
import static ch.bbw.m450.tictactoe.TicTacToeMain.play;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CIRCLE;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CROSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

/**
 * Tests for the TicTacToe game logic in {@link TicTacToeMain}.
 * Method names and bodies follow the GIVEN_WHEN_THEN pattern.
 * <p>
 * The boards are built by the helper {@link BoardFixtures}, the players and the captured
 * console output are set up as fixtures in {@link #setUp()} and released in {@link #tearDown()}.
 */
class TicTacToeMainTest {

	/** Fixture: a fresh player for CROSS, recreated before every test. */
	private TicTacToePlayer xPlayer;

	/** Fixture: a fresh player for CIRCLE, recreated before every test. */
	private TicTacToePlayer oPlayer;

	/** Fixture: everything the game prints while a test runs. */
	private ByteArrayOutputStream gameOutput;

	private PrintStream originalOut;

	@BeforeEach
	void setUp() {
		xPlayer = new GreedyPlayer();
		oPlayer = new GreedyPlayer();
		gameOutput = new ByteArrayOutputStream();
		originalOut = System.out;
		System.setOut(new PrintStream(gameOutput, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() {
		System.setOut(originalOut);
	}

	@Test
	@DisplayName("GIVEN a board with a middle row of crosses WHEN isWin is called for CROSS THEN it returns true")
	void GIVEN_middleRowOfCrosses_WHEN_isWinForCross_THEN_returnsTrue() {
		// GIVEN a board where CROSS occupies the whole middle row
		var board = middleRowOfCrosses();

		// WHEN we ask whether CROSS has won
		var crossWins = isWin(board, CROSS);

		// THEN the row is recognised as a win
		assertThat(crossWins).isTrue();
	}

	@Test
	@DisplayName("GIVEN a board with circles on the anti-diagonal WHEN isWin is called for CIRCLE THEN it returns true")
	void GIVEN_antiDiagonalOfCircles_WHEN_isWinForCircle_THEN_returnsTrue() {
		// GIVEN a board where CIRCLE occupies the positions 2, 4 and 6
		var board = antiDiagonalOfCircles();

		// WHEN we ask whether CIRCLE has won
		var circleWins = isWin(board, CIRCLE);

		// THEN the diagonal is recognised as a win
		assertThat(circleWins).isTrue();
	}

	@Test
	@DisplayName("GIVEN a mixed line and an empty board WHEN isWin is called for both colours THEN it returns false")
	void GIVEN_mixedLineAndEmptyBoard_WHEN_isWinForBothColours_THEN_returnsFalse() {
		// GIVEN a top row filled by both colours and a completely empty board
		var mixed = mixedTopRow();
		var empty = emptyBoard();

		// WHEN we check both boards for both colours
		// THEN neither player has won
		assertNobodyWins(mixed);
		assertNobodyWins(empty);
	}

	@Test
	@DisplayName("GIVEN one single player instance WHEN play is started with it for both colours THEN an IllegalArgumentException is thrown")
	void GIVEN_singlePlayerInstance_WHEN_playStartedForBothColours_THEN_throwsIllegalArgumentException() {
		// GIVEN one single player instance (the CROSS fixture)
		// WHEN this instance is used for both colours
		// THEN the game refuses to start
		assertThatThrownBy(() -> play(xPlayer, xPlayer)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("players must differ");
	}

	@Test
	@DisplayName("GIVEN two greedy players WHEN a full game is played THEN CROSS wins")
	void GIVEN_twoGreedyPlayers_WHEN_fullGameIsPlayed_THEN_crossWins() {
		// GIVEN two greedy players (the fixtures), each always taking the most top-left free field

		// WHEN a full game is played
		var winner = play(xPlayer, oPlayer);

		// THEN CROSS wins, because it holds the fields 2, 4 and 6
		assertThat(winner).isEqualTo(CROSS);
		assertThat(gameOutput.toString(StandardCharsets.UTF_8)).contains("...and the winner is: " + CROSS);
	}

	// --- parameterized tests: many board constellations with one test body -------------------

	/**
	 * @return every one of the 8 winning lines, once for each colour (16 cases)
	 */
	static Stream<Arguments> allWinningLines() {
		return Stream.of(WINNING_LINES)
				.flatMap(line -> Stream.of(CROSS, CIRCLE)
						.map(color -> Arguments.of(color, line)));
	}

	@ParameterizedTest(name = "GIVEN {0} on the line {1} WHEN isWin is called THEN it returns true")
	@MethodSource("allWinningLines")
	@DisplayName("GIVEN each of the 8 winning lines for each colour WHEN isWin is called THEN only the owner wins")
	void GIVEN_everyWinningLine_WHEN_isWinIsCalled_THEN_onlyTheOwnerWins(Stone color, int[] line) {
		// GIVEN a board where the colour holds exactly this line
		var board = lineOf(color, line);

		// WHEN we check the board for both colours
		// THEN the owner of the line wins and the opponent does not
		assertThat(isWin(board, color)).as("%s should win the line %s", color, Arrays.toString(line))
				.isTrue();
		assertThat(isWin(board, color.opponent())).as("%s should not win", color.opponent())
				.isFalse();
	}

	@ParameterizedTest(name = "GIVEN the board {0} WHEN isWin is called THEN the winner is {1}")
	@CsvSource({
			// board, expected winner ('X', 'O' or '-' for nobody)
			"XXX------, X", // top row
			"---OOO---, O", // middle row
			"------XXX, X", // bottom row
			"X--X--X--, X", // left column
			"-O--O--O-, O", // middle column
			"--X--X--X, X", // right column
			"O---O---O, O", // diagonal
			"--X-X-X--, X", // anti-diagonal
			"---------, -", // empty board
			"XOX------, -", // mixed top row
			"XX-OO----, -", // two in a row is not enough
			"XOXXOOOXX, -"  // full board, nobody has a line (draw)
	})
	@DisplayName("GIVEN a table of board constellations WHEN isWin is called THEN the expected winner is reported")
	void GIVEN_tableOfBoards_WHEN_isWinIsCalled_THEN_expectedWinnerIsReported(String fields, char expected) {
		// GIVEN a board written in the compact notation
		var board = parse(fields);

		// WHEN we look for the expected winner
		var expectedWinner = switch (expected) {
			case 'X' -> CROSS;
			case 'O' -> CIRCLE;
			default -> null;
		};

		// THEN exactly this colour wins - or nobody at all
		if (expectedWinner == null) {
			assertNobodyWins(board);
		} else {
			assertThat(isWin(board, expectedWinner)).as("%s should win on %s", expectedWinner, fields)
					.isTrue();
			assertThat(isWin(board, expectedWinner.opponent())).as("%s should not win on %s",
					expectedWinner.opponent(), fields)
					.isFalse();
		}
	}

	@ParameterizedTest(name = "GIVEN {0} with two stones per line WHEN isWin is called THEN it returns false")
	@EnumSource(Stone.class)
	@DisplayName("GIVEN a colour holding only two stones per line WHEN isWin is called THEN nobody wins")
	void GIVEN_onlyTwoStonesPerLine_WHEN_isWinIsCalled_THEN_nobodyWins(Stone color) {
		// GIVEN a board where the colour holds 4 fields, but never three of them in a line
		var board = place(emptyBoard(), color, 0, 1, 3, 4);

		// WHEN we check the board for both colours
		// THEN nobody has won
		assertNobodyWins(board);
	}

	/**
	 * @return scripted games together with the colour that is expected to win (`null` on a draw)
	 */
	static Stream<Arguments> scriptedGames() {
		return Stream.of(
				Arguments.of(Named.of("CROSS completes the top row", new int[] {0, 1, 2}), new int[] {3, 4},
						CROSS),
				Arguments.of(Named.of("CIRCLE completes the middle row", new int[] {0, 1, 8}),
						new int[] {3, 4, 5}, CIRCLE),
				Arguments.of(Named.of("both fill the board without a line", new int[] {0, 2, 3, 7, 8}),
						new int[] {1, 4, 5, 6}, null));
	}

	@ParameterizedTest(name = "GIVEN {0} WHEN the game is played THEN the winner is {2}")
	@MethodSource("scriptedGames")
	@DisplayName("GIVEN scripted game constellations WHEN a full game is played THEN the expected winner is returned")
	void GIVEN_scriptedGames_WHEN_fullGameIsPlayed_THEN_expectedWinnerIsReturned(int[] xMoves, int[] oMoves,
			Stone expectedWinner) {
		// GIVEN two players replaying a fixed script of moves
		var scriptedX = new ScriptedPlayer(xMoves);
		var scriptedO = new ScriptedPlayer(oMoves);

		// WHEN the complete game is played
		var winner = play(scriptedX, scriptedO);

		// THEN the game ends with the expected result (`null` means a draw)
		assertThat(winner).isEqualTo(expectedWinner);
	}
}
