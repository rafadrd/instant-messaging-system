package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;
import pt.isel.repositories.TransactionManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface UserRepositoryContract {
    TransactionManager getTxManager();

    @Test
    default void testCreateAndFindById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("alice", new PasswordValidationInfo("hash123"));

            assertThat(user).isNotNull();
            assertThat(user.id()).isNotNull();
            assertThat(user.username()).isEqualTo("alice");

            User found = trx.repoUsers().findById(user.id());
            assertThat(found.id()).isEqualTo(user.id());
            assertThat(found.username()).isEqualTo("alice");
            assertThat(found.passwordValidation().validationInfo()).isEqualTo("hash123");
            return null;
        });
    }

    @Test
    default void testFindByUsername() {
        getTxManager().run(trx -> {
            trx.repoUsers().create("bob", new PasswordValidationInfo("hash456"));

            User found = trx.repoUsers().findByUsername("bob");
            assertThat(found).isNotNull();
            assertThat(found.username()).isEqualTo("bob");

            assertThat(trx.repoUsers().findByUsername("nonexistent")).isNull();
            return null;
        });
    }

    @Test
    default void testHasUsers() {
        getTxManager().run(trx -> {
            assertThat(trx.repoUsers().hasUsers()).isFalse();
            trx.repoUsers().create("charlie", new PasswordValidationInfo("hash789"));
            assertThat(trx.repoUsers().hasUsers()).isTrue();
            return null;
        });
    }

    @Test
    default void testFindAll() {
        getTxManager().run(trx -> {
            trx.repoUsers().create("user1", new PasswordValidationInfo("h1"));
            trx.repoUsers().create("user2", new PasswordValidationInfo("h2"));

            List<User> users = trx.repoUsers().findAll();
            assertThat(users).hasSize(2);
            return null;
        });
    }

    @Test
    default void testSaveUpdatesExistingUser() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("dave", new PasswordValidationInfo("h1"));
            User updated = new UserBuilder()
                    .withId(user.id())
                    .withUsername("dave_updated")
                    .withPasswordValidation(new PasswordValidationInfo("h2"))
                    .build();

            trx.repoUsers().save(updated);

            User found = trx.repoUsers().findById(user.id());
            assertThat(found.username()).isEqualTo("dave_updated");
            assertThat(found.passwordValidation().validationInfo()).isEqualTo("h2");
            return null;
        });
    }

    @Test
    default void testDeleteById() {
        getTxManager().run(trx -> {
            User user = trx.repoUsers().create("eve", new PasswordValidationInfo("h1"));
            trx.repoUsers().deleteById(user.id());

            assertThat(trx.repoUsers().findById(user.id())).isNull();
            assertThat(trx.repoUsers().hasUsers()).isFalse();
            return null;
        });
    }

    @Test
    default void testClear() {
        getTxManager().run(trx -> {
            trx.repoUsers().create("frank", new PasswordValidationInfo("h1"));
            trx.repoUsers().clear();

            assertThat(trx.repoUsers().hasUsers()).isFalse();
            assertThat(trx.repoUsers().findAll()).isEmpty();
            return null;
        });
    }
}