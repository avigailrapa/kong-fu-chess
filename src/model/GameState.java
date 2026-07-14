package src.model;

import java.util.HashMap;
import java.util.Map;

public class GameState {
    private boolean gameOver = false;
    private Piece.Color winner = null;
    private Map<Piece.Color, Integer> scores = new HashMap<>();

    public GameState() {
        scores.put(Piece.Color.WHITE, 0);
        scores.put(Piece.Color.BLACK, 0);
    }

    public void addScore(Piece.Color color, int points) {
        scores.put(color, scores.get(color) + points);
    }

    public int getScore(Piece.Color color) {
        return scores.get(color);
    }

    public void endGame(Piece.Color winner) {
        this.gameOver = true;
        this.winner = winner;
    }

    public void endGame() {
        this.gameOver = true;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public Piece.Color winner() {
        return winner;
    }
}