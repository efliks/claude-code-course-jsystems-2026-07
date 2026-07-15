package com.jsystems.hsdc.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Covers ADR-004 §8 "Schema idempotency" and the WAL / foreign_keys pragma
 * requirements from ADR-004 §3 and §6, plus TAC-004-01 (fresh run creates 5
 * tables + seeds; rerun does not duplicate rows).
 */
class DataSourceConfigTest {

    @Test
    void journalModeIsWalAndForeignKeysAreEnforced(@TempDir Path tempDir) throws Exception {
        try (HikariDataSource dataSource = TestDatabases.fresh(tempDir.resolve("pragmas.db"))) {
            try (Connection conn = dataSource.getConnection()) {
                assertThat(readPragma(conn, "journal_mode")).isEqualToIgnoringCase("wal");
                assertThat(readPragma(conn, "foreign_keys")).isEqualTo("1");
            }
        }
    }

    @Test
    void freshRunCreatesFiveTablesWithSeedRows(@TempDir Path tempDir) throws Exception {
        try (HikariDataSource dataSource = TestDatabases.fresh(tempDir.resolve("fresh.db"))) {
            try (Connection conn = dataSource.getConnection()) {
                assertThat(tableNames(conn)).containsExactlyInAnyOrder(
                        "case_session", "decision", "chat_message", "customer", "purchase");
                assertThat(countRows(conn, "customer")).isEqualTo(5);
                assertThat(countRows(conn, "purchase")).isEqualTo(10);
                assertThat(countRows(conn, "case_session")).isZero();
            }
        }
    }

    @Test
    void rerunningInitScriptsDoesNotDuplicateSeedsOrErrorOut(@TempDir Path tempDir) throws Exception {
        try (HikariDataSource dataSource = TestDatabases.fresh(tempDir.resolve("rerun.db"))) {
            // Simulate a second application start against the same file (AC-30 semantics).
            TestDatabases.initSchema(dataSource);
            TestDatabases.initSchema(dataSource);

            try (Connection conn = dataSource.getConnection()) {
                assertThat(countRows(conn, "customer")).isEqualTo(5);
                assertThat(countRows(conn, "purchase")).isEqualTo(10);
            }
        }
    }

    private static String readPragma(Connection conn, String pragma) throws Exception {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("PRAGMA " + pragma)) {
            assertThat(rs.next()).isTrue();
            return rs.getString(1);
        }
    }

    private static List<String> tableNames(Connection conn) throws Exception {
        List<String> names = new ArrayList<>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private static int countRows(Connection conn, String table) throws Exception {
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
