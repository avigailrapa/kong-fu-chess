package src.rules;

public class QueenRule extends SlidingRule {

    private static final int[][] DIRECTIONS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1},
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    @Override
    protected int[][] directions() {
        return DIRECTIONS;
    }
}
