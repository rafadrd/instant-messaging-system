package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.repositories.contracts.InvitationRepositoryContract;
import pt.isel.repositories.mem.TransactionManagerInMem;

class InvitationRepositoryInMemTest implements InvitationRepositoryContract {
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