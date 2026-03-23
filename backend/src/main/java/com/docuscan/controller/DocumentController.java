package com.docuscan.controller;

import com.docuscan.model.DocumentResult;
import com.docuscan.model.ExtractedEntity;
import com.docuscan.service.EntityExtractionService;
import com.docuscan.service.OcrService;
import com.docuscan.service.PdfService;
import com.docuscan.service.SummaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final OcrService ocrService;
    private final PdfService pdfService;
    private final EntityExtractionService entityExtractionService;
    private final SummaryService summaryService;

    private static final String UPLOAD_DIR = "uploads";

    public DocumentController(OcrService ocrService, PdfService pdfService,
                              EntityExtractionService entityExtractionService,
                              SummaryService summaryService) {
        this.ocrService = ocrService;
        this.pdfService = pdfService;
        this.entityExtractionService = entityExtractionService;
        this.summaryService = summaryService;
        new File(UPLOAD_DIR).mkdirs();
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(processSingleFile(file));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error processing document: " + e.getMessage()));
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<?> uploadMultipleDocuments(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "No files provided"));
        }

        List<DocumentResult> results = new ArrayList<>();
        List<Map<String, String>> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                results.add(processSingleFile(file));
            } catch (Exception e) {
                Map<String, String> error = new HashMap<>();
                error.put("fileName", file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename());
                error.put("message", e.getMessage());
                errors.add(error);
            }
        }

        if (results.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process all uploaded files",
                    "errors", errors
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("results", results);
        response.put("errors", errors);
        response.put("totalFiles", files.length);
        response.put("processedFiles", results.size());
        return ResponseEntity.ok(response);
    }

    private DocumentResult processSingleFile(MultipartFile file) throws Exception {
        long startTime = System.currentTimeMillis();

        // ── Step 1: Validate ──
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("No file provided");
        }

        String lowerName = originalFilename.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".png") &&
                !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
            throw new IllegalArgumentException("Unsupported format. Please upload PDF, PNG, or JPG.");
        }

        System.out.println("\n══════════════════════════════════════════");
        System.out.println("📄 Processing: " + originalFilename);
        System.out.println("══════════════════════════════════════════");

        // ── Step 2: Save uploaded file ──
        String fileId = UUID.randomUUID().toString();
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String savedFileName = fileId + extension;
        Path savedPath = Paths.get(UPLOAD_DIR, savedFileName);
        file.transferTo(savedPath.toAbsolutePath().toFile());
        System.out.println("💾 File saved: " + savedPath);

        // ── Step 3: Get images for OCR ──
        List<File> imageFiles = new ArrayList<>();
        List<String> pageImageUrls = new ArrayList<>();

        if (lowerName.endsWith(".pdf")) {
            // PDF: Convert ALL pages to images
            System.out.println("📑 Converting PDF pages to images...");
            imageFiles = pdfService.convertAllPagesToImages(
                    savedPath.toFile(), UPLOAD_DIR, fileId);

            for (File img : imageFiles) {
                pageImageUrls.add("/uploads/" + img.getName());
            }
            System.out.println("🖼️ Converted " + imageFiles.size() + " page(s)");
        } else {
            // Single image
            imageFiles.add(savedPath.toFile());
            pageImageUrls.add("/uploads/" + savedFileName);
        }

        // ── Step 4: OCR ALL pages ──
        System.out.println("🔍 Running OCR on " + imageFiles.size() + " page(s)...");
        StringBuilder allText = new StringBuilder();
        List<String> pageTexts = new ArrayList<>();

        for (int i = 0; i < imageFiles.size(); i++) {
            System.out.println("   📖 OCR page " + (i + 1) + "/" + imageFiles.size());
            String pageText = ocrService.extractText(imageFiles.get(i));
            pageTexts.add(pageText == null ? "" : pageText.trim());

            if (imageFiles.size() > 1) {
                allText.append("\n═══ PAGE ").append(i + 1).append(" ═══\n\n");
            }
            allText.append(pageText).append("\n");
        }

        String extractedText = allText.toString().trim();
        System.out.println("📝 Total extracted: " + extractedText.length() + " characters from "
                + imageFiles.size() + " page(s)");

        // ── Step 5: Extract entities ──
        System.out.println("🏷️ Extracting entities...");
        List<ExtractedEntity> entities = entityExtractionService.extractEntities(extractedText);
        System.out.println("🔑 Found " + entities.size() + " entities");

        // ── Step 6: Generate summary ──
        System.out.println("🧠 Generating summary...");
        String summary = summaryService.generateSummary(extractedText);

        // ── Step 7: Build response ──
        long processingTime = System.currentTimeMillis() - startTime;

        DocumentResult result = new DocumentResult();
        result.setExtractedText(extractedText);
        result.setPageTexts(pageTexts);
        result.setEntities(entities);
        result.setSummary(summary);
        result.setImageUrl(pageImageUrls.get(0));       // First page
        result.setPageImageUrls(pageImageUrls);          // ALL pages
        result.setPageCount(imageFiles.size());
        result.setFileName(originalFilename);
        result.setProcessingTimeMs(processingTime);

        System.out.println("══════════════════════════════════════════");
        System.out.println("✅ Done in " + processingTime + "ms");
        System.out.println("   Pages: " + imageFiles.size());
        System.out.println("   Characters: " + extractedText.length());
        System.out.println("   Entities: " + entities.size());
        System.out.println("══════════════════════════════════════════\n");

        return result;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "running",
                "service", "DocuScan API"
        ));
    }
}
