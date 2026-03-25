package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.mem.UserRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserRepositoryInMemTest {

    private UserRepositoryInMem repo;

    @BeforeEach
    void setUp() {
        repo = new UserRepositoryInMem();
    }

    @Test
    void testCreateAndFindById() {
        User user = repo.create("alice", new PasswordValidationInfo("hash123"));

        assertNotNull(user);
        assertEquals(1L, user.id());
        assertEquals("alice", user.username());

        User found = repo.findById(user.id());
        assertEquals(user, found);
    }

    @Test
    void testFindByUsername() {
        repo.create("bob", new PasswordValidationInfo("hash456"));

        User found = repo.findByUsername("bob");
        assertNotNull(found);
        assertEquals("bob", found.username());

        assertNull(repo.findByUsername("nonexistent"));
    }

    @Test
    void testHasUsers() {
        assertFalse(repo.hasUsers());
        repo.create("charlie", new PasswordValidationInfo("hash789"));
        assertTrue(repo.hasUsers());
    }

    @Test
    void testFindAll() {
        repo.create("user1", new PasswordValidationInfo("h1"));
        repo.create("user2", new PasswordValidationInfo("h2"));

        List<User> users = repo.findAll();
        assertEquals(2, users.size());
    }

    @Test
    void testSaveUpdatesExistingUser() {
        User user = repo.create("dave", new PasswordValidationInfo("h1"));
        User updated = new User(user.id(), "dave_updated", new PasswordValidationInfo("h2"));

        repo.save(updated);

        User found = repo.findById(user.id());
        assertEquals("dave_updated", found.username());
        assertEquals("h2", found.passwordValidation().validationInfo());
    }

    @Test
    void testDeleteById() {
        User user = repo.create("eve", new PasswordValidationInfo("h1"));
        repo.deleteById(user.id());

        assertNull(repo.findById(user.id()));
        assertFalse(repo.hasUsers());
    }

    @Test
    void testClear() {
        repo.create("frank", new PasswordValidationInfo("h1"));
        repo.clear();

        assertFalse(repo.hasUsers());
        assertEquals(1L, repo.create("grace", new PasswordValidationInfo("h2")).id()); // ID resets to 1
    }
}