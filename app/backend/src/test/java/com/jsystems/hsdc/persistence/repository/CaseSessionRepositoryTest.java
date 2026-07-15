package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.CaseSession;
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

/** ADR-004 §8 "Case round-trip" scenario, against a real temp-file SQLite DB. */
class CaseSessionRepositoryTest {

    private HikariDataSource dataSource;
    private CaseSessionRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("case-session.db"));
        repository = new CaseSessionRepository(JdbcClient.create(dataSource));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void insertAndFindByIdRoundTripsAllColumns() {
        String id = UUID.randomUUID().toString();
        Instant createdAt = Instant.parse("2026-07-15T10:00:00Z");
        CaseSession session = new CaseSession(
                id,
                RequestType.COMPLAINT,
                "Vacuum Cleaner",
                "X200",
                "2026-07-10",
                "ORD-1001",
                "Motor stopped working after two days",
                null,
                null,
                createdAt);

        repository.insert(session);

        CaseSession found = repository.findById(id).orElseThrow();
        assertThat(found.id()).isEqualTo(id);
        assertThat(found.requestType()).isEqualTo(RequestType.COMPLAINT);
        assertThat(found.equipmentCategory()).isEqualTo("Vacuum Cleaner");
        assertThat(found.equipmentModel()).isEqualTo("X200");
        assertThat(found.purchaseDate()).isEqualTo("2026-07-10");
        assertThat(found.orderNumber()).isEqualTo("ORD-1001");
        assertThat(found.reason()).isEqualTo("Motor stopped working after two days");
        assertThat(found.imageAnalysis()).isNull();
        assertThat(found.orderVerified()).isNull();
        assertThat(found.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void nullOrderNumberAndReasonAreStoredAndReadBackAsNull() {
        String id = UUID.randomUUID().toString();
        CaseSession session = new CaseSession(
                id,
                RequestType.RETURN,
                "Blender",
                "Pro",
                "2026-07-01",
                null,
                null,
                null,
                null,
                Instant.parse("2026-07-15T09:30:00Z"));

        repository.insert(session);

        CaseSession found = repository.findById(id).orElseThrow();
        assertThat(found.orderNumber()).isNull();
        assertThat(found.reason()).isNull();
    }

    @Test
    void orderVerifiedIsTriState() {
        String trueId = UUID.randomUUID().toString();
        String falseId = UUID.randomUUID().toString();
        String unknownId = UUID.randomUUID().toString();
        Instant now = Instant.parse("2026-07-15T09:00:00Z");

        repository.insert(withOrderVerified(baseSession(trueId, now), true));
        repository.insert(withOrderVerified(baseSession(falseId, now), false));
        repository.insert(withOrderVerified(baseSession(unknownId, now), null));

        assertThat(repository.findById(trueId).orElseThrow().orderVerified()).isTrue();
        assertThat(repository.findById(falseId).orElseThrow().orderVerified()).isFalse();
        assertThat(repository.findById(unknownId).orElseThrow().orderVerified()).isNull();
    }

    @Test
    void updateAnalysisSetsImageAnalysisAndOrderVerified() {
        String id = UUID.randomUUID().toString();
        repository.insert(baseSession(id, Instant.parse("2026-07-15T09:00:00Z")));

        repository.updateAnalysis(id, "Visible crack on the housing, consistent with impact damage.", true);

        CaseSession found = repository.findById(id).orElseThrow();
        assertThat(found.imageAnalysis()).isEqualTo("Visible crack on the housing, consistent with impact damage.");
        assertThat(found.orderVerified()).isTrue();
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID().toString())).isEmpty();
    }

    private static CaseSession baseSession(String id, Instant createdAt) {
        return new CaseSession(
                id, RequestType.COMPLAINT, "Toaster", "T1", "2026-06-01", "ORD-9999", "Broken", null, null, createdAt);
    }

    /** Small local helper since {@link CaseSession} is an immutable record. */
    private static CaseSession withOrderVerified(CaseSession base, Boolean orderVerified) {
        return new CaseSession(
                base.id(), base.requestType(), base.equipmentCategory(), base.equipmentModel(),
                base.purchaseDate(), base.orderNumber(), base.reason(), base.imageAnalysis(),
                orderVerified, base.createdAt());
    }
}
