package com.jsystems.hsdc.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.persistence.repository.CaseSessionRepository;
import com.jsystems.hsdc.persistence.repository.ChatMessageRepository;
import com.jsystems.hsdc.persistence.repository.DecisionRepository;
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
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

/**
 * ADR-004 §8 "SessionRecorder tolerance" — the atomic-write edge case
 * ("partial tx rollback: no orphan rows after forced mid-tx failure"),
 * exercised against a real temp-file SQLite DB with real repositories
 * (only the last write in the chain is forced to fail via a Mockito spy),
 * plus the plain success path.
 */
class SessionRecorderIntegrationTest {

    private HikariDataSource dataSource;
    private JdbcClient jdbcClient;
    private CaseSessionRepository caseSessionRepository;
    private DecisionRepository decisionRepository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("session-recorder.db"));
        jdbcClient = JdbcClient.create(dataSource);
        caseSessionRepository = new CaseSessionRepository(jdbcClient);
        decisionRepository = new DecisionRepository(jdbcClient);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void successfulCaseCreationPersistsAllThreeRowsInOneGo() {
        ChatMessageRepository chatMessageRepository = new ChatMessageRepository(jdbcClient);
        SessionRecorder recorder = new SessionRecorder(
                caseSessionRepository, decisionRepository, chatMessageRepository,
                new DataSourceTransactionManager(dataSource));
        String caseId = UUID.randomUUID().toString();

        recorder.recordCaseCreated(caseSession(caseId), decision(caseId), message(caseId));

        assertThat(caseSessionRepository.findById(caseId)).isPresent();
        assertThat(decisionRepository.findLatestByCaseId(caseId)).isPresent();
        assertThat(chatMessageRepository.listByCaseId(caseId)).hasSize(1);
    }

    @Test
    void failureMidTransactionRollsBackEarlierWritesLeavingNoOrphanRows() {
        ChatMessageRepository realChatMessageRepository = new ChatMessageRepository(jdbcClient);
        ChatMessageRepository failingChatMessageRepository = spy(realChatMessageRepository);
        doThrow(new RuntimeException("forced failure on the last write"))
                .when(failingChatMessageRepository).insert(any());

        SessionRecorder recorder = new SessionRecorder(
                caseSessionRepository, decisionRepository, failingChatMessageRepository,
                new DataSourceTransactionManager(dataSource));
        String caseId = UUID.randomUUID().toString();

        recorder.recordCaseCreated(caseSession(caseId), decision(caseId), message(caseId));

        // The case_session insert and the decision insert ran successfully before the
        // forced failure, but must have been rolled back along with it (AC-29 + atomicity).
        assertThat(caseSessionRepository.findById(caseId)).isEmpty();
        assertThat(decisionRepository.findLatestByCaseId(caseId)).isEmpty();
        assertThat(realChatMessageRepository.listByCaseId(caseId)).isEmpty();
    }

    private static CaseSession caseSession(String id) {
        return new CaseSession(
                id, RequestType.COMPLAINT, "Toaster", "T1", "2026-06-01", "ORD-9999",
                "Broken", null, null, Instant.parse("2026-07-15T08:00:00Z"));
    }

    private static Decision decision(String caseId) {
        return new Decision(
                UUID.randomUUID().toString(), caseId, DecisionCategory.NEEDS_MORE_INFO,
                "justification", "full message", Instant.parse("2026-07-15T08:00:00Z"));
    }

    private static ChatMessage message(String caseId) {
        return new ChatMessage(
                UUID.randomUUID().toString(), caseId, Sender.AGENT, "content",
                Instant.parse("2026-07-15T08:00:00Z"));
    }
}
