package com.jsystems.hsdc.persistence.domain;

import java.time.Instant;

/**
 * One submitted complaint/return form (ADR-004 §4, ADR-000 §5).
 *
 * <p>{@code orderNumber} is a plain recorded value, intentionally not a
 * foreign key to {@code purchase} — unverified orders must be storable
 * (AC-15). {@code orderVerified} is tri-state: {@code null} means "not
 * checked yet / order number not provided", {@code true}/{@code false} is
 * the result of the purchase lookup.
 *
 * @param id                app-generated UUID string primary key
 * @param requestType       COMPLAINT or RETURN
 * @param equipmentCategory customer-entered equipment category
 * @param equipmentModel    customer-entered equipment model
 * @param purchaseDate      customer-entered purchase date (ISO-8601 date text)
 * @param orderNumber       nullable, not FK-enforced
 * @param reason            nullable (typically absent for RETURN)
 * @param imageAnalysis     nullable until the image-analysis step completes
 * @param orderVerified     tri-state: null = not provided/checked, true/false = lookup result
 * @param createdAt         creation timestamp, UTC
 */
public record CaseSession(
        String id,
        RequestType requestType,
        String equipmentCategory,
        String equipmentModel,
        String purchaseDate,
        String orderNumber,
        String reason,
        String imageAnalysis,
        Boolean orderVerified,
        Instant createdAt) {
}
