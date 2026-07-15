package com.jsystems.hsdc.persistence.domain;

import java.time.Instant;

/**
 * One chat turn belonging to a case (ADR-004 §4, ADR-000 §5). The agent's
 * first (decision) message is stored both as a {@link ChatMessage} and as
 * a {@link Decision}.
 *
 * @param id        app-generated UUID string primary key
 * @param caseId    FK to {@code case_session.id}
 * @param sender    CUSTOMER or AGENT
 * @param content   message text
 * @param createdAt creation timestamp, UTC
 */
public record ChatMessage(
        String id,
        String caseId,
        Sender sender,
        String content,
        Instant createdAt) {
}
