package app;

import src.bus.EventBus;
import src.view.sound.ClipSoundPlayer;
import src.view.sound.EffectsController;

final class AppSupport {

    private AppSupport() {
    }

    static void disableHiDpiScaling() {
        System.setProperty("sun.java2d.uiScale", "1");
        System.setProperty("sun.java2d.dpiaware", "true");
    }

    static EffectsController startEffects(EventBus bus) {
        EffectsController effects = new EffectsController(bus,
                new ClipSoundPlayer(ClipSoundPlayer.DEFAULT_SOUNDS_ROOT));
        effects.announceGameStart();
        return effects;
    }
}
