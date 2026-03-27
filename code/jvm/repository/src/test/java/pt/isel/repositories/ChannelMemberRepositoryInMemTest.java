package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.repositories.contracts.ChannelMemberRepositoryContract;
import pt.isel.repositories.mem.TransactionManagerInMem;

class ChannelMemberRepositoryInMemTest implements ChannelMemberRepositoryContract {
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