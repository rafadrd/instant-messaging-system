package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.repositories.contracts.TokenBlacklistRepositoryContract;
import pt.isel.repositories.mem.TransactionManagerInMem;

class TokenBlacklistRepositoryInMemTest implements TokenBlacklistRepositoryContract {
    private TransactionManagerInMem txManager;

    @BeforeEach
    void setUp() {
        txManager = new TransactionManagerInMem();
    }

    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}