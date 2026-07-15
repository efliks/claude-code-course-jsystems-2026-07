package com.jsystems.hsdc.persistence.repository;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.RequestType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@code case_session} rows (ADR-004 §3, §5). All SQL is
 * hand-written {@link JdbcClient} with bound named parameters — no user
 * input is ever concatenated into a statement (TAC-004-05).
 */
@Repository
public class CaseSessionRepository {

    private final JdbcClient jdbcClient;

    public CaseSessionRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /** Inserts a new case session row. */
    public void insert(CaseSession session) {
        jdbcClient.sql("""
                        INSERT INTO case_session
                            (id, request_type, equipment_category, equipment_model, purchase_date,
                             order_number, reason, image_analysis, order_verified, created_at)
                        VALUES
                            (:id, :requestType, :equipmentCategory, :equipmentModel, :purchaseDate,
                             :orderNumber, :reason, :imageAnalysis, :orderVerified, :createdAt)
                        """)
                .param("id", session.id())
                .param("requestType", session.requestType().name())
                .param("equipmentCategory", session.equipmentCategory())
                .param("equipmentModel", session.equipmentModel())
                .param("purchaseDate", session.purchaseDate())
                .param("orderNumber", session.orderNumber())
                .param("reason", session.reason())
                .param("imageAnalysis", session.imageAnalysis())
                .param("orderVerified", toDbBoolean(session.orderVerified()))
                .param("createdAt", session.createdAt().toString())
                .update();
    }

    /**
     * Updates the image-analysis result and order-verified flag produced
     * after the case's image-analysis step completes.
     */
    public void updateAnalysis(String caseId, String imageAnalysis, Boolean orderVerified) {
        jdbcClient.sql("""
                        UPDATE case_session
                        SET image_analysis = :imageAnalysis, order_verified = :orderVerified
                        WHERE id = :id
                        """)
                .param("imageAnalysis", imageAnalysis)
                .param("orderVerified", toDbBoolean(orderVerified))
                .param("id", caseId)
                .update();
    }

    /** Finds a case session by id. */
    public Optional<CaseSession> findById(String id) {
        return jdbcClient.sql("SELECT * FROM case_session WHERE id = :id")
                .param("id", id)
                .query(CaseSessionRepository::mapRow)
                .optional();
    }

    private static Integer toDbBoolean(Boolean value) {
        return value == null ? null : (value ? 1 : 0);
    }

    private static CaseSession mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer orderVerifiedRaw = (Integer) rs.getObject("order_verified");
        return new CaseSession(
                rs.getString("id"),
                RequestType.valueOf(rs.getString("request_type")),
                rs.getString("equipment_category"),
                rs.getString("equipment_model"),
                rs.getString("purchase_date"),
                rs.getString("order_number"),
                rs.getString("reason"),
                rs.getString("image_analysis"),
                orderVerifiedRaw == null ? null : orderVerifiedRaw != 0,
                Instant.parse(rs.getString("created_at")));
    }
}
