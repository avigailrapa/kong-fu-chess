import src.engine.MoveLogger;
import src.engine.GameEngine;
import src.input.BoardMapper;
import src.input.Controller;
import src.io.BoardParser;
import src.model.Board;
import src.view.GameWindow;
import src.view.Renderer;

import javax.swing.*;
import java.util.function.Supplier;

public class GuiMain {

    private static final String STARTING_BOARD = """
            bR bN bB bQ bK bB bN bR
            bP bP bP bP bP bP bP bP
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            .  .  .  .  .  .  .  .
            wP wP wP wP wP wP wP wP
            wR wN wB wQ wK wB wN wR
            """;

    public static void main(String[] args) {
        Supplier<GameWindow.GameComponents> gameFactory = GuiMain::createGame;
        GameWindow window = new GameWindow(gameFactory);

        SwingUtilities.invokeLater(window::open);
    }

    private static GameWindow.GameComponents createGame() {
        Board board = new BoardParser().parse(STARTING_BOARD);
        GameEngine engine = GameEngine.fromBoard(board);
        MoveLogger moveLogger = new MoveLogger();
        engine.addMoveObserver(moveLogger);
        Controller controller = new Controller(new BoardMapper(board.getWidth(), board.getHeight()), engine);
        Renderer renderer = new Renderer("assets/pieces", moveLogger);
        return new GameWindow.GameComponents(engine, controller, renderer);
    }
}