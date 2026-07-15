package com.jsystems.hsdc.persistence.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

/**
 * Builds the single SQLite {@link DataSource} for the application
 * (ADR-004 §3, §6 "Single-connection pool + WAL mode").
 *
 * <p>SQLite allows exactly one writer at a time; a Hikari pool larger than
 * 1 would only invite intermittent {@code SQLITE_BUSY} errors under
 * concurrent writes for no benefit at this scale. WAL journal mode and
 * {@code foreign_keys} enforcement are set on the underlying
 * {@link SQLiteConfig} so the xerial driver applies both pragmas to every
 * physical connection it opens (SQLite executes one pragma per statement,
 * so this must happen at the driver level, not via a single
 * Hikari {@code connection-init-sql} string).
 */
@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${HSDC_DB_PATH:./data/hsdc.db}")
    private String dbPath;

    @Bean
    public DataSource dataSource() throws IOException {
        return buildDataSource(dbPath);
    }

    @Bean
    public JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    /**
     * Builds a pool-of-1 Hikari {@link DataSource} backed by a
     * {@link SQLiteDataSource} configured for WAL journaling and enforced
     * foreign keys, creating the parent directory of {@code dbPath} if
     * needed. Package-visible (not private) so tests can build isolated
     * temp-file datasources with the exact same configuration as
     * production (ADR-004 §8: "real temp-file SQLite database... via the
     * same schema.sql").
     *
     * @param dbPath filesystem path to the SQLite database file
     * @return a ready-to-use, pool-of-1 DataSource
     * @throws IOException if the parent directory cannot be created
     */
    public static DataSource buildDataSource(String dbPath) throws IOException {
        Path path = Path.of(dbPath).toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.enforceForeignKeys(true);

        SQLiteDataSource sqliteDataSource = new SQLiteDataSource(sqliteConfig);
        sqliteDataSource.setUrl("jdbc:sqlite:" + path);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(sqliteDataSource);
        hikariConfig.setMaximumPoolSize(1);
        hikariConfig.setPoolName("hsdc-sqlite-pool");
        log.info("Opening SQLite datasource at {} (pool size 1, WAL, foreign_keys=ON)", path);
        return new HikariDataSource(hikariConfig);
    }
}
