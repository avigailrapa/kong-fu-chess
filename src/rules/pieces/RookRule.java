package src.rules.pieces;

public class RookRule extends SlidingRule {

    @Override
    protected int[][] directions() {
        return STRAIGHT_DIRECTIONS;
    }
}
