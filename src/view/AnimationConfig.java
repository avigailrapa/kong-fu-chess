package src.view;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record AnimationConfig(double speedMetersPerSecond, String nextStateWhenFinished,
                               int framesPerSecond, boolean loop) {

    private static final Pattern SPEED = Pattern.compile("\"speed_m_per_sec\"\\s*:\\s*([-\\d.]+)");
    private static final Pattern NEXT_STATE = Pattern.compile("\"next_state_when_finished\"\\s*:\\s*\"(\\w+)\"");
    private static final Pattern FRAMES_PER_SEC = Pattern.compile("\"frames_per_sec\"\\s*:\\s*([-\\d.]+)");
    private static final Pattern IS_LOOP = Pattern.compile("\"is_loop\"\\s*:\\s*(true|false)");

    public boolean isLoop() {
        return loop;
    }

    public static AnimationConfig load(String path) {
        String json;
        try {
            json = Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot read animation config: " + path, e);
        }
        int framesPerSecond = (int) Math.round(parseDouble(FRAMES_PER_SEC, json, path));
        if (framesPerSecond <= 0) {
            throw new IllegalArgumentException("frames_per_sec must be positive in " + path + " but was " + framesPerSecond);
        }

        return new AnimationConfig(
                parseDouble(SPEED, json, path),
                parseString(NEXT_STATE, json, path),
                framesPerSecond,
                parseBoolean(IS_LOOP, json, path));
    }

    private static double parseDouble(Pattern pattern, String json, String path) {
        return Double.parseDouble(require(pattern, json, path));
    }

    private static String parseString(Pattern pattern, String json, String path) {
        return require(pattern, json, path);
    }

    private static boolean parseBoolean(Pattern pattern, String json, String path) {
        return Boolean.parseBoolean(require(pattern, json, path));
    }

    private static String require(Pattern pattern, String json, String path) {
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Missing field " + pattern.pattern() + " in " + path);
        }
        return matcher.group(1);
    }
}
