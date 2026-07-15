package com.jsystems.hsdc.persistence.domain;

/**
 * Author of a {@link ChatMessage}.
 *
 * <p>Mirrors the {@code chat_message.sender} CHECK constraint (ADR-004 §4).
 */
public enum Sender {
    CUSTOMER,
    AGENT
}
