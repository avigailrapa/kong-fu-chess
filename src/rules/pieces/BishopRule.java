package src.rules.pieces;

public class BishopRule extends SlidingRule {

    private static final int[][] DIRECTIONS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }
}
