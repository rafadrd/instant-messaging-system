package pt.isel.repositories.jdbi.transaction;

import org.junit.jupiter.api.Test;
import pt.isel.domain.common.Either;
import pt.isel.domain.common.EitherAssert;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.RepositoryTestHelper;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionManagerJdbiTest extends AbstractJdbiTest implements RepositoryTestHelper {

    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }

    @Test
    void Run_SuccessfulTransaction_CommitsData() {
        String result = txManager.run(trx -> {
            insertUser(trx, "alice");
            return "Success";
        });

        assertThat(result).isEqualTo("Success");
        txManager.run(trx -> {
            assertThat(trx.repoUsers().hasUsers()).isTrue();
            return null;
        });
    }

    @Test
    void Run_ReturnsLeft_RollsBackData() {
        Either<String, String> result = txManager.run(trx -> {
            insertUser(trx, "bob");
            return Either.failure("Business Error");
        });

        EitherAssert.assertThat(result).containsLeft("Business Error");
        txManager.run(trx -> {
            assertThat(trx.repoUsers().findByUsername("bob")).isNull();
            return null;
        });
    }

    @Test
    void Run_ThrowsException_RollsBackData() {
        assertThatThrownBy(() -> txManager.run(trx -> {
            insertUser(trx, "charlie");
            throw new RuntimeException("Unexpected Error");
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unexpected Error");

        txManager.run(trx -> {
            assertThat(trx.repoUsers().findByUsername("charlie")).isNull();
            return null;
        });
    }

    @Test
    void Run_ExplicitRollback_RollsBackData() {
        txManager.run(trx -> {
            insertUser(trx, "dave");
            trx.rollback();
            return null;
        });

        txManager.run(trx -> {
            assertThat(trx.repoUsers().findByUsername("dave")).isNull();
            return null;
        });
    }
}