import model.Board;
import java.util.List;
import java.util.Scanner;
import engine.GameEngine;
import io.BoardReader;
import io.CommandProcessor;
public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        List<String[]> boardRows = BoardReader.readBoard(scanner);
        if (boardRows == null) {
            scanner.close();
            return;
        }

        Board board = BoardReader.createBoardFromRows(boardRows);
        if (board == null) {
            scanner.close();
            return;
        }

        GameEngine gameEngine = new GameEngine(board);
        CommandProcessor commandProcessor = new CommandProcessor(gameEngine);
        
        commandProcessor.processCommands(scanner);
        
        scanner.close();
    }
}
