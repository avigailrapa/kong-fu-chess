package engine;

import model.Board;
import model.GameState;
import model.Piece;
import model.Position;
import realtime.ArrivalEvent;
import realtime.RealTimeArbiter;
import rules.BishopRule;
import rules.KingRule;
import rules.KnightRule;
import rules.MoveValidation;
import rules.PawnRule;
import rules.PieceRules;
import rules.QueenRule;
import rules.RookRule;
import rules.RuleEngine;

import java.util.EnumMap;
import java.util.Map;
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

    public static GameEngine fromBoard(Board board) {
        RuleEngine ruleEngine = new RuleEngine(createStandardRules());
        GameState gameState = new GameState();
        RealTimeArbiter arbiter = new RealTimeArbiter(board);
        return new GameEngine(board, gameState, ruleEngine, arbiter);
    }

    private static Map<Piece.Kind, PieceRules> createStandardRules() {
        Map<Piece.Kind, PieceRules> rules = new EnumMap<>(Piece.Kind.class);
        rules.put(Piece.Kind.ROOK, new RookRule());
        rules.put(Piece.Kind.BISHOP, new BishopRule());
        rules.put(Piece.Kind.QUEEN, new QueenRule());
        rules.put(Piece.Kind.KNIGHT, new KnightRule());
        rules.put(Piece.Kind.KING, new KingRule());
        rules.put(Piece.Kind.PAWN, new PawnRule());
        return rules;
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
        if (ms < 0) {
            throw new IllegalArgumentException("ms must not be negative");
        }
        Optional<ArrivalEvent> event = arbiter.advanceTime(ms);
        event.filter(ArrivalEvent::kingCaptured).ifPresent(e -> gameState.endGame());
    }

    public GameSnapshot snapshot() {
        return new GameSnapshot(board.occupiedPositions());
    }

    public Board settledBoard() {
        return board;
    }
}
