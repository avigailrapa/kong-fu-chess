package src.rules;

import lombok.RequiredArgsConstructor;
import src.model.IBoard;
import src.model.Piece;
import src.model.Position;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class RuleEngine {

    private final Map<Piece.Kind, PieceRules> rulesByKind;

    public Set<Position> legalDestinations(IBoard board, Position source) {
        if (!board.isWithinBorder(source)) {
            return Set.of();
        }

        Piece movingPiece = board.pieceAt(source).orElse(null);
        if (movingPiece == null) {
            return Set.of();
        }

        return rulesFor(movingPiece.kind()).legalDestinations(board, movingPiece);
    }

    public MoveValidation validateMove(IBoard board, Position source, Position destination) {
        if (!board.isWithinBorder(source) || !board.isWithinBorder(destination)) {
            return MoveValidation.invalid("outside_board");
        }

        Piece movingPiece = board.pieceAt(source).orElse(null);
        if (movingPiece == null) {
            return MoveValidation.invalid("empty_source");
        }

        Piece targetPiece = board.pieceAt(destination).orElse(null);
        if (targetPiece != null && targetPiece.color() == movingPiece.color()) {
            return MoveValidation.invalid("friendly_destination");
        }

        Set<Position> legalDestinations = rulesFor(movingPiece.kind()).legalDestinations(board, movingPiece);
        if (!legalDestinations.contains(destination)) {
            return MoveValidation.invalid("illegal_piece_move");
        }

        return MoveValidation.ok();
    }

    private PieceRules rulesFor(Piece.Kind kind) {
        PieceRules pieceRules = rulesByKind.get(kind);
        if (pieceRules == null) {
            throw new IllegalStateException("No movement rule configured for " + kind);
        }
        return pieceRules;
    }
}
