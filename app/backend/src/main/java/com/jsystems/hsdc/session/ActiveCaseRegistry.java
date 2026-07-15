package com.jsystems.hsdc.session;

import com.jsystems.hsdc.llm.CaseContext;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/**
 * The only mutable server state (ADR-001 §3 "State management"): a
 * thread-safe, in-memory map of caseId -&gt; case context (form data,
 * image analysis, running chat message list) for cases currently in
 * progress. Entries idle past {@link #idleTimeout} are evicted to bound
 * memory; eviction only removes live-chat ability — persisted records in
 * SQLite are untouched (ADR-001 §3, §7 class diagram: {@code evictIdle()}).
 *
 * <p>Losing the registry (app restart) orphans live chats — accepted per
 * PRD (no session resume).
 *
 * <p>{@link Clock} and the idle timeout are injectable so eviction is
 * unit-testable without real waiting (see the constructor overload).
 */
@Component
public class ActiveCaseRegistry {

    /** Default idle timeout before a case is evicted from the registry (ADR-001 §3). */
    public static final Duration DEFAULT_IDLE_TIMEOUT = Duration.ofHours(2);

    private final Map<String, CaseEntry> cases = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration idleTimeout;

    public ActiveCaseRegistry() {
        this(Clock.systemUTC(), DEFAULT_IDLE_TIMEOUT);
    }

    /**
     * @param clock       time source used to stamp/compare last-access times; inject a
     *                    fixed/mutable clock in tests to exercise eviction deterministically
     * @param idleTimeout duration of inactivity after which an entry is evicted
     */
    public ActiveCaseRegistry(Clock clock, Duration idleTimeout) {
        this.clock = clock;
        this.idleTimeout = idleTimeout;
    }

    /**
     * Registers a newly created case with its assembled context and any
     * initial chat messages (typically the first agent decision message).
     * Overwrites any existing entry for the same {@code caseId}.
     */
    public void register(String caseId, CaseContext context, List<ChatMessage> initialMessages) {
        cases.put(caseId, new CaseEntry(context, initialMessages, clock.instant()));
    }

    /**
     * Looks up a case by id. Absent for an unknown id, or one evicted for
     * idling past the configured timeout. A successful lookup counts as
     * activity and resets the entry's idle timer.
     */
    public Optional<RegisteredCase> get(String caseId) {
        evictIdle();
        CaseEntry entry = cases.get(caseId);
        if (entry == null) {
            return Optional.empty();
        }
        entry.touch(clock.instant());
        return Optional.of(entry.snapshot());
    }

    /**
     * Appends one chat message to a case's running message list.
     *
     * @return {@code true} if the case was known (message appended), {@code false}
     *         if the id is unknown or was evicted
     */
    public boolean appendMessage(String caseId, ChatMessage message) {
        evictIdle();
        CaseEntry entry = cases.get(caseId);
        if (entry == null) {
            return false;
        }
        entry.appendMessage(message);
        entry.touch(clock.instant());
        return true;
    }

    /**
     * Removes every entry idle past {@link #idleTimeout}. Called
     * internally before each read/write; also safe to invoke from an
     * external scheduled task.
     */
    public void evictIdle() {
        Instant now = clock.instant();
        cases.values().removeIf(entry -> Duration.between(entry.lastAccessed(), now).compareTo(idleTimeout) > 0);
    }

    private static final class CaseEntry {
        private final CaseContext context;
        private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
        private volatile Instant lastAccessed;

        CaseEntry(CaseContext context, List<ChatMessage> initialMessages, Instant now) {
            this.context = context;
            this.messages.addAll(initialMessages);
            this.lastAccessed = now;
        }

        void touch(Instant now) {
            this.lastAccessed = now;
        }

        void appendMessage(ChatMessage message) {
            messages.add(message);
        }

        Instant lastAccessed() {
            return lastAccessed;
        }

        RegisteredCase snapshot() {
            return new RegisteredCase(context, messages);
        }
    }
}
