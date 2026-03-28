package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.ChannelBuilder;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface ChannelRepositoryContract extends RepositoryTestHelper {

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User owner = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", owner, true);

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
            User owner = insertUser(trx, "alice");
            insertChannel(trx, "Random", owner, true);

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
            User owner = insertUser(trx, "alice");

            insertChannel(trx, "C1", owner, true);
            insertChannel(trx, "C2", owner, false);

            List<Channel> channels = trx.repoChannels().findAll();
            assertThat(channels).hasSize(2);
            return null;
        });
    }

    @Test
    default void testFindAllByOwner() {
        getTxManager().run(trx -> {
            User owner1 = insertUser(trx, "alice");
            User owner2 = insertUser(trx, "bob");

            insertChannel(trx, "C1", owner1, true);
            insertChannel(trx, "C2", owner1, false);
            insertChannel(trx, "C3", owner2, true);

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
            User owner = insertUser(trx, "alice");

            insertChannel(trx, "Pub1", owner, true);
            insertChannel(trx, "Priv1", owner, false);
            insertChannel(trx, "Pub2", owner, true);
            insertChannel(trx, "Pub3", owner, true);

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
            User owner = insertUser(trx, "alice");

            insertChannel(trx, "Java Devs", owner, true);
            insertChannel(trx, "JavaScript Devs", owner, true);
            insertChannel(trx, "Secret Java", owner, false);

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
            User owner = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "OldName", owner, true);

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
            User owner = insertUser(trx, "alice");
            Channel c1 = insertChannel(trx, "C1", owner, true);

            trx.repoChannels().deleteById(c1.id());
            assertThat(trx.repoChannels().findById(c1.id())).isNull();
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User owner = insertUser(trx, "alice");
            insertChannel(trx, "C1", owner, true);

            trx.repoChannels().clear();
            assertThat(trx.repoChannels().findAll()).isEmpty();
            return null;
        });
    }
}