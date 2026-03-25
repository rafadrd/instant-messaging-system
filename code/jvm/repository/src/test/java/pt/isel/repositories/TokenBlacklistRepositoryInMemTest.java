package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.repositories.mem.TokenBlacklistRepositoryInMem;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenBlacklistRepositoryInMemTest {

    private TokenBlacklistRepositoryInMem repo;

    @BeforeEach
    void setUp() {
        repo = new TokenBlacklistRepositoryInMem();
    }

    @Test
    void testAddAndExists() {
        String jti = "token-uuid-123";
        assertFalse(repo.exists(jti));

        repo.add(jti, LocalDateTime.now().plusHours(1));
        assertTrue(repo.exists(jti));
    }

    @Test
    void testClear() {
        repo.add("t1", LocalDateTime.now());
        repo.add("t2", LocalDateTime.now());

        repo.clear();

        assertFalse(repo.exists("t1"));
        assertFalse(repo.exists("t2"));
    }

    @Test
    void testCleanupExpiredDoesNotCrash() {
        assertDoesNotThrow(() -> repo.cleanupExpired());
    }
}