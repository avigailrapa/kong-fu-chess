package engine;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.realtime.RealTimeArbiter;
import src.rules.RuleEngine;
import src.rules.pieces.RookRule;
import src.rules.PieceRules;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ScoreDisplayTest {

    @Test
    public void capturesIncreaseScoreInSnapshot() {
        Board board = new Board(8, 8);
        Piece whiteRook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(whiteRook, new Position(7, 0));
        board.addPiece(new Piece("q1", Piece.Color.BLACK, Piece.Kind.QUEEN, new Position(4, 0)), new Position(4, 0));

        GameState gameState = new GameState();
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        GameEngine engine = new GameEngine(board, gameState, new RuleEngine(rulesByKind), new RealTimeArbiter(board));

        engine.requestMove(new Position(7, 0), new Position(4, 0));
        engine.waitMs(3000);

        assertEquals(9, gameState.score(Piece.Color.WHITE));
        assertEquals(0, gameState.score(Piece.Color.BLACK));
        assertEquals(9, engine.snapshot(null).whiteScore());
    }
}
