package com.jsystems.hsdc.llm;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import com.jsystems.hsdc.persistence.domain.ChatMessage;
import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.persistence.domain.Sender;
import com.jsystems.hsdc.policy.PolicyService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for {@link LlmService} against a WireMock stub standing
 * in for OpenRouter (TAC-002-01: no test in this class opens a socket to
 * any host except localhost — the SDK client's {@code baseUrl} always
 * points at {@link #wm}). Covers ADR-002 §8 rows 3-7 (structured decision
 * parsing, schema-violation retry, image payload format, streaming
 * assembly, transport failures) plus TAC-002-04 (API key never in DEBUG
 * logs) and TAC-002-05 (unknown category string never mapped).
 */
class LlmServiceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm =
            WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String CHAT_PATH = "/chat/completions";

    @TempDir
    static Path policiesDir;

    static PolicyService policyService;

    @BeforeAll
    static void loadPolicies() throws IOException {
        Files.writeString(policiesDir.resolve("complaint-policy.md"), "# Complaint policy\nRule 3.2: 30-day window.");
        Files.writeString(policiesDir.resolve("return-policy.md"), "# Return policy\nRule 1.1: 14-day window.");
        policyService = new PolicyService(policiesDir.toString());
        policyService.loadPolicies();
    }

    @BeforeEach
    void resetStubs() {
        wm.resetAll();
    }

    // --- Row 3: Structured decision parsing per category ----------------------------------

    @Test
    void decideParsesApproveCategoryFromStructuredReply() throws Exception {
        stubDecision("APPROVE", validDecisionMessage());

        DecisionResult result = newService("test-key").decide(sampleCaseContext());

        assertThat(result.category()).isEqualTo(DecisionCategory.APPROVE);
        assertThat(result.message()).contains(LlmService.MANDATORY_DISCLAIMER);
    }

    @Test
    void decideParsesRejectCategoryFromStructuredReply() throws Exception {
        stubDecision("REJECT", validDecisionMessage());

        DecisionResult result = newService("test-key").decide(sampleCaseContext());

        assertThat(result.category()).isEqualTo(DecisionCategory.REJECT);
    }

    @Test
    void decideParsesNeedsMoreInfoCategoryFromStructuredReply() throws Exception {
        stubDecision("NEEDS_MORE_INFO", validDecisionMessage());

        DecisionResult result = newService("test-key").decide(sampleCaseContext());

        assertThat(result.category()).isEqualTo(DecisionCategory.NEEDS_MORE_INFO);
    }

    // --- Row 4: Schema-violation retry (also covers TAC-002-05) ---------------------------

    @Test
    void invalidThenValidReplySucceedsAfterOneRetry() throws Exception {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .inScenario("decision-retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(okJson(decisionCompletionBody(decisionContent("MAYBE", validDecisionMessage()))))
                .willSetStateTo("retried"));
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .inScenario("decision-retry")
                .whenScenarioStateIs("retried")
                .willReturn(okJson(decisionCompletionBody(decisionContent("APPROVE", validDecisionMessage())))));

        DecisionResult result = newService("test-key").decide(sampleCaseContext());

        assertThat(result.category()).isEqualTo(DecisionCategory.APPROVE);
        wm.verify(2, postRequestedFor(urlPathEqualTo(CHAT_PATH)));
    }

    @Test
    void unknownCategoryStringTwiceThrowsAndNeverMapsAGuessedCategory() throws Exception {
        stubDecision("MAYBE", validDecisionMessage());

        assertThatThrownBy(() -> newService("test-key").decide(sampleCaseContext()))
                .isInstanceOf(LlmCallException.class);
        wm.verify(2, postRequestedFor(urlPathEqualTo(CHAT_PATH)));
    }

    // --- Row 5: Image payload format --------------------------------------------------------

    @Test
    void analyzeImageSendsBase64DataUrlImagePartAndTextPartToVisionModel() throws Exception {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(okJson(plainCompletionBody("The device shows a cracked rear panel."))));

        byte[] jpegBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        ImageAnalysisResult result =
                newService("test-key").analyzeImage(RequestType.COMPLAINT, "Category: Laptops", jpegBytes);

        assertThat(result.usable()).isTrue();
        assertThat(result.rawText()).contains("cracked rear panel");

        String expectedBase64 = java.util.Base64.getEncoder().encodeToString(jpegBytes);
        wm.verify(postRequestedFor(urlPathEqualTo(CHAT_PATH))
                .withRequestBody(matchingJsonPath("$.model", equalTo(LlmProperties.DEFAULT_VISION_MODEL)))
                .withRequestBody(containing("data:image/jpeg;base64," + expectedBase64))
                .withRequestBody(containing("Category: Laptops")));
    }

    // --- Row 6: Streaming assembly ----------------------------------------------------------

    @Test
    void streamChatForwardsDeltasInOrderAndAssemblesFullTextWithoutMarker() throws Exception {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody(List.of("Hello", ", ", "world."), null))));

        List<String> deltas = new ArrayList<>();
        ChatFinal result = newService("test-key")
                .streamChat(sampleCaseContext(), sampleHistory(), deltas::add);

        assertThat(deltas).containsExactly("Hello", ", ", "world.");
        assertThat(result.fullText()).isEqualTo("Hello, world.");
        assertThat(result.revisedCategory()).isNull();
    }

    @Test
    void streamChatParsesAndStripsRevisionMarker() throws Exception {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withBody(sseBody(
                                List.of("We can approve your return now.\n"),
                                "[[REVISED_DECISION: APPROVE]]"))));

        List<String> deltas = new ArrayList<>();
        ChatFinal result = newService("test-key")
                .streamChat(sampleCaseContext(), sampleHistory(), deltas::add);

        assertThat(result.fullText()).isEqualTo("We can approve your return now.");
        assertThat(result.fullText()).doesNotContain("REVISED_DECISION");
        assertThat(result.revisedCategory()).isEqualTo(DecisionCategory.APPROVE);
    }

    // --- Row 7: Transport failures -----------------------------------------------------------

    @Test
    void serverErrorThrowsLlmCallException() {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH)).willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> newService("test-key").decide(sampleCaseContext()))
                .isInstanceOf(LlmCallException.class);
    }

    @Test
    void connectionResetThrowsLlmCallException() {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThatThrownBy(() -> newService("test-key").decide(sampleCaseContext()))
                .isInstanceOf(LlmCallException.class);
    }

    @Test
    void timeoutThrowsLlmCallException() {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(aResponse()
                        .withFixedDelay(2000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));

        OpenAIClient shortTimeoutClient = OpenAIOkHttpClient.builder()
                .apiKey("test-key")
                .baseUrl(wm.baseUrl())
                .timeout(Duration.ofMillis(300))
                .maxRetries(0)
                .build();
        LlmProperties properties = new LlmProperties(
                "test-key", wm.baseUrl(), LlmProperties.DEFAULT_VISION_MODEL, LlmProperties.DEFAULT_DECISION_MODEL);
        LlmService service = new LlmService(shortTimeoutClient, properties, policyService);

        assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
                assertThatThrownBy(() -> service.decide(sampleCaseContext())).isInstanceOf(LlmCallException.class));
    }

    @Test
    void streamMidStreamFailureThrowsLlmCallExceptionAndDoesNotReturnAResult() {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/event-stream")
                        .withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        List<String> deltas = new ArrayList<>();
        assertThatThrownBy(() ->
                        newService("test-key").streamChat(sampleCaseContext(), sampleHistory(), deltas::add))
                .isInstanceOf(LlmCallException.class);
    }

    // --- TAC-002-04: API key never in DEBUG logs -------------------------------------------

    /**
     * Root is set to DEBUG so anything unexpected in our code, the openai-java
     * SDK, or its OkHttp transport is caught. The WireMock stub server's own
     * loggers ({@code org.eclipse.jetty}, {@code com.github.tomakehurst.wiremock})
     * are explicitly silenced first: as the local HTTP server terminating this
     * test's connection, they log the raw inbound request line-by-line at
     * DEBUG (including the {@code Authorization} header) as a normal part of
     * being an HTTP server — that is the test harness echoing what it
     * received, not our application (or a real OpenRouter server, which we
     * don't control) leaking the key. TAC-002-04 is about *our* logging.
     */
    @Test
    void apiKeyNeverAppearsInDebugLogsOfAStubbedPipelineRun() throws Exception {
        String secretKey = "sk-or-TEST-SECRET-DO-NOT-LOG-98765";
        stubDecision("APPROVE", validDecisionMessage());

        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        Logger jettyLogger = (Logger) LoggerFactory.getLogger("org.eclipse.jetty");
        Logger wireMockLogger = (Logger) LoggerFactory.getLogger("com.github.tomakehurst.wiremock");
        Level originalRootLevel = rootLogger.getLevel();
        Level originalJettyLevel = jettyLogger.getLevel();
        Level originalWireMockLevel = wireMockLogger.getLevel();

        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        rootLogger.addAppender(appender);
        rootLogger.setLevel(Level.DEBUG);
        jettyLogger.setLevel(Level.OFF);
        wireMockLogger.setLevel(Level.OFF);
        try {
            newService(secretKey).decide(sampleCaseContext());
        } finally {
            rootLogger.detachAppender(appender);
            rootLogger.setLevel(originalRootLevel);
            jettyLogger.setLevel(originalJettyLevel);
            wireMockLogger.setLevel(originalWireMockLevel);
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(msg -> msg != null && msg.contains(secretKey));
    }

    // --- fixtures ----------------------------------------------------------------------------

    private static LlmService newService(String apiKey) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(wm.baseUrl())
                .timeout(Duration.ofSeconds(5))
                .maxRetries(0)
                .build();
        LlmProperties properties = new LlmProperties(
                apiKey, wm.baseUrl(), LlmProperties.DEFAULT_VISION_MODEL, LlmProperties.DEFAULT_DECISION_MODEL);
        return new LlmService(client, properties, policyService);
    }

    private static CaseContext sampleCaseContext() {
        return new CaseContext(
                RequestType.COMPLAINT,
                "Laptops",
                "ThinkPad X1",
                "2026-05-01",
                "ORD-123",
                "Screen cracked on arrival",
                "Analysis: visible crack on the display panel.",
                null,
                null);
    }

    private static List<ChatMessage> sampleHistory() {
        return List.of(new ChatMessage("msg-1", "case-1", Sender.CUSTOMER, "Here is another photo.", Instant.now()));
    }

    private static String validDecisionMessage() {
        return "## Decision\nApprove\n\n## Justification\nRule 3.2: within the 30-day window.\n\n"
                + "## Next steps\nShip using the prepaid label.\n\n" + LlmService.MANDATORY_DISCLAIMER;
    }

    private static void stubDecision(String category, String message) throws Exception {
        wm.stubFor(post(urlPathEqualTo(CHAT_PATH))
                .willReturn(okJson(decisionCompletionBody(decisionContent(category, message)))));
    }

    private static String decisionContent(String category, String message) throws Exception {
        return MAPPER.writeValueAsString(Map.of("category", category, "message", message));
    }

    private static String decisionCompletionBody(String innerContentJson) throws Exception {
        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "assistant");
        message.put("content", innerContentJson);
        return completionEnvelope(message);
    }

    private static String plainCompletionBody(String content) throws Exception {
        ObjectNode message = MAPPER.createObjectNode();
        message.put("role", "assistant");
        message.put("content", content);
        return completionEnvelope(message);
    }

    private static String completionEnvelope(ObjectNode message) throws Exception {
        ObjectNode choice = MAPPER.createObjectNode();
        choice.put("index", 0);
        choice.set("message", message);
        choice.put("finish_reason", "stop");

        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", "chatcmpl-test");
        root.put("object", "chat.completion");
        root.put("created", 1_710_000_000);
        root.put("model", "openai/gpt-5.6-terra");
        root.set("choices", MAPPER.createArrayNode().add(choice));
        return MAPPER.writeValueAsString(root);
    }

    private static String sseBody(List<String> deltas, String finalMarkerAppendedToLastDelta) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("data: ").append(chunkJson(null, true)).append("\n\n");
        for (String delta : deltas) {
            sb.append("data: ").append(chunkJson(delta, false)).append("\n\n");
        }
        if (finalMarkerAppendedToLastDelta != null) {
            sb.append("data: ")
                    .append(chunkJson("\n" + finalMarkerAppendedToLastDelta, false))
                    .append("\n\n");
        }
        sb.append("data: [DONE]\n\n");
        return sb.toString();
    }

    private static String chunkJson(String content, boolean roleOnly) throws Exception {
        ObjectNode delta = MAPPER.createObjectNode();
        if (roleOnly) {
            delta.put("role", "assistant");
        }
        if (content != null) {
            delta.put("content", content);
        }
        ObjectNode choice = MAPPER.createObjectNode();
        choice.put("index", 0);
        choice.set("delta", delta);

        ObjectNode root = MAPPER.createObjectNode();
        root.put("id", "chatcmpl-test-stream");
        root.put("object", "chat.completion.chunk");
        root.put("created", 1_710_000_000);
        root.put("model", "openai/gpt-5.6-terra");
        root.set("choices", MAPPER.createArrayNode().add(choice));
        return MAPPER.writeValueAsString(root);
    }
}
