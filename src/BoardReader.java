import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Reads and validates board configuration from input.
 * SRP: Responsible only for I/O and board validation.
 */
public class BoardReader {

    public static List<String[]> readBoard(Scanner scanner) {
        List<String[]> boardRows = new ArrayList<>();
        int expectedWidth = -1;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            
            if (line.isEmpty()) continue;
            if (line.startsWith("Commands:")) break;
            if (line.startsWith("Board:")) continue;
            
            String[] tokens = line.split("\\s+");
            
            for (String token : tokens) {
                if (!Piece.isValidToken(token)) {
                    System.out.println("ERROR UNKNOWN_TOKEN");
                    return null;
                }
            }
            
            if (expectedWidth == -1) {
                expectedWidth = tokens.length;
            } else if (tokens.length != expectedWidth) {
                System.out.println("ERROR ROW_WIDTH_MISMATCH");
                return null; 
            }
            
            boardRows.add(tokens);
        }
        
        return boardRows.isEmpty() ? null : boardRows;
    }

    public static Board createBoardFromRows(List<String[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return new Board(rows.toArray(new String[0][]));
    }
}
