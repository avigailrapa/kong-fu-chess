package src.rules;

import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Map;
import java.util.Set;

public class RuleEngine {

    private final Map<Piece.Kind, PieceRules> rulesByKind;

    public RuleEngine(Map<Piece.Kind, PieceRules> rulesByKind) {
        this.rulesByKind = rulesByKind;
    }

    public MoveValidation validateMove(IBoard board, Position source, Position destination) {
        if (!board.isWithinBorder(source) || !board.isWithinBorder(destination)) {
            return MoveValidation.invalid("outside_board");
        }

        Piece movingPiece = board.getPieceAt(source).orElse(null);
        if (movingPiece == null) {
            return MoveValidation.invalid("empty_source");
        }

        Piece targetPiece = board.getPieceAt(destination).orElse(null);
        if (targetPiece != null && targetPiece.getColor() == movingPiece.getColor()) {
            return MoveValidation.invalid("friendly_destination");
        }

        PieceRules pieceRules = rulesByKind.get(movingPiece.getKind());
        if (pieceRules == null) {
            throw new IllegalStateException("No movement rule configured for " + movingPiece.getKind());
        }
        Set<Position> legalDestinations = pieceRules.legalDestinations(board, movingPiece);
        if (!legalDestinations.contains(destination)) {
            return MoveValidation.invalid("illegal_piece_move");
        }

        return MoveValidation.ok();
    }
}
