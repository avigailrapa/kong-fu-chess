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
    @Setter
    private int rating;
    @Setter
    private Piece.Color assignedColor;
    @Setter
    private Position selectedCell;
    @Setter
    private Role role;

    public Session(ClientConnection connection, String username, int rating) {
        this.connection = connection;
        this.username = username;
        this.rating = rating;
    }

    public enum Role { WHITE, BLACK, SPECTATOR }
}
