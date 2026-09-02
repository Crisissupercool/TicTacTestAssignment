package ch.bbw.m450.tictactoe;

/**
 * Test helper: a player that does not think at all but replays a fixed script of moves.
 * It lets a test drive {@link TicTacToeMain#play} into an exactly known board constellation.
 */
final class ScriptedPlayer implements TicTacToePlayer {

	private final int[] moves;

	private int nextMove;

	/**
	 * @param moves the positions (0-8) to play, in order
	 */
	ScriptedPlayer(int... moves) {
		this.moves = moves.clone();
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		if (nextMove >= moves.length) {
			throw new IllegalStateException("the script is exhausted, no move left for " + colorToPlay);
		}
		return moves[nextMove++];
	}
}
