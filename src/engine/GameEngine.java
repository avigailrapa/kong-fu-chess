package engine;

import model.Board;
import model.GameState;
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
    private long currentTimeMs = 0;

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

        arbiter.startMotion(source, destination, currentTimeMs);
        return new MoveResult(true, "ok");
    }

    public void waitMs(long ms) {
        currentTimeMs += ms;
        Optional<ArrivalEvent> event = arbiter.advanceTime(currentTimeMs);
        event.filter(ArrivalEvent::kingCaptured).ifPresent(e -> gameState.endGame());
    }
}
