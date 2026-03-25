package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.repositories.mem.TransactionManagerInMem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TransactionManagerInMemTest {

    private TransactionManagerInMem txManager;

    @BeforeEach
    void setUp() {
        txManager = new TransactionManagerInMem();
    }

    @Test
    void testRunExecutesBlockAndReturnsResult() {
        String result = txManager.run(trx -> {
            assertNotNull(trx.repoUsers());
            assertNotNull(trx.repoChannels());
            assertNotNull(trx.repoMessages());
            assertNotNull(trx.repoMemberships());
            assertNotNull(trx.repoInvitations());
            assertNotNull(trx.repoTokenBlacklist());

            return "Success";
        });

        assertEquals("Success", result);
    }

    @Test
    void testRollbackIsNoOpButDoesNotCrash() {
        assertDoesNotThrow(() -> {
            txManager.run(trx -> {
                trx.rollback();
                return null;
            });
        });
    }
}