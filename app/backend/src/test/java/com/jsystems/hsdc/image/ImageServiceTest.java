package com.jsystems.hsdc.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * ADR-001 §8 "Image format/size" and "Compression behavior" scenarios for
 * {@link ImageService} (AC-05, AC-06, AC-09).
 */
class ImageServiceTest {

    private static final long FIVE_MB = 5L * 1024 * 1024;

    private final ImageService imageService = new ImageService();

    // --- validate() ---------------------------------------------------

    @Test
    void validateAcceptsJpegUnderFiveMegabytes() throws IOException {
        MultipartFile file = jpegFile(800, 600);

        assertThatCode(() -> imageService.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validateAcceptsPngUnderFiveMegabytes() throws IOException {
        MultipartFile file = pngFile(800, 600);

        assertThatCode(() -> imageService.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsGifWithMessageNamingAllowedFormats() throws IOException {
        MultipartFile file = gifFile(200, 200);

        assertThatThrownBy(() -> imageService.validate(file))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("JPEG")
                .hasMessageContaining("PNG");
    }

    @Test
    void validateRejectsFileOverFiveMegabytesWithMessageNamingLimit() throws IOException {
        byte[] oversized = padJpegToSize(jpegBytes(50, 50), (int) (FIVE_MB + 512 * 1024));
        MultipartFile file = new MockMultipartFile("image", "big.jpg", "image/jpeg", oversized);
        assertThat(file.getSize()).isGreaterThan(FIVE_MB);

        assertThatThrownBy(() -> imageService.validate(file))
                .isInstanceOf(ImageValidationException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void validateAccepts4Point9MegabyteJpeg() throws IOException {
        int targetSize = (int) (4.9 * 1024 * 1024);
        byte[] bytes = padJpegToSize(jpegBytes(50, 50), targetSize);
        MultipartFile file = new MockMultipartFile("image", "large.jpg", "image/jpeg", bytes);
        assertThat(file.getSize()).isLessThan(FIVE_MB);

        assertThatCode(() -> imageService.validate(file)).doesNotThrowAnyException();
    }

    @Test
    void validateRejectsCorruptGarbageBytes() {
        byte[] garbage = new byte[2048];
        new Random(42).nextBytes(garbage);
        MultipartFile file = new MockMultipartFile("image", "garbage.jpg", "image/jpeg", garbage);

        assertThatThrownBy(() -> imageService.validate(file)).isInstanceOf(ImageValidationException.class);
    }

    @Test
    void validateRejectsMissingFile() {
        MultipartFile file = new MockMultipartFile("image", new byte[0]);

        assertThatThrownBy(() -> imageService.validate(file)).isInstanceOf(ImageValidationException.class);
    }

    // --- compress() -----------------------------------------------------

    @Test
    void compressDownscalesLargeJpegToLongestEdge1568AndShrinksPayload() throws IOException {
        MultipartFile file = jpegFile(4000, 3000);

        byte[] compressed = imageService.compress(file);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));

        assertThat(result.getWidth()).isEqualTo(1568);
        assertThat(result.getHeight()).isEqualTo(1176);
        assertThat(compressed.length).isLessThan((int) file.getSize());
        assertThat(readFormatName(compressed)).isEqualTo("JPEG");
    }

    @Test
    void compressPassesThroughSmallJpegDimensionsUnchangedStillValidJpeg() throws IOException {
        MultipartFile file = jpegFile(800, 600);

        byte[] compressed = imageService.compress(file);
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));

        assertThat(result.getWidth()).isEqualTo(800);
        assertThat(result.getHeight()).isEqualTo(600);
        assertThat(readFormatName(compressed)).isEqualTo("JPEG");
    }

    @Test
    void compressReencodesLargePngToSmallerJpeg() throws IOException {
        MultipartFile file = new MockMultipartFile(
                "image", "photo.png", "image/png", encode(perPixelNoiseImage(1600, 1200), "png"));
        assertThat(file.getSize()).isGreaterThan(1024 * 1024);

        byte[] compressed = imageService.compress(file);

        assertThat(compressed.length).isLessThan((int) file.getSize());
        assertThat(readFormatName(compressed)).isEqualTo("JPEG");
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(compressed));
        assertThat(Math.max(result.getWidth(), result.getHeight())).isLessThanOrEqualTo(1568);
    }

    @Test
    void compressRejectsCorruptInput() {
        byte[] garbage = new byte[1024];
        new Random(7).nextBytes(garbage);
        MultipartFile file = new MockMultipartFile("image", "garbage.jpg", "image/jpeg", garbage);

        assertThatThrownBy(() -> imageService.compress(file)).isInstanceOf(ImageValidationException.class);
    }

    // --- test fixtures ----------------------------------------------------

    private static MultipartFile jpegFile(int width, int height) throws IOException {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", jpegBytes(width, height));
    }

    private static MultipartFile pngFile(int width, int height) throws IOException {
        return new MockMultipartFile("image", "photo.png", "image/png", pngBytes(width, height));
    }

    private static MultipartFile gifFile(int width, int height) throws IOException {
        return new MockMultipartFile("image", "photo.gif", "image/gif", gifBytes(width, height));
    }

    private static byte[] jpegBytes(int width, int height) throws IOException {
        return encode(noisyImage(width, height), "jpg");
    }

    private static byte[] pngBytes(int width, int height) throws IOException {
        return encode(noisyImage(width, height), "png");
    }

    private static byte[] gifBytes(int width, int height) throws IOException {
        return encode(noisyImage(width, height), "gif");
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean written = ImageIO.write(image, format, out);
        if (!written) {
            throw new IOException("No writer available for format " + format);
        }
        return out.toByteArray();
    }

    /** Random-noise pixels so PNG encoding does not compress trivially small. */
    private static BufferedImage noisyImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        Random random = new Random(width * 31L + height);
        for (int y = 0; y < height; y += 4) {
            for (int x = 0; x < width; x += 4) {
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256)));
                g.fillRect(x, y, 4, 4);
            }
        }
        g.dispose();
        return image;
    }

    /** True per-pixel random noise so PNG (lossless) encoding stays incompressibly large. */
    private static BufferedImage perPixelNoiseImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(width * 31L + height);
        int[] row = new int[width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                row[x] = random.nextInt(0xFFFFFF);
            }
            image.setRGB(0, y, width, 1, row, 0, width);
        }
        return image;
    }

    private static String readFormatName(byte[] bytes) throws IOException {
        try (var iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IOException("Not a readable image");
            }
            return readers.next().getFormatName().toUpperCase(java.util.Locale.ROOT);
        }
    }

    /**
     * Pads a valid JPEG with harmless COM (comment) marker segments so the
     * total byte size reaches exactly {@code targetSize} while the file
     * stays a valid, decodable JPEG (used to build deterministic-size
     * fixtures for the 5 MB boundary tests).
     */
    private static byte[] padJpegToSize(byte[] jpeg, int targetSize) throws IOException {
        byte[] result = jpeg;
        while (result.length < targetSize) {
            int need = targetSize - result.length;
            int dataLen = Math.min(65533, Math.max(1, need - 4));
            result = insertComSegment(result, dataLen);
        }
        return result;
    }

    private static byte[] insertComSegment(byte[] jpeg, int dataLen) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2); // SOI marker (FF D8)
        out.write(0xFF);
        out.write(0xFE); // COM marker
        int segLen = dataLen + 2;
        out.write((segLen >> 8) & 0xFF);
        out.write(segLen & 0xFF);
        out.write(new byte[dataLen]);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
