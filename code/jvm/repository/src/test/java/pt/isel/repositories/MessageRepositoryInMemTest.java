package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.mem.MessageRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRepositoryInMemTest {

    private MessageRepositoryInMem repo;
    private UserInfo user;
    private Channel channel;

    @BeforeEach
    void setUp() {
        repo = new MessageRepositoryInMem();
        user = new UserInfo(1L, "alice");
        channel = new Channel(10L, "General", user, true);
    }

    @Test
    void testCreateAndFindById() {
        Message msg = repo.create("Hello World", user, channel);

        assertNotNull(msg);
        assertEquals(1L, msg.id());
        assertEquals("Hello World", msg.content());
        assertEquals(user, msg.user());
        assertEquals(channel, msg.channel());

        Message found = repo.findById(msg.id());
        assertEquals(msg, found);
    }

    @Test
    void testFindAllInChannelWithPagination() {
        repo.create("Msg 1", user, channel);
        repo.create("Msg 2", user, channel);
        repo.create("Msg 3", user, channel);

        Channel otherChannel = new Channel(20L, "Other", user, true);
        repo.create("Other Msg", user, otherChannel);

        List<Message> page1 = repo.findAllInChannel(channel, 2, 0);
        assertEquals(2, page1.size());
        assertEquals("Msg 1", page1.get(0).content());
        assertEquals("Msg 2", page1.get(1).content());

        List<Message> page2 = repo.findAllInChannel(channel, 2, 2);
        assertEquals(1, page2.size());
        assertEquals("Msg 3", page2.getFirst().content());
    }

    @Test
    void testSaveUpdatesMessage() {
        Message msg = repo.create("Original", user, channel);
        Message updated = new Message(msg.id(), "Edited", user, channel, msg.createdAt());

        repo.save(updated);

        assertEquals("Edited", repo.findById(msg.id()).content());
    }

    @Test
    void testDeleteByIdAndClear() {
        Message msg = repo.create("To Delete", user, channel);
        repo.deleteById(msg.id());
        assertNull(repo.findById(msg.id()));

        repo.create("Another", user, channel);
        repo.clear();
        assertTrue(repo.findAll().isEmpty());
    }
}