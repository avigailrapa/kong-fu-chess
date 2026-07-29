package src.server.matchmaking;

import lombok.RequiredArgsConstructor;
import src.server.core.Session;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class RoomRegistry<T> {

    public enum JoinOutcome { SEATED_BLACK, SPECTATING, NOT_FOUND }

    private static final String ROOM_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 6;

    private final Supplier<T> matchFactory;
    private final BiConsumer<T, Session> seatSession;
    private final Consumer<T> onMatchReady;
    private final BiConsumer<T, Session> addSpectator;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, T> matchByRoomId = new HashMap<>();
    private final Map<String, Integer> seatedCountByRoomId = new HashMap<>();

    public synchronized String createRoom(Session creator) {
        T match = matchFactory.get();
        seatSession.accept(match, creator);
        String roomId = generateUniqueRoomId();
        matchByRoomId.put(roomId, match);
        seatedCountByRoomId.put(roomId, 1);
        return roomId;
    }

    public synchronized JoinOutcome joinRoom(String roomId, Session joiner) {
        T match = matchByRoomId.get(roomId);
        if (match == null) {
            return JoinOutcome.NOT_FOUND;
        }
        if (seatedCountByRoomId.get(roomId) < 2) {
            seatSession.accept(match, joiner);
            seatedCountByRoomId.put(roomId, 2);
            onMatchReady.accept(match);
            return JoinOutcome.SEATED_BLACK;
        }
        addSpectator.accept(match, joiner);
        return JoinOutcome.SPECTATING;
    }

    private String generateUniqueRoomId() {
        String roomId;
        do {
            roomId = generateRoomId();
        } while (matchByRoomId.containsKey(roomId));
        return roomId;
    }

    private String generateRoomId() {
        StringBuilder roomId = new StringBuilder(ROOM_ID_LENGTH);
        for (int i = 0; i < ROOM_ID_LENGTH; i++) {
            roomId.append(ROOM_ID_ALPHABET.charAt(random.nextInt(ROOM_ID_ALPHABET.length())));
        }
        return roomId.toString();
    }
}
