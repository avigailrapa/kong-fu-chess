package src.view;

import src.engine.GameEngine;

public class GameLoop {

    private final GameEngine engine;
    private boolean wasActive = true;

    public GameLoop(GameEngine engine) {
        this.engine = engine;
    }

    public boolean tick(long ms) {
        engine.waitMs(ms);
        boolean active = engine.hasActivity();
        boolean shouldRepaint = active || wasActive;
        wasActive = active;
        return shouldRepaint;
    }
}
