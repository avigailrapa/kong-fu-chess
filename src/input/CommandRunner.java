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
        if (command instanceof CommandParser.ClickCommand) {
            CommandParser.ClickCommand click = (CommandParser.ClickCommand) command;
            controller.click(click.x, click.y);

        } else if (command instanceof CommandParser.WaitCommand) {
            CommandParser.WaitCommand wait = (CommandParser.WaitCommand) command;
            engine.waitMs(wait.milliseconds);

        } else if (command instanceof CommandParser.JumpCommand) {
            CommandParser.JumpCommand jump = (CommandParser.JumpCommand) command;
            controller.jump(jump.x, jump.y);

        } else if (command instanceof CommandParser.PrintBoardCommand) {
            System.out.println(boardPrinter.print(engine.settledBoard()));
        }
    }
}
