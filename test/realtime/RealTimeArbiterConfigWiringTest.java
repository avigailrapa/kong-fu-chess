package realtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import src.model.Board;
import src.model.Piece;
import src.model.Position;
import src.realtime.ArrivalEvent;
import src.realtime.RealTimeArbiter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RealTimeArbiterConfigWiringTest {

    @TempDir
    Path tempDir;

    private void writeConfig(String code, String state, String json) throws IOException {
        Path dir = tempDir.resolve(code).resolve("states").resolve(state);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"), json);
    }

    private String moveConfig(double speedMetersPerSec, String nextState) {
        return "{\n" +
                "  \"physics\": {\n" +
                "    \"speed_m_per_sec\": " + speedMetersPerSec + ",\n" +
                "    \"next_state_when_finished\": \"" + nextState + "\"\n" +
                "  },\n" +
                "  \"graphics\": {\n" +
                "    \"frames_per_sec\": 12,\n" +
                "    \"is_loop\": true\n" +
                "  }\n" +
                "}";
    }

    @Test
    public void testMoveDurationScalesWithConfiguredSpeed() throws IOException {
        writeConfig("RW", "move", moveConfig(3.0, "long_rest"));

        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board, tempDir.toString());

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        assertEquals(250L, arbiter.activeMotion(rook).orElseThrow().durationMs());
    }

    @Test
    public void testHalvedSpeedDoublesMoveDuration() throws IOException {
        writeConfig("RW", "move", moveConfig(0.75, "long_rest"));

        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board, tempDir.toString());

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));

        assertEquals(1000L, arbiter.activeMotion(rook).orElseThrow().durationMs());
    }

    @Test
    public void testNextStateWhenFinishedIdleSkipsLongRest() throws IOException {
        writeConfig("RW", "move", moveConfig(1.5, "idle"));

        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board, tempDir.toString());

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        List<ArrivalEvent> events = arbiter.advanceTime(1000);

        assertEquals(1, events.size());
        assertFalse(arbiter.isLongResting(rook));
        assertFalse(arbiter.isResting(rook));
    }

    @Test
    public void testNextStateWhenFinishedLongRestAppliesRestAsToday() throws IOException {
        writeConfig("RW", "move", moveConfig(1.5, "long_rest"));

        Board board = new Board(8, 8);
        Piece rook = new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0));
        board.addPiece(rook, new Position(7, 0));
        RealTimeArbiter arbiter = new RealTimeArbiter(board, tempDir.toString());

        arbiter.startMotion(rook, new Position(7, 0), new Position(6, 0));
        arbiter.advanceTime(1000);

        assertTrue(arbiter.isLongResting(rook));
    }
}
