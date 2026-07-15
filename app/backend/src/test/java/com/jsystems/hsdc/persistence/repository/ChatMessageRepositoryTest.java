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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/** ADR-004 §8 "Message round-trip" scenario, against a real temp-file SQLite DB. */
class ChatMessageRepositoryTest {

    private HikariDataSource dataSource;
    private ChatMessageRepository repository;
    private String caseId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("chat-message.db"));
        JdbcClient jdbcClient = JdbcClient.create(dataSource);
        repository = new ChatMessageRepository(jdbcClient);
        caseId = UUID.randomUUID().toString();
        new CaseSessionRepository(jdbcClient).insert(new CaseSession(
                caseId, RequestType.RETURN, "Blender", "Pro", "2026-07-01", "ORD-1004",
                null, null, null, Instant.parse("2026-07-15T08:00:00Z")));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void listByCaseIdReturnsMessagesOrderedByCreatedAtWithCorrectSenders() {
        repository.insert(message(caseId, Sender.AGENT, "How can I help you today?",
                Instant.parse("2026-07-15T08:00:00Z")));
        repository.insert(message(caseId, Sender.CUSTOMER, "My blender stopped working.",
                Instant.parse("2026-07-15T08:01:00Z")));
        repository.insert(message(caseId, Sender.AGENT, "I'm sorry to hear that.",
                Instant.parse("2026-07-15T08:02:00Z")));

        List<ChatMessage> messages = repository.listByCaseId(caseId);

        assertThat(messages).hasSize(3);
        assertThat(messages).extracting(ChatMessage::sender)
                .containsExactly(Sender.AGENT, Sender.CUSTOMER, Sender.AGENT);
        assertThat(messages).extracting(ChatMessage::content)
                .containsExactly("How can I help you today?", "My blender stopped working.", "I'm sorry to hear that.");
    }

    @Test
    void longContentRoundTrips() {
        String longContent = "x".repeat(10_000);
        repository.insert(message(caseId, Sender.CUSTOMER, longContent, Instant.parse("2026-07-15T08:00:00Z")));

        List<ChatMessage> messages = repository.listByCaseId(caseId);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).content()).hasSize(10_000).isEqualTo(longContent);
    }

    @Test
    void listByCaseIdReturnsEmptyListWhenNoMessagesExist() {
        assertThat(repository.listByCaseId(caseId)).isEmpty();
    }

    private static ChatMessage message(String caseId, Sender sender, String content, Instant createdAt) {
        return new ChatMessage(UUID.randomUUID().toString(), caseId, sender, content, createdAt);
    }
}
