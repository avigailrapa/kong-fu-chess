package src.texttests;

import src.engine.GameEngine;
import src.input.BoardMapper;
import src.input.Controller;
import src.io.BoardParser;
import src.io.BoardPrinter;
import src.model.Board;

import java.util.List;

public class ScriptRunner {

    private final ScriptParser scriptParser = new ScriptParser();
    private final BoardParser boardParser = new BoardParser();
    private final BoardPrinter boardPrinter = new BoardPrinter();

    private GameEngine engine;
    private Controller controller;

    public void run(String script) {
        List<ScriptCommand> commands = scriptParser.parse(script);
        for (ScriptCommand command : commands) {
            execute(command);
        }
    }

    private void execute(ScriptCommand command) {
        if (command instanceof ScriptCommand.BoardCommand) {
            ScriptCommand.BoardCommand boardCommand = (ScriptCommand.BoardCommand) command;
            initializeGame(boardCommand.boardText());
            
        } else if (command instanceof ScriptCommand.ClickCommand) {
            ScriptCommand.ClickCommand clickCommand = (ScriptCommand.ClickCommand) command;
            requireInitializedGame();
            controller.click(clickCommand.x(), clickCommand.y());
            
        } else if (command instanceof ScriptCommand.WaitCommand) {
            ScriptCommand.WaitCommand waitCommand = (ScriptCommand.WaitCommand) command;
            requireInitializedGame();
            engine.waitMs(waitCommand.milliseconds());
            
        } else if (command instanceof ScriptCommand.PrintBoardCommand) {
            ScriptCommand.PrintBoardCommand printBoardCommand = (ScriptCommand.PrintBoardCommand) command;
            requireInitializedGame();
            assertBoardPrints(printBoardCommand.expectedText());
        }
    }

    private void initializeGame(String boardText) {
        Board board = boardParser.parse(boardText);
        engine = GameEngine.fromBoard(board);
        BoardMapper mapper = new BoardMapper(board.getWidth(), board.getHeight());
        controller = new Controller(mapper, engine);
    }

    private void assertBoardPrints(String expectedText) {
        String actual = boardPrinter.print(engine.settledBoard());
        if (!actual.equals(expectedText)) {
            throw new AssertionError("print board mismatch.\nExpected:\n" + expectedText + "\nActual:\n" + actual);
        }
    }

    private void requireInitializedGame() {
        if (engine == null || controller == null) {
            throw new IllegalStateException("No Board has been set up yet; script must start with a Board command");
        }
    }
}