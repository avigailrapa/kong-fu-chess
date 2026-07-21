package src.server;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RoomRegistry {

    public enum JoinOutcome { SEATED_BLACK, SPECTATING, NOT_FOUND }

    private static final String ROOM_ID_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int ROOM_ID_LENGTH = 6;

    private final Supplier<Match> matchFactory;
    private final BiConsumer<Match, Session> seatSession;
    private final Consumer<Match> onMatchReady;
    private final BiConsumer<Match, Session> addSpectator;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Match> matchByRoomId = new HashMap<>();

    public RoomRegistry(Supplier<Match> matchFactory, BiConsumer<Match, Session> seatSession,
                         Consumer<Match> onMatchReady, BiConsumer<Match, Session> addSpectator) {
        this.matchFactory = matchFactory;
        this.seatSession = seatSession;
        this.onMatchReady = onMatchReady;
        this.addSpectator = addSpectator;
    }

    public synchronized String createRoom(Session creator) {
        Match match = matchFactory.get();
        seatSession.accept(match, creator);
        String roomId = generateUniqueRoomId();
        matchByRoomId.put(roomId, match);
        return roomId;
    }

    public synchronized JoinOutcome joinRoom(String roomId, Session joiner) {
        Match match = matchByRoomId.get(roomId);
        if (match == null) {
            return JoinOutcome.NOT_FOUND;
        }
        if (match.seated().size() < 2) {
            seatSession.accept(match, joiner);
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
