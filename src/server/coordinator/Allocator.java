package src.server.coordinator;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Allocator {

    private final List<String> nodeIds;
    private final AtomicInteger cursor = new AtomicInteger();

    public Allocator(List<String> nodeIds) {
        if (nodeIds.isEmpty()) {
            throw new IllegalArgumentException("no game nodes configured");
        }
        this.nodeIds = List.copyOf(nodeIds);
    }

    public String pickNode() {
        int index = Math.floorMod(cursor.getAndIncrement(), nodeIds.size());
        return nodeIds.get(index);
    }
}
