package pt.isel.repositories.jdbi.users;

import org.jdbi.v3.core.Handle;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.jdbi.utils.JdbiUtils;
import pt.isel.repositories.users.UserRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class UserRepositoryJdbi implements UserRepository {
    private final Handle handle;

    public UserRepositoryJdbi(Handle handle) {
        this.handle = handle;
    }

    @Override
    public User create(String username, PasswordValidationInfo passwordValidationInfo) {
        Long id = JdbiUtils.executeUpdateAndReturnId(handle, """
                INSERT INTO users (username, password_validation)
                VALUES (:username, :password_validation)
                """, JdbiUtils.params(
                "username", username,
                "password_validation", passwordValidationInfo.validationInfo()
        ));
        return new User(id, username, passwordValidationInfo);
    }

    @Override
    public User findById(Long id) {
        return JdbiUtils.executeQueryToSingle(handle, "SELECT * FROM users WHERE id = :id",
                Map.of("id", id), this::mapRowToUser);
    }

    @Override
    public User findByUsername(String username) {
        return JdbiUtils.executeQueryToSingle(handle, "SELECT * FROM users WHERE username = :username",
                Map.of("username", username), this::mapRowToUser);
    }

    @Override
    public boolean hasUsers() {
        return handle.createQuery("SELECT 1 FROM users LIMIT 1")
                .mapTo(Integer.class)
                .findOne()
                .isPresent();
    }

    @Override
    public List<User> findAll() {
        return JdbiUtils.executeQueryToList(handle, "SELECT * FROM users", Map.of(), this::mapRowToUser);
    }

    @Override
    public void save(User entity) {
        JdbiUtils.executeUpdate(handle, """
                UPDATE users
                SET username = :username, password_validation = :password_validation
                WHERE id = :id
                """, JdbiUtils.params(
                "id", entity.id(),
                "username", entity.username(),
                "password_validation", entity.passwordValidation().validationInfo()
        ));
    }

    @Override
    public void deleteById(Long id) {
        JdbiUtils.executeUpdate(handle, "DELETE FROM users WHERE id = :id", Map.of("id", id));
    }

    @Override
    public void clear() {
        JdbiUtils.executeUpdate(handle, "DELETE FROM users", Map.of());
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getString("username"),
                new PasswordValidationInfo(rs.getString("password_validation"))
        );
    }
}