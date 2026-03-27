package pt.isel.host;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("ims_db")
            .withUsername("dbuser")
            .withPassword("dbpass");

    @SuppressWarnings("resource")
    static final GenericContainer<?> redis = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    static {
        postgres.start();
        redis.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Configure PostgreSQL
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl() + "&currentSchema=dbo");
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        // Configure Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        // Configure JWT
        registry.add("jwt.secret", () -> "9RIMv2cGYBeRFYtA0/k9KIo+uOj3cgwGh6qgLZ1U1mI=");
    }
}