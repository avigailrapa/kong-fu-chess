package src.texttests;

public interface ScriptCommand {

    record BoardCommand(String boardText) implements ScriptCommand {
    }

    record ClickCommand(int x, int y) implements ScriptCommand {
    }

    record WaitCommand(long milliseconds) implements ScriptCommand {
    }

    record PrintBoardCommand(String expectedText) implements ScriptCommand {
    }
}
