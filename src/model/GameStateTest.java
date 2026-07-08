package model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class GameStateTest {
    private GameState gameState;

    @BeforeEach
    public void setUp() {
        gameState = new GameState();
    }

    @Test
    public void testInitialState() {
        assertFalse(gameState.hasSelection());
        assertFalse(gameState.isGameOver());
        assertEquals(0, gameState.getPendingMoves().size());
        assertEquals(0, gameState.getActiveJumps().size());
    }

    @Test
    public void testSelectPiece() {
        gameState.selectPiece(0, 0);
        assertTrue(gameState.hasSelection());
        assertEquals(0, gameState.getSelectedRow());
        assertEquals(0, gameState.getSelectedCol());
    }

    @Test
    public void testClearSelection() {
        gameState.selectPiece(3, 3);
        gameState.clearSelection();
        assertFalse(gameState.hasSelection());
    }

    @Test
    public void testChangeSelection() {
        gameState.selectPiece(0, 0);
        gameState.selectPiece(5, 5);
        assertEquals(5, gameState.getSelectedRow());
        assertEquals(5, gameState.getSelectedCol());
    }

    @Test
    public void testAddPendingMove() {
        PendingMove move = new PendingMove(0, 0, 2, 2, "wQ", 1000);
        gameState.addPendingMove(move);
        assertEquals(1, gameState.getPendingMoves().size());
    }

    @Test
    public void testCleanupCompletedJumps() {
        JumpState jump1 = new JumpState(3, 3, 1000, 2000);
        JumpState jump2 = new JumpState(4, 4, 1000, 3000);
        gameState.addJump(jump1);
        gameState.addJump(jump2);
        
        assertEquals(2, gameState.getActiveJumps().size());
        
        gameState.cleanupCompletedJumps(2500);
        assertEquals(1, gameState.getActiveJumps().size());
        
        gameState.cleanupCompletedJumps(3500);
        assertEquals(0, gameState.getActiveJumps().size());
    }

    @Test
    public void testReset() {
        gameState.selectPiece(3, 3);
        gameState.endGame();
        gameState.addPendingMove(new PendingMove(0, 0, 2, 2, "wQ", 1000));
        gameState.addJump(new JumpState(3, 3, 1000, 2000));
        
        gameState.reset();
        
        assertFalse(gameState.hasSelection());
        assertFalse(gameState.isGameOver());
        assertEquals(0, gameState.getPendingMoves().size());
        assertEquals(0, gameState.getActiveJumps().size());
    }
}