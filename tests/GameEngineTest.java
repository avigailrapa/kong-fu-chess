import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

import features.*;

public class GameEngineTest {
    private String[][] initialGrid;
    private Board board;
    private GameEngine engine;

    @BeforeEach
    public void setUp() {
        initialGrid = new String[][] {
            {"wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR"},
            {"wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP"},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {".", ".", ".", ".", ".", ".", ".", "."},
            {"bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP"},
            {"bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR"}
        };
        board = new Board(initialGrid);
        engine = new GameEngine(board);
    }

    @Test
    public void testEngineInitialization() {
        assertNotNull(engine.getBoard());
        assertNotNull(engine.getGameState());
        assertNotNull(engine.getGameClock());
        assertFalse(engine.getGameState().isGameOver());
    }

    @Test
    public void testSelectPiece() {
        engine.handleClick(0, 0);
        assertTrue(engine.getGameState().hasSelection());
        assertEquals(0, engine.getGameState().getSelectedRow());
        assertEquals(0, engine.getGameState().getSelectedCol());
    }

    @Test
    public void testClickEmptyCell() {
        engine.handleClick(200, 200);
        assertFalse(engine.getGameState().hasSelection());
    }

    @Test
    public void testMoveWhiteKingValidly() {
        engine.handleClick(400, 0);
        engine.handleClick(400, 100);
        engine.advanceTime(1000);
        
        assertEquals(".", engine.getBoard().getPieceAt(0, 4));
        assertEquals("wK", engine.getBoard().getPieceAt(1, 4));
    }

    @Test
    public void testMoveWhitePawnForward() {
        engine.handleClick(0, 100);
        engine.handleClick(0, 200);
        engine.advanceTime(1000);
        
        assertEquals(".", engine.getBoard().getPieceAt(1, 0));
        assertEquals("wP", engine.getBoard().getPieceAt(2, 0));
    }

    @Test
    public void testInvalidMoveBlocked() {
        GameState state = engine.getGameState();
        GameClock clock = engine.getGameClock();
        
        state.selectPiece(0, 0);
        state.addPendingMove(new PendingMove(0, 0, 2, 0, "wR", clock.getCurrentTime() + 1000));
        
        engine.handleClick(300, 100);
        
        state.clearSelection();
        state.addPendingMove(new PendingMove(0, 0, 2, 0, "wR", clock.getCurrentTime() + 1000));
        engine.advanceTime(1500);
        
        engine.handleClick(200, 100);
        assertFalse(state.hasActiveMoveOfOppositeColor('w'));
    }

    @Test
    public void testClearSelection() {
        engine.handleClick(0, 0);
        assertTrue(engine.getGameState().hasSelection());
        
        engine.handleClick(0, 0);
        assertFalse(engine.getGameState().hasSelection());
    }

    @Test
    public void testSwitchSelection() {
        engine.handleClick(0, 100);
        assertEquals(1, engine.getGameState().getSelectedRow());
        
        engine.handleClick(100, 100);
        assertEquals(1, engine.getGameState().getSelectedRow());
        assertEquals(1, engine.getGameState().getSelectedCol());
    }

    @Test
    public void testAdvanceTime() {
        assertEquals(0, engine.getGameClock().getCurrentTime());
        engine.advanceTime(500);
        assertEquals(500, engine.getGameClock().getCurrentTime());
        engine.advanceTime(500);
        assertEquals(1000, engine.getGameClock().getCurrentTime());
    }

    @Test
    public void testHandleJump() {
        engine.handleClick(400, 0);
        engine.handleJump(400, 0);
        assertEquals(1, engine.getGameState().getActiveJumps().size());
    }

    @Test
    public void testPawnPromotion() {
        engine.getBoard().clearCell(2, 0);
        engine.getBoard().clearCell(3, 0);
        engine.getBoard().clearCell(4, 0);
        engine.getBoard().clearCell(5, 0);
        engine.getBoard().clearCell(6, 0);
        engine.getBoard().setPieceAt(1, 0, "wP");
        
        engine.handleClick(0, 100);
        engine.handleClick(0, 0);
        engine.advanceTime(1000);
        
        assertEquals("wQ", engine.getBoard().getPieceAt(0, 0));
    }

    @Test
    public void testBlackPawnPromotion() {
        engine.getBoard().clearCell(2, 0);
        engine.getBoard().clearCell(3, 0);
        engine.getBoard().clearCell(4, 0);
        engine.getBoard().clearCell(5, 0);
        engine.getBoard().setPieceAt(6, 0, "bP");
        
        engine.handleClick(0, 600);
        engine.handleClick(0, 700);
        engine.advanceTime(1000);
        
        assertEquals("bQ", engine.getBoard().getPieceAt(7, 0));
    }

    @Test
    public void testGameOverOnKingCapture() {
        engine.getBoard().setPieceAt(4, 4, "bK");
        engine.getBoard().setPieceAt(3, 3, "wQ");
        
        engine.handleClick(300, 300);
        engine.handleClick(400, 400);
        engine.advanceTime(1000);
        
        assertTrue(engine.getGameState().isGameOver());
    }

    @Test
    public void testGameStopOnGameOver() {
        engine.getGameState().endGame();
        engine.handleClick(100, 100);
        assertFalse(engine.getGameState().hasSelection());
    }

    @Test
    public void testClickOutOfBounds() {
        engine.handleClick(-100, -100);
        assertFalse(engine.getGameState().hasSelection());
        
        engine.handleClick(900, 900);
        assertFalse(engine.getGameState().hasSelection());
    }

    @Test
    public void testPendingMoveExecution() {
        engine.getBoard().setPieceAt(3, 3, "wQ");
        engine.handleClick(300, 300);
        engine.handleClick(400, 400);
        
        assertEquals(1, engine.getGameState().getPendingMoves().size());
        engine.advanceTime(1000);
        assertEquals(0, engine.getGameState().getPendingMoves().size());
        assertEquals("wQ", engine.getBoard().getPieceAt(4, 4));
    }

    @Test
    public void testJumpInterceptsMove() {
        engine.getBoard().setPieceAt(3, 3, "wQ");
        engine.getBoard().setPieceAt(4, 4, "bK");
        
        engine.handleClick(300, 300);
        engine.handleClick(400, 400);
        
        engine.handleJump(400, 400);
        engine.advanceTime(1000);
        
        assertTrue(engine.getGameState().isGameOver());
    }

    @Test
    public void testBoardAccess() {
        Board retrievedBoard = engine.getBoard();
        assertNotNull(retrievedBoard);
        assertEquals("wR", retrievedBoard.getPieceAt(0, 0));
    }

    @Test
    public void testGameStateAccess() {
        GameState state = engine.getGameState();
        assertNotNull(state);
        assertFalse(state.isGameOver());
    }

    @Test
    public void testGameClockAccess() {
        GameClock clock = engine.getGameClock();
        assertNotNull(clock);
        assertEquals(0, clock.getCurrentTime());
    }
}
