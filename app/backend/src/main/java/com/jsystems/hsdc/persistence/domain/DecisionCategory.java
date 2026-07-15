package com.jsystems.hsdc.persistence.domain;

/**
 * Outcome category of a {@link Decision}.
 *
 * <p>Mirrors the {@code decision.category} CHECK constraint (ADR-004 §4).
 */
public enum DecisionCategory {
    APPROVE,
    REJECT,
    NEEDS_MORE_INFO
}
