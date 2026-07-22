package src.rules.pieces;

public class QueenRule extends SlidingRule {

    private static final int[][] DIRECTIONS = concat(STRAIGHT_DIRECTIONS, DIAGONAL_DIRECTIONS);

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }

    private static int[][] concat(int[][] a, int[][] b) {
        int[][] merged = new int[a.length + b.length][];
        System.arraycopy(a, 0, merged, 0, a.length);
        System.arraycopy(b, 0, merged, a.length, b.length);
        return merged;
    }
}
