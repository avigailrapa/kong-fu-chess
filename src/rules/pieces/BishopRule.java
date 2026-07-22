package src.rules.pieces;

public class BishopRule extends SlidingRule {

    @Override
    protected int[][] directions() {
        return DIAGONAL_DIRECTIONS;
    }
}
