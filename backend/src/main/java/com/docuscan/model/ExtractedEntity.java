package com.docuscan.model;

/**
 * Represents a single extracted entity from the document.
 * Example: { type: "AMOUNT", value: "$45,000.00", label: "Amount" }
 */
public class ExtractedEntity {
    private String type;    // Category: DATE, AMOUNT, SIGNATORY, etc.
    private String value;   // The actual extracted text
    private String label;   // Human-friendly label for display

    public ExtractedEntity() {}

    public ExtractedEntity(String type, String value, String label) {
        this.type = type;
        this.value = value;
        this.label = label;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
