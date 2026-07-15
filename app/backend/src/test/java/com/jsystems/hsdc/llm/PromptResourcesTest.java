package com.jsystems.hsdc.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;

/**
 * Covers TAC-002-02: the four prompt resources exist, are non-empty, and
 * each decision prompt contains the mandatory disclaimer string exactly
 * once. Reads the raw classpath resources directly, no {@link LlmService}
 * or network involved.
 */
class PromptResourcesTest {

    @ParameterizedTest
    @ValueSource(strings = {"complaint-analysis.md", "return-analysis.md", "complaint-decision.md", "return-decision.md"})
    void promptResourceExistsAndIsNonEmpty(String filename) throws IOException {
        String content = read(filename);
        assertThat(content).isNotBlank();
    }

    @ParameterizedTest
    @ValueSource(strings = {"complaint-decision.md", "return-decision.md"})
    void decisionPromptContainsMandatoryDisclaimerExactlyOnce(String filename) throws IOException {
        String content = read(filename);
        assertThat(occurrences(content, LlmService.MANDATORY_DISCLAIMER)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"complaint-decision.md", "return-decision.md"})
    void decisionPromptContainsPolicyPlaceholder(String filename) throws IOException {
        String content = read(filename);
        assertThat(content).contains("{{POLICY}}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"complaint-analysis.md", "return-analysis.md"})
    void analysisPromptContainsFormSummaryPlaceholderAndUnusableImageInstruction(String filename) throws IOException {
        String content = read(filename);
        assertThat(content).contains("{{FORM_SUMMARY}}");
        assertThat(content).contains("UNUSABLE_IMAGE:");
    }

    private static String read(String filename) throws IOException {
        return new ClassPathResource("prompts/" + filename).getContentAsString(StandardCharsets.UTF_8);
    }

    private static int occurrences(String haystack, String needle) {
        int count = 0;
        Matcher matcher = Pattern.compile(Pattern.quote(needle)).matcher(haystack);
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
