package pt.isel.repositories.contracts;

import org.junit.jupiter.api.Test;
import pt.isel.domain.builders.UserBuilder;
import pt.isel.domain.security.PasswordValidationInfo;
import pt.isel.domain.users.User;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public interface UserRepositoryContract extends RepositoryTestHelper {

    @Test
    default void Create_ValidInput_CreatesAndFindsById() {
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
    default void FindByUsername_ValidUsername_ReturnsUser() {
        getTxManager().run(trx -> {
            insertUser(trx, "bob");

            User found = trx.repoUsers().findByUsername("bob");
            assertThat(found).isNotNull();
            assertThat(found.username()).isEqualTo("bob");

            assertThat(trx.repoUsers().findByUsername("nonexistent")).isNull();
            return null;
        });
    }

    @Test
    default void HasUsers_HasRecords_ReturnsTrue() {
        getTxManager().run(trx -> {
            assertThat(trx.repoUsers().hasUsers()).isFalse();
            insertUser(trx, "charlie");
            assertThat(trx.repoUsers().hasUsers()).isTrue();
            return null;
        });
    }

    @Test
    default void FindAll_HasRecords_ReturnsAllRecords() {
        getTxManager().run(trx -> {
            insertUser(trx, "user1");
            insertUser(trx, "user2");

            List<User> users = trx.repoUsers().findAll();
            assertThat(users).hasSize(2);
            return null;
        });
    }

    @Test
    default void Save_UpdatedUser_UpdatesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "dave");
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
    default void DeleteById_ValidId_DeletesRecord() {
        getTxManager().run(trx -> {
            User user = insertUser(trx, "eve");
            trx.repoUsers().deleteById(user.id());

            assertThat(trx.repoUsers().findById(user.id())).isNull();
            assertThat(trx.repoUsers().hasUsers()).isFalse();
            return null;
        });
    }

    @Test
    default void Clear_HasRecords_RemovesAllRecords() {
        getTxManager().run(trx -> {
            insertUser(trx, "frank");
            trx.repoUsers().clear();

            assertThat(trx.repoUsers().hasUsers()).isFalse();
            assertThat(trx.repoUsers().findAll()).isEmpty();
            return null;
        });
    }
}