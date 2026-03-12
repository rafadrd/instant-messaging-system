package pt.isel.repositories.mem;

import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.users.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class UserRepositoryInMem implements UserRepository {
    private final List<User> users = new ArrayList<>();
    private long nextId = 1;

    @Override
    public User create(String username, PasswordValidationInfo passwordValidationInfo) {
        User user = new User(nextId++, username, passwordValidationInfo);
        users.add(user);
        return user;
    }

    @Override
    public User findById(Long id) {
        return users.stream().filter(u -> u.id().equals(id)).findFirst().orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        return users.stream().filter(u -> u.username().equals(username)).findFirst().orElse(null);
    }

    @Override
    public boolean hasUsers() {
        return !users.isEmpty();
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    @Override
    public void save(User entity) {
        users.removeIf(u -> u.id().equals(entity.id()));
        users.add(entity);
    }

    @Override
    public void deleteById(Long id) {
        users.removeIf(u -> u.id().equals(id));
    }

    @Override
    public void clear() {
        users.clear();
        nextId = 1;
    }
}