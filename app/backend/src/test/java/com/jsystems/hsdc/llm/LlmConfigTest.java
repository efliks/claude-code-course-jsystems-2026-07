package com.jsystems.hsdc.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.openai.client.OpenAIClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Covers TAC-002-03: with env vars unset, effective config equals the
 * documented defaults (base URL, both model IDs); with env vars set,
 * overrides win. Uses {@link ApplicationContextRunner} rather than a full
 * {@code @SpringBootTest} so it stays a narrow configuration test — no
 * datasource, no policy files, no network. {@code OPENROUTER_API_KEY} has
 * no default (required), so every run supplies a harmless test value
 * explicitly; the real course-VM env var is never read here.
 */
class LlmConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(LlmConfig.class);

    @Test
    void defaultsApplyWhenOptionalEnvVarsAreUnset() {
        contextRunner.withPropertyValues("OPENROUTER_API_KEY=test-key-not-real").run(context -> {
            assertThat(context).hasSingleBean(LlmProperties.class);
            LlmProperties properties = context.getBean(LlmProperties.class);

            assertThat(properties.apiKey()).isEqualTo("test-key-not-real");
            assertThat(properties.baseUrl()).isEqualTo(LlmProperties.DEFAULT_BASE_URL);
            assertThat(properties.visionModel()).isEqualTo(LlmProperties.DEFAULT_VISION_MODEL);
            assertThat(properties.decisionModel()).isEqualTo(LlmProperties.DEFAULT_DECISION_MODEL);

            assertThat(context).hasSingleBean(OpenAIClient.class);
        });
    }

    @Test
    void envVarsOverrideDefaults() {
        contextRunner
                .withPropertyValues(
                        "OPENROUTER_API_KEY=test-key-not-real",
                        "OPENROUTER_BASE_URL=https://example.test/v1",
                        "HSDC_VISION_MODEL=custom/vision-model",
                        "HSDC_DECISION_MODEL=custom/decision-model")
                .run(context -> {
                    LlmProperties properties = context.getBean(LlmProperties.class);

                    assertThat(properties.baseUrl()).isEqualTo("https://example.test/v1");
                    assertThat(properties.visionModel()).isEqualTo("custom/vision-model");
                    assertThat(properties.decisionModel()).isEqualTo("custom/decision-model");
                });
    }
}
