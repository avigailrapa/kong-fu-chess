package engine;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.engine.MoveObserver;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.realtime.RealTimeArbiter;
import src.rules.PieceRules;
import src.rules.pieces.RookRule;
import src.rules.RuleEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class GameEngineEventBusTest {

    private RuleEngine rookOnlyRuleEngine() {
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        return new RuleEngine(rulesByKind);
    }

    @Test
    public void testCapturingMoveDoesNotPublishGameOverEvent() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        board.addPiece(new Piece("q1", Piece.Color.BLACK, Piece.Kind.QUEEN, new Position(4, 0)), new Position(4, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        List<GameOverEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(GameOverEvent.class, received::add);

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertTrue(received.isEmpty());
    }

    @Test
    public void testKingCapturePublishesGameOverEvent() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        board.addPiece(new Piece("k1", Piece.Color.BLACK, Piece.Kind.KING, new Position(4, 0)), new Position(4, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        List<GameOverEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(GameOverEvent.class, received::add);

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertEquals(List.of(new GameOverEvent(Piece.Color.WHITE)), received);
    }

    @Test
    public void testMoveEventPublishedToBusEvenWithoutAnyMoveObserverRegistered() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        List<MoveEvent> received = new ArrayList<>();
        engine.eventBus().subscribe(MoveEvent.class, received::add);

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertEquals(1, received.size());
        assertEquals(new Position(7, 0), received.get(0).from());
        assertEquals(new Position(4, 0), received.get(0).to());
    }

    @Test
    public void testLegacyMoveObserverStillNotifiedAlongsideEventBus() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        GameEngine engine = new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
        List<MoveEvent> observerReceived = new ArrayList<>();
        List<MoveEvent> busReceived = new ArrayList<>();
        engine.addMoveObserver((MoveObserver) observerReceived::add);
        engine.eventBus().subscribe(MoveEvent.class, busReceived::add);

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertEquals(1, observerReceived.size());
        assertEquals(1, busReceived.size());
    }
}
