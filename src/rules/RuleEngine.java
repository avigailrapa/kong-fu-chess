package rules;

import model.IBoard;
import model.Piece;
import model.Position;

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
        Set<Position> legalDestinations = pieceRules.legalDestinations(board, movingPiece);
        if (!legalDestinations.contains(destination)) {
            return MoveValidation.invalid("illegal_piece_move");
        }

        return MoveValidation.ok();
    }
}
