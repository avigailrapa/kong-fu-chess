package src.view.sound;

import lombok.RequiredArgsConstructor;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Toolkit;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class ClipSoundPlayer implements SoundPlayer {

    public static final String DEFAULT_SOUNDS_ROOT = "assets";

    private final String soundsRoot;
    private final Map<String, Clip> clips = new ConcurrentHashMap<>();

    @Override
    public void play(String name) {
        Clip clip = clips.computeIfAbsent(name, this::loadClip);
        if (clip == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        clip.stop();
        clip.flush();
        clip.setFramePosition(0);
        clip.start();
    }

    private Clip loadClip(String name) {
        File file = new File(soundsRoot, name + ".wav");
        if (!file.isFile()) {
            return null;
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            return clip;
        } catch (Exception e) {
            return null;
        }
    }
}
