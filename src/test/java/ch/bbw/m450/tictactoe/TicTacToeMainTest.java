package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.TicTacToeMain.isWin;
import static ch.bbw.m450.tictactoe.TicTacToeMain.play;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CIRCLE;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CROSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

/**
 * Tests for the TicTacToe game logic in {@link TicTacToeMain}.
 * Method names and bodies follow the GIVEN_WHEN_THEN pattern.
 */
class TicTacToeMainTest {

	/** Shorthand for a board position that is still empty. */
	private static final Stone E = null;

	@Test
	@DisplayName("GIVEN a board with a middle row of crosses WHEN isWin is called for CROSS THEN it returns true")
	void GIVEN_middleRowOfCrosses_WHEN_isWinForCross_THEN_returnsTrue() {
		// GIVEN a board where CROSS occupies the whole middle row
		var board = new Stone[] {CIRCLE, CIRCLE, E, CROSS, CROSS, CROSS, E, E, E};

		// WHEN we ask whether CROSS has won
		var crossWins = isWin(board, CROSS);

		// THEN the row is recognised as a win
		assertThat(crossWins).isTrue();
	}

	@Test
	@DisplayName("GIVEN a board with circles on the anti-diagonal WHEN isWin is called for CIRCLE THEN it returns true")
	void GIVEN_antiDiagonalOfCircles_WHEN_isWinForCircle_THEN_returnsTrue() {
		// GIVEN a board where CIRCLE occupies the positions 2, 4 and 6
		var board = new Stone[] {CROSS, CROSS, CIRCLE, E, CIRCLE, E, CIRCLE, E, E};

		// WHEN we ask whether CIRCLE has won
		var circleWins = isWin(board, CIRCLE);

		// THEN the diagonal is recognised as a win
		assertThat(circleWins).isTrue();
	}

	@Test
	@DisplayName("GIVEN a mixed line and an empty board WHEN isWin is called for both colours THEN it returns false")
	void GIVEN_mixedLineAndEmptyBoard_WHEN_isWinForBothColours_THEN_returnsFalse() {
		// GIVEN a top row filled by both colours and a completely empty board
		var mixed = new Stone[] {CROSS, CIRCLE, CROSS, E, E, E, E, E, E};
		var empty = new Stone[TicTacToeMain.BOARD_SIZE];

		// WHEN we check both boards for both colours
		// THEN neither player has won
		assertThat(isWin(mixed, CROSS)).isFalse();
		assertThat(isWin(mixed, CIRCLE)).isFalse();
		assertThat(isWin(empty, CROSS)).isFalse();
		assertThat(isWin(empty, CIRCLE)).isFalse();
	}

	@Test
	@DisplayName("GIVEN one single player instance WHEN play is started with it for both colours THEN an IllegalArgumentException is thrown")
	void GIVEN_singlePlayerInstance_WHEN_playStartedForBothColours_THEN_throwsIllegalArgumentException() {
		// GIVEN one single player instance
		var player = new GreedyPlayer();

		// WHEN this instance is used for both colours
		// THEN the game refuses to start
		assertThatThrownBy(() -> play(player, player)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("players must differ");
	}

	@Test
	@DisplayName("GIVEN two greedy players WHEN a full game is played THEN CROSS wins")
	void GIVEN_twoGreedyPlayers_WHEN_fullGameIsPlayed_THEN_crossWins() {
		// GIVEN two greedy players, each always taking the most top-left free field
		var xPlayer = new GreedyPlayer();
		var oPlayer = new GreedyPlayer();

		// WHEN a full game is played
		var winner = play(xPlayer, oPlayer);

		// THEN CROSS wins, because it holds the fields 0, 2, 4 and 6
		assertThat(winner).isEqualTo(CROSS);
	}
}
