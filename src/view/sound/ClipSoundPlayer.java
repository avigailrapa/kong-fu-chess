package src.view.sound;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.awt.Toolkit;
import java.io.File;

public class ClipSoundPlayer implements SoundPlayer {

    private final String soundsRoot;

    public ClipSoundPlayer(String soundsRoot) {
        this.soundsRoot = soundsRoot;
    }

    @Override
    public void play(String name) {
        File file = new File(soundsRoot, name + ".wav");
        if (!file.isFile()) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.open(stream);
            clip.start();
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}
