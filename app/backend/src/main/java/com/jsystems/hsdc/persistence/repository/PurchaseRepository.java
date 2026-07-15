package com.jsystems.hsdc.persistence.repository;

import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Read-only repository over the seeded {@code purchase}/{@code customer}
 * tables (ADR-004 §3, §5). Order-number lookups are case-sensitive
 * (SQLite's default BINARY collation on TEXT {@code =}); this is an
 * intentional decision, not an oversight — seeded order numbers are
 * canonical uppercase and callers are expected to pass them as entered.
 */
@Repository
public class PurchaseRepository {

    private final JdbcClient jdbcClient;

    public PurchaseRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    /**
     * Finds a purchase (with its owning customer) by order number.
     *
     * @param orderNumber order number to look up; {@code null} always
     *                    yields an empty result rather than issuing a query
     * @return the matching {@link PurchaseInfo}, or empty if not found
     */
    public Optional<PurchaseInfo> findByOrderNumber(String orderNumber) {
        if (orderNumber == null) {
            return Optional.empty();
        }
        return jdbcClient.sql("""
                        SELECT c.name AS customer_name, p.order_number, p.product_name,
                               p.category, p.purchase_date, p.price_cents
                        FROM purchase p
                        JOIN customer c ON c.id = p.customer_id
                        WHERE p.order_number = :orderNumber
                        """)
                .param("orderNumber", orderNumber)
                .query(PurchaseRepository::mapRow)
                .optional();
    }

    private static PurchaseInfo mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new PurchaseInfo(
                rs.getString("customer_name"),
                rs.getString("order_number"),
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getString("purchase_date"),
                rs.getLong("price_cents"));
    }
}
