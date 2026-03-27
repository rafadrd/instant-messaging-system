package pt.isel.repositories.jdbi.channels;

import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.ChannelRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

class ChannelRepositoryJdbiTest extends AbstractJdbiTest implements ChannelRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}