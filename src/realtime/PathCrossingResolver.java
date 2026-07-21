package src.realtime;

import src.model.Piece;
import src.model.Position;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PathCrossingResolver {

    public Map<Piece, Motion> truncateLaterArrivals(Map<Piece, Motion> activeMotions, Map<Piece, Long> motionElapsedMs) {
        Map<Piece, Motion> truncations = new LinkedHashMap<>();
        List<Piece> pieces = new ArrayList<>(activeMotions.keySet());

        for (int a = 0; a < pieces.size(); a++) {
            for (int b = a + 1; b < pieces.size(); b++) {
                Piece pieceA = pieces.get(a);
                Piece pieceB = pieces.get(b);
                if (pieceA.color() != pieceB.color()) {
                    continue;
                }
                if (!isSliding(pieceA.kind()) || !isSliding(pieceB.kind())) {
                    continue;
                }

                Motion motionA = activeMotions.get(pieceA);
                Motion motionB = activeMotions.get(pieceB);
                List<Position> pathA = intermediateCells(motionA);
                List<Position> pathB = intermediateCells(motionB);

                Position crossing = firstSharedCell(pathA, pathB);
                if (crossing == null) {
                    continue;
                }

                int indexA = pathA.indexOf(crossing);
                int indexB = pathB.indexOf(crossing);
                double perCellA = perCellDurationMs(motionA, pathA);
                double perCellB = perCellDurationMs(motionB, pathB);
                long elapsedA = motionElapsedMs.getOrDefault(pieceA, 0L);
                long elapsedB = motionElapsedMs.getOrDefault(pieceB, 0L);

               
                double windowStartA = indexA * perCellA - elapsedA;
                double windowEndA = (indexA + 1) * perCellA - elapsedA;
                double windowStartB = indexB * perCellB - elapsedB;
                double windowEndB = (indexB + 1) * perCellB - elapsedB;

               
                if (windowEndA <= 0 || windowEndB <= 0) {
                    continue;
                }

               
                boolean windowsOverlap = windowStartA < windowEndB && windowStartB < windowEndA;
                if (!windowsOverlap) {
                    continue;
                }

                if (windowStartA > windowStartB) {
                    keepShortest(truncations, pieceA, truncateBeforeStep(motionA, indexA, perCellA));
                } else if (windowStartB > windowStartA) {
                    keepShortest(truncations, pieceB, truncateBeforeStep(motionB, indexB, perCellB));
                }
            }
        }

        return truncations;
    }

    private void keepShortest(Map<Piece, Motion> truncations, Piece piece, Motion candidate) {
        Motion existing = truncations.get(piece);
        if (existing == null || candidate.durationMs() < existing.durationMs()) {
            truncations.put(piece, candidate);
        }
    }

    private Motion truncateBeforeStep(Motion motion, int crossingIndex, double perCellDurationMs) {
        Position newDestination = cellAtDistance(motion, crossingIndex);
        long newDurationMs = Math.round(crossingIndex * perCellDurationMs);
        return new Motion(motion.piece(), motion.source(), newDestination, newDurationMs);
    }

    private Position firstSharedCell(List<Position> pathA, List<Position> pathB) {
        for (Position cell : pathA) {
            if (pathB.contains(cell)) {
                return cell;
            }
        }
        return null;
    }

    private double perCellDurationMs(Motion motion, List<Position> intermediateCells) {
        int totalCells = intermediateCells.size() + 1;  
        return (double) motion.durationMs() / totalCells;
    }

  
    private List<Position> intermediateCells(Motion motion) {
        List<Position> cells = new ArrayList<>();
        int deltaRow = motion.destination().row() - motion.source().row();
        int deltaCol = motion.destination().col() - motion.source().col();
        boolean isStraightLine = deltaRow == 0 || deltaCol == 0 || Math.abs(deltaRow) == Math.abs(deltaCol);
        if (!isStraightLine) {
            return cells;
        }

        int rowStep = Integer.signum(deltaRow);
        int colStep = Integer.signum(deltaCol);
        Position current = motion.source();

        while (!current.equals(motion.destination())) {
            current = new Position(current.row() + rowStep, current.col() + colStep);
            if (current.equals(motion.destination())) {
                break;
            }
            cells.add(current);
        }
        return cells;
    }

    private Position cellAtDistance(Motion motion, int distance) {
        int rowStep = Integer.signum(motion.destination().row() - motion.source().row());
        int colStep = Integer.signum(motion.destination().col() - motion.source().col());
        return new Position(motion.source().row() + rowStep * distance, motion.source().col() + colStep * distance);
    }

    private boolean isSliding(Piece.Kind kind) {
        return kind == Piece.Kind.ROOK || kind == Piece.Kind.BISHOP || kind == Piece.Kind.QUEEN;
    }
}
