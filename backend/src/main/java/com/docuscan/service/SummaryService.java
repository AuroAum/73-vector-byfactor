package com.docuscan.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * SummaryService: Generates a one-paragraph executive summary using Groq AI.
 *
 * Groq provides free access to Llama 3.1 model via an OpenAI-compatible API.
 * We send the extracted text → Groq returns a concise summary.
 *
 * If Groq is unavailable (no API key, network error), we fall back to
 * a simple extractive summarizer that picks the most important sentences.
 */
@Service
public class SummaryService {

    @Value("${groq.api.key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";

    /**
     * Generates a one-paragraph summary of the document text.
     */
    public String generateSummary(String text) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("PASTE_YOUR_GROQ_API_KEY_HERE")) {
            System.out.println("⚠️ No Groq API key configured, using fallback summarizer");
            return generateFallbackSummary(text);
        }

        try {
            // Truncate if too long (LLM context limit)
            String truncatedText = text.length() > 4000 ? text.substring(0, 4000) + "..." : text;

            // Build the JSON request using Jackson (handles escaping automatically)
            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("model", MODEL);
            requestNode.put("temperature", 0.3);
            requestNode.put("max_tokens", 300);

            ArrayNode messages = requestNode.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "You are a document analysis assistant. Provide a concise one-paragraph " +
                    "executive summary of the document. Focus on: document type, parties involved, " +
                    "important dates, financial figures, and key terms or obligations. " +
                    "Keep it professional and under 100 words.");

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content",
                    "Please summarize this document:\n\n" + truncatedText);

            String requestBody = objectMapper.writeValueAsString(requestNode);

            // Make HTTP request to Groq
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GROQ_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String summary = root.path("choices").get(0)
                        .path("message").path("content").asText();
                System.out.println("✅ Summary generated via Groq LLM");
                return summary;
            } else {
                System.err.println("❌ Groq API error " + response.statusCode() +
                        ": " + response.body());
                return generateFallbackSummary(text);
            }

        } catch (Exception e) {
            System.err.println("❌ Groq API call failed: " + e.getMessage());
            return generateFallbackSummary(text);
        }
    }

    /**
     * Fallback: Simple extractive summary.
     * Picks the first few meaningful sentences from the document.
     * Not as good as LLM, but works offline.
     */
    private String generateFallbackSummary(String text) {
        String[] sentences = text.split("[.!?]+");
        StringBuilder summary = new StringBuilder();
        int count = 0;

        for (String sentence : sentences) {
            String trimmed = sentence.trim().replaceAll("\\s+", " ");
            if (trimmed.length() > 20) {
                summary.append(trimmed).append(". ");
                count++;
                if (count >= 5) break;
            }
        }

        if (summary.isEmpty()) {
            return "Document text was extracted successfully. Please review the extracted " +
                   "text and entities above for key information.";
        }

        return summary.toString().trim();
    }
}
