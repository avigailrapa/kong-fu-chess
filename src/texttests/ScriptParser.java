package src.texttests;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {

    private static final Pattern CLICK_PATTERN = Pattern.compile("^click\\s+(-?\\d+)\\s+(-?\\d+)$");
    private static final Pattern WAIT_PATTERN = Pattern.compile("^wait\\s+(\\d+)$");

    public List<ScriptCommand> parse(String script) {
        List<ScriptCommand> commands = new ArrayList<>();
        String[] lines = script.split("\\r?\\n");

        int i = 0;
        while (i < lines.length) {
            String line = lines[i].trim();

            if (line.isEmpty()) {
                i++;
                continue;
            }

            if (line.equals("Board:")) {
                List<String> dataLines = new ArrayList<>();
                i = collectDataLines(lines, i + 1, dataLines);
                commands.add(new ScriptCommand.BoardCommand(String.join("\n", dataLines)));
                continue;
            }

            if (line.equals("print board")) {
                List<String> dataLines = new ArrayList<>();
                i = collectDataLines(lines, i + 1, dataLines);
                commands.add(new ScriptCommand.PrintBoardCommand(String.join("\n", dataLines)));
                continue;
            }

            Matcher clickMatcher = CLICK_PATTERN.matcher(line);
            if (clickMatcher.matches()) {
                commands.add(new ScriptCommand.ClickCommand(
                        Integer.parseInt(clickMatcher.group(1)),
                        Integer.parseInt(clickMatcher.group(2))));
                i++;
                continue;
            }

            Matcher waitMatcher = WAIT_PATTERN.matcher(line);
            if (waitMatcher.matches()) {
                commands.add(new ScriptCommand.WaitCommand(Long.parseLong(waitMatcher.group(1))));
                i++;
                continue;
            }

            throw new IllegalArgumentException("Unrecognized script line: " + line);
        }

        return commands;
    }

    private int collectDataLines(String[] lines, int start, List<String> out) {
        int i = start;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            if (isCommandLine(trimmed)) {
                break;
            }
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
            i++;
        }
        return i;
    }

    private boolean isCommandLine(String line) {
        return line.equals("Board:")
                || line.equals("print board")
                || CLICK_PATTERN.matcher(line).matches()
                || WAIT_PATTERN.matcher(line).matches();
    }
}
