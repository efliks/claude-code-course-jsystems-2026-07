package com.jsystems.hsdc.session;

import com.jsystems.hsdc.llm.CaseContext;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import java.util.List;

/**
 * A read-only snapshot of one in-progress case as held by
 * {@link ActiveCaseRegistry}: the assembled {@link CaseContext} (form
 * fields, image analysis, purchase history) plus the ordered chat history
 * so far. {@code messages} is defensively copied so callers can't mutate
 * the registry's internal state.
 *
 * @param context  the case's LLM context (form data + image analysis + purchase history)
 * @param messages ordered chat messages recorded for this case so far
 */
public record RegisteredCase(CaseContext context, List<ChatMessage> messages) {

    public RegisteredCase {
        messages = List.copyOf(messages);
    }
}
