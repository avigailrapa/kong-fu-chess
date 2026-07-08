package engine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;

public class GameClockTest {
    private GameClock clock;

    @BeforeEach
    public void setUp() {
        clock = new GameClock();
    }

    @Test
    public void testInitialTime() {
        assertEquals(0, clock.getCurrentTime());
    }

    @Test
    public void testAdvanceTime() {
        clock.advance(500);
        assertEquals(500, clock.getCurrentTime());
        
        clock.advance(500);
        assertEquals(1000, clock.getCurrentTime());
    }

    @Test
    public void testAdvanceMultipleTimes() {
        clock.advance(100);
        clock.advance(200);
        clock.advance(300);
        assertEquals(600, clock.getCurrentTime());
    }

    @Test
    public void testNegativeAdvance() {
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-1));
    }

    @Test
    public void testZeroAdvance() {
        clock.advance(0);
        assertEquals(0, clock.getCurrentTime());
    }

    @Test
    public void testReset() {
        clock.advance(1000);
        assertEquals(1000, clock.getCurrentTime());
        
        clock.reset();
        assertEquals(0, clock.getCurrentTime());
    }
}