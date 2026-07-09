package texttests;

import io.BoardParser;
import io.BoardPrinter;
import model.Board;

import java.util.List;

public class ScriptRunner {

    private final ScriptParser scriptParser = new ScriptParser();
    private final BoardParser boardParser = new BoardParser();
    private final BoardPrinter boardPrinter = new BoardPrinter();

    private Board board;

    public void run(String script) {
        List<ScriptCommand> commands = scriptParser.parse(script);
        for (ScriptCommand command : commands) {
            execute(command);
        }
    }

    private void execute(ScriptCommand command) {
        switch (command) {
            case ScriptCommand.BoardCommand boardCommand -> board = boardParser.parse(boardCommand.boardText());
            case ScriptCommand.PrintBoardCommand printBoardCommand -> assertBoardPrints(printBoardCommand.expectedText());
            case ScriptCommand.ClickCommand _ -> throw new UnsupportedOperationException(
                    "click is not supported yet: Controller/GameEngine do not exist (Iteration 2/4)");
            case ScriptCommand.WaitCommand _ -> throw new UnsupportedOperationException(
                    "wait is not supported yet: RealTimeArbiter does not exist (Iteration 5)");
        }
    }

    private void assertBoardPrints(String expectedText) {
        if (board == null) {
            throw new IllegalStateException("No Board has been set up yet; script must start with a Board command");
        }
        String actual = boardPrinter.print(board);
        if (!actual.equals(expectedText)) {
            throw new AssertionError("print board mismatch.\nExpected:\n" + expectedText + "\nActual:\n" + actual);
        }
    }
}
