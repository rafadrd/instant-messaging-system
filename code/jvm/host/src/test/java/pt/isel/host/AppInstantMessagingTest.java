package pt.isel.host;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.repositories.TransactionManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class AppInstantMessagingTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void Context_Loads_Successfully() {
        assertThat(context).as("The Spring application context should load successfully.").isNotNull();
    }

    @Test
    void Context_CustomBeans_AreLoaded() {
        assertThat(context.getBean(Jdbi.class)).as("Jdbi bean should be present in the context").isNotNull();
        assertThat(context.getBean(TransactionManager.class)).as("TransactionManager bean should be present in the context").isNotNull();
        assertThat(context.getBean(PasswordEncoder.class)).as("PasswordEncoder bean should be present in the context").isNotNull();
        assertThat(context.getBean(Clock.class)).as("Clock bean should be present in the context").isNotNull();

        PasswordPolicyConfig passwordPolicyConfig = context.getBean(PasswordPolicyConfig.class);
        assertThat(passwordPolicyConfig).as("PasswordPolicyConfig bean should be present in the context").isNotNull();
    }
}