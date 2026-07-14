import src.engine.GameEngine;
import src.input.BoardMapper;
import src.input.Controller;
import src.io.BoardParser;
import src.model.Board;
import src.view.GameWindow;
import src.view.Renderer;

import javax.swing.*;

public class GuiMain {

    private static final String STARTING_BOARD =
            "bR bN bB bQ bK bB bN bR\n" +
            "bP bP bP bP bP bP bP bP\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            ".  .  .  .  .  .  .  .\n" +
            "wP wP wP wP wP wP wP wP\n" +
            "wR wN wB wQ wK wB wN wR\n";

    public static void main(String[] args) {
        Board board = new BoardParser().parse(STARTING_BOARD);
        GameEngine engine = GameEngine.fromBoard(board);
        Controller controller = new Controller(new BoardMapper(board.getWidth(), board.getHeight()), engine);
        GameWindow window = new GameWindow(engine, controller, new Renderer());

        SwingUtilities.invokeLater(window::open);
    }
}
