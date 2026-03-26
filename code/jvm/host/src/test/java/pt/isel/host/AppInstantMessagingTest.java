package pt.isel.host;

import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.repositories.TransactionManager;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppInstantMessagingTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void contextLoads() {
        assertNotNull(context, "The Spring application context should load successfully.");
    }

    @Test
    void verifyCustomBeansAreLoaded() {
        assertNotNull(context.getBean(Jdbi.class), "Jdbi bean should be present in the context");
        assertNotNull(context.getBean(TransactionManager.class), "TransactionManager bean should be present in the context");
        assertNotNull(context.getBean(PasswordEncoder.class), "PasswordEncoder bean should be present in the context");
        assertNotNull(context.getBean(Clock.class), "Clock bean should be present in the context");

        PasswordPolicyConfig passwordPolicyConfig = context.getBean(PasswordPolicyConfig.class);
        assertNotNull(passwordPolicyConfig, "PasswordPolicyConfig bean should be present in the context");
    }
}