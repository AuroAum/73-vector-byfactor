package com.docuscan.model;

import java.util.List;

public class DocumentResult {
    private String extractedText;
    private List<String> pageTexts;         // OCR text per page, aligned with pageImageUrls
    private List<ExtractedEntity> entities;
    private String summary;
    private String imageUrl;                // First page image (backward compat)
    private List<String> pageImageUrls;     // ALL page images
    private int pageCount;                  // Total number of pages
    private String fileName;
    private long processingTimeMs;

    public DocumentResult() {}

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public List<String> getPageTexts() { return pageTexts; }
    public void setPageTexts(List<String> pageTexts) { this.pageTexts = pageTexts; }

    public List<ExtractedEntity> getEntities() { return entities; }
    public void setEntities(List<ExtractedEntity> entities) { this.entities = entities; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getPageImageUrls() { return pageImageUrls; }
    public void setPageImageUrls(List<String> pageImageUrls) { this.pageImageUrls = pageImageUrls; }

    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { this.processingTimeMs = processingTimeMs; }
}
