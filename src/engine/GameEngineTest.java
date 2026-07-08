package engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import model.*;

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
        engine.handleClick(0, 0);
        assertTrue(engine.getGameState().hasSelection());
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
        IBoard retrievedBoard = engine.getBoard();
        assertNotNull(retrievedBoard);
        assertEquals("wR", retrievedBoard.getPieceAt(0, 0));
    }
}