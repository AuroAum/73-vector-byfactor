package com.docuscan.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * ImagePreprocessor: Enhances images BEFORE sending to Tesseract OCR.
 *
 * Why is this needed?
 * Tesseract works best on clean, high-contrast, black-text-on-white-background images.
 * Scanned documents and handwritten text are often:
 *   - Low contrast (gray text on gray background)
 *   - Noisy (specks, smudges, shadows)
 *   - Small or blurry
 *
 * Our preprocessing pipeline:
 *   1. Convert to grayscale (removes color distractions)
 *   2. Scale up small images (Tesseract needs at least 300 DPI)
 *   3. Increase contrast (makes text darker, background lighter)
 *   4. Apply adaptive thresholding (converts to pure black & white)
 *   5. Remove noise (eliminate small specks)
 *
 * This dramatically improves OCR accuracy, especially for handwriting.
 */
@Service
public class ImagePreprocessor {

    /**
     * Main method: takes an image file, preprocesses it, saves the result.
     * Returns the preprocessed file.
     */
    public File preprocess(File inputFile) throws IOException {
        System.out.println("🔧 Preprocessing image: " + inputFile.getName());

        BufferedImage original = ImageIO.read(inputFile);
        if (original == null) {
            System.err.println("⚠️ Could not read image, skipping preprocessing");
            return inputFile;
        }

        // Step 1: Scale up small images (Tesseract needs decent resolution)
        BufferedImage scaled = scaleUpIfNeeded(original);

        // Step 2: Convert to grayscale
        BufferedImage grayscale = toGrayscale(scaled);

        // Step 3: Increase contrast
        BufferedImage contrasted = enhanceContrast(grayscale, 1.8f, -80f);

        // Step 4: Apply adaptive thresholding (black & white)
        BufferedImage thresholded = adaptiveThreshold(contrasted, 15, 10);

        // Step 5: Remove small noise
        BufferedImage cleaned = removeNoise(thresholded);

        // Save preprocessed image
        String parentPath = inputFile.getParent();
        String name = inputFile.getName();
        String preprocessedName = "preprocessed_" + name;
        if (!preprocessedName.toLowerCase().endsWith(".png")) {
            preprocessedName = preprocessedName.replaceAll("\\.[^.]+$", ".png");
        }
        File outputFile = new File(parentPath, preprocessedName);
        ImageIO.write(cleaned, "png", outputFile);

        System.out.println("✅ Preprocessing complete: " + original.getWidth() + "x" + original.getHeight()
                + " → " + cleaned.getWidth() + "x" + cleaned.getHeight());

        return outputFile;
    }

    /**
     * Scale up images smaller than 2000px wide.
     * Small images produce terrible OCR results.
     */
    private BufferedImage scaleUpIfNeeded(BufferedImage img) {
        int minWidth = 2000;
        if (img.getWidth() >= minWidth) {
            return img;
        }

        double scale = (double) minWidth / img.getWidth();
        int newWidth = (int) (img.getWidth() * scale);
        int newHeight = (int) (img.getHeight() * scale);

        BufferedImage scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.drawImage(img, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        System.out.println("   📐 Scaled up: " + img.getWidth() + "→" + newWidth + "px");
        return scaled;
    }

    /**
     * Convert to grayscale — removes color information.
     * OCR doesn't need color; grayscale reduces noise.
     */
    private BufferedImage toGrayscale(BufferedImage img) {
        BufferedImage gray = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = gray.createGraphics();
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();
        return gray;
    }

    /**
     * Enhance contrast using a linear rescale operation.
     * scaleFactor > 1 increases contrast, offset adjusts brightness.
     *
     * For handwritten text: makes ink strokes darker, paper lighter.
     */
    private BufferedImage enhanceContrast(BufferedImage img, float scaleFactor, float offset) {
        BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        RescaleOp rescale = new RescaleOp(scaleFactor, offset, null);
        rescale.filter(img, result);
        return result;
    }

    /**
     * Adaptive thresholding: converts to pure black & white.
     *
     * Unlike simple thresholding (one cutoff for entire image),
     * adaptive thresholding calculates a different cutoff for each
     * local region. This handles uneven lighting (common in scans).
     *
     * blockSize: size of the local region to analyze
     * constant: adjustment value (higher = more aggressive whitening)
     */
    private BufferedImage adaptiveThreshold(BufferedImage img, int blockSize, int constant) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        // Get pixel data
        int[] pixels = new int[width * height];
        img.getRaster().getPixels(0, 0, width, height, pixels);

        int[] output = new int[width * height];

        // Compute integral image for fast local mean calculation
        long[][] integral = new long[height + 1][width + 1];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                integral[y + 1][x + 1] = pixels[y * width + x]
                        + integral[y][x + 1]
                        + integral[y + 1][x]
                        - integral[y][x];
            }
        }

        int half = blockSize / 2;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Define the local region bounds
                int x1 = Math.max(0, x - half);
                int y1 = Math.max(0, y - half);
                int x2 = Math.min(width - 1, x + half);
                int y2 = Math.min(height - 1, y + half);

                int count = (x2 - x1 + 1) * (y2 - y1 + 1);
                long sum = integral[y2 + 1][x2 + 1]
                        - integral[y1][x2 + 1]
                        - integral[y2 + 1][x1]
                        + integral[y1][x1];

                long localMean = sum / count;

                // If pixel is darker than local mean minus constant → black
                // Otherwise → white
                output[y * width + x] = (pixels[y * width + x] < localMean - constant) ? 0 : 255;
            }
        }

        result.getRaster().setPixels(0, 0, width, height, output);
        return result;
    }

    /**
     * Remove small noise specks (isolated black pixels).
     * A simple morphological operation: if a black pixel has
     * very few black neighbors, it's probably noise — make it white.
     */
    private BufferedImage removeNoise(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

        int[] pixels = new int[width * height];
        img.getRaster().getPixels(0, 0, width, height, pixels);

        int[] output = pixels.clone();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (pixels[y * width + x] == 0) { // Black pixel
                    // Count black neighbors in 3x3 area
                    int blackNeighbors = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dy == 0 && dx == 0) continue;
                            if (pixels[(y + dy) * width + (x + dx)] == 0) {
                                blackNeighbors++;
                            }
                        }
                    }
                    // If isolated (1 or fewer neighbors), it's noise
                    if (blackNeighbors <= 1) {
                        output[y * width + x] = 255;
                    }
                }
            }
        }

        result.getRaster().setPixels(0, 0, width, height, output);
        return result;
    }
}
