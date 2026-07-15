package com.jsystems.hsdc.llm;

import com.jsystems.hsdc.persistence.domain.DecisionCategory;

/**
 * Result of {@link LlmService#decide}: the structured decision (ADR-002 §4).
 *
 * <p>{@code category} is typed as {@link DecisionCategory} (not a raw
 * string), so a case can never carry a category outside the three-value
 * enum at the type level (TAC-002-05, PRD AC-12); {@link LlmService} throws
 * {@link LlmCallException} rather than construct this record with a guessed
 * or unparseable category.
 *
 * @param category            APPROVE, REJECT, or NEEDS_MORE_INFO
 * @param message             full markdown decision message (greeting, decision, justification, next steps, disclaimer)
 * @param justificationExcerpt short excerpt of the justification, for the decision DB row
 */
public record DecisionResult(DecisionCategory category, String message, String justificationExcerpt) {
}
