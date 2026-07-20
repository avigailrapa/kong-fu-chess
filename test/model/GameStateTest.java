package model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import src.model.*;


public class GameStateTest {
    private GameState gameState;

    @BeforeEach
    public void setUp() {
        gameState = new GameState();
    }

    @Test
    public void testInitialStateIsNotGameOver() {
        assertFalse(gameState.gameOver());
    }

    @Test
    public void testEndGameSetsGameOver() {
        gameState.endGame();
        assertTrue(gameState.gameOver());
    }

    @Test
    public void testEndGameIsIdempotent() {
        gameState.endGame();
        gameState.endGame();
        assertTrue(gameState.gameOver());
    }
}
