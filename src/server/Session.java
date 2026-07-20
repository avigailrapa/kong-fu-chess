package src.server;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.java_websocket.WebSocket;
import src.model.Piece;
import src.model.Position;

@Accessors(fluent = true)
@Getter
@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class Session {

    private final WebSocket connection;
    private final String username;
    private final Piece.Color assignedColor;
    @Setter
    private Position selectedCell;
}
