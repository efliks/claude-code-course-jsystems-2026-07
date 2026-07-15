package com.jsystems.hsdc.session;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.llm.CaseContext;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * ADR-001 §3 ({@code ActiveCaseRegistry} bullet) + §"State management": a
 * thread-safe, in-memory map of caseId -&gt; case context (form data, image
 * analysis, message list) with idle-TTL eviction (default 2h) so a
 * restart-free process doesn't grow unbounded. Eviction is exercised via an
 * injectable {@link Clock} rather than real waiting.
 */
class ActiveCaseRegistryTest {

    @Test
    void registerThenGetReturnsTheStoredContext() {
        ActiveCaseRegistry registry = new ActiveCaseRegistry();
        String caseId = UUID.randomUUID().toString();
        CaseContext context = caseContext();

        registry.register(caseId, context, List.of());

        Optional<RegisteredCase> result = registry.get(caseId);
        assertThat(result).isPresent();
        assertThat(result.get().context()).isEqualTo(context);
    }

    @Test
    void getUnknownIdReturnsEmpty() {
        ActiveCaseRegistry registry = new ActiveCaseRegistry();

        assertThat(registry.get("unknown-case-id")).isEmpty();
    }

    @Test
    void appendMessageAddsMessageInOrder() {
        ActiveCaseRegistry registry = new ActiveCaseRegistry();
        String caseId = UUID.randomUUID().toString();
        registry.register(caseId, caseContext(), List.of());

        ChatMessage first = message(caseId, "first");
        ChatMessage second = message(caseId, "second");
        assertThat(registry.appendMessage(caseId, first)).isTrue();
        assertThat(registry.appendMessage(caseId, second)).isTrue();

        List<ChatMessage> messages = registry.get(caseId).orElseThrow().messages();
        assertThat(messages).containsExactly(first, second);
    }

    @Test
    void appendMessageForUnknownCaseReturnsFalse() {
        ActiveCaseRegistry registry = new ActiveCaseRegistry();

        assertThat(registry.appendMessage("unknown-case-id", message("unknown-case-id", "hi")))
                .isFalse();
    }

    @Test
    void idleEntryPastTtlIsEvicted() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T08:00:00Z"));
        ActiveCaseRegistry registry = new ActiveCaseRegistry(clock, Duration.ofHours(2));
        String caseId = UUID.randomUUID().toString();
        registry.register(caseId, caseContext(), List.of());

        clock.advance(Duration.ofHours(2).plusMinutes(1));

        assertThat(registry.get(caseId)).isEmpty();
    }

    @Test
    void nonIdleEntrySurvivesWithinTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T08:00:00Z"));
        ActiveCaseRegistry registry = new ActiveCaseRegistry(clock, Duration.ofHours(2));
        String caseId = UUID.randomUUID().toString();
        registry.register(caseId, caseContext(), List.of());

        clock.advance(Duration.ofMinutes(119));

        assertThat(registry.get(caseId)).isPresent();
    }

    @Test
    void accessingACaseResetsItsIdleTimer() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-15T08:00:00Z"));
        ActiveCaseRegistry registry = new ActiveCaseRegistry(clock, Duration.ofHours(2));
        String caseId = UUID.randomUUID().toString();
        registry.register(caseId, caseContext(), List.of());

        clock.advance(Duration.ofMinutes(119));
        assertThat(registry.get(caseId)).isPresent(); // touches the entry, resets idle timer

        clock.advance(Duration.ofMinutes(119));
        assertThat(registry.get(caseId)).isPresent();
    }

    @Test
    void concurrentAppendsAreAllRecordedSafely() throws InterruptedException {
        ActiveCaseRegistry registry = new ActiveCaseRegistry();
        String caseId = UUID.randomUUID().toString();
        registry.register(caseId, caseContext(), List.of());

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int i = 0; i < threadCount; i++) {
                int index = i;
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    registry.appendMessage(caseId, message(caseId, "msg-" + index));
                });
            }
            ready.await();
            start.countDown();
        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }

        assertThat(registry.get(caseId).orElseThrow().messages()).hasSize(threadCount);
    }

    private static CaseContext caseContext() {
        return new CaseContext(
                RequestType.COMPLAINT, "KITCHEN", "Toaster T1", "2026-06-01", "ORD-1001",
                "Broken heating element", "Visible crack near the heating coil", true,
                new com.jsystems.hsdc.persistence.domain.PurchaseInfo(
                        "Jane Doe", "ORD-1001", "Toaster T1", "KITCHEN", "2026-06-01", 4999L));
    }

    private static ChatMessage message(String caseId, String content) {
        return new ChatMessage(UUID.randomUUID().toString(), caseId, Sender.CUSTOMER, content, Instant.now());
    }

    /** Test double: a {@link Clock} whose instant can be advanced manually — no real waiting. */
    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant initial) {
            this.instant = initial;
        }

        void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
