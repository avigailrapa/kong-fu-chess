package src.view;

import lombok.RequiredArgsConstructor;
import src.engine.GameEngine;

@RequiredArgsConstructor
public class GameLoop {

    private final GameEngine engine;
    private boolean wasActive = true;

    public boolean tick(long ms) {
        engine.waitMs(ms);
        boolean active = engine.hasActivity();
        boolean shouldRepaint = active || wasActive;
        wasActive = active;
        return shouldRepaint;
    }
}
