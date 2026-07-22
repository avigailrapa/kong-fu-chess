package src.engine;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import src.bus.EventBus;
import src.model.*;
import src.realtime.*;
import src.rules.*;
import src.view.GameSnapshot;
import src.view.PieceSnapshot;
import src.view.SelectionSnapshot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class GameEngine implements GameCommands {

    private final Board board;
    private final GameState gameState;
    private final RuleEngine ruleEngine;
    private final RealTimeArbiter arbiter;
    private final List<MoveObserver> moveObservers = new ArrayList<>();
    private final Map<Piece, Long> motionRequestTimestampMs = new HashMap<>();
    @Getter
    @Accessors(fluent = true)
    private final EventBus eventBus = new EventBus();
    private long gameClockMs = 0;

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

    private void notifyMoveObservers(MoveEvent event) {
        for (MoveObserver observer : moveObservers) {
            observer.onMove(event);
        }
    }

    private void fireMoveEvent(Piece piece, Position source, Position destination, boolean capture, boolean kingCapture, boolean promotion, long requestTimestampMs) {
        MoveEvent event = new MoveEvent(piece.color(), piece.kind(), source, destination, capture, kingCapture, promotion, requestTimestampMs);
        eventBus.publish(event);
        if (!moveObservers.isEmpty()) {
            notifyMoveObservers(event);
        }
    }

    public MoveResult requestMove(Position source, Position destination) {
        if (gameState.gameOver()) {
            return new MoveResult(false, "game_over");
        }
        Piece pieceAtSource = board.isWithinBorder(source) ? board.pieceAt(source).orElse(null) : null;
        if (pieceAtSource != null && (arbiter.isMoving(pieceAtSource) || arbiter.isJumping(pieceAtSource))) {
            return new MoveResult(false, "motion_in_progress");
        }
        if (pieceAtSource != null && arbiter.isResting(pieceAtSource)) {
            return new MoveResult(false, "resting");
        }

        MoveValidation validation = ruleEngine.validateMove(board, source, destination);
        if (!validation.isValid()) {
            return new MoveResult(false, validation.reason());
        }

        Piece piece = board.pieceAt(source).orElseThrow();
        arbiter.startMotion(piece, source, destination);
        motionRequestTimestampMs.put(piece, gameClockMs);
        return new MoveResult(true, "ok");
    }

    public void requestMove(Position source, Position destination, Consumer<MoveResult> onResult) {
        onResult.accept(requestMove(source, destination));
    }

    public boolean isOccupied(Position position) {
        return board.isWithinBorder(position) && board.pieceAt(position).isPresent();
    }

    public boolean hasActivity() {
        return !arbiter.isIdle();
    }

    public void requestJump(Position cell) {
        if (gameState.gameOver()) {
            return;
        }
        board.pieceAt(cell).ifPresent(piece -> arbiter.startJump(piece, cell));
    }

    public void resign(Piece.Color resigningColor) {
        if (gameState.gameOver()) {
            return;
        }
        Piece.Color winner = resigningColor == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
        gameState.endGame(winner);
        eventBus.publish(new GameOverEvent(winner));
    }

    public void waitMs(long ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("ms must not be negative");
        }
        gameClockMs += ms;
        List<ArrivalEvent> events = arbiter.advanceTime(ms);
        for (ArrivalEvent event : events) {
            if (event.kingCaptured()) {
                Piece.Color loserColor = event.capturedPiece().color();
                Piece.Color winner = loserColor == Piece.Color.WHITE ? Piece.Color.BLACK : Piece.Color.WHITE;
                gameState.endGame(winner);
                eventBus.publish(new GameOverEvent(winner));
            }

            if (event.capturedPiece() != null && event.movedPiece() != null) {
                Piece.Color capturingColor = event.movedPiece().color();
                int points = getPieceValue(event.capturedPiece().kind());
                if (points > 0) {
                    gameState.addScore(capturingColor, points);
                }
            }

            if (event.movedPiece() != null) {
                Long requestTimestamp = motionRequestTimestampMs.remove(event.movedPiece());
                long effectiveTimestamp = requestTimestamp != null ? requestTimestamp : gameClockMs;
                fireMoveEvent(event.movedPiece(), event.from(), event.to(), event.capturedPiece() != null, event.kingCaptured(), event.promoted(), effectiveTimestamp);
            }
        }
    }

    private int getPieceValue(Piece.Kind kind) {
        return switch (kind) {
            case PAWN -> 1;
            case KNIGHT, BISHOP -> 3;
            case ROOK -> 5;
            case QUEEN -> 9;
            case KING -> 0;
        };
    }

    public GameSnapshot snapshot(Position selectedPosition) {
        return snapshot(selectedPosition, List.of(), List.of(), GameSnapshot.DEFAULT_ZOOM);
    }

    public GameSnapshot snapshot(Position selectedPosition, List<String> whiteMoveLog, List<String> blackMoveLog) {
        return snapshot(selectedPosition, whiteMoveLog, blackMoveLog, GameSnapshot.DEFAULT_ZOOM);
    }

    public GameSnapshot snapshot(Position selectedPosition, List<String> whiteMoveLog, List<String> blackMoveLog, double zoom) {
        int width = board.width();
        int height = board.height();
        PieceSnapshot[][] grid = new PieceSnapshot[height][width];
        for (Position position : board.occupiedPositions()) {
            Piece piece = board.pieceAt(position).orElseThrow();
            grid[position.row()][position.col()] = pieceSnapshotOf(piece, position, zoom);
        }
        Set<Position> legalDestinations = legalDestinationsFor(selectedPosition);
        List<SelectionSnapshot> selections = selectedPosition == null
                ? List.of()
                : board.pieceAt(selectedPosition)
                        .map(piece -> new SelectionSnapshot(piece.color(), selectedPosition))
                        .map(List::of)
                        .orElse(List.of());
        return new GameSnapshot(width, height, grid, selections, legalDestinations, gameState.gameOver(),
                               gameState.winner(), gameState.score(Piece.Color.WHITE),
                               gameState.score(Piece.Color.BLACK), whiteMoveLog, blackMoveLog, zoom);
    }

    private Set<Position> legalDestinationsFor(Position selectedPosition) {
        if (selectedPosition == null) {
            return Set.of();
        }
        Piece selectedPiece = board.pieceAt(selectedPosition).orElse(null);
        if (selectedPiece == null) {
            return Set.of();
        }
        if (arbiter.isMoving(selectedPiece) || arbiter.isResting(selectedPiece) || arbiter.isJumping(selectedPiece)) {
            return Set.of();
        }
        return ruleEngine.legalDestinations(board, selectedPosition);
    }

    private PieceSnapshot pieceSnapshotOf(Piece piece, Position position, double zoom) {
        double cellWidth = GameSnapshot.CELL_WIDTH * zoom;
        double cellHeight = GameSnapshot.CELL_HEIGHT * zoom;
        int pixelX = (int) Math.round(position.col() * cellWidth);
        int pixelY = (int) Math.round(position.row() * cellHeight);
        long elapsedMillis;
        long restDurationMs = 0;
        Piece.State state = piece.state();

        Optional<Motion> motion = arbiter.activeMotion(piece);
        if (motion.isPresent()) {
            Motion m = motion.get();
            elapsedMillis = arbiter.motionElapsedMs(piece);

            double progress = easeInOut(Math.min(1.0, (double) elapsedMillis / m.durationMs()));
            pixelX = (int) Math.round(interpolate(m.source().col(), m.destination().col(), progress) * cellWidth);
            pixelY = (int) Math.round(interpolate(m.source().row(), m.destination().row(), progress) * cellHeight);
        } else if (state == Piece.State.JUMPING) {
            elapsedMillis = arbiter.jumpElapsedMs(piece);
        } else if (state == Piece.State.LONG_REST) {
            elapsedMillis = arbiter.longRestElapsedMs(piece);
            restDurationMs = RealTimeArbiter.LONG_REST_MS;
        } else if (state == Piece.State.SHORT_REST) {
            elapsedMillis = arbiter.shortRestElapsedMs(piece);
            restDurationMs = RealTimeArbiter.SHORT_REST_MS;
        } else {
            elapsedMillis = gameClockMs;
        }

        return new PieceSnapshot(piece.id(), piece.color(), piece.kind(), state,
                pixelX, pixelY, elapsedMillis, restDurationMs);
    }

    private static double interpolate(int from, int to, double progress) {
        return from + (to - from) * progress;
    }

    private static double easeInOut(double t) {
        return t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2;
    }
}