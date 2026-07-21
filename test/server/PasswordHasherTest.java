package server;

import org.junit.jupiter.api.Test;
import src.server.PasswordHasher;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    @Test
    public void testSameSaltAndPasswordProduceSameHash() {
        String salt = PasswordHasher.newSalt();

        assertEquals(PasswordHasher.hash("secret", salt), PasswordHasher.hash("secret", salt));
    }

    @Test
    public void testDifferentPasswordsProduceDifferentHashes() {
        String salt = PasswordHasher.newSalt();

        assertNotEquals(PasswordHasher.hash("secret", salt), PasswordHasher.hash("other", salt));
    }

    @Test
    public void testSamePasswordWithDifferentSaltsProducesDifferentHashes() {
        String saltA = PasswordHasher.newSalt();
        String saltB = PasswordHasher.newSalt();

        assertNotEquals(PasswordHasher.hash("secret", saltA), PasswordHasher.hash("secret", saltB));
    }

    @Test
    public void testNewSaltIsNotEmptyAndVariesBetweenCalls() {
        String saltA = PasswordHasher.newSalt();
        String saltB = PasswordHasher.newSalt();

        assertFalse(saltA.isEmpty());
        assertNotEquals(saltA, saltB);
    }
}
