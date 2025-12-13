package bbq.excon.exconversationbackend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class GeminiService {
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String modelName;
    
    @Value("${gemini.api.max-tokens:8192}")
    private int maxTokens;
    
    @Value("${gemini.api.temperature:0.1}")
    private float temperature;
    
    @Value("${gemini.api.rate-limit-per-minute:60}")
    private int rateLimitPerMinute;
    
    @Value("${gemini.api.retry-max-attempts:3}")
    private int maxRetries;
    
    @Value("${gemini.api.retry-delay-ms:2000}")
    private long retryDelayMs;
    
    @Value("${gemini.api.request-delay-ms:8000}")
    private long requestDelayMs;
    
    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    
    private RestTemplate restTemplate;
    private Semaphore rateLimiter;
    private long lastRequestTime = 0;
    
    @org.springframework.beans.factory.annotation.Autowired
    public void init() {
        this.restTemplate = new RestTemplate();
        // Initialize rate limiter after @Value injection
        // Free tier: 10 requests/min, set to 8 for safety
        this.rateLimiter = new Semaphore(rateLimitPerMinute, true);
    }
    
    /**
     * Analyze text chunk to detect questions, answers, and chapters
     * Returns JSON structure with detected elements
     */
    @Cacheable(value = "geminiResponses", key = "#text.hashCode()")
    public String analyzeTextChunk(String text) {
        String prompt = buildAnalysisPrompt(text);
        
        return executeWithRateLimit(() -> {
            String url = String.format(GEMINI_API_URL, modelName, apiKey);
            
            // Build request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", prompt);
            
            List<Map<String, Object>> parts = List.of(part);
            content.put("parts", parts);
            
            List<Map<String, Object>> contents = List.of(content);
            requestBody.put("contents", contents);
            
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("maxOutputTokens", maxTokens);
            generationConfig.put("temperature", temperature);
            requestBody.put("generationConfig", generationConfig);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Make API call
            @SuppressWarnings({"unchecked", "rawtypes"})
            ResponseEntity<Map> response;
            try {
                response = restTemplate.postForEntity(url, request, Map.class);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // Log error response body
                String errorBody = e.getResponseBodyAsString();
                System.err.println("Gemini API HTTP Error (" + e.getStatusCode() + "): " + errorBody);
                // Re-throw to be handled by executeWithRateLimit
                throw e;
            } catch (org.springframework.web.client.RestClientException e) {
                System.err.println("Gemini API RestClientException: " + e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
            }
            
            // Extract response text
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = (Map<String, Object>) response.getBody();
                
                // Check for error in response
                if (body.containsKey("error")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> error = (Map<String, Object>) body.get("error");
                    String errorMessage = error != null ? (String) error.get("message") : "Unknown error";
                    throw new RuntimeException("Gemini API error: " + errorMessage);
                }
                
                if (body != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> candidates = (List<Map<String, Object>>) body.get("candidates");
                    if (candidates != null && !candidates.isEmpty()) {
                        Map<String, Object> candidate = candidates.get(0);
                        
                        // Check finishReason
                        String finishReason = (String) candidate.get("finishReason");
                        if (finishReason != null && !finishReason.equals("STOP")) {
                            String reason = finishReason;
                            String safetyRatings = "";
                            if (candidate.containsKey("safetyRatings")) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> ratings = (List<Map<String, Object>>) candidate.get("safetyRatings");
                                if (ratings != null && !ratings.isEmpty()) {
                                    safetyRatings = " Safety ratings: " + ratings.toString();
                                }
                            }
                            throw new RuntimeException("Gemini API finish reason: " + reason + safetyRatings);
                        }
                        
                        @SuppressWarnings("unchecked")
                        Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                        if (contentMap != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> partsList = (List<Map<String, Object>>) contentMap.get("parts");
                            if (partsList != null && !partsList.isEmpty()) {
                                Object textObj = partsList.get(0).get("text");
                                if (textObj != null) {
                                    String responseText = textObj.toString();
                                    // Extract JSON from markdown code blocks if present
                                    return extractJsonFromResponse(responseText);
                                } else {
                                    System.err.println("Warning: Response has parts but no text field. Parts: " + partsList);
                                }
                            } else {
                                System.err.println("Warning: Response has content but no parts. Content: " + contentMap);
                            }
                        } else {
                            System.err.println("Warning: Response has candidate but no content. Candidate: " + candidate);
                        }
                    } else {
                        System.err.println("Warning: Response has no candidates. Body: " + body);
                    }
                }
            } else {
                System.err.println("Warning: Invalid response status or body. Status: " + 
                    (response != null ? response.getStatusCode() : "null") + 
                    ", Body: " + (response != null ? response.getBody() : "null"));
            }
            
            // Log full response for debugging
            System.err.println("Full Gemini API response: " + (response != null ? response.getBody() : "null"));
            throw new RuntimeException("Invalid response from Gemini API - check logs for details");
        });
    }
    
    /**
     * Batch analyze multiple text chunks
     * Optimized for parallel processing
     */
    public List<String> analyzeTextChunksBatch(List<String> chunks) {
        return chunks.parallelStream()
                .map(this::analyzeTextChunk)
                .collect(Collectors.toList());
    }
    
    /**
     * Analyze text chunk asynchronously
     */
    public CompletableFuture<String> analyzeTextChunkAsync(String text) {
        return CompletableFuture.supplyAsync(() -> analyzeTextChunk(text));
    }
    
    /**
     * Build prompt for AI analysis
     */
    private String buildAnalysisPrompt(String text) {
        return """
            Phân tích đoạn văn bản sau và trả về kết quả dưới dạng JSON:
            
            {
              "chapters": [
                {"number": 1, "name": "Tên chương", "startIndex": 0}
              ],
              "questions": [
                {
                  "number": 1,
                  "content": "Nội dung câu hỏi",
                  "startIndex": 100,
                  "hasImage": false
                }
              ],
              "answers": [
                {
                  "questionNumber": 1,
                  "label": "A",
                  "content": "Nội dung đáp án",
                  "isCorrect": false,
                  "startIndex": 200
                }
              ]
            }
            
            Quy tắc:
            1. Chỉ detect câu hỏi trắc nghiệm (có đáp án A, B, C, D)
            2. Detect chương nếu có từ khóa "Chương", "Chapter", "CHƯƠNG"
            3. Đáp án phải liên kết với câu hỏi gần nhất
            4. Nếu không tìm thấy element nào, trả về mảng rỗng []
            5. Chỉ trả về JSON, không có text thêm
            
            Văn bản cần phân tích:
            """ + text;
    }
    
    /**
     * Execute with rate limiting and retry logic
     * Handles 429 errors with RetryInfo from API response
     */
    private String executeWithRateLimit(ThrowingSupplier<String> supplier) {
        int attempts = 0;
        Exception lastException = null;
        long retryAfterSeconds = 0;
        
        while (attempts < maxRetries) {
            try {
                // Rate limiting: Ensure minimum delay between requests
                long currentTime = System.currentTimeMillis();
                long timeSinceLastRequest = currentTime - lastRequestTime;
                if (timeSinceLastRequest < requestDelayMs) {
                    try {
                        Thread.sleep(requestDelayMs - timeSinceLastRequest);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                // Acquire permit (rate limiting)
                if (!rateLimiter.tryAcquire(60, TimeUnit.SECONDS)) {
                    throw new RuntimeException("Rate limit exceeded. Please try again later.");
                }
                
                try {
                    lastRequestTime = System.currentTimeMillis();
                    return supplier.get();
                } catch (org.springframework.web.client.HttpClientErrorException e) {
                    // Handle 429 Too Many Requests
                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                        // Try to parse RetryInfo from error response
                        retryAfterSeconds = parseRetryAfterFromError(e);
                        if (retryAfterSeconds > 0) {
                            throw new RateLimitException("Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.", retryAfterSeconds);
                        }
                    }
                    throw e;
                } finally {
                    // Release permit after delay to maintain rate limit
                    long delayMs = Math.max(60000 / rateLimitPerMinute, requestDelayMs);
                    new Thread(() -> {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        rateLimiter.release();
                    }).start();
                }
                
            } catch (RateLimitException rle) {
                lastException = rle;
                attempts++;
                retryAfterSeconds = rle.getRetryAfterSeconds();
                
                if (attempts < maxRetries) {
                    long waitTime = Math.max(retryAfterSeconds * 1000, retryDelayMs * attempts);
                    try {
                        System.out.println("Rate limit exceeded. Waiting " + (waitTime / 1000) + " seconds before retry...");
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxRetries) {
                    long waitTime = retryDelayMs * attempts;
                    try {
                        Thread.sleep(waitTime); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("Failed after " + maxRetries + " attempts: " + 
                (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }
    
    /**
     * Parse RetryInfo from 429 error response
     */
    private long parseRetryAfterFromError(org.springframework.web.client.HttpClientErrorException e) {
        try {
            String responseBody = e.getResponseBodyAsString();
            if (responseBody != null && responseBody.contains("retryDelay")) {
                // Try to extract retry delay from JSON response
                // Format: "retryDelay": "58s" or "retryDelay": 58
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"retryDelay\"\\s*:\\s*\"?(\\d+)[sS]?\"?");
                java.util.regex.Matcher matcher = pattern.matcher(responseBody);
                if (matcher.find()) {
                    return Long.parseLong(matcher.group(1));
                }
            }
        } catch (Exception ex) {
            // Ignore parsing errors
        }
        return 0;
    }
    
    /**
     * Custom exception for rate limit errors with retry info
     */
    private static class RateLimitException extends RuntimeException {
        private final long retryAfterSeconds;
        
        public RateLimitException(String message, long retryAfterSeconds) {
            super(message);
            this.retryAfterSeconds = retryAfterSeconds;
        }
        
        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
    
    /**
     * Extract JSON from response text
     * Handles markdown code blocks (```json ... ```) and plain JSON
     */
    private String extractJsonFromResponse(String responseText) {
        if (responseText == null || responseText.trim().isEmpty()) {
            return responseText;
        }
        
        String trimmed = responseText.trim();
        
        // Check if response is wrapped in markdown code block
        if (trimmed.startsWith("```")) {
            // Extract content between ```json and ```
            int startIndex = trimmed.indexOf("```");
            if (startIndex != -1) {
                // Find the language identifier (json, JSON, etc.)
                int jsonStart = trimmed.indexOf("json", startIndex);
                if (jsonStart != -1) {
                    // Find the start of actual JSON (after "json" and newline)
                    int contentStart = trimmed.indexOf("\n", jsonStart);
                    if (contentStart == -1) {
                        contentStart = jsonStart + 4; // "json".length()
                    } else {
                        contentStart += 1; // Skip newline
                    }
                    
                    // Find the end (```)
                    int endIndex = trimmed.lastIndexOf("```");
                    if (endIndex > contentStart) {
                        return trimmed.substring(contentStart, endIndex).trim();
                    }
                } else {
                    // No language identifier, try to find content between first and last ```
                    int firstBacktick = trimmed.indexOf("```");
                    int lastBacktick = trimmed.lastIndexOf("```");
                    if (lastBacktick > firstBacktick + 3) {
                        String content = trimmed.substring(firstBacktick + 3, lastBacktick).trim();
                        // Remove leading newline if present
                        if (content.startsWith("\n")) {
                            content = content.substring(1);
                        }
                        return content.trim();
                    }
                }
            }
        }
        
        // Check if response starts with { or [ (valid JSON)
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            // Find the last } or ]
            int lastBrace = trimmed.lastIndexOf("}");
            int lastBracket = trimmed.lastIndexOf("]");
            int endIndex = Math.max(lastBrace, lastBracket);
            if (endIndex > 0) {
                return trimmed.substring(0, endIndex + 1);
            }
        }
        
        // Return as-is if no markdown detected
        return trimmed;
    }
    
    /**
     * Functional interface for throwing supplier
     */
    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}

