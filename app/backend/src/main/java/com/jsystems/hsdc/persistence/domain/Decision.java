package com.jsystems.hsdc.persistence.domain;

import java.time.Instant;

/**
 * One decision issued by the agent for a case, initial or revised
 * (ADR-004 §4, ADR-000 §5). Many rows may exist per case; the row with the
 * latest {@code createdAt} (id as tiebreak) is the current valid decision.
 *
 * @param id            app-generated UUID string primary key
 * @param caseId        FK to {@code case_session.id}
 * @param category      APPROVE, REJECT or NEEDS_MORE_INFO
 * @param justification short justification text
 * @param fullMessage   the full agent message text shown to the customer
 * @param createdAt     creation timestamp, UTC
 */
public record Decision(
        String id,
        String caseId,
        DecisionCategory category,
        String justification,
        String fullMessage,
        Instant createdAt) {
}
