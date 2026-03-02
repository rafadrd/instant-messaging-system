package pt.isel.host;

import org.jdbi.v3.core.Jdbi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.isel.domain.security.PasswordPolicyConfig;
import pt.isel.repositories.TransactionManager;
import pt.isel.repositories.jdbi.transaction.TransactionManagerJdbi;
import pt.isel.repositories.jdbi.utils.JdbiConfig;

import javax.sql.DataSource;
import java.time.Clock;

@SpringBootApplication(scanBasePackages = "pt.isel")
@EnableScheduling
public class AppInstantMessaging {

    public static void main(String[] args) {
        SpringApplication.run(AppInstantMessaging.class, args);
    }

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        return JdbiConfig.configureWithAppRequirements(Jdbi.create(dataSource));
    }

    @Bean
    @Profile("jdbi")
    public TransactionManager trxManagerJdbi(Jdbi jdbi) {
        return new TransactionManagerJdbi(jdbi);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordPolicyConfig passwordPolicyConfig(
            @Value("${password.policy.min-length}") int minLength,
            @Value("${password.policy.requires-uppercase}") boolean requiresUppercase,
            @Value("${password.policy.requires-lowercase}") boolean requiresLowercase,
            @Value("${password.policy.requires-digit}") boolean requiresDigit,
            @Value("${password.policy.requires-special-char}") boolean requiresSpecialChar
    ) {
        return new PasswordPolicyConfig(minLength, requiresUppercase, requiresLowercase, requiresDigit, requiresSpecialChar);
    }
}