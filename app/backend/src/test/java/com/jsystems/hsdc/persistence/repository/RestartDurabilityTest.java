package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * ADR-004 §8 "Restart durability" scenario (AC-30): rows written before a
 * clean shutdown must still be present after reopening the same file, with
 * a proper WAL checkpoint on close.
 */
class RestartDurabilityTest {

    @Test
    void rowsWrittenBeforeCloseArePresentAfterReopeningTheSameFile(@TempDir Path tempDir) throws Exception {
        Path dbFile = tempDir.resolve("restart.db");
        String caseId = UUID.randomUUID().toString();

        try (HikariDataSource firstRun = TestDatabases.fresh(dbFile)) {
            new CaseSessionRepository(JdbcClient.create(firstRun)).insert(new CaseSession(
                    caseId, RequestType.COMPLAINT, "Toaster", "T1", "2026-06-01", "ORD-9999",
                    "Broken", null, null, Instant.parse("2026-07-15T08:00:00Z")));
        }
        // firstRun is closed here: Hikari shutdown closes the sole pooled connection,
        // which triggers SQLite's WAL checkpoint back into the main db file.

        try (HikariDataSource secondRun = TestDatabases.open(dbFile)) {
            CaseSession found = new CaseSessionRepository(JdbcClient.create(secondRun))
                    .findById(caseId)
                    .orElseThrow();
            assertThat(found.equipmentModel()).isEqualTo("T1");
        }
    }
}
