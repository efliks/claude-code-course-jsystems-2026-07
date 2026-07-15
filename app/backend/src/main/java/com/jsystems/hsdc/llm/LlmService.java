package com.jsystems.hsdc.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.policy.PolicyService;
import com.openai.client.OpenAIClient;
import com.openai.core.http.StreamResponse;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.StructuredChatCompletion;
import com.openai.models.chat.completions.StructuredChatCompletionCreateParams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Sole owner of the OpenAI SDK client (ADR-002 §5: "No other class may
 * touch the SDK"). Implements the three LLM operations the rest of the
 * backend needs: {@link #analyzeImage}, {@link #decide}, and
 * {@link #streamChat}.
 *
 * <p>Uses the Chat Completions API exclusively (ADR-002 §6 — the
 * OpenRouter Responses endpoint is beta and stateless, so it buys nothing
 * over Chat Completions here). Decision calls use the SDK's structured
 * outputs (JSON schema response format) so a case can never carry a
 * category outside {@link DecisionCategory}'s three values (TAC-002-05);
 * a schema-violating reply is retried once, then surfaces as
 * {@link LlmCallException} — never a guessed category (PRD §11).
 */
@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    /** Mandatory disclaimer text, verbatim, required in every decision message (PRD §11). */
    static final String MANDATORY_DISCLAIMER =
            "This is an automated recommendation based on our published policies."
                    + " It does not limit your statutory consumer rights.";

    private static final String POLICY_PLACEHOLDER = "{{POLICY}}";
    private static final String FORM_SUMMARY_PLACEHOLDER = "{{FORM_SUMMARY}}";

    private static final Pattern REVISED_DECISION_PATTERN =
            Pattern.compile("(?m)^[ \\t]*\\[\\[REVISED_DECISION:\\s*(APPROVE|REJECT|NEEDS_MORE_INFO)\\s*\\]\\][ \\t]*$");

    private static final Pattern JUSTIFICATION_HEADING_PATTERN =
            Pattern.compile("(?is)#{1,6}\\s*justification\\s*:?\\s*\\R(.*?)(?=\\R#{1,6}\\s|\\z)");

    private final OpenAIClient client;
    private final LlmProperties properties;
    private final PolicyService policyService;

    private final String complaintAnalysisPrompt;
    private final String returnAnalysisPrompt;
    private final String complaintDecisionPrompt;
    private final String returnDecisionPrompt;

    public LlmService(OpenAIClient client, LlmProperties properties, PolicyService policyService) {
        this.client = client;
        this.properties = properties;
        this.policyService = policyService;
        this.complaintAnalysisPrompt = loadPrompt("complaint-analysis.md");
        this.returnAnalysisPrompt = loadPrompt("return-analysis.md");
        this.complaintDecisionPrompt = loadPrompt("complaint-decision.md");
        this.returnDecisionPrompt = loadPrompt("return-decision.md");
    }

    /**
     * One Chat Completions call to {@link LlmProperties#visionModel()}: user
     * message = text part (scenario analysis prompt with form context) +
     * image part (base64 {@code data:} URL of the compressed JPEG).
     *
     * @param requestType COMPLAINT or RETURN — selects the analysis prompt
     * @param formSummary rendered form field summary, substituted into the prompt
     * @param jpegBytes   compressed JPEG bytes (see {@code ImageService})
     * @throws LlmCallException on timeout, non-2xx response, or a reply with no content
     */
    public ImageAnalysisResult analyzeImage(RequestType requestType, String formSummary, byte[] jpegBytes) {
        String template = requestType == RequestType.COMPLAINT ? complaintAnalysisPrompt : returnAnalysisPrompt;
        String promptText = template.replace(FORM_SUMMARY_PLACEHOLDER, formSummary == null ? "" : formSummary);
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpegBytes);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(properties.visionModel())
                .addUserMessageOfArrayOfContentParts(List.of(
                        ChatCompletionContentPart.ofText(
                                ChatCompletionContentPartText.builder().text(promptText).build()),
                        ChatCompletionContentPart.ofImageUrl(
                                ChatCompletionContentPartImage.builder()
                                        .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                                                .url(dataUrl)
                                                .build())
                                        .build())))
                .build();

        ChatCompletion completion;
        try {
            completion = client.chat().completions().create(params);
        } catch (OpenAIException e) {
            throw new LlmCallException("Image analysis call failed", e);
        }

        String text = completion.choices().stream()
                .findFirst()
                .flatMap(choice -> choice.message().content())
                .orElseThrow(() -> new LlmCallException("Image analysis reply had no content"));

        return ImageAnalysisResult.from(text);
    }

    /**
     * One call to {@link LlmProperties#decisionModel()} using structured
     * outputs (JSON schema: {@code category} + {@code message}). A reply
     * that fails to parse into a valid {@link DecisionCategory} is treated
     * as a schema violation and retried once (fresh call); a second failure
     * throws {@link LlmCallException} — never a guessed category.
     *
     * @throws LlmCallException on transport failure, or two consecutive schema-violating replies
     */
    public DecisionResult decide(CaseContext context) {
        String systemPrompt = buildDecisionSystemPrompt(context.requestType());
        String userSummary = buildCaseSummary(context);

        Optional<DecisionResponse> result = callDecisionModel(systemPrompt, userSummary);
        if (result.isEmpty()) {
            log.warn("Decision reply failed schema validation; retrying once");
            result = callDecisionModel(systemPrompt, userSummary);
        }
        DecisionResponse response = result.orElseThrow(
                () -> new LlmCallException("Decision model returned a schema-violating reply after one retry"));

        DecisionCategory category = DecisionCategory.valueOf(response.category.trim());
        return new DecisionResult(category, response.message, extractJustification(response.message));
    }

    /**
     * Streaming call to {@link LlmProperties#decisionModel()} with the same
     * decision system prompt plus the full message {@code history}; deltas
     * are forwarded to {@code onDelta} as they arrive. The trailing
     * machine-readable revision marker (if present) is parsed and stripped
     * before the visible text is returned.
     *
     * @param context CaseContext for this case (selects prompt + policy)
     * @param history full ordered conversation so far, including the latest customer message
     * @param onDelta callback invoked with each text chunk as it streams in
     * @throws LlmCallException on transport failure (including failures mid-stream)
     */
    public ChatFinal streamChat(CaseContext context, List<ChatMessage> history, Consumer<String> onDelta) {
        String systemPrompt = buildDecisionSystemPrompt(context.requestType());

        ChatCompletionCreateParams.Builder builder =
                ChatCompletionCreateParams.builder().model(properties.decisionModel()).addSystemMessage(systemPrompt);
        for (ChatMessage message : history) {
            if (message.sender() == Sender.CUSTOMER) {
                builder.addUserMessage(message.content());
            } else {
                builder.addAssistantMessage(message.content());
            }
        }
        ChatCompletionCreateParams params = builder.build();

        StringBuilder full = new StringBuilder();
        try (StreamResponse<ChatCompletionChunk> response = client.chat().completions().createStreaming(params)) {
            response.stream()
                    .forEach(chunk -> chunk.choices().forEach(choice -> choice.delta().content().ifPresent(text -> {
                        full.append(text);
                        onDelta.accept(text);
                    })));
        } catch (OpenAIException e) {
            throw new LlmCallException("Streaming chat call failed", e);
        }

        return parseChatFinal(full.toString());
    }

    /** Package-visible for unit tests (ADR-002 §8 "Prompt routing" / "Prompt completeness"). */
    String buildDecisionSystemPrompt(RequestType requestType) {
        String template = requestType == RequestType.COMPLAINT ? complaintDecisionPrompt : returnDecisionPrompt;
        String policy = requestType == RequestType.COMPLAINT
                ? policyService.complaintPolicy()
                : policyService.returnPolicy();
        return template.replace(POLICY_PLACEHOLDER, policy);
    }

    /** Package-visible for unit tests. Assembles the decision call's user-message case summary. */
    static String buildCaseSummary(CaseContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Request type: ").append(context.requestType()).append('\n');
        sb.append("Equipment category: ").append(context.equipmentCategory()).append('\n');
        sb.append("Equipment model: ").append(context.equipmentModel()).append('\n');
        sb.append("Purchase date: ").append(context.purchaseDate()).append('\n');
        sb.append("Order number: ")
                .append(context.orderNumber() == null ? "(not provided)" : context.orderNumber())
                .append('\n');
        sb.append("Reason: ").append(context.reason() == null ? "(not provided)" : context.reason()).append('\n');
        sb.append("\nImage analysis result:\n").append(context.imageAnalysis()).append('\n');

        if (Boolean.FALSE.equals(context.orderVerified())) {
            sb.append("\nNote: the order number provided could not be verified against purchase records."
                    + " Decide based on the information above without confirmed purchase history.\n");
        } else if (Boolean.TRUE.equals(context.orderVerified()) && context.purchaseHistory() != null) {
            PurchaseInfo history = context.purchaseHistory();
            sb.append("\nVerified purchase history:\n")
                    .append("Customer: ").append(history.customerName()).append('\n')
                    .append("Order number: ").append(history.orderNumber()).append('\n')
                    .append("Product: ").append(history.productName())
                    .append(" (").append(history.category()).append(")\n")
                    .append("Purchase date: ").append(history.purchaseDate()).append('\n')
                    .append("Price (cents): ").append(history.priceCents()).append('\n');
        }
        return sb.toString();
    }

    /** Package-visible for unit tests. Extracts a short justification excerpt for the decision DB row. */
    static String extractJustification(String message) {
        if (message == null) {
            return "";
        }
        Matcher matcher = JUSTIFICATION_HEADING_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1).strip();
        }
        return message.strip();
    }

    /** Package-visible for unit tests. Parses and strips the trailing revision marker, if present. */
    static ChatFinal parseChatFinal(String fullText) {
        Matcher matcher = REVISED_DECISION_PATTERN.matcher(fullText);
        if (matcher.find()) {
            DecisionCategory category = DecisionCategory.valueOf(matcher.group(1));
            String stripped = (fullText.substring(0, matcher.start()) + fullText.substring(matcher.end())).strip();
            return new ChatFinal(stripped, category);
        }
        return new ChatFinal(fullText.strip(), null);
    }

    /**
     * Calls the decision model once and returns the parsed reply, or empty
     * if the reply is missing content, missing fields, or has a category
     * string outside {@link DecisionCategory} — any of which count as a
     * schema violation for the one-retry policy in {@link #decide}.
     */
    private Optional<DecisionResponse> callDecisionModel(String systemPrompt, String userSummary) {
        StructuredChatCompletionCreateParams<DecisionResponse> params = ChatCompletionCreateParams.builder()
                .model(properties.decisionModel())
                .addSystemMessage(systemPrompt)
                .addUserMessage(userSummary)
                .responseFormat(DecisionResponse.class)
                .build();

        StructuredChatCompletion<DecisionResponse> completion;
        try {
            completion = client.chat().completions().create(params);
        } catch (OpenAIException e) {
            throw new LlmCallException("Decision call failed", e);
        }

        try {
            Optional<DecisionResponse> content =
                    completion.choices().stream().findFirst().flatMap(choice -> choice.message().content());
            if (content.isEmpty()) {
                return Optional.empty();
            }
            DecisionResponse response = content.get();
            if (response.category == null || response.message == null) {
                return Optional.empty();
            }
            DecisionCategory.valueOf(response.category.trim());
            return Optional.of(response);
        } catch (RuntimeException e) {
            log.warn("Decision reply failed schema validation: {}", e.toString());
            return Optional.empty();
        }
    }

    private static String loadPrompt(String filename) {
        try {
            return new ClassPathResource("prompts/" + filename).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load prompt resource: prompts/" + filename, e);
        }
    }

    /**
     * Structured-output wire type for {@link #decide}. Public with public
     * fields and an implicit no-arg constructor, mirroring the openai-java
     * structured-outputs POJO convention (reflection-based JSON schema
     * generation + Jackson deserialization). {@code category} is a plain
     * {@code String}, not {@link DecisionCategory}, so an off-schema value
     * from the model is caught by explicit parsing in this class rather
     * than relying on the SDK's schema-to-enum mapping (TAC-002-05).
     */
    public static class DecisionResponse {

        @JsonPropertyDescription("Exactly one of: APPROVE, REJECT, NEEDS_MORE_INFO.")
        public String category;

        @JsonPropertyDescription("Full markdown decision message: greeting, decision, justification citing a"
                + " concrete policy rule, next steps, and the mandatory disclaimer verbatim at the end.")
        public String message;
    }
}
