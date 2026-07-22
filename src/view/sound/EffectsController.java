package src.view.sound;

import src.bus.EventBus;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;

import java.util.Optional;

public class EffectsController {

    private static final long BANNER_DURATION_MS = 1500;

    private final SoundPlayer soundPlayer;
    private String bannerText;
    private long bannerRemainingMs;

    public EffectsController(EventBus bus, SoundPlayer soundPlayer) {
        this.soundPlayer = soundPlayer;
        bus.subscribe(MoveEvent.class, this::onMove);
        bus.subscribe(GameOverEvent.class, this::onGameOver);
    }

    public void announceGameStart() {
        soundPlayer.play("game_start");
        bannerText = "GAME START!";
        bannerRemainingMs = BANNER_DURATION_MS;
    }

    public void announceIllegalMove() {
        soundPlayer.play("illegal_move");
    }

    public void announceMoveAccepted() {
        soundPlayer.play("move");
    }

    public void tick(long ms) {
        if (ms < 0) {
            throw new IllegalArgumentException("ms must not be negative");
        }
        if (bannerRemainingMs <= 0) {
            return;
        }
        bannerRemainingMs -= ms;
        if (bannerRemainingMs <= 0) {
            bannerText = null;
        }
    }

    public Optional<String> activeBanner() {
        return Optional.ofNullable(bannerText);
    }

    private void onMove(MoveEvent event) {
        if (event.promotion()) {
            soundPlayer.play("promotion");
        } else if (event.capture()) {
            soundPlayer.play("capture");
        }
    }

    private void onGameOver(GameOverEvent event) {
        soundPlayer.play("game_over");
    }
}
