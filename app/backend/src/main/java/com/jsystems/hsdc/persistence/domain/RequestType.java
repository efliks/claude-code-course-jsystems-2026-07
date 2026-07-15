package com.jsystems.hsdc.persistence.domain;

/**
 * Type of a customer request captured on a {@link CaseSession}.
 *
 * <p>Mirrors the {@code case_session.request_type} CHECK constraint
 * (ADR-004 §4).
 */
public enum RequestType {
    COMPLAINT,
    RETURN
}
