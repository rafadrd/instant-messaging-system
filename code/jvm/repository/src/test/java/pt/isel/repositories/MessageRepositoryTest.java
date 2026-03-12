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

public class MessageRepositoryTest {
    private MessageRepositoryInMem repo;
    private UserInfo author;
    private Channel channel;

    @BeforeEach
    void setUp() {
        repo = new MessageRepositoryInMem();
        author = new UserInfo(1L, "author");
        channel = new Channel(1L, "channel", author, true);
    }

    @Test
    void create_message() {
        Message msg = repo.create("Hello World", author, channel);
        assertNotNull(msg.id());
        assertEquals("Hello World", msg.content());
        assertEquals(author.id(), msg.user().id());
    }

    @Test
    void find_all_in_channel() {
        repo.create("Msg 1", author, channel);
        repo.create("Msg 2", author, channel);

        List<Message> messages = repo.findAllInChannel(channel, 10, 0);
        assertEquals(2, messages.size());
    }

    @Test
    void delete_message() {
        Message msg = repo.create("Delete me", author, channel);
        repo.deleteById(msg.id());
        assertNull(repo.findById(msg.id()));
    }

    @Test
    void save_updated_message() {
        Message msg = repo.create("Original", author, channel);
        Message updated = new Message(msg.id(), "Updated", author, channel, msg.createdAt());
        repo.save(updated);
        assertEquals("Updated", repo.findById(msg.id()).content());
    }
}