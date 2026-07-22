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

        PieceRules pieceRules = rulesByKind.get(movingPiece.kind());
        if (pieceRules == null) {
            throw new IllegalStateException("No movement rule configured for " + movingPiece.kind());
        }
        return pieceRules.legalDestinations(board, movingPiece);
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

        PieceRules pieceRules = rulesByKind.get(movingPiece.kind());
        if (pieceRules == null) {
            throw new IllegalStateException("No movement rule configured for " + movingPiece.kind());
        }
        Set<Position> legalDestinations = pieceRules.legalDestinations(board, movingPiece);
        if (!legalDestinations.contains(destination)) {
            return MoveValidation.invalid("illegal_piece_move");
        }

        return MoveValidation.ok();
    }
}
