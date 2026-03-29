package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.InvitationBuilder;
import pt.isel.domain.channels.AccessType;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.invitations.Invitation;
import pt.isel.domain.invitations.InvitationStatus;
import pt.isel.domain.users.User;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface InvitationRepositoryContract extends RepositoryTestHelper {

    @Test
    default void Create_ValidInput_CreatesAndFindsByToken() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1).truncatedTo(ChronoUnit.MILLIS);

            Invitation inv = trx.repoInvitations().create("token-123", toUserInfo(creator), channel, AccessType.READ_ONLY, expiry);

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
    default void FindAll_HasRecords_ReturnsAllRecords() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", toUserInfo(creator), channel, AccessType.READ_WRITE, expiry);

            List<Invitation> allInvitations = trx.repoInvitations().findAll();

            assertThat(allInvitations).hasSize(2);
            return null;
        });
    }

    @Test
    default void FindByChannelId_ValidChannelId_ReturnsInvitations() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            LocalDateTime expiry = LocalDateTime.now(ZoneOffset.UTC).plusDays(1);
            trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, expiry);
            trx.repoInvitations().create("t2", toUserInfo(creator), channel, AccessType.READ_WRITE, expiry);

            List<Invitation> invs = trx.repoInvitations().findByChannelId(channel.id());

            assertThat(invs).hasSize(2);
            return null;
        });
    }

    @Test
    default void Consume_ValidId_UpdatesStatusToAccepted() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            Invitation inv = trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            boolean consumed = trx.repoInvitations().consume(inv.id());

            assertThat(consumed).isTrue();
            Invitation updated = trx.repoInvitations().findById(inv.id());
            assertThat(updated.status()).isEqualTo(InvitationStatus.ACCEPTED);
            assertThat(trx.repoInvitations().consume(inv.id())).isFalse();
            return null;
        });
    }

    @Test
    default void Save_UpdatedInvitation_UpdatesRecord() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            Invitation inv = trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));
            Invitation rejected = new InvitationBuilder()
                    .withId(inv.id())
                    .withToken(inv.token())
                    .withCreatedBy(inv.createdBy())
                    .withChannel(inv.channel())
                    .withAccessType(inv.accessType())
                    .withExpiresAt(inv.expiresAt())
                    .withStatus(InvitationStatus.REJECTED)
                    .build();

            trx.repoInvitations().save(rejected);

            assertThat(trx.repoInvitations().findById(inv.id()).status()).isEqualTo(InvitationStatus.REJECTED);
            return null;
        });
    }

    @Test
    default void DeleteById_ValidId_DeletesRecord() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            Invitation inv = trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().deleteById(inv.id());

            assertThat(trx.repoInvitations().findById(inv.id())).isNull();
            return null;
        });
    }

    @Test
    default void Clear_HasRecords_RemovesAllRecords() {
        getTxManager().run(trx -> {
            User creator = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "Secret", creator, false);
            trx.repoInvitations().create("t1", toUserInfo(creator), channel, AccessType.READ_ONLY, LocalDateTime.now(ZoneOffset.UTC).plusDays(1));

            trx.repoInvitations().clear();

            assertThat(trx.repoInvitations().findAll()).isEmpty();
            return null;
        });
    }
}