package engine;

import model.Board;
import model.GameState;
import model.Position;
import rules.MoveValidation;
import rules.RuleEngine;

public class GameEngine {

    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;

    public GameEngine(Board board, GameState gameState, RuleEngine ruleEngine) {
        this.board = board;
        this.gameState = gameState;
        this.ruleEngine = ruleEngine;
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.isGameOver()) {
            return new MoveResult(false, "game_over");
        }

        MoveValidation validation = ruleEngine.validateMove(board, source, destination);
        return new MoveResult(validation.isValid(), validation.reason());
    }
}
