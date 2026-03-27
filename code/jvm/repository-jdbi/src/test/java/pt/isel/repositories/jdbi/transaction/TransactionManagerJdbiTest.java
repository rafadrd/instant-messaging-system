package pt.isel.repositories.jdbi.transaction;

import org.junit.jupiter.api.Test;
import pt.isel.domain.common.Either;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionManagerJdbiTest extends AbstractJdbiTest {

    @Test
    void testSuccessfulTransactionCommits() {
        String result = txManager.run(trx -> {
            trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            return "Success";
        });

        assertThat(result).isEqualTo("Success");

        txManager.run(trx -> {
            // Verify data was committed
            assertThat(trx.repoUsers().hasUsers()).isTrue();
            return null;
        });
    }

    @Test
    void testRollbackOnEitherLeft() {
        Either<String, String> result = txManager.run(trx -> {
            trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));
            return Either.failure("Business Error");
        });

        assertThat(result).isInstanceOf(Either.Left.class);
        assertThat(((Either.Left<String, String>) result).value()).isEqualTo("Business Error");

        txManager.run(trx -> {
            // Verify data was rolled back
            assertThat(trx.repoUsers().findByUsername("bob")).isNull();
            return null;
        });
    }

    @Test
    void testRollbackOnException() {
        assertThatThrownBy(() -> txManager.run(trx -> {
            trx.repoUsers().create("charlie", new PasswordValidationInfo("hash"));
            throw new RuntimeException("Unexpected Error");
        })).isInstanceOf(RuntimeException.class).hasMessage("Unexpected Error");

        txManager.run(trx -> {
            // Verify data was rolled back
            assertThat(trx.repoUsers().findByUsername("charlie")).isNull();
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
            assertThat(trx.repoUsers().findByUsername("dave")).isNull();
            return null;
        });
    }
}