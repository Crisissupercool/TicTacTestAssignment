package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.TicTacToeMain.isWin;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CIRCLE;
import static ch.bbw.m450.tictactoe.TicTacToePlayer.Stone.CROSS;
import static org.assertj.core.api.Assertions.assertThat;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Test helper: builds the boards used in the tests and offers small assertion shortcuts.
 * <p>
 * The board is a one-dimensional array of length 9, the index for row r and column c is r*3+c:
 *
 * <pre>
 *  0 | 1 | 2
 * ---+---+---
 *  3 | 4 | 5
 * ---+---+---
 *  6 | 7 | 8
 * </pre>
 */
final class BoardFixtures {

	/** Shorthand for a board position that is still empty. */
	static final Stone E = null;

	private BoardFixtures() {
		// helper class, not meant to be instantiated
	}

	/** @return a board where every field is still free */
	static Stone[] emptyBoard() {
		return new Stone[TicTacToeMain.BOARD_SIZE];
	}

	/**
	 * @param fields all 9 fields, top-left to bottom-right ({@link #E} for a free field)
	 * @return a board with exactly these fields
	 */
	static Stone[] boardOf(Stone... fields) {
		if (fields.length != TicTacToeMain.BOARD_SIZE) {
			throw new IllegalArgumentException("a board needs exactly " + TicTacToeMain.BOARD_SIZE + " fields");
		}
		return fields.clone();
	}

	/**
	 * @param board the board to start from (stays unchanged)
	 * @param color the color to place
	 * @param positions the positions (0-8) to place it on
	 * @return a copy of the board with the given positions taken by the color
	 */
	static Stone[] place(Stone[] board, Stone color, int... positions) {
		var result = board.clone();
		for (var position : positions) {
			result[position] = color;
		}
		return result;
	}

	/**
	 * All 8 lines that win a game: the three rows, the three columns and the two diagonals.
	 */
	static final int[][] WINNING_LINES = {
			{0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
			{0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
			{0, 4, 8}, {2, 4, 6}             // diagonals
	};

	/**
	 * @param color the color that takes the line
	 * @param line the positions (0-8) forming the line
	 * @return an otherwise empty board where the color holds exactly this line
	 */
	static Stone[] lineOf(Stone color, int... line) {
		return place(emptyBoard(), color, line);
	}

	/** @return a board where CROSS holds the whole middle row (3, 4, 5) */
	static Stone[] middleRowOfCrosses() {
		return place(place(emptyBoard(), CIRCLE, 0, 1), CROSS, 3, 4, 5);
	}

	/** @return a board where CIRCLE holds the anti-diagonal (2, 4, 6) */
	static Stone[] antiDiagonalOfCircles() {
		return place(place(emptyBoard(), CROSS, 0, 1), CIRCLE, 2, 4, 6);
	}

	/** @return a board with a top row filled by both colours, so nobody has a line */
	static Stone[] mixedTopRow() {
		return boardOf(CROSS, CIRCLE, CROSS, E, E, E, E, E, E);
	}

	/**
	 * Parses a compact board notation, e.g. "XOX--O--X": `X` = CROSS, `O` = CIRCLE, `-` = free.
	 *
	 * @param fields exactly 9 characters, top-left to bottom-right
	 * @return the parsed board
	 */
	static Stone[] parse(String fields) {
		if (fields.length() != TicTacToeMain.BOARD_SIZE) {
			throw new IllegalArgumentException("a board needs exactly " + TicTacToeMain.BOARD_SIZE + " fields");
		}
		var board = emptyBoard();
		for (var i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
			board[i] = switch (fields.charAt(i)) {
				case 'X' -> CROSS;
				case 'O' -> CIRCLE;
				case '-' -> E;
				default -> throw new IllegalArgumentException("unknown field: " + fields.charAt(i));
			};
		}
		return board;
	}

	/**
	 * Asserts that neither CROSS nor CIRCLE has won on the given board.
	 *
	 * @param board the board to check
	 */
	static void assertNobodyWins(Stone[] board) {
		assertThat(isWin(board, CROSS)).as("CROSS wins on %s", TicTacToeMain.toString(board))
				.isFalse();
		assertThat(isWin(board, CIRCLE)).as("CIRCLE wins on %s", TicTacToeMain.toString(board))
				.isFalse();
	}
}
