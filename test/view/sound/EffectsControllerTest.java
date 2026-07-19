package view.sound;

import org.junit.jupiter.api.Test;
import src.bus.EventBus;
import src.engine.GameOverEvent;
import src.engine.MoveEvent;
import src.model.Piece;
import src.model.Position;
import src.view.sound.EffectsController;
import src.view.sound.SoundPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EffectsControllerTest {

    private static class FakeSoundPlayer implements SoundPlayer {
        final List<String> played = new ArrayList<>();

        @Override
        public void play(String name) {
            played.add(name);
        }
    }

    private MoveEvent moveEvent(boolean capture) {
        return new MoveEvent(Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0), new Position(4, 0),
                capture, false, 0L);
    }

    @Test
    public void testNoBannerBeforeAnyEvent() {
        EffectsController effects = new EffectsController(new EventBus(), new FakeSoundPlayer());

        assertTrue(effects.activeBanner().isEmpty());
    }

    @Test
    public void testAnnounceGameStartShowsBanner() {
        EffectsController effects = new EffectsController(new EventBus(), new FakeSoundPlayer());

        effects.announceGameStart();

        assertTrue(effects.activeBanner().isPresent());
        assertTrue(effects.activeBanner().get().contains("START"));
    }

    @Test
    public void testAnnounceGameStartPlaysSound() {
        FakeSoundPlayer soundPlayer = new FakeSoundPlayer();
        EffectsController effects = new EffectsController(new EventBus(), soundPlayer);

        effects.announceGameStart();

        assertEquals(List.of("game_start"), soundPlayer.played);
    }

    @Test
    public void testBannerStillActiveBeforeDurationElapses() {
        EffectsController effects = new EffectsController(new EventBus(), new FakeSoundPlayer());

        effects.announceGameStart();
        effects.tick(500);

        assertTrue(effects.activeBanner().isPresent());
    }

    @Test
    public void testBannerExpiresAfterDurationElapses() {
        EffectsController effects = new EffectsController(new EventBus(), new FakeSoundPlayer());

        effects.announceGameStart();
        effects.tick(5000);

        assertTrue(effects.activeBanner().isEmpty());
    }

    @Test
    public void testCapturingMoveEventPlaysCaptureSound() {
        EventBus bus = new EventBus();
        FakeSoundPlayer soundPlayer = new FakeSoundPlayer();
        new EffectsController(bus, soundPlayer);

        bus.publish(moveEvent(true));

        assertEquals(List.of("capture"), soundPlayer.played);
    }

    @Test
    public void testNonCapturingMoveEventDoesNotPlayCaptureSound() {
        EventBus bus = new EventBus();
        FakeSoundPlayer soundPlayer = new FakeSoundPlayer();
        new EffectsController(bus, soundPlayer);

        bus.publish(moveEvent(false));

        assertTrue(soundPlayer.played.isEmpty());
    }

    @Test
    public void testGameOverEventPlaysGameOverSound() {
        EventBus bus = new EventBus();
        FakeSoundPlayer soundPlayer = new FakeSoundPlayer();
        new EffectsController(bus, soundPlayer);

        bus.publish(new GameOverEvent(Piece.Color.WHITE));

        assertEquals(List.of("game_over"), soundPlayer.played);
    }

    @Test
    public void testGameOverEventDoesNotShowBannerWhenNoneWasActive() {
        EventBus bus = new EventBus();
        EffectsController effects = new EffectsController(bus, new FakeSoundPlayer());

        bus.publish(new GameOverEvent(Piece.Color.WHITE));

        assertTrue(effects.activeBanner().isEmpty());
    }

    @Test
    public void testGameOverEventDoesNotChangeAnAlreadyActiveBanner() {
        EventBus bus = new EventBus();
        EffectsController effects = new EffectsController(bus, new FakeSoundPlayer());
        effects.announceGameStart();

        bus.publish(new GameOverEvent(Piece.Color.WHITE));

        assertEquals(java.util.Optional.of("GAME START!"), effects.activeBanner());
    }

    @Test
    public void testTickRejectsNegativeMilliseconds() {
        EffectsController effects = new EffectsController(new EventBus(), new FakeSoundPlayer());

        assertThrows(IllegalArgumentException.class, () -> effects.tick(-1));
    }
}
