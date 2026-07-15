package com.jsystems.hsdc.llm;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the single OpenRouter-configured OpenAI SDK client (ADR-002 §3
 * "Client configuration").
 *
 * <p>The client is built explicitly via the builder with an explicit
 * {@code baseUrl} and {@code apiKey} — the SDK's own {@code OPENAI_API_KEY}
 * / {@code OPENAI_BASE_URL} environment auto-detection is deliberately not
 * relied on, to avoid surprising precedence on shared course VMs (ADR-002
 * §3, ADR-000 §7). Request timeout 120 s, SDK {@code maxRetries} 2 for
 * transient failures (ADR-002 §6 "Timeouts, retries, and failure
 * semantics").
 *
 * <p>{@link LlmProperties} is exposed as its own bean (rather than folded
 * into the client bean method) so {@code OPENROUTER_BASE_URL} /
 * {@code HSDC_VISION_MODEL} / {@code HSDC_DECISION_MODEL} resolution can be
 * asserted in isolation without constructing the SDK client (TAC-002-03).
 */
@Configuration
public class LlmConfig {

    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(120);
    static final int MAX_RETRIES = 2;

    @Bean
    public LlmProperties llmProperties(
            @Value("${OPENROUTER_API_KEY}") String apiKey,
            @Value("${OPENROUTER_BASE_URL:" + LlmProperties.DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${HSDC_VISION_MODEL:" + LlmProperties.DEFAULT_VISION_MODEL + "}") String visionModel,
            @Value("${HSDC_DECISION_MODEL:" + LlmProperties.DEFAULT_DECISION_MODEL + "}") String decisionModel) {
        return new LlmProperties(apiKey, baseUrl, visionModel, decisionModel);
    }

    @Bean
    public OpenAIClient openAiClient(LlmProperties properties) {
        return OpenAIOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .baseUrl(properties.baseUrl())
                .timeout(REQUEST_TIMEOUT)
                .maxRetries(MAX_RETRIES)
                .build();
    }
}
