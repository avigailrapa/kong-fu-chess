package src.model;

public class GameState {

    private boolean gameOver;
    private Piece.Color winner;

    public GameState() {
        this.gameOver = false;
        this.winner = null;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void endGame() {
        this.gameOver = true;
    }

    public void endGame(Piece.Color winner) {
        this.gameOver = true;
        this.winner = winner;
    }

    public Piece.Color winner() {
        return winner;
    }
}
