package com.jsystems.hsdc.persistence;

import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.repository.CaseSessionRepository;
import com.jsystems.hsdc.persistence.repository.ChatMessageRepository;
import com.jsystems.hsdc.persistence.repository.DecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Failure-tolerant write facade over the persistence repositories
 * (ADR-004 §3, §6, sequence diagram in §7). Every write goes through here
 * so that {@code CaseService}/{@code ChatService} contain no persistence
 * error handling: a persistence failure is logged at ERROR with the case
 * id and never propagates to the caller (AC-29).
 *
 * <p>{@link #recordCaseCreated} groups the case row, its first decision,
 * and its first chat message into a single programmatic transaction
 * ({@link TransactionTemplate}, not {@code @Transactional}) so that a
 * mid-write failure rolls back everything already written in that call —
 * no orphan case/decision rows — rather than leaving a half-persisted
 * case. Using {@link TransactionTemplate} instead of AOP-driven
 * {@code @Transactional} also makes the rollback behavior directly
 * testable against a real transaction manager without a Spring
 * ApplicationContext.
 */
@Component
public class SessionRecorder {

    private static final Logger log = LoggerFactory.getLogger(SessionRecorder.class);

    private final CaseSessionRepository caseSessionRepository;
    private final DecisionRepository decisionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final TransactionTemplate transactionTemplate;

    public SessionRecorder(
            CaseSessionRepository caseSessionRepository,
            DecisionRepository decisionRepository,
            ChatMessageRepository chatMessageRepository,
            PlatformTransactionManager transactionManager) {
        this.caseSessionRepository = caseSessionRepository;
        this.decisionRepository = decisionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Persists a newly created case together with its first decision and
     * first chat message as one atomic unit (AC-26, AC-27, AC-28). On any
     * failure, whatever was already written in this call is rolled back,
     * the error is logged with the case id, and the method returns
     * normally (AC-29).
     */
    public void recordCaseCreated(CaseSession caseSession, Decision decision, ChatMessage firstMessage) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                caseSessionRepository.insert(caseSession);
                decisionRepository.insert(decision);
                chatMessageRepository.insert(firstMessage);
            });
        } catch (Exception e) {
            log.error("Failed to persist case creation for caseId={}", caseSession.id(), e);
        }
    }

    /** Persists a revised decision for an existing case (AC-27). */
    public void recordDecision(Decision decision) {
        try {
            decisionRepository.insert(decision);
        } catch (Exception e) {
            log.error("Failed to persist decision for caseId={}", decision.caseId(), e);
        }
    }

    /** Persists one chat turn, customer or agent (AC-28). */
    public void recordMessage(ChatMessage message) {
        try {
            chatMessageRepository.insert(message);
        } catch (Exception e) {
            log.error("Failed to persist chat message for caseId={}", message.caseId(), e);
        }
    }

    /** Persists the image-analysis result and order-verified flag for a case. */
    public void recordAnalysisUpdate(String caseId, String imageAnalysis, Boolean orderVerified) {
        try {
            caseSessionRepository.updateAnalysis(caseId, imageAnalysis, orderVerified);
        } catch (Exception e) {
            log.error("Failed to persist analysis update for caseId={}", caseId, e);
        }
    }
}
