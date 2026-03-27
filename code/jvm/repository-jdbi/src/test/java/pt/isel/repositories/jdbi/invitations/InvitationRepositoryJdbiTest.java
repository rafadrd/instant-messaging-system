package pt.isel.repositories.jdbi.invitations;

import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.InvitationRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

class InvitationRepositoryJdbiTest extends AbstractJdbiTest implements InvitationRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }
}