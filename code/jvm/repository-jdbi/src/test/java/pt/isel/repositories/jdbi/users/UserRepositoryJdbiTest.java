package pt.isel.repositories.jdbi.users;

import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.UserRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

class UserRepositoryJdbiTest extends AbstractJdbiTest implements UserRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}