package pt.isel.repositories.jdbi.invitations;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationRepositoryJdbiTest extends AbstractJdbiTest {

    @Test
    void testCreateAndFindByToken() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1).truncatedTo(ChronoUnit.MILLIS);
            Invitation inv = trx.repoInvitations().create("token-123", creatorInfo, channel, AccessType.READ_ONLY, expiry);

            assertNotNull(inv);
            assertNotNull(inv.id());
            assertEquals("token-123", inv.token());
            assertEquals(InvitationStatus.PENDING, inv.status());

            Invitation found = trx.repoInvitations().findByToken("token-123");
            assertEquals(inv.id(), found.id());
            assertEquals(expiry, found.expiresAt());
            return null;
        });
    }

    @Test
    void testFindAll() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", creatorInfo, channel, AccessType.READ_WRITE, expiry);

            List<Invitation> allInvitations = trx.repoInvitations().findAll();
            assertEquals(2, allInvitations.size());
            return null;
        });
    }


    @Test
    void testFindByChannelId() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", creatorInfo, channel, AccessType.READ_WRITE, expiry);

            List<Invitation> invs = trx.repoInvitations().findByChannelId(channel.id());
            assertEquals(2, invs.size());
            return null;
        });
    }

    @Test
    void testConsumeInvitation() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            assertTrue(trx.repoInvitations().consume(inv.id()));

            Invitation updated = trx.repoInvitations().findById(inv.id());
            assertEquals(InvitationStatus.ACCEPTED, updated.status());

            assertFalse(trx.repoInvitations().consume(inv.id()));
            return null;
        });
    }

    @Test
    void testSaveUpdatesInvitation() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
            Invitation rejected = new Invitation(inv.id(), inv.token(), inv.createdBy(), inv.channel(), inv.accessType(), inv.expiresAt(), InvitationStatus.REJECTED);

            trx.repoInvitations().save(rejected);

            assertEquals(InvitationStatus.REJECTED, trx.repoInvitations().findById(inv.id()).status());
            return null;
        });
    }

    @Test
    void testDeleteById() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            Invitation inv = trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().deleteById(inv.id());
            assertNull(trx.repoInvitations().findById(inv.id()));
            return null;
        });
    }

    @Test
    void testClear() {
        txManager.run(trx -> {
            User creator = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo creatorInfo = new UserInfo(creator.id(), creator.username());
            Channel channel = trx.repoChannels().create("Secret", creatorInfo, false);

            trx.repoInvitations().create("t1", creatorInfo, channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().clear();
            assertTrue(trx.repoInvitations().findAll().isEmpty());
            return null;
        });
    }
}