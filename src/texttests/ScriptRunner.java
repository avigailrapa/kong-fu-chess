package texttests;

import engine.GameEngine;
import input.BoardMapper;
import input.Controller;
import io.BoardParser;
import io.BoardPrinter;
import model.Board;

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
        switch (command) {
            case ScriptCommand.BoardCommand boardCommand -> initializeGame(boardCommand.boardText());
            case ScriptCommand.ClickCommand clickCommand -> {
                requireInitializedGame();
                controller.click(clickCommand.x(), clickCommand.y());
            }
            case ScriptCommand.WaitCommand waitCommand -> {
                requireInitializedGame();
                engine.waitMs(waitCommand.milliseconds());
            }
            case ScriptCommand.PrintBoardCommand printBoardCommand -> {
                requireInitializedGame();
                assertBoardPrints(printBoardCommand.expectedText());
            }
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
