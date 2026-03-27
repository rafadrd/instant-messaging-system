package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.repositories.contracts.UserRepositoryContract;
import pt.isel.repositories.mem.TransactionManagerInMem;

class UserRepositoryInMemTest implements UserRepositoryContract {
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