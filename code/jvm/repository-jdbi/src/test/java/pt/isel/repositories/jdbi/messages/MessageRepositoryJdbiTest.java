package pt.isel.repositories.jdbi.messages;

import org.junit.jupiter.api.Test;
import pt.isel.domain.channels.Channel;
import pt.isel.domain.messages.Message;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.domain.users.UserInfo;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.contracts.MessageRepositoryContract;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageRepositoryJdbiTest extends AbstractJdbiTest implements MessageRepositoryContract {
    @Override
    public TransactionManager getTxManager() {
        return txManager;
    }

    @Test
    void testMessageAuthorIsNullWhenUserIsDeleted() {
        txManager.run(trx -> {
            User owner = trx.repoUsers().create("owner", new PasswordValidationInfo("hash"));
            UserInfo ownerInfo = new UserInfo(owner.id(), owner.username());
            Channel channel = trx.repoChannels().create("General", ownerInfo, true);

            User author = trx.repoUsers().create("author", new PasswordValidationInfo("hash"));
            UserInfo authorInfo = new UserInfo(author.id(), author.username());

            Message msg = trx.repoMessages().create("Ghost message", authorInfo, channel);
            assertNotNull(msg.user());

            trx.repoUsers().deleteById(author.id());

            Message found = trx.repoMessages().findById(msg.id());
            assertNotNull(found);
            assertEquals("Ghost message", found.content());
            assertNull(found.user(), "Author should be null because the user was deleted (ON DELETE SET NULL)");

            return null;
        });
    }
}