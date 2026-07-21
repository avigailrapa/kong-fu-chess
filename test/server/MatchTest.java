package server;

import org.junit.jupiter.api.Test;
import src.engine.GameEngine;
import src.model.Board;
import src.model.GameState;
import src.model.Piece;
import src.model.Position;
import src.realtime.RealTimeArbiter;
import src.rules.PieceRules;
import src.rules.RookRule;
import src.rules.RuleEngine;
import src.server.Match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MatchTest {

    private RuleEngine rookOnlyRuleEngine() {
        Map<Piece.Kind, PieceRules> rulesByKind = Map.of(Piece.Kind.ROOK, new RookRule());
        return new RuleEngine(rulesByKind);
    }

    private GameEngine freshEngine() {
        Board board = new Board(8, 8);
        board.addPiece(new Piece("r1", Piece.Color.WHITE, Piece.Kind.ROOK, new Position(7, 0)), new Position(7, 0));
        return new GameEngine(board, new GameState(), rookOnlyRuleEngine(), new RealTimeArbiter(board));
    }

    @Test
    public void testStartInvokesOnTickRepeatedly() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        Match match = new Match(freshEngine(), 20);

        match.start(latch::countDown);
        boolean reachedZero = latch.await(2, TimeUnit.SECONDS);
        match.stop();

        assertTrue(reachedZero, "expected at least 3 ticks within 2 seconds");
    }

    @Test
    public void testTickAdvancesEngineAndResolvesAMove() throws InterruptedException {
        GameEngine engine = freshEngine();
        CountDownLatch latch = new CountDownLatch(1);
        Match match = new Match(engine, 2000);
        try {
            match.submit(() -> engine.requestMove(new Position(7, 0), new Position(4, 0)));
            match.start(latch::countDown);

            assertTrue(latch.await(4, TimeUnit.SECONDS), "expected at least one tick within 4 seconds");
            assertNotNull(engine.snapshot(null).pieceAt(new Position(4, 0)));
        } finally {
            match.stop();
        }
    }

    @Test
    public void testSubmitRunsTasksSequentiallyOnMatchThread() throws InterruptedException {
        Match match = new Match(freshEngine(), 1000);
        List<Integer> order = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(5);
        try {
            for (int i = 0; i < 5; i++) {
                int value = i;
                match.submit(() -> {
                    order.add(value);
                    latch.countDown();
                });
            }

            assertTrue(latch.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(0, 1, 2, 3, 4), order);
        } finally {
            match.stop();
        }
    }

    @Test
    public void testStopPreventsFurtherTicks() throws InterruptedException {
        AtomicInteger tickCount = new AtomicInteger();
        Match match = new Match(freshEngine(), 20);

        match.start(tickCount::incrementAndGet);
        Thread.sleep(100);
        match.stop();
        Thread.sleep(50); // let any tick already in flight when stop() was called finish
        int countAtStop = tickCount.get();
        Thread.sleep(150);

        assertEquals(countAtStop, tickCount.get());
    }

    @Test
    public void testStartTwiceThrows() {
        Match match = new Match(freshEngine(), 1000);
        try {
            match.start(() -> {
            });
            assertThrows(IllegalStateException.class, () -> match.start(() -> {
            }));
        } finally {
            match.stop();
        }
    }

    @Test
    public void testEngineAccessorReturnsConstructorArgument() {
        GameEngine engine = freshEngine();
        Match match = new Match(engine, 1000);

        assertSame(engine, match.engine());
    }

    @Test
    public void testMoveLoggerIsWiredToEngineMoveObserver() throws InterruptedException {
        GameEngine engine = freshEngine();
        CountDownLatch latch = new CountDownLatch(1);
        Match match = new Match(engine, 2000);
        try {
            match.submit(() -> engine.requestMove(new Position(7, 0), new Position(4, 0)));
            match.start(latch::countDown);

            assertTrue(latch.await(4, TimeUnit.SECONDS));
            assertFalse(match.moveLogger().getWhiteMoves().isEmpty());
        } finally {
            match.stop();
        }
    }

    @Test
    public void testNewGameReplacesEngine() {
        Match match = new Match(freshEngine(), 1000);
        GameEngine replacementEngine = freshEngine();

        match.newGame(replacementEngine);

        assertSame(replacementEngine, match.engine());
    }

    @Test
    public void testNewGameInvokesRegisteredListener() {
        Match match = new Match(freshEngine(), 1000);
        AtomicInteger listenerCalls = new AtomicInteger();
        match.onNewGame(listenerCalls::incrementAndGet);

        match.newGame(freshEngine());

        assertEquals(1, listenerCalls.get());
    }

    @Test
    public void testNewGameWiresFreshMoveLoggerToReplacementEngine() throws InterruptedException {
        GameEngine replacementEngine = freshEngine();
        Match match = new Match(freshEngine(), 2000);
        match.newGame(replacementEngine);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            match.submit(() -> replacementEngine.requestMove(new Position(7, 0), new Position(4, 0)));
            match.start(latch::countDown);

            assertTrue(latch.await(4, TimeUnit.SECONDS));
            assertFalse(match.moveLogger().getWhiteMoves().isEmpty());
        } finally {
            match.stop();
        }
    }
}
