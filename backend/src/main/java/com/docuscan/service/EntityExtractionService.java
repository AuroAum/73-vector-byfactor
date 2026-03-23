package com.docuscan.service;

import com.docuscan.model.ExtractedEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * EntityExtractionService: Scans OCR text and extracts key data points.
 *
 * Uses regular expressions (regex) to find patterns like:
 * - Dates in various formats
 * - Money amounts ($, USD, EUR, INR)
 * - Invoice/bill numbers
 * - Signatories and contact names
 * - Email addresses and phone numbers
 *
 * This is faster and more reliable than ML for structured documents
 * like invoices and contracts.
 */
@Service
public class EntityExtractionService {

    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern NON_WORD_RATIO = Pattern.compile("[^A-Za-z0-9\\s@./:+()\\-#]");
    private static final Set<String> SIGNATORY_STOPWORDS = new HashSet<>(Arrays.asList(
            "invoice", "total", "amount", "due", "date", "receipt", "balance", "tax", "net", "page"
    ));

    // ──── DATE PATTERNS ────
    private static final Pattern[] DATE_PATTERNS = {
            // MM/DD/YYYY or DD-MM-YYYY
            Pattern.compile("\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b"),
            // YYYY-MM-DD (ISO format)
            Pattern.compile("\\b(\\d{4}[/-]\\d{1,2}[/-]\\d{1,2})\\b"),
            // January 15, 2024
            Pattern.compile("\\b((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2},?\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE),
            // 15 January 2024
            Pattern.compile("\\b(\\d{1,2}\\s+(?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE),
            // Jan 15, 2024
            Pattern.compile("\\b((?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\.?\\s+\\d{1,2},?\\s+\\d{4})\\b", Pattern.CASE_INSENSITIVE)
    };

    // ──── MONEY/AMOUNT PATTERNS ────
    private static final Pattern[] AMOUNT_PATTERNS = {
            // $1,234.56
            Pattern.compile("\\$[\\d,]+\\.?\\d*"),
            // USD 1,234.56 or EUR 500
            Pattern.compile("(?:USD|EUR|GBP|INR)\\s*[\\d,]+\\.?\\d*", Pattern.CASE_INSENSITIVE),
            // Rs. 50,000 or ₹50,000
            Pattern.compile("(?:Rs\\.?|\\u20B9)\\s*[\\d,]+\\.?\\d*")
    };

    // ──── TOTAL AMOUNT (contextual) ────
    private static final Pattern TOTAL_PATTERN = Pattern.compile(
            "(?:Total|Grand\\s+Total|Sub\\s*total|Amount\\s+Due|Balance\\s+Due|Net\\s+Amount)\\s*:?\\s*([\\$\\u20AC\\u00A3\\u20B9]?\\s*[\\d,]+\\.?\\d*)",
            Pattern.CASE_INSENSITIVE
    );

    // ──── DUE DATE (contextual) ────
    private static final Pattern DUE_DATE_PATTERN = Pattern.compile(
            "(?:Due\\s+Date|Payment\\s+Due|Due\\s+By|Deadline|Expiry\\s+Date|Expiration)\\s*:?\\s*(.+?)(?:\\n|$)",
            Pattern.CASE_INSENSITIVE
    );

    // ──── INVOICE/BILL NUMBER ────
    private static final Pattern INVOICE_PATTERN = Pattern.compile(
            "(?:Invoice|Inv|Bill|Receipt|Reference|Ref)\\s*(?:#|No\\.?|Number)?\\s*:?\\s*([A-Za-z0-9][A-Za-z0-9\\-\\/]+)",
            Pattern.CASE_INSENSITIVE
    );

    // ──── SIGNATORIES ────
    private static final Pattern[] SIGNATORY_PATTERNS = {
            Pattern.compile("(?:Signed\\s+by|Signatory|Authorized\\s+by|Approved\\s+by)\\s*:?\\s*(.+?)(?:\\n|$)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:Prepared\\s+by|Contact\\s+Person)\\s*:?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)", Pattern.CASE_INSENSITIVE)
    };

    // ──── EMAIL ────
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    // ──── PHONE ────
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?:\\+?\\d{1,3}[\\-.\\s]?)?(?:\\(?\\d{2,4}\\)?[\\-.\\s]?)?\\d{3,4}[\\-.\\s]?\\d{4}"
    );

    /**
     * Main method: scans the full OCR text and returns all found entities.
     */
    public List<ExtractedEntity> extractEntities(String text) {
        List<ExtractedEntity> entities = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return entities;
        }

        // Extract dates
        for (Pattern p : DATE_PATTERNS) {
            extractMatches(p, text, "DATE", "Date", entities);
        }

        // Extract money amounts
        for (Pattern p : AMOUNT_PATTERNS) {
            extractMatches(p, text, "AMOUNT", "Amount", entities);
        }

