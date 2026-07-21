package src.server;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public class ActivityLog {

    private final PrintWriter writer;

    public ActivityLog(String filePath) {
        try {
            this.writer = new PrintWriter(new FileWriter(filePath, true), true);
        } catch (IOException e) {
            throw new IllegalStateException("could not open activity log at " + filePath, e);
        }
    }

    public void log(String event) {
        String line = Instant.now() + " " + event;
        System.out.println(line);
        writer.println(line);
    }
}
