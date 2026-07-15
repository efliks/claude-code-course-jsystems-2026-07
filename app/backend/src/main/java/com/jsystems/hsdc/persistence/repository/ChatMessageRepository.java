package com.jsystems.hsdc.persistence.repository;

import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.Sender;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code chat_message} rows (ADR-004 §3, §5). All SQL is
 * hand-written {@link JdbcClient} with bound named parameters (TAC-004-05).
 */
@Repository
public class ChatMessageRepository {

    private final JdbcClient jdbcClient;

    public ChatMessageRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Inserts a new chat message row (customer or agent turn). */
    public void insert(ChatMessage message) {
        jdbcClient.sql("""
                        INSERT INTO chat_message (id, case_id, sender, content, created_at)
                        VALUES (:id, :caseId, :sender, :content, :createdAt)
                        """)
                .param("id", message.id())
                .param("caseId", message.caseId())
                .param("sender", message.sender().name())
                .param("content", message.content())
                .param("createdAt", message.createdAt().toString())
                .update();
    }

    /** Lists all messages for a case, oldest first. */
    public List<ChatMessage> listByCaseId(String caseId) {
        return jdbcClient.sql("""
                        SELECT * FROM chat_message
                        WHERE case_id = :caseId
                        ORDER BY created_at ASC, id ASC
                        """)
                .param("caseId", caseId)
                .query(ChatMessageRepository::mapRow)
                .list();
    }

    private static ChatMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ChatMessage(
                rs.getString("id"),
                rs.getString("case_id"),
                Sender.valueOf(rs.getString("sender")),
                rs.getString("content"),
                Instant.parse(rs.getString("created_at")));
    }
}
