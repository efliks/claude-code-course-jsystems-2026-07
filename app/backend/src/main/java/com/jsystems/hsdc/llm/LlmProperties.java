package com.jsystems.hsdc.llm;

/**
 * Resolved LLM configuration (ADR-002 §3 "Client configuration", ADR-000 §7
 * env table). Built once from environment-backed {@code @Value} properties
 * in {@link LlmConfig} and shared by the {@link LlmService} and the SDK
 * client bean.
 *
 * <p>A plain record (not a Spring-managed {@code @ConfigurationProperties}
 * type) so tests can construct it directly without a Spring context, the
 * same pattern already used by {@code PolicyService} and
 * {@code DataSourceConfig} in this codebase.
 *
 * <p>{@code apiKey} must never be logged, printed, or otherwise exposed
 * (TAC-002-04).
 *
 * @param apiKey        OpenRouter API key ({@code OPENROUTER_API_KEY})
 * @param baseUrl       OpenRouter API base URL ({@code OPENROUTER_BASE_URL})
 * @param visionModel   multimodal model id for image analysis ({@code HSDC_VISION_MODEL})
 * @param decisionModel reasoning model id for decisions + chat ({@code HSDC_DECISION_MODEL})
 */
public record LlmProperties(String apiKey, String baseUrl, String visionModel, String decisionModel) {

    /** Default OpenRouter API base URL (ADR-000 §7). */
    public static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";

    /**
     * Default vision model id. Confirmed against the live OpenRouter
     * catalog and the {@code ChatModel} enum baked into
     * {@code openai-java-core:4.43.0} at implementation time (2026-07-15);
     * the originally planned {@code openai/gpt-4o-mini} is no longer
     * offered (ADR-002 "Model defaults" decision record).
     */
    public static final String DEFAULT_VISION_MODEL = "openai/gpt-5.6-luna";

    /**
     * Default decision/chat model id. Same validation as
     * {@link #DEFAULT_VISION_MODEL}; replaces the retired
     * {@code openai/gpt-4o}.
     */
    public static final String DEFAULT_DECISION_MODEL = "openai/gpt-5.6-terra";
}
