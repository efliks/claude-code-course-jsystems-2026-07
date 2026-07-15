package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * ADR-004 §8 "Concurrent writes" scenario: two threads inserting chat
 * messages in parallel against the pool-of-1 datasource must both succeed
 * with no {@code SQLITE_BUSY}, proving Hikari serializes access.
 */
class ConcurrentWritesTest {

    private HikariDataSource dataSource;
    private ChatMessageRepository repository;
    private String caseId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("concurrent.db"));
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        repository = new ChatMessageRepository(jdbcClient);
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
    void twoThreadsInsertingChatMessagesBothSucceedWithNoBusyError() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> repository.insert(
                    new ChatMessage(UUID.randomUUID().toString(), caseId, Sender.CUSTOMER,
                            "message from thread 1", Instant.parse("2026-07-15T08:00:00Z"))));
            Future<?> second = executor.submit(() -> repository.insert(
                    new ChatMessage(UUID.randomUUID().toString(), caseId, Sender.AGENT,
                            "message from thread 2", Instant.parse("2026-07-15T08:00:01Z"))));

            // .get() rethrows any exception the task raised (e.g. SQLITE_BUSY) as the test failure.
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
        }

        List<ChatMessage> messages = repository.listByCaseId(caseId);
        assertThat(messages).hasSize(2);
    }
}
