import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class JumpStateTest {
    private JumpState jump;

    @Before
    public void setUp() {
        jump = new JumpState(3, 3, 1000, 2000);
    }

    @Test
    public void testJumpStateCreation() {
        assertEquals(3, jump.getRow());
        assertEquals(3, jump.getCol());
        assertEquals(1000, jump.getStartTime());
        assertEquals(2000, jump.getEndTime());
    }

    @Test
    public void testIsAtLocation() {
        assertTrue(jump.isAtLocation(3, 3));
        assertFalse(jump.isAtLocation(3, 4));
        assertFalse(jump.isAtLocation(4, 3));
        assertFalse(jump.isAtLocation(0, 0));
    }

    @Test
    public void testIsActiveAt() {
        assertFalse(jump.isActiveAt(999));
        assertTrue(jump.isActiveAt(1000));
        assertTrue(jump.isActiveAt(1500));
        assertTrue(jump.isActiveAt(2000));
        assertFalse(jump.isActiveAt(2001));
    }

    @Test
    public void testHasLanded() {
        assertFalse(jump.hasLanded(999));
        assertFalse(jump.hasLanded(2000));
        assertTrue(jump.hasLanded(2001));
    }

    @Test
    public void testInvalidTimeRange() {
        assertThrows(IllegalArgumentException.class, () -> new JumpState(3, 3, 2000, 1000));
    }

    @Test
    public void testEqualStartAndEndTime() {
        JumpState instantJump = new JumpState(3, 3, 1000, 1000);
        assertFalse(instantJump.isActiveAt(999));
        assertTrue(instantJump.isActiveAt(1000));
        assertTrue(instantJump.hasLanded(1001));
    }

    @Test
    public void testMultipleJumps() {
        JumpState jump1 = new JumpState(0, 0, 500, 1500);
        JumpState jump2 = new JumpState(7, 7, 2000, 3000);

        assertTrue(jump1.isActiveAt(1000));
        assertFalse(jump2.isActiveAt(1000));
        
        assertFalse(jump1.isActiveAt(2000));
        assertTrue(jump2.isActiveAt(2000));
    }
}
