package pt.isel.repositories.jdbi.users;

import org.junit.jupiter.api.Test;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.jdbi.AbstractJdbiTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryJdbiTest extends AbstractJdbiTest {

    @Test
    void testCreateAndFindById() {
        txManager.run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash123"));

            assertNotNull(user);
            assertNotNull(user.id());
            assertEquals("alice", user.username());

            User found = trx.repoUsers().findById(user.id());
            assertEquals(user.id(), found.id());
            assertEquals("alice", found.username());
            assertEquals("hash123", found.passwordValidation().validationInfo());
            return null;
        });
    }

    @Test
    void testFindByUsername() {
        txManager.run(trx -> {
            trx.repoUsers().create("bob", new PasswordValidationInfo("hash456"));

            User found = trx.repoUsers().findByUsername("bob");
            assertNotNull(found);
            assertEquals("bob", found.username());

            assertNull(trx.repoUsers().findByUsername("nonexistent"));
            return null;
        });
    }

    @Test
    void testHasUsers() {
        txManager.run(trx -> {
            assertFalse(trx.repoUsers().hasUsers());
            trx.repoUsers().create("charlie", new PasswordValidationInfo("hash789"));
            assertTrue(trx.repoUsers().hasUsers());
            return null;
        });
    }

    @Test
    void testFindAll() {
        txManager.run(trx -> {
            trx.repoUsers().create("user1", new PasswordValidationInfo("h1"));
            trx.repoUsers().create("user2", new PasswordValidationInfo("h2"));

            List<User> users = trx.repoUsers().findAll();
            assertEquals(2, users.size());
            return null;
        });
    }

    @Test
    void testSaveUpdatesExistingUser() {
        txManager.run(trx -> {
            User user = trx.repoUsers().create("dave", new PasswordValidationInfo("h1"));
            User updated = new User(user.id(), "dave_updated", new PasswordValidationInfo("h2"));

            trx.repoUsers().save(updated);

            User found = trx.repoUsers().findById(user.id());
            assertEquals("dave_updated", found.username());
            assertEquals("h2", found.passwordValidation().validationInfo());
            return null;
        });
    }

    @Test
    void testDeleteById() {
        txManager.run(trx -> {
            User user = trx.repoUsers().create("eve", new PasswordValidationInfo("h1"));
            trx.repoUsers().deleteById(user.id());

            assertNull(trx.repoUsers().findById(user.id()));
            assertFalse(trx.repoUsers().hasUsers());
            return null;
        });
    }

    @Test
    void testClear() {
        txManager.run(trx -> {
            trx.repoUsers().create("frank", new PasswordValidationInfo("h1"));
            trx.repoUsers().clear();

            assertFalse(trx.repoUsers().hasUsers());
            assertTrue(trx.repoUsers().findAll().isEmpty());
            return null;
        });
    }
}