        // Extract total amounts (contextual — near "Total:" keyword)
        Matcher totalMatcher = TOTAL_PATTERN.matcher(text);
        while (totalMatcher.find()) {
            String amount = totalMatcher.group(1);
            addIfValid("TOTAL_AMOUNT", "Total Amount", amount, text, entities);
        }

        // Extract due dates (contextual)
        Matcher dueDateMatcher = DUE_DATE_PATTERN.matcher(text);
        while (dueDateMatcher.find()) {
            addIfValid("DUE_DATE", "Due Date", dueDateMatcher.group(1), text, entities);
        }

        // Extract invoice numbers
        Matcher invoiceMatcher = INVOICE_PATTERN.matcher(text);
        while (invoiceMatcher.find()) {
            addIfValid("INVOICE_NUMBER", "Invoice/Ref Number", invoiceMatcher.group(1), text, entities);
        }

        // Extract signatories
        for (Pattern p : SIGNATORY_PATTERNS) {
            Matcher m = p.matcher(text);
            while (m.find()) {
                addIfValid("SIGNATORY", "Signatory", m.group(1), text, entities);
            }
        }

        // Extract emails
        extractMatches(EMAIL_PATTERN, text, "EMAIL", "Email", entities);

        // Extract phone numbers
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            addIfValid("PHONE", "Phone Number", phoneMatcher.group(), text, entities);
        }

        // Remove duplicates (same type + value = duplicate)
        return removeDuplicates(entities);
    }

    private void extractMatches(Pattern pattern, String text, String type, String label,
                                 List<ExtractedEntity> entities) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            addIfValid(type, label, matcher.group(), text, entities);
        }
    }

    private void addIfValid(String type, String label, String rawValue, String fullText,
                            List<ExtractedEntity> entities) {
        if (rawValue == null) {
            return;
        }

        String value = normalize(rawValue);
        if (value.isEmpty()) {
            return;
        }

        if (!existsInDocument(fullText, value)) {
            return;
        }

        if (!passesTypeValidation(type, value)) {
            return;
        }

        entities.add(new ExtractedEntity(type, value, label));
    }

    private String normalize(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private boolean existsInDocument(String fullText, String value) {
        String normalizedDoc = normalize(fullText).toLowerCase();
        String normalizedValue = normalize(value).toLowerCase();
        return normalizedDoc.contains(normalizedValue);
    }

    private boolean passesTypeValidation(String type, String value) {
        if (value.length() < 2 || value.length() > 80) {
            return false;
        }

        int nonWordCount = 0;
        Matcher noiseMatcher = NON_WORD_RATIO.matcher(value);
        while (noiseMatcher.find()) {
            nonWordCount++;
        }
        if (nonWordCount > value.length() / 3) {
            return false;
        }

        switch (type) {
            case "DATE":
            case "DUE_DATE":
                return isLikelyDate(value);
            case "AMOUNT":
            case "TOTAL_AMOUNT":
                return HAS_DIGIT.matcher(value).matches() && value.length() <= 30;
            case "INVOICE_NUMBER":
                return HAS_DIGIT.matcher(value).matches() && value.length() >= 3 && value.length() <= 40;
            case "SIGNATORY":
                return isLikelyPersonName(value);
            case "EMAIL":
                return value.contains("@") && value.contains(".");
            case "PHONE":
                return isLikelyPhone(value);
            default:
                return true;
        }
    }

    private boolean isLikelyDate(String value) {
        String lower = value.toLowerCase();
        boolean hasMonthWord = lower.matches(".*\\b(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec|january|february|march|april|june|july|august|september|october|november|december)\\b.*");
        boolean hasSlashDate = value.matches(".*\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}.*");
        return hasMonthWord || hasSlashDate;
    }

    private boolean isLikelyPersonName(String value) {
        if (HAS_DIGIT.matcher(value).matches()) {
            return false;
        }

        String[] tokens = value.split("\\s+");
        if (tokens.length < 2 || tokens.length > 5) {
            return false;
        }

        int longAlphaTokens = 0;
        for (String token : tokens) {
            String cleaned = token.replaceAll("[^A-Za-z]", "");
            if (SIGNATORY_STOPWORDS.contains(cleaned.toLowerCase())) {
                return false;
            }
            if (cleaned.length() >= 2) {
                longAlphaTokens++;
            }
        }
        return longAlphaTokens >= 2;
    }

    private boolean isLikelyPhone(String value) {
        String digitsOnly = value.replaceAll("\\D", "");
        return digitsOnly.length() >= 7 && digitsOnly.length() <= 15;
    }

    private List<ExtractedEntity> removeDuplicates(List<ExtractedEntity> entities) {
        List<ExtractedEntity> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ExtractedEntity e : entities) {
            String key = e.getType() + ":" + e.getValue();
            if (seen.add(key)) {
                unique.add(e);
            }
        }
        return unique;
    }
}
