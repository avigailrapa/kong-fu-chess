package src.input;

import lombok.RequiredArgsConstructor;

import java.util.Scanner;

@RequiredArgsConstructor
public class ConsoleRunner {

    private final CommandParser parser;
    private final CommandRunner runner;

    public void run(String input) {
        try {
            parser.parse(input);
        } catch (CommandParser.InvalidInputException e) {
            System.out.println("ERROR " + e.getMessage());
            return;
        }

        runner.initialize(parser.boardText());
        for (CommandParser.Command command : parser.commands()) {
            runner.run(command);
        }
    }

    public static String readAll(Scanner scanner) {
        StringBuilder text = new StringBuilder();
        while (scanner.hasNextLine()) {
            text.append(scanner.nextLine()).append("\n");
        }
        return text.toString();
    }
}
