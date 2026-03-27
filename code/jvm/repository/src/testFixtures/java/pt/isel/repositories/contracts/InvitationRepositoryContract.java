package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface InvitationRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindByToken() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1).truncatedTo(ChronoUnit.MILLIS);
            Invitation inv = trx.repoInvitations().create("token-123", creatorInfo, channel, AccessType.READ_ONLY, expiry);

            assertThat(inv).isNotNull();
            assertThat(inv.id()).isNotNull();
            assertThat(inv.token()).isEqualTo("token-123");
            assertThat(inv.status()).isEqualTo(InvitationStatus.PENDING);

            Invitation found = trx.repoInvitations().findByToken("token-123");
            assertThat(found.id()).isEqualTo(inv.id());
            assertThat(found.expiresAt()).isEqualTo(expiry);
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", creatorInfo, channel, AccessType.READ_WRITE, expiry);

            List<Invitation> allInvitations = trx.repoInvitations().findAll();
            assertThat(allInvitations).hasSize(2);
            return null;
        });
    }

    @Test
    default void testFindByChannelId() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", creatorInfo, channel, AccessType.READ_WRITE, expiry);

            List<Invitation> invs = trx.repoInvitations().findByChannelId(channel.id());
            assertThat(invs).hasSize(2);
            return null;
        });
    }

    @Test
    default void testConsumeInvitation() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            assertThat(trx.repoInvitations().consume(inv.id())).isTrue();

            Invitation updated = trx.repoInvitations().findById(inv.id());
            assertThat(updated.status()).isEqualTo(InvitationStatus.ACCEPTED);

            assertThat(trx.repoInvitations().consume(inv.id())).isFalse();
            return null;
        });
    }

    @Test
    default void testSaveUpdatesInvitation() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
            Invitation rejected = new Invitation(inv.id(), inv.token(), inv.createdBy(), inv.channel(), inv.accessType(), inv.expiresAt(), InvitationStatus.REJECTED);

            trx.repoInvitations().save(rejected);

            assertThat(trx.repoInvitations().findById(inv.id()).status()).isEqualTo(InvitationStatus.REJECTED);
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().deleteById(inv.id());
            assertThat(trx.repoInvitations().findById(inv.id())).isNull();
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().clear();
            assertThat(trx.repoInvitations().findAll()).isEmpty();
            return null;
        });
    }
}