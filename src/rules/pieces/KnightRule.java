package src.rules.pieces;

public class KnightRule extends FixedOffsetRule {

    private static final int[][] OFFSETS = {
            {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2},
            {1, -2}, {1, 2}, {2, -1}, {2, 1}
    };

    @Override
    protected int[][] offsets() {
        return OFFSETS;
    }
}
