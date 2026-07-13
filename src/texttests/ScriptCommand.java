package src.texttests;

public interface ScriptCommand {

    public static class BoardCommand implements ScriptCommand {
        private final String boardText;

        public BoardCommand(String boardText) {
            this.boardText = boardText;
        }

        public String boardText() {
            return boardText;
        }
    }

    public static class ClickCommand implements ScriptCommand {
        private final int x;
        private final int y;

        public ClickCommand(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int x() { return x; }
        public int y() { return y; }
    }

    public static class WaitCommand implements ScriptCommand {
        private final long milliseconds;

        public WaitCommand(long milliseconds) {
            this.milliseconds = milliseconds;
        }

        public long milliseconds() { return milliseconds; }
    }

    public static class PrintBoardCommand implements ScriptCommand {
        private final String expectedText;

        public PrintBoardCommand(String expectedText) {
            this.expectedText = expectedText;
        }

        public String expectedText() { return expectedText; }
    }
}