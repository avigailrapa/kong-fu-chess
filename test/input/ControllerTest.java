package input;
import src.engine.*;
import src.model.*;
import src.rules.*;
import src.input.*;
import src.realtime.*;
import src.view.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerTest {

    private static class FakeGameEngine extends GameEngine {
        private Position lastSource;
        private Position lastDestination;
        private int callCount = 0;
        private boolean acceptNextMove = true;
        private Set<Position> occupiedCells = Set.of();

        FakeGameEngine() {
            super(new Board(8, 8), new GameState(), new RuleEngine(Map.<Piece.Kind, PieceRules>of()), new RealTimeArbiter(new Board(8, 8)));
        }

        @Override
        public boolean isOccupied(Position position) {
            return occupiedCells.contains(position);
        }

        @Override
        public MoveResult requestMove(Position source, Position destination) {
            lastSource = source;
            lastDestination = destination;
            callCount++;
            return acceptNextMove ? new MoveResult(true, "ok") : new MoveResult(false, "illegal_piece_move");
        }

        @Override
        public GameSnapshot snapshot(Position selectedPosition) {
            PieceSnapshot[][] grid = new PieceSnapshot[8][8];
            for (Position position : occupiedCells) {
                grid[position.getRow()][position.getCol()] =
                        new PieceSnapshot("p", Piece.Color.WHITE, Piece.Kind.PAWN, PieceSnapshot.RenderState.IDLE, 0, 0, 0, 0);
            }
            return new GameSnapshot(8, 8, grid, List.of(), Set.of(), false, null, 0, 0, List.of(), List.of());
        }
    }

    @Test
    public void testFirstClickOnPieceSelectsCell() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1));
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150); // -> row 1, col 1

        assertEquals(new Position(1, 1), controller.getSelectedCell().orElseThrow());
    }

    @Test
    public void testFirstClickOnEmptyCellLeavesSelectionEmpty() {
        Controller controller = new Controller(new BoardMapper(8, 8), new FakeGameEngine());

        controller.click(150, 150);

        assertTrue(controller.getSelectedCell().isEmpty());
    }

    @Test
    public void testOutsideClickWithNoSelectionDoesNothing() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(-50, 150);

        assertTrue(controller.getSelectedCell().isEmpty());
        assertEquals(0, fakeEngine.callCount);
    }

    @Test
    public void testOutsideClickWithSelectedPieceCancelsSelectionWithoutCallingEngine() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1));
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150); // select (1,1)
        controller.click(-50, 150); // outside click

        assertTrue(controller.getSelectedCell().isEmpty());
        assertEquals(0, fakeEngine.callCount);
    }

    @Test
    public void testSecondClickSendsCorrectSourceAndDestinationToEngine() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1));
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150); // select (1,1)
        controller.click(350, 150); // second click -> row 1, col 3

        assertEquals(1, fakeEngine.callCount);
        assertEquals(new Position(1, 1), fakeEngine.lastSource);
        assertEquals(new Position(1, 3), fakeEngine.lastDestination);
    }

    @Test
    public void testSecondClickClearsSelectionOnLegalMove() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1));
        fakeEngine.acceptNextMove = true;
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150);
        controller.click(350, 150);

        assertTrue(controller.getSelectedCell().isEmpty());
    }

    @Test
    public void testSecondClickClearsSelectionEvenOnIllegalMove() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1));
        fakeEngine.acceptNextMove = false;
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150);
        controller.click(350, 150);

        assertTrue(controller.getSelectedCell().isEmpty());
    }

    @Test
    public void testThirdClickStartsFreshIndependentSelectionCycle() {
        FakeGameEngine fakeEngine = new FakeGameEngine();
        fakeEngine.occupiedCells = Set.of(new Position(1, 1), new Position(3, 3));
        Controller controller = new Controller(new BoardMapper(8, 8), fakeEngine);

        controller.click(150, 150); // select (1,1)
        controller.click(350, 150); // second click -> sends move, clears selection
        controller.click(350, 350); // third click -> select (3,3), a fresh first click

        assertEquals(1, fakeEngine.callCount);
        assertEquals(new Position(3, 3), controller.getSelectedCell().orElseThrow());
    }
}
