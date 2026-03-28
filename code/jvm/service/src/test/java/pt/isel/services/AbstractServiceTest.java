package pt.isel.services;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.User;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.RepositoryTestHelper;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public abstract class AbstractServiceTest implements RepositoryTestHelper {

    protected TransactionManagerInMem trxManager;
    protected Clock clock;

    protected User alice;
    protected User bob;
    protected User charlie;

    @Override
    public TransactionManager getTxManager() {
        return trxManager;
    }

    @BeforeEach
    protected void setUpBaseState() {
        trxManager = new TransactionManagerInMem();
        clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

        alice = trxManager.run(trx -> insertUser(trx, "alice"));
        bob = trxManager.run(trx -> insertUser(trx, "bob"));
        charlie = trxManager.run(trx -> insertUser(trx, "charlie"));
    }

    protected Channel createChannelWithMembers(String name, boolean isPublic) {
        return trxManager.run(trx -> {
            Channel c = insertChannel(trx, name, alice, isPublic);

            insertMember(trx, alice, c, AccessType.READ_WRITE);
            insertMember(trx, bob, c, AccessType.READ_WRITE);
            insertMember(trx, charlie, c, AccessType.READ_ONLY);

            return c;
        });
    }
}