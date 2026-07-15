package com.jsystems.hsdc.llm;

/**
 * Thrown by {@link LlmService} whenever an LLM call cannot be completed
 * successfully: transport failures (timeout, connection error), non-2xx
 * responses from OpenRouter, or a decision reply that still violates the
 * structured-output schema after one retry (ADR-002 §5 "Interface
 * Contracts", §6 "Timeouts, retries, and failure semantics").
 *
 * <p>The system must never fabricate a decision category (PRD §11) — this
 * exception is the only failure signal callers see; there is no fallback
 * value.
 */
public class LlmCallException extends RuntimeException {

    public LlmCallException(String message) {
        super(message);
    }

    public LlmCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
