package com.jsystems.hsdc.customer;

import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.repository.PurchaseRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * Order-number lookup against seeded customer/purchase data (ADR-000 §4
 * module map, ADR-004 §5). A failed lookup must never break case
 * processing (PRD AC-15's spirit): an unknown, blank, or {@code null}
 * order number, as well as a repository failure, are all treated as a
 * plain miss — only a genuine match returns purchase history.
 */
@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final PurchaseRepository purchaseRepository;

    public CustomerService(PurchaseRepository purchaseRepository) {
        this.purchaseRepository = purchaseRepository;
    }

    /**
     * Looks up a purchase (with owning customer) by order number.
     *
     * @param orderNumber customer-entered order number; {@code null} or
     *                    blank yields an empty result without querying
     *                    the database
     * @return the matching {@link PurchaseInfo}, or empty if not found,
     *         not provided, or the lookup failed
     */
    public Optional<PurchaseInfo> findByOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return Optional.empty();
        }
        try {
            return purchaseRepository.findByOrderNumber(orderNumber);
        } catch (DataAccessException e) {
            log.warn("Purchase lookup failed for orderNumber={}; treating as miss", orderNumber, e);
            return Optional.empty();
        }
    }
}
