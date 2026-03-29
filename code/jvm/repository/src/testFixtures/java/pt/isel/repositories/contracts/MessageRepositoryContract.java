package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.MessageBuilder;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface MessageRepositoryContract extends RepositoryTestHelper {

    @Test
    default void Create_ValidInput_CreatesAndFindsById() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);

            Message msg = trx.repoMessages().create("Hello World", toUserInfo(user), channel, FIXED_TIME);

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
    default void FindAll_HasRecords_ReturnsAllRecords() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            trx.repoMessages().create("Msg 1", toUserInfo(user), channel, FIXED_TIME);
            trx.repoMessages().create("Msg 2", toUserInfo(user), channel, FIXED_TIME);

            List<Message> allMessages = trx.repoMessages().findAll();

            assertThat(allMessages).hasSize(2);
            return null;
        });
    }

    @Test
    default void FindAllInChannel_ValidPagination_ReturnsMessages() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            trx.repoMessages().create("Msg 1", toUserInfo(user), channel, FIXED_TIME.minusMinutes(3));
            trx.repoMessages().create("Msg 2", toUserInfo(user), channel, FIXED_TIME.minusMinutes(2));
            trx.repoMessages().create("Msg 3", toUserInfo(user), channel, FIXED_TIME.minusMinutes(1));
            Channel otherChannel = insertChannel(trx, "Other", user, true);
            trx.repoMessages().create("Other Msg", toUserInfo(user), otherChannel, FIXED_TIME);

            List<Message> page1 = trx.repoMessages().findAllInChannel(channel, 2, 0);
            List<Message> page2 = trx.repoMessages().findAllInChannel(channel, 2, 2);

            assertThat(page1).hasSize(2);
            assertThat(page1.get(0).content()).isEqualTo("Msg 3");
            assertThat(page1.get(1).content()).isEqualTo("Msg 2");
            assertThat(page2).hasSize(1);
            assertThat(page2.getFirst().content()).isEqualTo("Msg 1");
            return null;
        });
    }

    @Test
    default void Save_UpdatedMessage_UpdatesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            Message msg = trx.repoMessages().create("Original", toUserInfo(user), channel, FIXED_TIME);
            Message updated = new MessageBuilder()
                    .withId(msg.id())
                    .withContent("Edited")
                    .withUser(toUserInfo(user))
                    .withChannel(channel)
                    .withCreatedAt(msg.createdAt())
                    .build();

            trx.repoMessages().save(updated);

            assertThat(trx.repoMessages().findById(msg.id()).content()).isEqualTo("Edited");
            return null;
        });
    }

    @Test
    default void DeleteById_ValidId_DeletesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            Message msg = trx.repoMessages().create("To Delete", toUserInfo(user), channel, FIXED_TIME);

            trx.repoMessages().deleteById(msg.id());

            assertThat(trx.repoMessages().findById(msg.id())).isNull();
            return null;
        });
    }

    @Test
    default void Clear_HasRecords_RemovesAllRecords() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "alice");
            Channel channel = insertChannel(trx, "General", user, true);
            trx.repoMessages().create("Msg", toUserInfo(user), channel, FIXED_TIME);

            trx.repoMessages().clear();

            assertThat(trx.repoMessages().findAll()).isEmpty();
            return null;
        });
    }
}