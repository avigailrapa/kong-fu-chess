package server;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.io.BoardParser;
import src.model.Board;
import src.server.Match;
import src.server.RoomRegistry;
import src.server.Session;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static src.server.RoomRegistry.JoinOutcome.*;

public class RoomRegistryTest {

    private Session session(String username) {
        return new Session(text -> {
        }, username, 1200);
    }

    private Match freshMatch() {
        Board board = new BoardParser().parse(BoardParser.STANDARD_STARTING_POSITION);
        return new Match(GameEngine.fromBoard(board), 1000);
    }

    private RoomRegistry freshRegistry(AtomicInteger onMatchReadyCalls) {
        return new RoomRegistry(this::freshMatch,
                (match, session) -> match.addSession(session),
                match -> onMatchReadyCalls.incrementAndGet(),
                (match, session) -> match.addSpectator(session));
    }

    @Test
    public void testCreateRoomReturnsNonBlankRoomId() {
        RoomRegistry registry = freshRegistry(new AtomicInteger());

        String roomId = registry.createRoom(session("alice"));

        assertNotNull(roomId);
        assertFalse(roomId.isBlank());
    }

    @Test
    public void testFirstJoinIsSeatedBlackAndTriggersOnMatchReady() {
        AtomicInteger onMatchReadyCalls = new AtomicInteger();
        RoomRegistry registry = freshRegistry(onMatchReadyCalls);
        String roomId = registry.createRoom(session("alice"));

        RoomRegistry.JoinOutcome outcome = registry.joinRoom(roomId, session("bob"));

        assertEquals(SEATED_BLACK, outcome);
        assertEquals(1, onMatchReadyCalls.get());
    }

    @Test
    public void testSecondJoinOnwardsIsSpectatingAndDoesNotRetriggerOnMatchReady() {
        AtomicInteger onMatchReadyCalls = new AtomicInteger();
        RoomRegistry registry = freshRegistry(onMatchReadyCalls);
        String roomId = registry.createRoom(session("alice"));
        registry.joinRoom(roomId, session("bob"));

        RoomRegistry.JoinOutcome first = registry.joinRoom(roomId, session("carol"));
        RoomRegistry.JoinOutcome second = registry.joinRoom(roomId, session("dave"));

        assertEquals(SPECTATING, first);
        assertEquals(SPECTATING, second);
        assertEquals(1, onMatchReadyCalls.get());
    }

    @Test
    public void testJoiningUnknownRoomReturnsNotFound() {
        RoomRegistry registry = freshRegistry(new AtomicInteger());

        RoomRegistry.JoinOutcome outcome = registry.joinRoom("NOSUCHROOM", session("bob"));

        assertEquals(NOT_FOUND, outcome);
    }
}
