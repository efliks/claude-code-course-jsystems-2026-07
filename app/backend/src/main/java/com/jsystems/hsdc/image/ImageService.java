package com.jsystems.hsdc.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates and compresses uploaded case images (ADR-001 §3 "ImageService",
 * §6 "Image compression target").
 *
 * <p>{@link #validate(MultipartFile)} rejects anything other than a
 * decodable JPEG/PNG image (checked from the actual bytes via
 * {@link ImageIO}, not the declared content type or file extension) and
 * anything larger than 5 MB (AC-05, AC-06).
 *
 * <p>{@link #compress(MultipartFile)} downscales so the longest edge is at
 * most {@value #MAX_LONGEST_EDGE_PX} px (images already within that bound
 * keep their original size) and always re-encodes the result as JPEG at
 * quality {@value #JPEG_OUTPUT_QUALITY}, so the payload forwarded to the
 * vision LLM is smaller than the original upload for images above the
 * compression threshold (AC-09).
 */
@Service
public class ImageService {

    static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;
    static final int MAX_LONGEST_EDGE_PX = 1568;
    static final float JPEG_OUTPUT_QUALITY = 0.8f;

    private static final Set<String> ALLOWED_FORMATS = Set.of("JPEG", "PNG");

    /**
     * Rejects the file if it is missing/empty, larger than 5 MB (AC-06), or
     * not a decodable JPEG/PNG image (AC-05). Throws
     * {@link ImageValidationException} with an English message naming the
     * allowed formats or the 5 MB limit; returns normally otherwise.
     */
    public void validate(MultipartFile file) {
        byte[] bytes = readBytes(file);
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new ImageValidationException("Image exceeds the maximum allowed size of 5 MB.");
        }
        decode(bytes);
    }

    /**
     * Downscales the image so its longest edge is at most
     * {@value #MAX_LONGEST_EDGE_PX} px and re-encodes it as JPEG at quality
     * {@value #JPEG_OUTPUT_QUALITY}. The output is always valid JPEG bytes.
     * Does not re-check the 5 MB limit; call {@link #validate} first.
     */
    public byte[] compress(MultipartFile file) {
        byte[] input = readBytes(file);
        DecodedImage decoded = decode(input);
        return compress(decoded.image());
    }

    private byte[] compress(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int longestEdge = Math.max(width, height);

        int targetWidth = width;
        int targetHeight = height;
        if (longestEdge > MAX_LONGEST_EDGE_PX) {
            double scale = (double) MAX_LONGEST_EDGE_PX / longestEdge;
            targetWidth = Math.max(1, (int) Math.round(width * scale));
            targetHeight = Math.max(1, (int) Math.round(height * scale));
        }

        BufferedImage flattened = flattenToRgb(image);

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(flattened)
                    .size(targetWidth, targetHeight)
                    .outputFormat("jpg")
                    .outputQuality(JPEG_OUTPUT_QUALITY)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ImageValidationException("Failed to compress the uploaded image.", e);
        }
    }

    /**
     * Draws the image onto a fresh RGB (no alpha) canvas with a white
     * background so that indexed/alpha PNG input encodes cleanly to JPEG,
     * which has no alpha channel.
     */
    private BufferedImage flattenToRgb(BufferedImage image) {
        BufferedImage flattened =
                new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = flattened.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return flattened;
    }

    private byte[] readBytes(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageValidationException("No image file was provided.");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ImageValidationException("Failed to read the uploaded file.", e);
        }
    }

    /**
     * Identifies the actual image format from the file bytes (not the
     * declared content type/extension), rejects anything other than
     * JPEG/PNG, and fully decodes the image to catch corrupt data that only
     * has a valid-looking header.
     */
    private DecodedImage decode(byte[] bytes) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (iis == null) {
                throw undecodable(null);
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw undecodable(null);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                String formatName = reader.getFormatName().toUpperCase(Locale.ROOT);
                if (!ALLOWED_FORMATS.contains(formatName)) {
                    throw new ImageValidationException(
                            "Unsupported image format: only JPEG and PNG images are allowed.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw undecodable(null);
                }
                return new DecodedImage(formatName, image);
            } catch (IOException e) {
                throw undecodable(e);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw undecodable(e);
        }
    }

    private ImageValidationException undecodable(Exception cause) {
        return cause == null
                ? new ImageValidationException("The uploaded file is not a readable image.")
                : new ImageValidationException("The uploaded file is not a readable image.", cause);
    }

    private record DecodedImage(String format, BufferedImage image) {}
}
