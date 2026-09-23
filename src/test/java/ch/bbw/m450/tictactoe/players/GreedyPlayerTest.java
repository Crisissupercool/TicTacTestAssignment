package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CIRCLE;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CROSS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Tests for {@link GreedyPlayer}.
 * Method names and bodies follow the GIVEN_WHEN_THEN pattern.
 */
class GreedyPlayerTest {

	@Test
	@DisplayName("GIVEN a board with a free field WHEN the greedy player moves THEN it takes the most top-left one")
	void GIVEN_boardWithFreeField_WHEN_greedyPlayerMoves_THEN_takesMostTopLeftField() {
		// GIVEN a board where the fields 0 and 1 are already taken
		var board = new Stone[TicTacToeMain.BOARD_SIZE];
		board[0] = CROSS;
		board[1] = CIRCLE;

		// WHEN the greedy player is asked for a move
		var move = new GreedyPlayer().play(board, CROSS);

		// THEN it takes field 2, the most top-left free one
		assertThat(move).isEqualTo(2);
	}

	@Test
	@DisplayName("GIVEN a completely full board WHEN the greedy player moves THEN an IllegalStateException is thrown")
	void GIVEN_fullBoard_WHEN_greedyPlayerMoves_THEN_throwsIllegalStateException() {
		// GIVEN a board without a single free field
		var board = new Stone[TicTacToeMain.BOARD_SIZE];
		for (var i = 0; i < board.length; i++) {
			board[i] = i % 2 == 0 ? CROSS : CIRCLE;
		}

		// WHEN the greedy player is asked for a move
		// THEN it reports that it cannot play at all
		assertThatThrownBy(() -> new GreedyPlayer().play(board, CROSS)).isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play at all");
	}
}
