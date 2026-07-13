package src.input;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    public static class InvalidInputException extends RuntimeException {
        public InvalidInputException(String errorCode) {
            super(errorCode);
        }
    }

    public interface Command {
    }

    public static class ClickCommand implements Command {
        public final int x;
        public final int y;

        public ClickCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class WaitCommand implements Command {
        public final long milliseconds;

        public WaitCommand(long milliseconds) {
            this.milliseconds = milliseconds;
        }
    }

    public static class JumpCommand implements Command {
        public final int x;
        public final int y;

        public JumpCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class PrintBoardCommand implements Command {
    }

    private static final Pattern CLICK_PATTERN = Pattern.compile("^click\\s+(-?\\d+)\\s+(-?\\d+)$");
    private static final Pattern WAIT_PATTERN = Pattern.compile("^wait\\s+(\\d+)$");
    private static final Pattern JUMP_PATTERN = Pattern.compile("^jump\\s+(-?\\d+)\\s+(-?\\d+)$");
    private static final Pattern VALID_TOKEN = Pattern.compile("^\\.$|^[wb][KQRBNP]$");

    private String boardText;
    private List<Command> commands;

    public String boardText() {
        return boardText;
    }

    public List<Command> commands() {
        return commands;
    }

    public void parse(String input) {
        String[] lines = input.split("\\r?\\n");
        int i = 0;

        while (i < lines.length && !lines[i].trim().equals("Board:")) {
            i++;
        }
        i++;

        List<String> boardLines = new ArrayList<>();
        while (i < lines.length && !lines[i].trim().equals("Commands:")) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                boardLines.add(line);
            }
            i++;
        }
        i++;

        validateBoardTokens(boardLines);

        List<Command> parsedCommands = new ArrayList<>();
        while (i < lines.length) {
            String line = lines[i].trim();
            if (!line.isEmpty()) {
                parsedCommands.add(parseCommand(line));
            }
            i++;
        }

        this.boardText = String.join("\n", boardLines);
        this.commands = parsedCommands;
    }

    private void validateBoardTokens(List<String> boardLines) {
        int width = -1;
        for (String line : boardLines) {
            String[] tokens = line.split("\\s+");
            for (String token : tokens) {
                if (!VALID_TOKEN.matcher(token).matches()) {
                    throw new InvalidInputException("UNKNOWN_TOKEN");
                }
            }
            if (width == -1) {
                width = tokens.length;
            } else if (tokens.length != width) {
                throw new InvalidInputException("ROW_WIDTH_MISMATCH");
            }
        }
    }

    private Command parseCommand(String line) {
        if (line.equals("print board")) {
            return new PrintBoardCommand();
        }

        Matcher clickMatcher = CLICK_PATTERN.matcher(line);
        if (clickMatcher.matches()) {
            return new ClickCommand(
                    Integer.parseInt(clickMatcher.group(1)),
                    Integer.parseInt(clickMatcher.group(2)));
        }

        Matcher waitMatcher = WAIT_PATTERN.matcher(line);
        if (waitMatcher.matches()) {
            return new WaitCommand(Long.parseLong(waitMatcher.group(1)));
        }

        Matcher jumpMatcher = JUMP_PATTERN.matcher(line);
        if (jumpMatcher.matches()) {
            return new JumpCommand(
                    Integer.parseInt(jumpMatcher.group(1)),
                    Integer.parseInt(jumpMatcher.group(2)));
        }

        throw new InvalidInputException("UNKNOWN_COMMAND");
    }
}
