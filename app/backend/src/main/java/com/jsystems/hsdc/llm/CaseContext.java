package com.jsystems.hsdc.llm;

import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.domain.RequestType;

/**
 * Everything {@link LlmService#decide} and {@link LlmService#streamChat}
 * need about one case: form fields, the image-analysis result, and
 * (optionally) verified purchase history (ADR-002 §4, PRD AC-14/AC-15).
 * Assembled once by {@code CaseService}, reused by {@code ChatService}.
 *
 * <p>{@code orderVerified} is tri-state, mirroring {@code CaseSession}:
 * {@code null} = no order number was provided / not checked,
 * {@code true}/{@code false} = the result of the purchase lookup. When
 * {@code false}, the decision prompt's case summary adds an explicit
 * "order could not be verified" note (AC-15).
 *
 * @param requestType       COMPLAINT or RETURN
 * @param equipmentCategory customer-entered equipment category
 * @param equipmentModel    customer-entered equipment model
 * @param purchaseDate      customer-entered purchase date (ISO-8601 date text)
 * @param orderNumber       nullable, customer-entered order number
 * @param reason            nullable, customer-entered reason
 * @param imageAnalysis     raw analysis text from {@link LlmService#analyzeImage}
 * @param orderVerified     tri-state: null = not provided/checked, true/false = lookup result
 * @param purchaseHistory   verified purchase record, present only when {@code orderVerified} is true
 */
public record CaseContext(
        RequestType requestType,
        String equipmentCategory,
        String equipmentModel,
        String purchaseDate,
        String orderNumber,
        String reason,
        String imageAnalysis,
        Boolean orderVerified,
        PurchaseInfo purchaseHistory) {
}
