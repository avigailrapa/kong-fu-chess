package server.allocator;

import org.junit.jupiter.api.Test;
import src.server.allocator.Allocator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AllocatorTest {

    @Test
    public void testRoundRobinCyclesThroughNodes() {
        Allocator allocator = new Allocator(List.of("node-1", "node-2", "node-3"));

        assertEquals("node-1", allocator.pickNode());
        assertEquals("node-2", allocator.pickNode());
        assertEquals("node-3", allocator.pickNode());
        assertEquals("node-1", allocator.pickNode());
    }

    @Test
    public void testSingleNodeAlwaysReturnsSameNode() {
        Allocator allocator = new Allocator(List.of("only-node"));

        assertEquals("only-node", allocator.pickNode());
        assertEquals("only-node", allocator.pickNode());
    }

    @Test
    public void testEmptyNodeListThrows() {
        assertThrows(IllegalArgumentException.class, () -> new Allocator(List.of()));
    }
}
