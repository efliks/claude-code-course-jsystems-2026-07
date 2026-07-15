package com.jsystems.hsdc.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.jsystems.hsdc.persistence.domain.DecisionCategory;
import com.jsystems.hsdc.persistence.domain.PurchaseInfo;
import com.jsystems.hsdc.persistence.domain.RequestType;
import com.jsystems.hsdc.policy.PolicyService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Pure-logic unit tests (no network, no WireMock) for the parts of
 * {@link LlmService} that ADR-002 §8 classifies as "Unit": prompt routing
 * per request type, prompt/case-summary completeness, and the unusable-image
 * detectable-phrase logic. Rows 1, 2, and 8 of the ADR-002 §8 test matrix.
 */
class LlmServiceUnitTest {

    private static final String COMPLAINT_POLICY_MARKER = "COMPLAINT_POLICY_MARKER_TEXT_XYZ";
    private static final String RETURN_POLICY_MARKER = "RETURN_POLICY_MARKER_TEXT_XYZ";

    // --- Row 1: Prompt routing per request type -----------------------------------------

    @Test
    void complaintRequestUsesComplaintTemplateAndComplaintPolicyOnly(@TempDir Path dir) throws IOException {
        LlmService service = newServiceWithPolicies(dir);

        String prompt = service.buildDecisionSystemPrompt(RequestType.COMPLAINT);

        assertThat(prompt).contains(COMPLAINT_POLICY_MARKER);
        assertThat(prompt).doesNotContain(RETURN_POLICY_MARKER);
        assertThat(prompt).contains("single customer **complaint** case");
        assertThat(prompt).doesNotContain("{{POLICY}}");
    }

    @Test
    void returnRequestUsesReturnTemplateAndReturnPolicyOnly(@TempDir Path dir) throws IOException {
        LlmService service = newServiceWithPolicies(dir);

        String prompt = service.buildDecisionSystemPrompt(RequestType.RETURN);

        assertThat(prompt).contains(RETURN_POLICY_MARKER);
        assertThat(prompt).doesNotContain(COMPLAINT_POLICY_MARKER);
        assertThat(prompt).contains("single customer **return** case");
        assertThat(prompt).doesNotContain("{{POLICY}}");
    }

    // --- Row 2: Prompt / case-summary completeness ---------------------------------------

    @Test
    void decisionSystemPromptContainsPolicyVerbatimAndDisclaimerVerbatim(@TempDir Path dir) throws IOException {
        LlmService service = newServiceWithPolicies(dir);

        String prompt = service.buildDecisionSystemPrompt(RequestType.COMPLAINT);

        assertThat(prompt).contains(COMPLAINT_POLICY_MARKER);
        assertThat(prompt).contains(LlmService.MANDATORY_DISCLAIMER);
    }

    @Test
    void caseSummaryContainsAllFormFieldsAndAnalysisText() {
        CaseContext context = new CaseContext(
                RequestType.COMPLAINT,
                "Laptops",
                "ThinkPad X1",
                "2026-05-01",
                "ORD-123",
                "Screen cracked on arrival",
                "Analysis: visible crack on the display panel.",
                null,
                null);

        String summary = LlmService.buildCaseSummary(context);

        assertThat(summary).contains("Laptops");
        assertThat(summary).contains("ThinkPad X1");
        assertThat(summary).contains("2026-05-01");
        assertThat(summary).contains("ORD-123");
        assertThat(summary).contains("Screen cracked on arrival");
        assertThat(summary).contains("Analysis: visible crack on the display panel.");
    }

    @Test
    void caseSummaryAddsNotVerifiedNoteWhenOrderVerifiedIsFalse() {
        CaseContext context = new CaseContext(
                RequestType.RETURN, "Phones", "Pixel 9", "2026-04-01", "ORD-999", null, "Looks new.", false, null);

        String summary = LlmService.buildCaseSummary(context);

        assertThat(summary).containsIgnoringCase("could not be verified");
    }

