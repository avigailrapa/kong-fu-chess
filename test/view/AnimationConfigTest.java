package view;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import src.view.AnimationConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class AnimationConfigTest {

    @TempDir
    Path tempDir;

    private Path writeConfig(String json) throws IOException {
        Path path = tempDir.resolve("config.json");
        Files.writeString(path, json);
        return path;
    }

    @Test
    public void testLoadParsesAllFields() throws IOException {
        Path path = writeConfig("{\n" +
                "  \"physics\": {\n" +
                "    \"speed_m_per_sec\": 1.5,\n" +
                "    \"next_state_when_finished\": \"long_rest\"\n" +
                "  },\n" +
                "  \"graphics\": {\n" +
                "    \"frames_per_sec\": 12,\n" +
                "    \"is_loop\": true\n" +
                "  }\n" +
                "}");

        AnimationConfig config = AnimationConfig.load(path.toString());

        assertEquals(1.5, config.speedMetersPerSecond());
        assertEquals("long_rest", config.nextStateWhenFinished());
        assertEquals(12, config.framesPerSecond());
        assertTrue(config.isLoop());
    }

    @Test
    public void testZeroFramesPerSecondThrowsClearError() throws IOException {
        Path path = writeConfig("{\n" +
                "  \"physics\": {\n" +
                "    \"speed_m_per_sec\": 0.0,\n" +
                "    \"next_state_when_finished\": \"idle\"\n" +
                "  },\n" +
                "  \"graphics\": {\n" +
                "    \"frames_per_sec\": 0,\n" +
                "    \"is_loop\": true\n" +
                "  }\n" +
                "}");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> AnimationConfig.load(path.toString()));
        assertTrue(exception.getMessage().contains("frames_per_sec"));
    }

    @Test
    public void testMissingFileThrowsIllegalArgumentException() {
        String missingPath = tempDir.resolve("does-not-exist.json").toString();

        assertThrows(IllegalArgumentException.class, () -> AnimationConfig.load(missingPath));
    }
}
