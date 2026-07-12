package engine;

import model.Board;
import model.GameState;
import model.Piece;
import model.Position;
import realtime.ArrivalEvent;
import realtime.RealTimeArbiter;
import rules.MoveValidation;
import rules.RuleEngine;

import java.util.Optional;

public class GameEngine {

    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine, RealTimeArbiter arbiter) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
        this.arbiter = arbiter;
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return new MoveResult(false, "game_over");
        }
        if (arbiter.hasActiveMotion()) {
            return new MoveResult(false, "motion_in_progress");
        }

        MoveValidation validation = ruleEngine.validateMove(board, source, destination);
        if (!validation.isValid()) {
            return new MoveResult(false, validation.reason());
        }

        Piece piece = board.getPieceAt(source).orElseThrow();
        arbiter.startMotion(piece, source, destination);
        return new MoveResult(true, "ok");
    }

    public void waitMs(long ms) {
        Optional<ArrivalEvent> event = arbiter.advanceTime(ms);
        event.filter(ArrivalEvent::kingCaptured).ifPresent(e -> gameState.endGame());
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(board.occupiedPositions());
    }
}
