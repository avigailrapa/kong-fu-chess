package input;

import model.Board;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerTest {

    private Board board;
    private FakeMoveRequestHandler fakeHandler;
    private Controller controller;

    @BeforeEach
    public void setUp() {
        board = new Board(8, 8);
        board.addPiece(new Piece("w1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(0, 0)), new Position(0, 0));
        BoardMapper boardMapper = new BoardMapper(8, 8);
        fakeHandler = new FakeMoveRequestHandler();
        controller = new Controller(boardMapper, board, fakeHandler);
    }

    @Test
    public void testFirstClickOnPieceSelectsCell() {
        controller.click(50, 50);
        assertEquals(new Position(0, 0), controller.getSelectedCell().get());
    }

    @Test
    public void testFirstClickOnEmptyCellLeavesSelectionEmpty() {
        controller.click(250, 250);
        assertTrue(controller.getSelectedCell().isEmpty());
    }

    @Test
    public void testOutsideClickWithNoSelectionDoesNothing() {
        controller.click(-50, -50);
        assertTrue(controller.getSelectedCell().isEmpty());
        assertEquals(0, fakeHandler.callCount());
    }

    @Test
    public void testOutsideClickWithSelectedPieceClearsSelectionAndDoesNotCallHandler() {
        controller.click(50, 50);
        controller.click(-50, -50);

        assertTrue(controller.getSelectedCell().isEmpty());
        assertEquals(0, fakeHandler.callCount());
    }

    @Test
    public void testSecondInBoardClickSendsCorrectSourceAndDestination() {
        controller.click(50, 50);
        controller.click(350, 50);

        assertEquals(1, fakeHandler.callCount());
        assertEquals(new Position(0, 0), fakeHandler.lastSource());
        assertEquals(new Position(0, 3), fakeHandler.lastDestination());
    }

    @Test
    public void testSecondInBoardClickClearsSelection() {
        controller.click(50, 50);
        controller.click(350, 50);

        assertTrue(controller.getSelectedCell().isEmpty());
    }

    @Test
    public void testSecondInBoardClickSendsRequestEvenToEmptyCell() {
        controller.click(50, 50);
        controller.click(250, 250);

        assertEquals(1, fakeHandler.callCount());
        assertEquals(new Position(2, 2), fakeHandler.lastDestination());
    }
}
