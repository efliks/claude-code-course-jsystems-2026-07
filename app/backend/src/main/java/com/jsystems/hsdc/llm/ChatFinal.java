package com.jsystems.hsdc.llm;

import com.jsystems.hsdc.persistence.domain.DecisionCategory;

/**
 * Final result of {@link LlmService#streamChat} once the stream ends
 * (ADR-002 §4). {@code fullText} is the visible reply with the trailing
 * machine-readable revision marker stripped; {@code revisedCategory} is
 * {@code null} when the marker was absent or malformed (no revision).
 *
 * @param fullText        assembled reply text, marker line removed
 * @param revisedCategory new decision category if this reply revised it, else {@code null}
 */
public record ChatFinal(String fullText, DecisionCategory revisedCategory) {
}
