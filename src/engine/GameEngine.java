package src.engine;
import src.engine.MoveEvent;
import src.engine.MoveObserver;
import src.model.*;
import src.realtime.*;
import src.rules.*;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameEngine {

    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;
    private final List<MoveObserver> moveObservers = new ArrayList<>();
    private final Map<Piece, Long> motionRequestTimestampMs = new HashMap<>();
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

    public void addMoveObserver(MoveObserver observer) {
        moveObservers.add(observer);
    }

    public void removeMoveObserver(MoveObserver observer) {
        moveObservers.remove(observer);
    }

    private void notifyMoveObservers(MoveEvent event) {
        for (MoveObserver observer : moveObservers) {
            observer.onMove(event);
        }
    }

    private void fireMoveEvent(Piece piece, Position source, Position destination, boolean capture, boolean kingCapture, long requestTimestampMs) {
        MoveEvent event = new MoveEvent(piece.getColor(), piece.getKind(), source, destination, capture, kingCapture, requestTimestampMs);
        notifyMoveObservers(event);
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
        motionRequestTimestampMs.put(piece, gameClockMs);
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
        
        if (event.capturedPiece() != null && event.movedPiece() != null) {
            Piece.Color capturingColor = event.movedPiece().getColor();
            int points = getPieceValue(event.capturedPiece().getKind());
            gameState.addScore(capturingColor, points);
        }

        if (event.movedPiece() != null) {
            Long requestTimestamp = motionRequestTimestampMs.remove(event.movedPiece());
            long effectiveTimestamp = requestTimestamp != null ? requestTimestamp : gameClockMs;
            fireMoveEvent(event.movedPiece(), event.from(), event.to(), event.capturedPiece() != null, event.kingCaptured(), effectiveTimestamp);
        }
    }
}

private int getPieceValue(Piece.Kind kind) {
    switch (kind) {
        case PAWN: return 1;
        case KNIGHT:
        case BISHOP: return 3;
        case ROOK: return 5;
        case QUEEN: return 9;
        case KING: return 0;
        default: return 0;
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
    return new GameSnapshot(width, height, grid, selectedPosition, gameState.isGameOver(), 
                           gameState.winner(), gameState.getScore(Piece.Color.WHITE), 
                           gameState.getScore(Piece.Color.BLACK));
}

    private PieceSnapshot pieceSnapshotOf(Piece piece, Position position) {
        int pixelX = (int) Math.round(position.getCol() * GameSnapshot.CELL_WIDTH);
        int pixelY = (int) Math.round(position.getRow() * GameSnapshot.CELL_HEIGHT);
        long elapsedMillis;
        PieceSnapshot.RenderState renderState;

        Optional<Motion> motion = arbiter.activeMotion(piece);
        if (motion.isPresent()) {
            Motion m = motion.get();
            elapsedMillis = arbiter.motionElapsedMs(piece);

            double progress = Math.min(1.0, (double) elapsedMillis / m.durationMs());
            pixelX = (int) Math.round(interpolate(m.source().getCol(), m.destination().getCol(), progress) * GameSnapshot.CELL_WIDTH);
            pixelY = (int) Math.round(interpolate(m.source().getRow(), m.destination().getRow(), progress) * GameSnapshot.CELL_HEIGHT);
            renderState = PieceSnapshot.RenderState.MOVING;
        } else if (arbiter.isJumping(piece)) {
            elapsedMillis = arbiter.jumpElapsedMs(piece);
            renderState = PieceSnapshot.RenderState.JUMPING;
        } else if (arbiter.isLongResting(piece)) {
            elapsedMillis = arbiter.longRestElapsedMs(piece);
            renderState = PieceSnapshot.RenderState.LONG_REST;
        } else if (arbiter.isShortResting(piece)) {
            elapsedMillis = arbiter.shortRestElapsedMs(piece);
            renderState = PieceSnapshot.RenderState.SHORT_REST;
        } else {
            elapsedMillis = gameClockMs;
            renderState = PieceSnapshot.RenderState.IDLE;
        }

        return new PieceSnapshot(piece.getId(), piece.getColor(), piece.getKind(), renderState,
                pixelX, pixelY, elapsedMillis);
    }

    private static int interpolate(int from, int to, double progress) {
        return (int) Math.round(from + (to - from) * progress);
    }

    public Board settledBoard() {
        return board;
    }
}