package com.jsystems.hsdc.persistence.support;

import com.jsystems.hsdc.persistence.config.DataSourceConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Test helper that builds a real, pool-of-1, WAL-mode SQLite
 * {@link DataSource} against a temp-file database and initializes it with
 * the exact same {@code schema.sql}/{@code data.sql} the application uses
 * (ADR-004 §8: no H2 / no different-engine substitute — TAC-004-02).
 *
 * <p>Returns {@link HikariDataSource} (not just {@link DataSource}) so
 * tests can {@code close()} it deterministically — WAL mode keeps
 * {@code -wal}/{@code -shm} sidecar files open until the pool is closed,
 * which otherwise blocks JUnit's {@code @TempDir} cleanup on Windows.
 */
public final class TestDatabases {

    private TestDatabases() {
    }

    /**
     * Builds a fresh datasource at {@code dbFile} and initializes it by
     * running {@code schema.sql} then {@code data.sql} once.
     */
    public static HikariDataSource fresh(Path dbFile) throws IOException {
        HikariDataSource dataSource = open(dbFile);
        initSchema(dataSource);
        return dataSource;
    }

    /**
     * Opens (without initializing) a datasource at {@code dbFile}. Useful
     * for restart-durability tests that need to reopen an already
     * initialized file.
     */
    public static HikariDataSource open(Path dbFile) throws IOException {
        return (HikariDataSource) DataSourceConfig.buildDataSource(dbFile.toString());
    }

    /** Runs {@code schema.sql} then {@code data.sql} against {@code dataSource}. */
    public static void initSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.addScript(new ClassPathResource("data.sql"));
        populator.execute(dataSource);
    }
}
