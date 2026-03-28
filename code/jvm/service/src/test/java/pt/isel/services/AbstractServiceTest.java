package pt.isel.services;

import org.junit.jupiter.api.BeforeEach;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.TransactionManagerInMem;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public abstract class AbstractServiceTest {

    protected TransactionManagerInMem trxManager;
    protected Clock clock;

    protected User alice;
    protected User bob;
    protected User charlie;

    @BeforeEach
    protected void setUpBaseState() {
        trxManager = new TransactionManagerInMem();
        clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC);

        alice = trxManager.run(trx -> trx.repoUsers().create("alice", new PasswordValidationInfo("hash")));
        bob = trxManager.run(trx -> trx.repoUsers().create("bob", new PasswordValidationInfo("hash")));
        charlie = trxManager.run(trx -> trx.repoUsers().create("charlie", new PasswordValidationInfo("hash")));
    }

    protected Channel createChannelWithMembers(String name, boolean isPublic) {
        return trxManager.run(trx -> {
            UserInfo aliceInfo = new UserInfoBuilder().withId(alice.id()).withUsername(alice.username()).build();
            Channel c = trx.repoChannels().create(name, aliceInfo, isPublic);

            trx.repoMemberships().addUserToChannel(aliceInfo, c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(
                    new UserInfoBuilder().withId(bob.id()).withUsername(bob.username()).build(), c, AccessType.READ_WRITE);
            trx.repoMemberships().addUserToChannel(
                    new UserInfoBuilder().withId(charlie.id()).withUsername(charlie.username()).build(), c, AccessType.READ_ONLY);

            return c;
        });
    }
}