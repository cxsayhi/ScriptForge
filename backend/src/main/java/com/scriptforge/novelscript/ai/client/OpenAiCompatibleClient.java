package com.scriptforge.novelscript.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptforge.novelscript.ai.config.AiModelProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "scriptforge.ai.enabled", havingValue = "true")
public class OpenAiCompatibleClient implements AiClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestTemplate restTemplate;
    private final AiModelProperties properties;

    public OpenAiCompatibleClient(AiModelProperties properties) {
        this.restTemplate = new RestTemplate(requestFactory(properties));
        this.properties = properties;
    }

    @Override
    public String chat(String userPrompt) {
        return chatWithSystem(null, userPrompt);
    }

    @Override
    public String chatWithSystem(String systemPrompt, String userPrompt) {
        return chatWithSystem(systemPrompt, userPrompt, null, null);
    }

    @Override
    public String chatJsonWithSystem(String systemPrompt,
                                     String userPrompt,
                                     String schemaName,
                                     Map<String, Object> jsonSchema) {
        return chatWithSystem(systemPrompt, appendJsonSchemaInstruction(userPrompt, schemaName, jsonSchema), schemaName, jsonSchema);
    }

    private String chatWithSystem(String systemPrompt, String userPrompt, String schemaName, Map<String, Object> jsonSchema) {
        String provider = normalizedProvider();

        if ("gemini".equals(provider)) {
            return callGemini(systemPrompt, userPrompt, jsonSchema);
        } else {
            return callOpenAiCompatible(systemPrompt, userPrompt, schemaName, jsonSchema);
        }
    }

    private String callOpenAiCompatible(String systemPrompt, String userPrompt, String schemaName, Map<String, Object> jsonSchema) {
        String endpoint = getEndpoint();
        String apiKey = getApiKey();
        String model = getModel();
        double temperature = getTemperature();
        int maxTokens = getMaxTokens();

        ChatRequest request = new ChatRequest();
        request.model = model;
        request.temperature = temperature;
        request.maxTokens = maxTokens;
        request.messages = new ArrayList<>();
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            request.responseFormat = responseFormat(schemaName, jsonSchema);
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            request.messages.add(new Message("system", systemPrompt));
        }
        request.messages.add(new Message("user", userPrompt));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(requireApiKey(apiKey));

        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);

        ChatResponse response = restTemplate.postForObject(
                endpoint + "/chat/completions",
                entity,
                ChatResponse.class
        );

        if (response != null && response.choices != null && !response.choices.isEmpty()) {
            return response.choices.get(0).message.content;
        }

        throw new RuntimeException("AI response is empty");
    }

    private String callGemini(String systemPrompt, String userPrompt, Map<String, Object> jsonSchema) {
        String endpoint = properties.getGemini().getEndpoint();
        String apiKey = requireApiKey(properties.getGemini().getApiKey());
        String model = properties.getGemini().getModel();
        double temperature = properties.getGemini().getTemperature();

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();

        List<Map<String, Object>> parts = new ArrayList<>();
        Map<String, Object> part = new HashMap<>();

        String fullPrompt = systemPrompt != null && !systemPrompt.isBlank()
                ? systemPrompt + "\n\n" + userPrompt
                : userPrompt;
        part.put("text", fullPrompt);
        parts.add(part);

        content.put("parts", parts);
        content.put("role", "user");
        contents.add(content);
        requestBody.put("contents", contents);

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", temperature);
        generationConfig.put("maxOutputTokens", properties.getGemini().getMaxTokens());
        if (jsonSchema != null && !jsonSchema.isEmpty()) {
            generationConfig.put("responseMimeType", "application/json");
            generationConfig.put("responseSchema", jsonSchema);
        }
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        String url = endpoint + "/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);

        if (response != null && response.containsKey("candidates")) {
            List<?> candidates = (List<?>) response.get("candidates");
            if (!candidates.isEmpty()) {
                Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
                if (candidate.containsKey("content")) {
                    Map<?, ?> contentResp = (Map<?, ?>) candidate.get("content");
                    if (contentResp.containsKey("parts")) {
                        List<?> partsResp = (List<?>) contentResp.get("parts");
                        if (!partsResp.isEmpty()) {
                            Map<?, ?> partResp = (Map<?, ?>) partsResp.get(0);
                            if (partResp.containsKey("text")) {
                                return (String) partResp.get("text");
                            }
                        }
                    }
                }
            }
        }

        throw new RuntimeException("Gemini response is empty");
    }

    private Map<String, Object> responseFormat(String schemaName, Map<String, Object> jsonSchema) {
        if ("openai".equals(normalizedProvider())) {
            Map<String, Object> schemaConfig = new HashMap<>();
            schemaConfig.put("name", sanitizeSchemaName(schemaName));
            schemaConfig.put("strict", false);
            schemaConfig.put("schema", jsonSchema);
            Map<String, Object> responseFormat = new HashMap<>();
            responseFormat.put("type", "json_schema");
            responseFormat.put("json_schema", schemaConfig);
            return responseFormat;
        }
        Map<String, Object> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        return responseFormat;
    }

    private String appendJsonSchemaInstruction(String userPrompt, String schemaName, Map<String, Object> jsonSchema) {
        if (jsonSchema == null || jsonSchema.isEmpty()) {
            return userPrompt;
        }
        return userPrompt
                + "\n\nReturn JSON only. It must conform to JSON schema `"
                + sanitizeSchemaName(schemaName)
                + "`:\n"
                + serializeSchema(jsonSchema);
    }

    private String serializeSchema(Map<String, Object> jsonSchema) {
        try {
            return OBJECT_MAPPER.writeValueAsString(jsonSchema);
        } catch (JsonProcessingException exception) {
            return String.valueOf(jsonSchema);
        }
    }

    private String sanitizeSchemaName(String schemaName) {
        if (schemaName == null || schemaName.isBlank()) {
            return "script_schema";
        }
        return schemaName.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String getEndpoint() {
        return switch (normalizedProvider()) {
            case "deepseek" -> properties.getDeepseek().getEndpoint();
            case "qwen" -> properties.getQwen().getEndpoint();
            case "gemini" -> properties.getGemini().getEndpoint();
            default -> properties.getOpenai().getEndpoint();
        };
    }

    private String getApiKey() {
        return switch (normalizedProvider()) {
            case "deepseek" -> properties.getDeepseek().getApiKey();
            case "qwen" -> properties.getQwen().getApiKey();
            case "gemini" -> properties.getGemini().getApiKey();
            default -> properties.getOpenai().getApiKey();
        };
    }

    private String getModel() {
        return switch (normalizedProvider()) {
            case "deepseek" -> properties.getDeepseek().getModel();
            case "qwen" -> properties.getQwen().getModel();
            case "gemini" -> properties.getGemini().getModel();
            default -> properties.getOpenai().getModel();
        };
    }

    private double getTemperature() {
        return switch (normalizedProvider()) {
            case "deepseek" -> properties.getDeepseek().getTemperature();
            case "qwen" -> properties.getQwen().getTemperature();
            case "gemini" -> properties.getGemini().getTemperature();
            default -> properties.getOpenai().getTemperature();
        };
    }

    private int getMaxTokens() {
        return switch (normalizedProvider()) {
            case "deepseek" -> properties.getDeepseek().getMaxTokens();
            case "qwen" -> properties.getQwen().getMaxTokens();
            case "gemini" -> properties.getGemini().getMaxTokens();
            default -> properties.getOpenai().getMaxTokens();
        };
    }

    private String requireApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AI API key is required when scriptforge.ai.enabled=true");
        }
        return apiKey;
    }

    private String normalizedProvider() {
        String provider = properties.getProvider();
        if (provider == null || provider.isBlank()) {
            return "openai";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }

    private SimpleClientHttpRequestFactory requestFactory(AiModelProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(toMillis(properties.getConnectTimeoutSeconds(), 10));
        factory.setReadTimeout(toMillis(properties.getReadTimeoutSeconds(), 90));
        return factory;
    }

    private int toMillis(int seconds, int fallbackSeconds) {
        int normalized = seconds > 0 ? seconds : fallbackSeconds;
        return Math.multiplyExact(normalized, 1000);
    }

    public static class ChatRequest {
        public String model;
        public List<Message> messages;
        public Double temperature;
        @JsonProperty("max_tokens")
        public Integer maxTokens;
        @JsonProperty("response_format")
        public Map<String, Object> responseFormat;
    }

    public static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static class ChatResponse {
        public List<Choice> choices;
    }

    public static class Choice {
        public Message message;
    }
}
