package server.auth;

import org.junit.jupiter.api.Test;
import src.server.auth.UserRecord;
import src.server.auth.UserStore;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class UserStoreTest {

    private UserStore freshStore() {
        return new UserStore("jdbc:sqlite::memory:");
    }

    @Test
    public void testFindReturnsEmptyForUnknownUsername() {
        UserStore store = freshStore();

        assertTrue(store.find("alice").isEmpty());
    }

    @Test
    public void testCreateUserDefaultsToRating1200() {
        UserStore store = freshStore();

        UserRecord created = store.createUser("alice", "secret");

        assertEquals("alice", created.username());
        assertEquals(1200, created.rating());
    }

    @Test
    public void testFindReturnsCreatedUser() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");

        Optional<UserRecord> found = store.find("alice");

        assertTrue(found.isPresent());
        assertEquals(1200, found.get().rating());
    }

    @Test
    public void testCheckPasswordAcceptsCorrectPassword() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");

        assertTrue(store.checkPassword("alice", "secret"));
    }

    @Test
    public void testCheckPasswordRejectsWrongPassword() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");

        assertFalse(store.checkPassword("alice", "wrong"));
    }

    @Test
    public void testCheckPasswordRejectsUnknownUsername() {
        UserStore store = freshStore();

        assertFalse(store.checkPassword("alice", "secret"));
    }

    @Test
    public void testUpdateRatingPersists() {
        UserStore store = freshStore();
        store.createUser("alice", "secret");

        store.updateRating("alice", 1350);

        assertEquals(1350, store.find("alice").orElseThrow().rating());
    }
}
