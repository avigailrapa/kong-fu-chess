package src.server;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import src.model.Piece;
import src.model.Position;

@Accessors(fluent = true)
@Getter
public class Session {

    private final ClientConnection connection;
    private final String username;
    private final Piece.Color assignedColor;
    @Setter
    private Position selectedCell;
    @Setter
    private int rating;

    public Session(ClientConnection connection, String username, Piece.Color assignedColor, int rating) {
        this.connection = connection;
        this.username = username;
        this.assignedColor = assignedColor;
        this.rating = rating;
    }
}
