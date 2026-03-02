package pt.isel.repositories.users;

import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.Repository;

public interface UserRepository extends Repository<User> {
    User create(String username, PasswordValidationInfo passwordValidationInfo);

    User findByUsername(String username);

    boolean hasUsers();
}