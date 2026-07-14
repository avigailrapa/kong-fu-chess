package src.input;

import src.engine.GameEngine;
import src.io.BoardParser;
import src.io.BoardPrinter;
import src.model.Board;

public class CommandRunner {

    private final BoardParser boardParser = new BoardParser();
    private final BoardPrinter boardPrinter = new BoardPrinter();

    private GameEngine engine;
    private Controller controller;

    public void initialize(String boardText) {
        Board board = boardParser.parse(boardText);
        engine = GameEngine.fromBoard(board);
        BoardMapper mapper = new BoardMapper(board.getWidth(), board.getHeight());
        controller = new Controller(mapper, engine);
    }

    public void run(CommandParser.Command command) {
        switch (command) {
            case CommandParser.ClickCommand click -> controller.click(click.x(), click.y());
            case CommandParser.WaitCommand wait -> engine.waitMs(wait.milliseconds());
            case CommandParser.JumpCommand jump -> controller.jump(jump.x(), jump.y());
            case CommandParser.PrintBoardCommand ignored -> System.out.println(boardPrinter.print(engine.settledBoard()));
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
}
