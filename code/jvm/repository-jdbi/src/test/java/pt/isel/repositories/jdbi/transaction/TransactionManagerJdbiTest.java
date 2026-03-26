package pt.isel.repositories.jdbi.transaction;

import org.junit.jupiter.api.Test;
import pt.isel.domain.common.Either;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionManagerJdbiTest extends AbstractJdbiTest {

    @Test
    void testSuccessfulTransactionCommits() {
        String result = txManager.run(trx -> {
            trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            return "Success";
        });

        assertEquals("Success", result);

        txManager.run(trx -> {
            // Verify data was committed
            assertTrue(trx.repoUsers().hasUsers());
            return null;
        });
    }

    @Test
    void testRollbackOnEitherLeft() {
        Either<String, String> result = txManager.run(trx -> {
            trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));
            return Either.failure("Business Error");
        });

        assertInstanceOf(Either.Left.class, result);
        assertEquals("Business Error", ((Either.Left<String, String>) result).value());

        txManager.run(trx -> {
            // Verify data was rolled back
            assertNull(trx.repoUsers().findByUsername("bob"));
            return null;
        });
    }

    @Test
    void testRollbackOnException() {
        assertThrows(RuntimeException.class, () -> txManager.run(trx -> {
            trx.repoUsers().create("charlie", new PasswordValidationInfo("hash"));
            throw new RuntimeException("Unexpected Error");
        }));

        txManager.run(trx -> {
            // Verify data was rolled back
            assertNull(trx.repoUsers().findByUsername("charlie"));
            return null;
        });
    }

    @Test
    void testExplicitRollback() {
        txManager.run(trx -> {
            trx.repoUsers().create("dave", new PasswordValidationInfo("hash"));

            // Explicitly rollback the transaction
            trx.rollback();
            return null;
        });

        txManager.run(trx -> {
            // Verify data was rolled back and not committed
            assertNull(trx.repoUsers().findByUsername("dave"));
            return null;
        });
    }
}