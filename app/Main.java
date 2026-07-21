package app;

import src.input.CommandParser;
import src.input.CommandRunner;
import src.input.ConsoleRunner;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String input = ConsoleRunner.readAll(scanner);

        CommandParser parser = new CommandParser();
        CommandRunner runner = new CommandRunner();
        ConsoleRunner consoleRunner = new ConsoleRunner(parser, runner);
        consoleRunner.run(input);
    }
}
