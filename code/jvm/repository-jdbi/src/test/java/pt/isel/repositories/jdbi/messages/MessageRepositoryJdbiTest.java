package pt.isel.repositories.jdbi.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.users.User;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.MessageRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRepositoryJdbiTest extends AbstractJdbiTest implements MessageRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }

    @Test
    void testMessageAuthorIsNullWhenUserIsDeleted() {
        txManager.run(trx -> {
            User owner = insertUser(trx, "owner");
            Channel channel = insertChannel(trx, "General", owner, true);

            User author = insertUser(trx, "author");

            Message msg = trx.repoMessages().create("Ghost message", toUserInfo(author), channel, LocalDateTime.now(ZoneOffset.UTC));
            assertThat(msg.user()).isNotNull();

            trx.repoUsers().deleteById(author.id());

            Message found = trx.repoMessages().findById(msg.id());
            assertThat(found).isNotNull();
            assertThat(found.content()).isEqualTo("Ghost message");
            assertThat(found.user()).as("Author should be null because the user was deleted (ON DELETE SET NULL)").isNull();

            return null;
        });
    }
}