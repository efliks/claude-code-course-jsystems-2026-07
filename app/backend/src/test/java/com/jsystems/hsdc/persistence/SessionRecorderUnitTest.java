package com.jsystems.hsdc.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jsystems.hsdc.persistence.domain.CaseSession;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.Decision;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.persistence.repository.CaseSessionRepository;
import com.jsystems.hsdc.persistence.repository.ChatMessageRepository;
import com.jsystems.hsdc.persistence.repository.DecisionRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ADR-004 §8 "SessionRecorder tolerance" (Unit) scenario: with mocked
 * repositories, a persistence failure must never propagate to the caller
 * (AC-29) and must be logged at ERROR with the case id.
 */
class SessionRecorderUnitTest {

    private CaseSessionRepository caseSessionRepository;
    private DecisionRepository decisionRepository;
    private ChatMessageRepository chatMessageRepository;
    private PlatformTransactionManager transactionManager;
    private SessionRecorder recorder;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        caseSessionRepository = mock(CaseSessionRepository.class);
        decisionRepository = mock(DecisionRepository.class);
        chatMessageRepository = mock(ChatMessageRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);
        recorder = new SessionRecorder(
                caseSessionRepository, decisionRepository, chatMessageRepository, transactionManager);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(SessionRecorder.class)).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger(SessionRecorder.class)).detachAppender(logAppender);
    }

    @Test
    void recordCaseCreatedSwallowsRepositoryFailureAndLogsErrorWithCaseId() {
        String caseId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(caseSessionRepository).insert(any());

        assertThatCode(() -> recorder.recordCaseCreated(caseSession(caseId), decision(caseId), message(caseId)))
                .doesNotThrowAnyException();

        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(event.getFormattedMessage()).contains(caseId);
                });
    }

    @Test
    void recordDecisionSwallowsRepositoryFailureAndLogsErrorWithCaseId() {
        String caseId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(decisionRepository).insert(any());

        assertThatCode(() -> recorder.recordDecision(decision(caseId))).doesNotThrowAnyException();

        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(event.getFormattedMessage()).contains(caseId);
                });
    }

    @Test
    void recordMessageSwallowsRepositoryFailureAndLogsErrorWithCaseId() {
        String caseId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(chatMessageRepository).insert(any());

        assertThatCode(() -> recorder.recordMessage(message(caseId))).doesNotThrowAnyException();

        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(event.getFormattedMessage()).contains(caseId);
                });
    }

    @Test
    void recordAnalysisUpdateSwallowsRepositoryFailureAndLogsErrorWithCaseId() {
        String caseId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(caseSessionRepository).updateAnalysis(any(), any(), any());

        assertThatCode(() -> recorder.recordAnalysisUpdate(caseId, "analysis text", true))
                .doesNotThrowAnyException();

        assertThat(logAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel().toString()).isEqualTo("ERROR");
                    assertThat(event.getFormattedMessage()).contains(caseId);
                });
    }

    @Test
    void recordCaseCreatedDoesNotTouchDecisionOrMessageRepositoriesWhenCaseInsertFails() {
        String caseId = UUID.randomUUID().toString();
        doThrow(new RuntimeException("boom")).when(caseSessionRepository).insert(any());

        recorder.recordCaseCreated(caseSession(caseId), decision(caseId), message(caseId));

        verifyNoInteractions(decisionRepository, chatMessageRepository);
    }

    private static CaseSession caseSession(String id) {
        return new CaseSession(
                id, RequestType.COMPLAINT, "Toaster", "T1", "2026-06-01", "ORD-9999",
                "Broken", null, null, Instant.parse("2026-07-15T08:00:00Z"));
    }

    private static Decision decision(String caseId) {
        return new Decision(
                UUID.randomUUID().toString(), caseId, DecisionCategory.NEEDS_MORE_INFO,
                "justification", "full message", Instant.parse("2026-07-15T08:00:00Z"));
    }

    private static ChatMessage message(String caseId) {
        return new ChatMessage(
                UUID.randomUUID().toString(), caseId, Sender.AGENT, "content",
                Instant.parse("2026-07-15T08:00:00Z"));
    }
}
