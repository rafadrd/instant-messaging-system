package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface MessageRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash"));
            UserInfo userInfo = new UserInfo(user.id(), user.username());
            Channel channel = trx.repoChannels().create("General", userInfo, true);

            Message msg = trx.repoMessages().create("Hello World", userInfo, channel);

            assertThat(msg).isNotNull();
            assertThat(msg.id()).isNotNull();
            assertThat(msg.content()).isEqualTo("Hello World");
            assertThat(msg.user().id()).isEqualTo(user.id());
            assertThat(msg.channel().id()).isEqualTo(channel.id());

            Message found = trx.repoMessages().findById(msg.id());
            assertThat(found.id()).isEqualTo(msg.id());
            assertThat(found.content()).isEqualTo(msg.content());
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
            assertThat(allMessages).hasSize(2);
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
            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).content()).isEqualTo("Msg 3");
            assertThat(page1.get(1).content()).isEqualTo("Msg 2");

            List<Message> page2 = trx.repoMessages().findAllInChannel(channel, 2, 2);
            assertThat(page2).hasSize(1);
            assertThat(page2.getFirst().content()).isEqualTo("Msg 1");
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

            assertThat(trx.repoMessages().findById(msg.id()).content()).isEqualTo("Edited");
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

            assertThat(trx.repoMessages().findById(msg.id())).isNull();
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
            assertThat(trx.repoMessages().findAll()).isEmpty();
            return null;
        });
    }
}