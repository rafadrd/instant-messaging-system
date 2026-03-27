package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.builders.UserInfoBuilder;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface ChannelRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build();

            Channel channel = trx.repoChannels().create("General", ownerInfo, true);

            assertThat(channel).isNotNull();
            assertThat(channel.id()).isNotNull();
            assertThat(channel.name()).isEqualTo("General");
            assertThat(channel.isPublic()).isTrue();

            Channel found = trx.repoChannels().findById(channel.id());
            assertThat(found.id()).isEqualTo(channel.id());
            assertThat(found.owner().id()).isEqualTo(owner.id());
            return null;
        });
    }

    @Test
    default void testFindByName() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            trx.repoChannels().create("Random", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), true);

            Channel found = trx.repoChannels().findByName("Random");
            assertThat(found).isNotNull();
            assertThat(found.name()).isEqualTo("Random");

            assertThat(trx.repoChannels().findByName("Unknown")).isNull();
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build();

            trx.repoChannels().create("C1", ownerInfo, true);
            trx.repoChannels().create("C2", ownerInfo, false);

            List<Channel> channels = trx.repoChannels().findAll();
            assertThat(channels).hasSize(2);
            return null;
        });
    }

    @Test
    default void testFindAllByOwner() {
        getTxManager().run(trx -> {
            User owner1 = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            User owner2 = trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));

            trx.repoChannels().create("C1", new UserInfoBuilder().withId(owner1.id()).withUsername(owner1.username()).build(), true);
            trx.repoChannels().create("C2", new UserInfoBuilder().withId(owner1.id()).withUsername(owner1.username()).build(), false);
            trx.repoChannels().create("C3", new UserInfoBuilder().withId(owner2.id()).withUsername(owner2.username()).build(), true);

            List<Channel> aliceChannels = trx.repoChannels().findAllByOwner(owner1.id());
            assertThat(aliceChannels).hasSize(2);

            List<Channel> bobChannels = trx.repoChannels().findAllByOwner(owner2.id());
            assertThat(bobChannels).hasSize(1);
            return null;
        });
    }

    @Test
    default void testFindAllPublicChannelsWithPagination() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build();

            trx.repoChannels().create("Pub1", ownerInfo, true);
            trx.repoChannels().create("Priv1", ownerInfo, false);
            trx.repoChannels().create("Pub2", ownerInfo, true);
            trx.repoChannels().create("Pub3", ownerInfo, true);

            List<Channel> page1 = trx.repoChannels().findAllPublicChannels(2, 0);
            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).name()).isEqualTo("Pub1");
            assertThat(page1.get(1).name()).isEqualTo("Pub2");

            List<Channel> page2 = trx.repoChannels().findAllPublicChannels(2, 2);
            assertThat(page2).hasSize(1);
            assertThat(page2.getFirst().name()).isEqualTo("Pub3");
            return null;
        });
    }

    @Test
    default void testSearchByName() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build();

            trx.repoChannels().create("Java Devs", ownerInfo, true);
            trx.repoChannels().create("JavaScript Devs", ownerInfo, true);
            trx.repoChannels().create("Secret Java", ownerInfo, false);

            List<Channel> results = trx.repoChannels().searchByName("java", 10, 0);
            assertThat(results).hasSize(2)
                    .anyMatch(c -> c.name().equals("Java Devs"))
                    .anyMatch(c -> c.name().equals("JavaScript Devs"));
            return null;
        });
    }

    @Test
    default void testSaveUpdatesChannel() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            Channel channel = trx.repoChannels().create("OldName", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), true);

            Channel updated = new ChannelBuilder()
                    .withId(channel.id())
                    .withName("NewName")
                    .withOwner(channel.owner())
                    .withIsPublic(false)
                    .build();
            trx.repoChannels().save(updated);

            Channel found = trx.repoChannels().findById(channel.id());
            assertThat(found.name()).isEqualTo("NewName");
            assertThat(found.isPublic()).isFalse();
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            Channel c1 = trx.repoChannels().create("C1", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), true);

            trx.repoChannels().deleteById(c1.id());
            assertThat(trx.repoChannels().findById(c1.id())).isNull();
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            trx.repoChannels().create("C1", new UserInfoBuilder().withId(owner.id()).withUsername(owner.username()).build(), true);

            trx.repoChannels().clear();
            assertThat(trx.repoChannels().findAll()).isEmpty();
            return null;
        });
    }
}