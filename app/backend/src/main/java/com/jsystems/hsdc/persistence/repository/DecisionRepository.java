package com.jsystems.hsdc.persistence.repository;

import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code decision} rows (ADR-004 §3, §5). All SQL is
 * hand-written {@link JdbcClient} with bound named parameters (TAC-004-05).
 */
@Repository
public class DecisionRepository {

    private final JdbcClient jdbcClient;

    public DecisionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Inserts a new decision row (initial or revised). */
    public void insert(Decision decision) {
        jdbcClient.sql("""
                        INSERT INTO decision (id, case_id, category, justification, full_message, created_at)
                        VALUES (:id, :caseId, :category, :justification, :fullMessage, :createdAt)
                        """)
                .param("id", decision.id())
                .param("caseId", decision.caseId())
                .param("category", decision.category().name())
                .param("justification", decision.justification())
                .param("fullMessage", decision.fullMessage())
                .param("createdAt", decision.createdAt().toString())
                .update();
    }

    /**
     * Returns the most recent decision for a case. Ties on {@code created_at}
     * are broken deterministically by {@code id} descending (documented
     * tiebreak, not a claim about which decision is "correct" — plain
     * timestamp equality should not occur in practice).
     */
    public Optional<Decision> findLatestByCaseId(String caseId) {
        return jdbcClient.sql("""
                        SELECT * FROM decision
                        WHERE case_id = :caseId
                        ORDER BY created_at DESC, id DESC
                        LIMIT 1
                        """)
                .param("caseId", caseId)
                .query(DecisionRepository::mapRow)
                .optional();
    }

    private static Decision mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Decision(
                rs.getString("id"),
                rs.getString("case_id"),
                DecisionCategory.valueOf(rs.getString("category")),
                rs.getString("justification"),
                rs.getString("full_message"),
                Instant.parse(rs.getString("created_at")));
    }
}
