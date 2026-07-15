package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/** ADR-004 §8 "Decision ordering" scenario, against a real temp-file SQLite DB. */
class DecisionRepositoryTest {

    private HikariDataSource dataSource;
    private DecisionRepository repository;
    private String caseId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("decision.db"));
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        repository = new DecisionRepository(jdbcClient);
        caseId = UUID.randomUUID().toString();
        new CaseSessionRepository(jdbcClient).insert(new CaseSession(
                caseId, RequestType.COMPLAINT, "Toaster", "T1", "2026-06-01", "ORD-9999",
                "Broken", null, null, Instant.parse("2026-07-15T08:00:00Z")));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void findLatestByCaseIdReturnsTheNewestByCreatedAt() {
        repository.insert(decision(caseId, DecisionCategory.NEEDS_MORE_INFO, Instant.parse("2026-07-15T08:00:00Z")));
        repository.insert(decision(caseId, DecisionCategory.REJECT, Instant.parse("2026-07-15T09:00:00Z")));
        repository.insert(decision(caseId, DecisionCategory.APPROVE, Instant.parse("2026-07-15T10:00:00Z")));

        Decision latest = repository.findLatestByCaseId(caseId).orElseThrow();

        assertThat(latest.category()).isEqualTo(DecisionCategory.APPROVE);
        assertThat(latest.createdAt()).isEqualTo(Instant.parse("2026-07-15T10:00:00Z"));
    }

    @Test
    void equalTimestampsBreakTieDeterministicallyById() {
        Instant same = Instant.parse("2026-07-15T10:00:00Z");
        Decision first = decision(caseId, DecisionCategory.REJECT, same);
        Decision second = decision(caseId, DecisionCategory.APPROVE, same);
        repository.insert(first);
        repository.insert(second);

        String expectedWinnerId = first.id().compareTo(second.id()) > 0 ? first.id() : second.id();

        // Calling twice must return the same row both times (deterministic, not flaky).
        assertThat(repository.findLatestByCaseId(caseId).orElseThrow().id()).isEqualTo(expectedWinnerId);
        assertThat(repository.findLatestByCaseId(caseId).orElseThrow().id()).isEqualTo(expectedWinnerId);
    }

    @Test
    void findLatestByCaseIdReturnsEmptyWhenNoDecisionsExist() {
        assertThat(repository.findLatestByCaseId(caseId)).isEmpty();
    }

    private static Decision decision(String caseId, DecisionCategory category, Instant createdAt) {
        return new Decision(
                UUID.randomUUID().toString(), caseId, category,
                "justification for " + category, "full message for " + category, createdAt);
    }
}
