package com.docuscan.service;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * OcrService: Extracts text from images using Tesseract OCR.
 *
 * NOW with image preprocessing for better handwriting recognition.
 * The flow is: raw image → preprocess → OCR → text
 */
@Service
public class OcrService {

    private final ImagePreprocessor preprocessor;
    private Tesseract tesseract;
    private boolean tess4jAvailable = false;

    public OcrService(ImagePreprocessor preprocessor) {
        this.preprocessor = preprocessor;

        try {
            tesseract = new Tesseract();
            String datapath = findTessdataPath();
            tesseract.setDatapath(datapath);
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(3);   // Fully automatic page segmentation
            tesseract.setOcrEngineMode(1); // LSTM only — BETTER for handwriting

            // Additional Tesseract variables for better handwriting recognition
            tesseract.setVariable("textord_heavy_nr", "1");           // Handle noisy text
            tesseract.setVariable("edges_max_children_per_outline", "40"); // Complex character edges

            tess4jAvailable = true;
            System.out.println("✅ Tess4J initialized (LSTM mode) with datapath: " + datapath);
        } catch (Exception e) {
            System.err.println("⚠️ Tess4J init failed: " + e.getMessage());
            System.out.println("📌 Will use Tesseract CLI fallback");
            tess4jAvailable = false;
        }
    }

    /**
     * Main method: preprocesses image then extracts text.
     *
     * @param imageFile  The image to OCR
     * @param usePreprocessing  Whether to preprocess (true for scans/handwriting)
     */
    public String extractText(File imageFile, boolean usePreprocessing) throws Exception {
        File fileToOcr = imageFile;

        // Preprocess the image for better OCR accuracy
        if (usePreprocessing) {
            try {
                fileToOcr = preprocessor.preprocess(imageFile);
            } catch (Exception e) {
                System.err.println("⚠️ Preprocessing failed, using original: " + e.getMessage());
                fileToOcr = imageFile;
            }
        }

        String result = performOcr(fileToOcr);

        // If preprocessing produced poor results, try without it
        if (usePreprocessing && result.trim().length() < 20) {
            System.out.println("🔄 Preprocessed result too short, trying original image...");
            String originalResult = performOcr(imageFile);
            if (originalResult.trim().length() > result.trim().length()) {
                System.out.println("   ↳ Original image gave better results");
                return originalResult;
            }
        }

        return result;
    }

    /**
     * Backward-compatible method (always preprocesses).
     */
    public String extractText(File imageFile) throws Exception {
        return extractText(imageFile, true);
    }

    /**
     * Performs actual OCR using Tess4J or CLI fallback.
     */
    private String performOcr(File imageFile) throws Exception {
        if (tess4jAvailable) {
            try {
                String result = tesseract.doOCR(imageFile);
                System.out.println("✅ OCR completed via Tess4J (" + result.trim().length() + " chars)");
                return result;
            } catch (Exception e) {
                System.err.println("⚠️ Tess4J OCR failed, falling back to CLI: " + e.getMessage());
                tess4jAvailable = false;
            }
        }
        return extractWithCli(imageFile);
    }

    /**
     * Fallback: calls the tesseract command-line tool directly.
     */
    private String extractWithCli(File imageFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "tesseract", imageFile.getAbsolutePath(), "stdout",
                "-l", "eng",
                "--oem", "1",     // LSTM engine — better for handwriting
                "--psm", "3"      // Fully automatic page segmentation
        );
        pb.redirectErrorStream(false);
        Process process = pb.start();

        String output;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            try (BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String error = errReader.lines().collect(Collectors.joining("\n"));
                throw new RuntimeException("Tesseract CLI failed: " + error);
            }
        }

        System.out.println("✅ OCR completed via CLI (" + output.trim().length() + " chars)");
        return output;
    }

    /**
     * Searches common Linux paths for the Tesseract tessdata directory.
     */
    private String findTessdataPath() {
        String[] paths = {
                "/usr/share/tesseract/tessdata",
                "/usr/share/tessdata",
                "/usr/local/share/tesseract/tessdata",
                "/usr/local/share/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata"
        };

        for (String path : paths) {
            if (new File(path, "eng.traineddata").exists()) {
                return path;
            }
        }

        return "/usr/share/tesseract/tessdata";
    }
}
