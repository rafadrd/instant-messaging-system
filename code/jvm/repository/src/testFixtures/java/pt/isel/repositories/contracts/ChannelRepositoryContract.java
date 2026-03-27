package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface ChannelRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());

            Channel channel = trx.repoChannels().create("General", ownerInfo, true);

            assertNotNull(channel);
            assertNotNull(channel.id());
            assertEquals("General", channel.name());
            assertTrue(channel.isPublic());

            Channel found = trx.repoChannels().findById(channel.id());
            assertEquals(channel.id(), found.id());
            assertEquals(owner.id(), found.owner().id());
            return null;
        });
    }

    @Test
    default void testFindByName() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            trx.repoChannels().create("Random", new UserInfo(owner.id(), owner.username()), true);

            Channel found = trx.repoChannels().findByName("Random");
            assertNotNull(found);
            assertEquals("Random", found.name());

            assertNull(trx.repoChannels().findByName("Unknown"));
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());

            trx.repoChannels().create("C1", ownerInfo, true);
            trx.repoChannels().create("C2", ownerInfo, false);

            List<Channel> channels = trx.repoChannels().findAll();
            assertEquals(2, channels.size());
            return null;
        });
    }

    @Test
    default void testFindAllByOwner() {
        getTxManager().run(trx -> {
            User owner1 = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            User owner2 = trx.repoUsers().create("bob", new PasswordValidationInfo("hash"));

            trx.repoChannels().create("C1", new UserInfo(owner1.id(), owner1.username()), true);
            trx.repoChannels().create("C2", new UserInfo(owner1.id(), owner1.username()), false);
            trx.repoChannels().create("C3", new UserInfo(owner2.id(), owner2.username()), true);

            List<Channel> aliceChannels = trx.repoChannels().findAllByOwner(owner1.id());
            assertEquals(2, aliceChannels.size());

            List<Channel> bobChannels = trx.repoChannels().findAllByOwner(owner2.id());
            assertEquals(1, bobChannels.size());
            return null;
        });
    }

    @Test
    default void testFindAllPublicChannelsWithPagination() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());

            trx.repoChannels().create("Pub1", ownerInfo, true);
            trx.repoChannels().create("Priv1", ownerInfo, false);
            trx.repoChannels().create("Pub2", ownerInfo, true);
            trx.repoChannels().create("Pub3", ownerInfo, true);

            List<Channel> page1 = trx.repoChannels().findAllPublicChannels(2, 0);
            assertEquals(2, page1.size());
            assertEquals("Pub1", page1.get(0).name());
            assertEquals("Pub2", page1.get(1).name());

            List<Channel> page2 = trx.repoChannels().findAllPublicChannels(2, 2);
            assertEquals(1, page2.size());
            assertEquals("Pub3", page2.getFirst().name());
            return null;
        });
    }

    @Test
    default void testSearchByName() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());

            trx.repoChannels().create("Java Devs", ownerInfo, true);
            trx.repoChannels().create("JavaScript Devs", ownerInfo, true);
            trx.repoChannels().create("Secret Java", ownerInfo, false);

            List<Channel> results = trx.repoChannels().searchByName("java", 10, 0);
            assertEquals(2, results.size());
            assertTrue(results.stream().anyMatch(c -> c.name().equals("Java Devs")));
            assertTrue(results.stream().anyMatch(c -> c.name().equals("JavaScript Devs")));
            return null;
        });
    }

    @Test
    default void testSaveUpdatesChannel() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            Channel channel = trx.repoChannels().create("OldName", new UserInfo(owner.id(), owner.username()), true);

            Channel updated = new Channel(channel.id(), "NewName", channel.owner(), false);
            trx.repoChannels().save(updated);

            Channel found = trx.repoChannels().findById(channel.id());
            assertEquals("NewName", found.name());
            assertFalse(found.isPublic());
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            Channel c1 = trx.repoChannels().create("C1", new UserInfo(owner.id(), owner.username()), true);

            trx.repoChannels().deleteById(c1.id());
            assertNull(trx.repoChannels().findById(c1.id()));
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User owner = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            trx.repoChannels().create("C1", new UserInfo(owner.id(), owner.username()), true);

            trx.repoChannels().clear();
            assertTrue(trx.repoChannels().findAll().isEmpty());
            return null;
        });
    }
}