import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class PendingMoveTest {
    private PendingMove move;

    @Before
    public void setUp() {
        move = new PendingMove(0, 0, 2, 2, "wQ", 1000);
    }

    @Test
    public void testPendingMoveCreation() {
        assertEquals(0, move.getStartRow());
        assertEquals(0, move.getStartCol());
        assertEquals(2, move.getTargetRow());
        assertEquals(2, move.getTargetCol());
        assertEquals("wQ", move.getPiece());
        assertEquals(1000, move.getArrivalTime());
    }

    @Test
    public void testHasArrived() {
        assertFalse(move.hasArrived(999));
        assertTrue(move.hasArrived(1000));
        assertTrue(move.hasArrived(2000));
    }

    @Test
    public void testOriginatesFrom() {
        assertTrue(move.originatesFrom(0, 0));
        assertFalse(move.originatesFrom(0, 1));
        assertFalse(move.originatesFrom(1, 0));
        assertFalse(move.originatesFrom(2, 2));
    }

    @Test
    public void testTargetsCell() {
        assertTrue(move.targetsCell(2, 2));
        assertFalse(move.targetsCell(2, 1));
        assertFalse(move.targetsCell(1, 2));
        assertFalse(move.targetsCell(0, 0));
    }

    @Test
    public void testNegativeArrivalTime() {
        assertThrows(IllegalArgumentException.class, () -> new PendingMove(0, 0, 2, 2, "wQ", -1));
    }

    @Test
    public void testZeroArrivalTime() {
        PendingMove zeroMove = new PendingMove(0, 0, 2, 2, "wQ", 0);
        assertTrue(zeroMove.hasArrived(0));
    }

    @Test
    public void testMultipleMoves() {
        PendingMove move1 = new PendingMove(0, 0, 1, 1, "wK", 500);
        PendingMove move2 = new PendingMove(7, 7, 6, 6, "bR", 1000);
        
        assertFalse(move1.hasArrived(499));
        assertTrue(move1.hasArrived(500));
        
        assertFalse(move2.hasArrived(999));
        assertTrue(move2.hasArrived(1000));
    }
}
