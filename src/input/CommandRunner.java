package src.input;

import src.engine.GameEngine;
import src.io.BoardParser;
import src.io.BoardPrinter;
import src.model.Board;

public class CommandRunner {

    private final BoardParser boardParser = new BoardParser();
    private final BoardPrinter boardPrinter = new BoardPrinter();

    private Board board;
    private GameEngine engine;
    private ClickHandler clickHandler;

    public void initialize(String boardText) {
        board = boardParser.parse(boardText);
        engine = GameEngine.fromBoard(board);
        BoardMapper mapper = new BoardMapper(board.width(), board.height());
        clickHandler = new ClickHandler(mapper, engine);
    }

    public void run(CommandParser.Command command) {
        switch (command) {
            case CommandParser.ClickCommand click -> clickHandler.click(click.x(), click.y());
            case CommandParser.WaitCommand wait -> engine.waitMs(wait.milliseconds());
            case CommandParser.JumpCommand jump -> clickHandler.jump(jump.x(), jump.y());
            case CommandParser.PrintBoardCommand _ -> System.out.println(boardPrinter.print(board));
            default -> throw new IllegalArgumentException("Unknown command: " + command);
        }
    }
}
