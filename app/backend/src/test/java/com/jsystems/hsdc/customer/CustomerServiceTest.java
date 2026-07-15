package com.jsystems.hsdc.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.repository.PurchaseRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * ADR-004 §5 (findPurchaseByOrderNumber row) + PRD AC-14/AC-15: a customer
 * lookup either returns a verified {@link PurchaseInfo} or is treated as a
 * miss (unknown order, blank input, or a repository failure) without ever
 * breaking the case-submission flow.
 */
class CustomerServiceTest {

    private PurchaseRepository purchaseRepository;
    private CustomerService customerService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        purchaseRepository = mock(PurchaseRepository.class);
        customerService = new CustomerService(purchaseRepository);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(CustomerService.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(CustomerService.class)).detachAppender(logAppender);
    }

    @Test
    void findByOrderNumberReturnsPurchaseInfoForKnownOrder() {
        PurchaseInfo info = new PurchaseInfo("Jane Doe", "ORD-1001", "Toaster", "KITCHEN", "2026-06-01", 4999L);
        when(purchaseRepository.findByOrderNumber("ORD-1001")).thenReturn(Optional.of(info));

        Optional<PurchaseInfo> result = customerService.findByOrderNumber("ORD-1001");

        assertThat(result).contains(info);
    }

    @Test
    void findByOrderNumberReturnsEmptyForUnknownOrder() {
        when(purchaseRepository.findByOrderNumber("ORD-UNKNOWN")).thenReturn(Optional.empty());

        Optional<PurchaseInfo> result = customerService.findByOrderNumber("ORD-UNKNOWN");

        assertThat(result).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void findByOrderNumberReturnsEmptyWithoutDbCallForBlankOrNullInput(String orderNumber) {
        Optional<PurchaseInfo> result = customerService.findByOrderNumber(orderNumber);

        assertThat(result).isEmpty();
        verifyNoInteractions(purchaseRepository);
    }

    @Test
    void findByOrderNumberTreatsDataAccessExceptionAsMissAndLogsWarn() {
        when(purchaseRepository.findByOrderNumber("ORD-1001"))
                .thenThrow(new DataAccessResourceFailureException("db unavailable"));

        Optional<PurchaseInfo> result = customerService.findByOrderNumber("ORD-1001");

        assertThat(result).isEmpty();
        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("WARN");
                    assertThat(event.getFormattedMessage()).contains("ORD-1001");
                });
    }
}
