package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.support.TestDatabases;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * ADR-004 §8 "Purchase lookup" scenario, against the real seeded temp-file
 * SQLite DB (seed rows documented in {@code data.sql}).
 */
class PurchaseRepositoryTest {

    private HikariDataSource dataSource;
    private PurchaseRepository repository;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        dataSource = TestDatabases.fresh(tempDir.resolve("purchase.db"));
        repository = new PurchaseRepository(JdbcClient.create(dataSource));
    }

    @AfterEach
    void tearDown() {
        dataSource.close();
    }

    @Test
    void findsSeededOrderWithJoinedCustomer() {
        PurchaseInfo info = repository.findByOrderNumber("ORD-1001").orElseThrow();

        assertThat(info.customerName()).isEqualTo("John Kowalski");
        assertThat(info.orderNumber()).isEqualTo("ORD-1001");
        assertThat(info.productName()).isEqualTo("Robot Vacuum X200");
        assertThat(info.category()).isEqualTo("Home Appliances");
        assertThat(info.purchaseDate()).isEqualTo("2026-07-10");
        assertThat(info.priceCents()).isEqualTo(129900);
    }

    @Test
    void returnsEmptyForUnknownOrderNumber() {
        assertThat(repository.findByOrderNumber("ORD-DOES-NOT-EXIST")).isEmpty();
    }

    @Test
    void returnsEmptyForNullOrderNumber() {
        assertThat(repository.findByOrderNumber(null)).isEmpty();
    }

    @Test
    void orderNumberLookupIsCaseSensitive() {
        assertThat(repository.findByOrderNumber("ord-1001")).isEmpty();
        assertThat(repository.findByOrderNumber("ORD-1001")).isPresent();
    }
}
