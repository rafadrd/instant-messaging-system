package pt.isel.repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.mem.UserRepositoryInMem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UserRepositoryTest {
    private static final String PASSWORD = "Aa1#2345";
    private UserRepositoryInMem repo;

    @BeforeEach
    void setUp() {
        repo = new UserRepositoryInMem();
    }

    @Test
    void create_user() {
        User newUser = repo.create("AntonioBanderas", new PasswordValidationInfo(PASSWORD));
        assertEquals(1L, newUser.id());
        assertEquals("AntonioBanderas", newUser.username());
        assertEquals(PASSWORD, newUser.passwordValidation().validationInfo());
    }

    @Test
    void create_multiple_users() {
        User u1 = repo.create("BradPitt", new PasswordValidationInfo("1234"));
        User u2 = repo.create("AngelinaJolie", new PasswordValidationInfo("1234"));
        assertEquals(1L, u1.id());
        assertEquals(2L, u2.id());
    }

    @Test
    void find_user_by_id() {
        User newUser = repo.create("AntonioBanderas", new PasswordValidationInfo(PASSWORD));
        User foundUser = repo.findById(newUser.id());
        assertEquals(newUser, foundUser);
    }

    @Test
    void find_user_by_username() {
        User newUser = repo.create("AntonioBanderas", new PasswordValidationInfo(PASSWORD));
        User foundUser = repo.findByUsername("AntonioBanderas");
        assertEquals(newUser, foundUser);
    }

    @Test
    void find_all_users() {
        User u1 = repo.create("U1", new PasswordValidationInfo(PASSWORD));
        User u2 = repo.create("U2", new PasswordValidationInfo(PASSWORD));
        List<User> all = repo.findAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(u1));
        assertTrue(all.contains(u2));
    }

    @Test
    void save_user() {
        User newUser = repo.create("Antonio", new PasswordValidationInfo(PASSWORD));
        User updatedUser = new User(newUser.id(), "AntonioUpdated", newUser.passwordValidation());
        repo.save(updatedUser);
        assertEquals("AntonioUpdated", repo.findById(newUser.id()).username());
    }

    @Test
    void delete_user_by_id() {
        User user = repo.create("Antonio", new PasswordValidationInfo(PASSWORD));
        repo.deleteById(user.id());
        assertNull(repo.findById(user.id()));
    }

    @Test
    void clear_all_users() {
        repo.create("U1", new PasswordValidationInfo(PASSWORD));
        repo.clear();
        assertTrue(repo.findAll().isEmpty());
    }
}