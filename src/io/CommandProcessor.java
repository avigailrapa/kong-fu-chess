package features;

import java.util.Scanner;

public class CommandProcessor {
    private final GameEngine gameEngine;

    public CommandProcessor(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }

    public void processCommands(Scanner scanner) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("click")) {
                handleClickCommand(line);
            } else if (line.startsWith("jump")) {
                handleJumpCommand(line);
            } else if (line.startsWith("wait")) {
                handleWaitCommand(line);
            } else if (line.equals("print board")) {
                gameEngine.printBoard();
            }
        }
    }

    private void handleClickCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            gameEngine.handleClick(x, y);
        } catch (NumberFormatException e) {
            // Silently ignore invalid input
        }
    }

    private void handleJumpCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 3) {
            return;
        }
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            gameEngine.handleJump(x, y);
        } catch (NumberFormatException e) {
            // Silently ignore invalid input
        }
    }

    private void handleWaitCommand(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return;
        }
        try {
            long ms = Long.parseLong(parts[1]);
            gameEngine.advanceTime(ms);
        } catch (NumberFormatException e) {
            // Silently ignore invalid input
        }
    }
}
