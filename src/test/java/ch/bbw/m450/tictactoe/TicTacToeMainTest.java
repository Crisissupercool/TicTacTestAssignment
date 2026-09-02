package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.BoardFixtures.antiDiagonalOfCircles;
import static ch.bbw.m450.tictactoe.BoardFixtures.assertNobodyWins;
import static ch.bbw.m450.tictactoe.BoardFixtures.emptyBoard;
import static ch.bbw.m450.tictactoe.BoardFixtures.middleRowOfCrosses;
import static ch.bbw.m450.tictactoe.BoardFixtures.mixedTopRow;
import static ch.bbw.m450.tictactoe.TicTacToeMain.isWin;
import static ch.bbw.m450.tictactoe.TicTacToeMain.play;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CIRCLE;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CROSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
