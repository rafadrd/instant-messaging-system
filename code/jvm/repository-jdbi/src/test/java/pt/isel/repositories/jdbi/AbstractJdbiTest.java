package pt.isel.repositories.jdbi;

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import pt.isel.repositories.jdbi.transaction.TransactionManagerJdbi;
import pt.isel.repositories.jdbi.utils.JdbiConfig;

public abstract class AbstractJdbiTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    protected static Jdbi jdbi;
    protected static TransactionManagerJdbi txManager;

    static {
        postgres.start();
    }

    @BeforeAll
    static void initAll() {
        PGSimpleDataSource ds = new PGSimpleDataSource();
        ds.setURL(postgres.getJdbcUrl() + "&currentSchema=dbo");
        ds.setUser(postgres.getUsername());
        ds.setPassword(postgres.getPassword());

        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .schemas("dbo")
                .defaultSchema("dbo")
                .cleanDisabled(false)
                .load();

        flyway.clean();
        flyway.migrate();

        jdbi = JdbiConfig.configureWithAppRequirements(Jdbi.create(ds));
        txManager = new TransactionManagerJdbi(jdbi);
    }

    @BeforeEach
    void cleanUp() {
        txManager.run(trx -> {
            trx.repoTokenBlacklist().clear();
            trx.repoMessages().clear();
            trx.repoInvitations().clear();
            trx.repoMemberships().clear();
            trx.repoChannels().clear();
            trx.repoUsers().clear();
            return null;
        });
    }
}