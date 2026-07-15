package com.jsystems.hsdc.llm;

/**
 * Result of {@link LlmService#analyzeImage}: the raw analysis text returned
 * by the vision model, plus a derived {@code usable} flag (ADR-002 §4).
 *
 * <p>{@code usable} is {@code false} whenever the analysis prompts'
 * "unusable photo rule" fired — the model is instructed (in both
 * {@code prompts/complaint-analysis.md} and {@code prompts/return-analysis.md})
 * to prefix its reply with the detectable phrase {@value #UNUSABLE_IMAGE_MARKER}
 * whenever the photo cannot support an assessment (AC-17).
 *
 * @param rawText raw analysis text returned by the vision model
 * @param usable  {@code false} when the detectable "unusable image" phrase is present
 */
public record ImageAnalysisResult(String rawText, boolean usable) {

    static final String UNUSABLE_IMAGE_MARKER = "UNUSABLE_IMAGE:";

    /** Builds a result from raw model text, deriving {@link #usable} from the marker phrase. */
    public static ImageAnalysisResult from(String rawText) {
        boolean usable = rawText == null || !rawText.contains(UNUSABLE_IMAGE_MARKER);
        return new ImageAnalysisResult(rawText, usable);
    }
}
