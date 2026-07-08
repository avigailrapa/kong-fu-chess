import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GameStateTest {
    private GameState gameState;

    @Before
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
    public void testIsPieceInFlight() {
        assertFalse(gameState.isPieceInFlight(0, 0));
        
        PendingMove move = new PendingMove(0, 0, 2, 2, "wQ", 1000);
        gameState.addPendingMove(move);
        
        assertTrue(gameState.isPieceInFlight(0, 0));
        assertFalse(gameState.isPieceInFlight(1, 1));
    }

    @Test
    public void testIsTargetClaimed() {
        assertFalse(gameState.isTargetClaimed(2, 2));
        
        PendingMove move = new PendingMove(0, 0, 2, 2, "wQ", 1000);
        gameState.addPendingMove(move);
        
        assertTrue(gameState.isTargetClaimed(2, 2));
        assertFalse(gameState.isTargetClaimed(1, 1));
    }

    @Test
    public void testHasActiveMoveOfOppositeColor() {
        assertFalse(gameState.hasActiveMoveOfOppositeColor(Piece.WHITE));
        
        PendingMove blackMove = new PendingMove(7, 0, 5, 0, "bP", 1000);
        gameState.addPendingMove(blackMove);
        
        assertTrue(gameState.hasActiveMoveOfOppositeColor(Piece.WHITE));
        assertFalse(gameState.hasActiveMoveOfOppositeColor(Piece.BLACK));
    }

    @Test
    public void testAddJump() {
        JumpState jump = new JumpState(3, 3, 1000, 2000);
        gameState.addJump(jump);
        assertEquals(1, gameState.getActiveJumps().size());
    }

    @Test
    public void testIsPieceAirborne() {
        JumpState jump = new JumpState(3, 3, 1000, 2000);
        gameState.addJump(jump);
        
        assertTrue(gameState.isPieceAirborne(3, 3, 1500));
        assertFalse(gameState.isPieceAirborne(3, 3, 2500));
        assertFalse(gameState.isPieceAirborne(4, 4, 1500));
    }

    @Test
    public void testFindActiveJumpAt() {
        JumpState jump = new JumpState(3, 3, 1000, 2000);
        gameState.addJump(jump);
        
        JumpState found = gameState.findActiveJumpAt(3, 3, 1500);
        assertNotNull(found);
        assertEquals(3, found.getRow());
        
        JumpState notFound = gameState.findActiveJumpAt(4, 4, 1500);
        assertNull(notFound);
    }

    @Test
    public void testEndGame() {
        assertFalse(gameState.isGameOver());
        gameState.endGame();
        assertTrue(gameState.isGameOver());
    }

    @Test
    public void testRemovePendingMove() {
        PendingMove move = new PendingMove(0, 0, 2, 2, "wQ", 1000);
        gameState.addPendingMove(move);
        assertEquals(1, gameState.getPendingMoves().size());
        
        gameState.removePendingMove(move);
        assertEquals(0, gameState.getPendingMoves().size());
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

    @Test
    public void testMultipleMoves() {
        PendingMove move1 = new PendingMove(0, 0, 2, 2, "wQ", 1000);
        PendingMove move2 = new PendingMove(7, 0, 5, 0, "bP", 1000);
        gameState.addPendingMove(move1);
        gameState.addPendingMove(move2);
        
        assertEquals(2, gameState.getPendingMoves().size());
        assertTrue(gameState.isPieceInFlight(0, 0));
        assertTrue(gameState.isPieceInFlight(7, 0));
    }
}
