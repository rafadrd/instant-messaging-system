package pt.isel.repositories.jdbi.channels;

import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.ChannelMemberRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

class ChannelMemberRepositoryJdbiTest extends AbstractJdbiTest implements ChannelMemberRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}