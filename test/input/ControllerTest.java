package input;

import engine.GameEngine;
import engine.GameSnapshot;
import engine.MoveResult;
import model.Board;
import model.GameState;
import model.Piece;
import model.Position;
import org.junit.jupiter.api.Test;
import realtime.RealTimeArbiter;
import rules.PieceRules;
import rules.RuleEngine;

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
        public MoveResult requestMove(Position source, Position destination) {
            lastSource = source;
            lastDestination = destination;
            callCount++;
            return acceptNextMove ? new MoveResult(true, "ok") : new MoveResult(false, "illegal_piece_move");
        }

        @Override
        public GameSnapshot snapshot() {
            return new GameSnapshot(occupiedCells);
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
}
