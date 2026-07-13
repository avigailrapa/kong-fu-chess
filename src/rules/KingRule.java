package src.rules;

public class KingRule extends FixedOffsetRule {

    private static final int[][] OFFSETS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1}, {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };

    @Override
    protected int[][] offsets() {
        return OFFSETS;
    }
}
