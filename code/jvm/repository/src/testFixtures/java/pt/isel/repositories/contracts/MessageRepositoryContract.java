package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public interface MessageRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            Message msg = trx.repoMessages().create("Hello World", userInfo, channel);

            assertNotNull(msg);
            assertNotNull(msg.id());
            assertEquals("Hello World", msg.content());
            assertEquals(user.id(), msg.user().id());
            assertEquals(channel.id(), msg.channel().id());

            Message found = trx.repoMessages().findById(msg.id());
            assertEquals(msg.id(), found.id());
            assertEquals(msg.content(), found.content());
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            trx.repoMessages().create("Msg 1", userInfo, channel);
            trx.repoMessages().create("Msg 2", userInfo, channel);

            List<Message> allMessages = trx.repoMessages().findAll();
            assertEquals(2, allMessages.size());
            return null;
        });
    }

    @Test
    default void testFindAllInChannelWithPagination() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            trx.repoMessages().create("Msg 1", userInfo, channel);
            trx.repoMessages().create("Msg 2", userInfo, channel);
            trx.repoMessages().create("Msg 3", userInfo, channel);

            Channel otherChannel = trx.repoChannels().create("Other", userInfo, true);
            trx.repoMessages().create("Other Msg", userInfo, otherChannel);

            List<Message> page1 = trx.repoMessages().findAllInChannel(channel, 2, 0);
            assertEquals(2, page1.size());
            assertEquals("Msg 3", page1.get(0).content());
            assertEquals("Msg 2", page1.get(1).content());

            List<Message> page2 = trx.repoMessages().findAllInChannel(channel, 2, 2);
            assertEquals(1, page2.size());
            assertEquals("Msg 1", page2.getFirst().content());
            return null;
        });
    }

    @Test
    default void testSaveUpdatesMessage() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            Message msg = trx.repoMessages().create("Original", userInfo, channel);
            Message updated = new Message(msg.id(), "Edited", userInfo, channel, msg.createdAt());

            trx.repoMessages().save(updated);

            assertEquals("Edited", trx.repoMessages().findById(msg.id()).content());
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            Message msg = trx.repoMessages().create("To Delete", userInfo, channel);
            trx.repoMessages().deleteById(msg.id());

            assertNull(trx.repoMessages().findById(msg.id()));
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            Channel channel = trx.repoChannels().create("General", new UserInfo(user.id(), user.username()), true);
            trx.repoMessages().create("Msg", new UserInfo(user.id(), user.username()), channel);

            trx.repoMessages().clear();
            assertTrue(trx.repoMessages().findAll().isEmpty());
            return null;
        });
    }
}