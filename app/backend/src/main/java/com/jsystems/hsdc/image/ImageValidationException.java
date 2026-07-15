package com.jsystems.hsdc.image;

/**
 * Thrown when an uploaded image fails validation: an unsupported content
 * type (only JPEG and PNG are accepted, AC-05), a file larger than 5 MB
 * (AC-06), or image bytes that are corrupt/undecodable (ADR-001 §8
 * "Image format/size" and "Compression behavior" scenarios). The message is
 * English and safe to show to the user (AC-05/AC-06 require it to name the
 * allowed formats or the 5 MB limit).
 */
public class ImageValidationException extends RuntimeException {

    public ImageValidationException(String message) {
        super(message);
    }

    public ImageValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
