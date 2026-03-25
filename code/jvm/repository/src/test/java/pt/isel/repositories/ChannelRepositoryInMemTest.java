package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.ChannelRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelRepositoryInMemTest {

    private ChannelRepositoryInMem repo;
    private UserInfo owner1;
    private UserInfo owner2;

    @BeforeEach
    void setUp() {
        repo = new ChannelRepositoryInMem();
        owner1 = new UserInfo(1L, "alice");
        owner2 = new UserInfo(2L, "bob");
    }

    @Test
    void testCreateAndFindById() {
        Channel channel = repo.create("General", owner1, true);

        assertNotNull(channel);
        assertEquals(1L, channel.id());
        assertEquals("General", channel.name());
        assertTrue(channel.isPublic());

        Channel found = repo.findById(channel.id());
        assertEquals(channel, found);
    }

    @Test
    void testFindByName() {
        repo.create("Random", owner1, true);

        Channel found = repo.findByName("Random");
        assertNotNull(found);
        assertEquals("Random", found.name());

        assertNull(repo.findByName("Unknown"));
    }

    @Test
    void testFindAllByOwner() {
        repo.create("C1", owner1, true);
        repo.create("C2", owner1, false);
        repo.create("C3", owner2, true);

        List<Channel> aliceChannels = repo.findAllByOwner(owner1.id());
        assertEquals(2, aliceChannels.size());

        List<Channel> bobChannels = repo.findAllByOwner(owner2.id());
        assertEquals(1, bobChannels.size());
    }

    @Test
    void testFindAllPublicChannelsWithPagination() {
        repo.create("Pub1", owner1, true);
        repo.create("Priv1", owner1, false);
        repo.create("Pub2", owner2, true);
        repo.create("Pub3", owner2, true);

        List<Channel> page1 = repo.findAllPublicChannels(2, 0);
        assertEquals(2, page1.size());
        assertEquals("Pub1", page1.get(0).name());
        assertEquals("Pub2", page1.get(1).name());

        List<Channel> page2 = repo.findAllPublicChannels(2, 2);
        assertEquals(1, page2.size());
        assertEquals("Pub3", page2.getFirst().name());
    }

    @Test
    void testSearchByName() {
        repo.create("Java Devs", owner1, true);
        repo.create("JavaScript Devs", owner2, true);
        repo.create("Secret Java", owner1, false); // Private, should not be found

        List<Channel> results = repo.searchByName("java", 10, 0);
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(c -> c.name().equals("Java Devs")));
        assertTrue(results.stream().anyMatch(c -> c.name().equals("JavaScript Devs")));
    }

    @Test
    void testSaveUpdatesChannel() {
        Channel channel = repo.create("OldName", owner1, true);
        Channel updated = new Channel(channel.id(), "NewName", owner1, false);

        repo.save(updated);

        Channel found = repo.findById(channel.id());
        assertEquals("NewName", found.name());
        assertFalse(found.isPublic());
    }

    @Test
    void testDeleteByIdAndClear() {
        Channel c1 = repo.create("C1", owner1, true);
        repo.deleteById(c1.id());
        assertNull(repo.findById(c1.id()));

        repo.create("C2", owner1, true);
        repo.clear();
        assertTrue(repo.findAll().isEmpty());
    }
}