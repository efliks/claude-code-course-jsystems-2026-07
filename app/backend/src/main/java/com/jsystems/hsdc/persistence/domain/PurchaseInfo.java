package com.jsystems.hsdc.persistence.domain;

/**
 * Read-only projection joining a seeded {@code purchase} row with its
 * owning {@code customer} — exactly what the decision prompt's history
 * block needs (ADR-004 §5, ADR-002).
 *
 * @param customerName the owning customer's name
 * @param orderNumber  the (unique) order number looked up
 * @param productName  purchased product name
 * @param category     product category
 * @param purchaseDate purchase date (ISO-8601 date text)
 * @param priceCents   price in integer cents
 */
public record PurchaseInfo(
        String customerName,
        String orderNumber,
        String productName,
        String category,
        String purchaseDate,
        long priceCents) {
}
