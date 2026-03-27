package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.repositories.mem.TransactionManagerInMem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TransactionManagerInMemTest {

    private TransactionManagerInMem txManager;

    @BeforeEach
    void setUp() {
        txManager = new TransactionManagerInMem();
    }

    @Test
    void testRunExecutesBlockAndReturnsResult() {
        String result = txManager.run(trx -> {
            assertThat(trx.repoUsers()).isNotNull();
            assertThat(trx.repoChannels()).isNotNull();
            assertThat(trx.repoMessages()).isNotNull();
            assertThat(trx.repoMemberships()).isNotNull();
            assertThat(trx.repoInvitations()).isNotNull();
            assertThat(trx.repoTokenBlacklist()).isNotNull();

            return "Success";
        });

        assertThat(result).isEqualTo("Success");
    }

    @Test
    void testRollbackIsNoOpButDoesNotCrash() {
        assertThatCode(() -> txManager.run(trx -> {
            trx.rollback();
            return null;
        })).doesNotThrowAnyException();
    }
}