    @Test
    void caseSummaryIncludesVerifiedPurchaseHistoryWhenOrderVerifiedIsTrue() {
        PurchaseInfo history = new PurchaseInfo("Jane Doe", "ORD-999", "Pixel 9", "Phones", "2026-04-01", 129999L);
        CaseContext context = new CaseContext(
                RequestType.RETURN, "Phones", "Pixel 9", "2026-04-01", "ORD-999", null, "Looks new.", true, history);

        String summary = LlmService.buildCaseSummary(context);

        assertThat(summary).contains("Jane Doe");
        assertThat(summary).contains("ORD-999");
        assertThat(summary).doesNotContainIgnoringCase("could not be verified");
    }

    @Test
    void caseSummaryOmitsHistoryNoteWhenOrderVerifiedIsNull() {
        CaseContext context = new CaseContext(
                RequestType.RETURN, "Phones", "Pixel 9", "2026-04-01", null, null, "Looks new.", null, null);

        String summary = LlmService.buildCaseSummary(context);

        assertThat(summary).doesNotContainIgnoringCase("could not be verified");
        assertThat(summary).doesNotContain("Verified purchase history");
    }

    // --- Row 8: Unusable image phrase -----------------------------------------------------

    @Test
    void analysisTextWithDetectablePhraseIsMarkedUnusable() {
        ImageAnalysisResult result =
                ImageAnalysisResult.from("UNUSABLE_IMAGE: the photo is too blurry to assess damage.");

        assertThat(result.usable()).isFalse();
    }

    @Test
    void analysisTextWithoutDetectablePhraseIsMarkedUsable() {
        ImageAnalysisResult result = ImageAnalysisResult.from("The device shows a cracked rear panel.");

        assertThat(result.usable()).isTrue();
    }

    // --- extractJustification / parseChatFinal pure-logic coverage -----------------------

    @Test
    void extractJustificationReadsTheJustificationHeadingSection() {
        String message = """
                ## Decision
                Approve

                ## Justification
                The item is within the 30-day return window per policy rule 3.2.

                ## Next steps
                Ship it back using the prepaid label.
                """;

        String excerpt = LlmService.extractJustification(message);

        assertThat(excerpt).contains("30-day return window per policy rule 3.2");
        assertThat(excerpt).doesNotContain("Next steps");
    }

    @Test
    void parseChatFinalStripsMarkerAndReturnsRevisedCategory() {
        String text = "Thanks for the extra photo. Based on this, we can approve your return.\n"
                + "[[REVISED_DECISION: APPROVE]]";

        ChatFinal result = LlmService.parseChatFinal(text);

        assertThat(result.fullText()).doesNotContain("REVISED_DECISION");
        assertThat(result.fullText()).contains("we can approve your return");
        assertThat(result.revisedCategory()).isEqualTo(DecisionCategory.APPROVE);
    }

    @Test
    void parseChatFinalReturnsNullCategoryWhenMarkerAbsent() {
        String text = "Sure, here is more detail about our return window policy.";

        ChatFinal result = LlmService.parseChatFinal(text);

        assertThat(result.fullText()).isEqualTo(text);
        assertThat(result.revisedCategory()).isNull();
    }

    private static LlmService newServiceWithPolicies(Path dir) throws IOException {
        Files.writeString(dir.resolve("complaint-policy.md"), COMPLAINT_POLICY_MARKER);
        Files.writeString(dir.resolve("return-policy.md"), RETURN_POLICY_MARKER);
        PolicyService policyService = new PolicyService(dir.toString());
        policyService.loadPolicies();

        OpenAIClient unusedClient =
                OpenAIOkHttpClient.builder().apiKey("unused").baseUrl("http://localhost:1").build();
        LlmProperties properties = new LlmProperties(
                "unused", "http://localhost:1", LlmProperties.DEFAULT_VISION_MODEL, LlmProperties.DEFAULT_DECISION_MODEL);

        return new LlmService(unusedClient, properties, policyService);
    }
}
