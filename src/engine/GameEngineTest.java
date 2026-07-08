package engine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import model.*;
import config.GameConfig;

public class GameEngineTest {
    private String[][] initialGrid;
    private IBoard board; 
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
        engine.handleClick(10, 10); 
        assertTrue(engine.getGameState().hasSelection());
    }

    @Test
    public void testPendingMoveExecution() {
        engine.getBoard().setPieceAt(3, 3, "wQ");
        
        int scale = GameConfig.PIXELS_PER_CELL;
        engine.handleClick(3 * scale + 10, 3 * scale + 10);
        engine.handleClick(4 * scale + 10, 4 * scale + 10);
        
        assertEquals(1, engine.getGameState().getPendingMoves().size());
        
        engine.advanceTime(GameConfig.MOVE_TRAVEL_TIME_MS);
        
        assertEquals(0, engine.getGameState().getPendingMoves().size());
        assertEquals("wQ", engine.getBoard().getPieceAt(4, 4));
    }

    @Test
    public void testJumpInterceptsMove() {
        engine.getBoard().setPieceAt(3, 3, "wQ");
        engine.getBoard().setPieceAt(4, 4, "bK");
        
        int scale = GameConfig.PIXELS_PER_CELL;
        engine.handleClick(3 * scale + 10, 3 * scale + 10);
        engine.handleClick(4 * scale + 10, 4 * scale + 10);
        
        engine.handleJump(4 * scale + 10, 4 * scale + 10);
        
        engine.advanceTime(GameConfig.MOVE_TRAVEL_TIME_MS);
        
        assertTrue(engine.getGameState().isGameOver());
    }

    @Test
    public void testBoardAccess() {
        IBoard retrievedBoard = engine.getBoard();
        assertNotNull(retrievedBoard);
        assertEquals("wR", retrievedBoard.getPieceAt(0, 0));
    }
}