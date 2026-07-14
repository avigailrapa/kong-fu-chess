package src.engine;
import src.model.*;
import src.realtime.*;
import src.rules.*;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameEngine {

    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;
    private long gameClockMs = 0;

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
        Piece pieceAtSource = board.isWithinBorder(source) ? board.getPieceAt(source).orElse(null) : null;
        if (pieceAtSource != null && arbiter.isMoving(pieceAtSource)) {
            return new MoveResult(false, "motion_in_progress");
        }
        if (pieceAtSource != null && arbiter.isResting(pieceAtSource)) {
            return new MoveResult(false, "resting");
        }

        MoveValidation validation = ruleEngine.validateMove(board, source, destination);
        if (!validation.isValid()) {
            return new MoveResult(false, validation.reason());
        }

        Piece piece = board.getPieceAt(source).orElseThrow();
        arbiter.startMotion(piece, source, destination);
        return new MoveResult(true, "ok");
    }

    public void requestJump(Position cell) {
        if (gameState.isGameOver()) {
            return;
        }
        board.getPieceAt(cell).ifPresent(piece -> arbiter.startJump(piece, cell));
    }

    public void waitMs(long ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("ms must not be negative");
        }
        gameClockMs += ms;
        List<ArrivalEvent> events = arbiter.advanceTime(ms);
        for (ArrivalEvent event : events) {
            if (event.kingCaptured()) {
                Piece.Color loserColor = event.capturedPiece().getColor();
                Piece.Color winner = loserColor == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
                gameState.endGame(winner);
            }
        }
    }

    public GameSnapshot snapshot(Position selectedPosition) {
        int width = board.getWidth();
        int height = board.getHeight();
        PieceSnapshot[][] grid = new PieceSnapshot[height][width];
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.getPieceAt(position).orElseThrow();
            grid[position.getRow()][position.getCol()] = pieceSnapshotOf(piece, position);
        }
        return new GameSnapshot(width, height, grid, selectedPosition, gameState.isGameOver(), gameState.winner());
    }

    private PieceSnapshot pieceSnapshotOf(Piece piece, Position position) {
        int pixelX = (int) Math.round(position.getCol() * GameSnapshot.CELL_WIDTH);
        int pixelY = (int) Math.round(position.getRow() * GameSnapshot.CELL_HEIGHT);
        long elapsedMillis;

        Optional<Motion> motion = arbiter.activeMotion(piece);
        if (motion.isPresent()) {
            Motion m = motion.get();
            elapsedMillis = arbiter.motionElapsedMs(piece);
            double progress = Math.min(1.0, (double) elapsedMillis / m.durationMs());
            pixelX = (int) Math.round(interpolate(m.source().getCol(), m.destination().getCol(), progress) * GameSnapshot.CELL_WIDTH);
            pixelY = (int) Math.round(interpolate(m.source().getRow(), m.destination().getRow(), progress) * GameSnapshot.CELL_HEIGHT);
        } else if (arbiter.isJumping(piece)) {
            elapsedMillis = arbiter.jumpElapsedMs(piece);
        } else if (arbiter.isResting(piece)) {
            elapsedMillis = arbiter.restElapsedMs(piece);
        } else {
            // Idle pieces still loop their idle animation continuously, driven by the game clock
            // instead of a per-action timer (which would otherwise sit frozen at 0).
            elapsedMillis = gameClockMs;
        }

        return new PieceSnapshot(piece.getId(), piece.getColor(), piece.getKind(), piece.getState(),
                pixelX, pixelY, elapsedMillis);
    }

    private static int interpolate(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    public Board settledBoard() {
        return board;
    }
}
