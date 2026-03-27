package pt.isel.repositories.jdbi.security;

import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.TokenBlacklistRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

class TokenBlacklistRepositoryJdbiTest extends AbstractJdbiTest implements TokenBlacklistRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}