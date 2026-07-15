package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

/** ADR-004 §8 "FK enforcement" scenario: pragma verified ON, violations rejected. */
class ForeignKeyEnforcementTest {

    private HikariDataSource dataSource;
    private DecisionRepository decisionRepository;
    private ChatMessageRepository chatMessageRepository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("fk.db"));
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        decisionRepository = new DecisionRepository(jdbcClient);
        chatMessageRepository = new ChatMessageRepository(jdbcClient);
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void insertingDecisionWithBogusCaseIdIsRejected() {
        Decision decision = new Decision(
                UUID.randomUUID().toString(), "no-such-case", DecisionCategory.APPROVE,
                "justification", "full message", Instant.parse("2026-07-15T10:00:00Z"));

        assertThatThrownBy(() -> decisionRepository.insert(decision))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void insertingChatMessageWithBogusCaseIdIsRejected() {
        ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(), "no-such-case", Sender.CUSTOMER, "hello",
                Instant.parse("2026-07-15T10:00:00Z"));

        assertThatThrownBy(() -> chatMessageRepository.insert(message))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void foreignKeysPragmaIsOn() throws Exception {
        try (var conn = dataSource.getConnection();
                var st = conn.createStatement();
                var rs = st.executeQuery("PRAGMA foreign_keys")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
