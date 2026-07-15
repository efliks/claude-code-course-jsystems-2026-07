package com.jsystems.hsdc.policy;

/**
 * Thrown when a policy markdown document configured via
 * {@code HSDC_POLICIES_DIR} is missing or unreadable at startup (ADR-001 §3
 * "PolicyService", TAC-001-04: "no half-working state"). Thrown from
 * {@link PolicyService#loadPolicies()}, an {@code @PostConstruct} method, so
 * Spring wraps it into a {@code BeanCreationException} that aborts
 * application startup. The message is English, human-readable, and names
 * the exact path that is missing/unreadable.
 */
public class PolicyLoadException extends RuntimeException {

    public PolicyLoadException(String message) {
        super(message);
    }

    public PolicyLoadException(String message, Throwable cause) {
        super(message, cause);
    }
}
