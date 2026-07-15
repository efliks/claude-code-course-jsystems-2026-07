package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * TAC-004-03: {@code data.sql} seed purchases must cover the return-window
 * (14 days) and warranty (24 months) boundaries relative to the fixed
 * reference "today" documented in {@code data.sql}: 2026-07-15.
 */
class SeedDataBoundaryTest {

    /** Must match the reference date documented in data.sql. */
    private static final LocalDate REFERENCE_TODAY = LocalDate.parse("2026-07-15");

    private HikariDataSource dataSource;
    private PurchaseRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("seed-boundary.db"));
        repository = new PurchaseRepository(JdbcClient.create(dataSource));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void seedsCoverWithinAndOutsideTheFourteenDayReturnWindow() {
        assertThat(daysSincePurchase("ORD-1001")).isLessThanOrEqualTo(14); // within
        assertThat(daysSincePurchase("ORD-1009")).isLessThanOrEqualTo(14); // within
        assertThat(daysSincePurchase("ORD-1004")).isEqualTo(14);           // exactly at the boundary
        assertThat(daysSincePurchase("ORD-1002")).isGreaterThan(14);       // outside
        assertThat(daysSincePurchase("ORD-1006")).isGreaterThan(14);       // just outside
    }

    @Test
    void seedsCoverWithinAndOutsideTheTwentyFourMonthWarranty() {
        // "Within warranty" = reference today has not yet passed purchaseDate + 24 months
        // (inclusive of the expiry day itself).
        assertThat(isWithinWarranty("ORD-1001")).isTrue();
        assertThat(isWithinWarranty("ORD-1002")).isTrue();
        assertThat(isWithinWarranty("ORD-1005")).isTrue();  // exactly at the boundary (expiry == today)
        assertThat(isWithinWarranty("ORD-1003")).isFalse();
        assertThat(isWithinWarranty("ORD-1007")).isFalse(); // just outside (expiry == today - 1 day)
    }

    private long daysSincePurchase(String orderNumber) {
        return ChronoUnit.DAYS.between(purchaseDate(orderNumber), REFERENCE_TODAY);
    }

    private boolean isWithinWarranty(String orderNumber) {
        LocalDate warrantyExpiry = purchaseDate(orderNumber).plusMonths(24);
        return !REFERENCE_TODAY.isAfter(warrantyExpiry);
    }

    private LocalDate purchaseDate(String orderNumber) {
        PurchaseInfo info = repository.findByOrderNumber(orderNumber).orElseThrow();
        return LocalDate.parse(info.purchaseDate());
    }
}
