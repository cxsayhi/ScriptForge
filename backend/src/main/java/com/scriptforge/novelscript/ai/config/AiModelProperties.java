package com.scriptforge.novelscript.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scriptforge.ai")
public class AiModelProperties {

    private boolean enabled = false;

    private String provider = "openai";

    private int connectTimeoutSeconds = 10;

    private int readTimeoutSeconds = 90;

    private OpenAiConfig openai = new OpenAiConfig();

    private DeepSeekConfig deepseek = new DeepSeekConfig();

    private QwenConfig qwen = new QwenConfig();

    private GeminiConfig gemini = new GeminiConfig();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

    public OpenAiConfig getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAiConfig openai) {
        this.openai = openai;
    }

    public DeepSeekConfig getDeepseek() {
        return deepseek;
    }

    public void setDeepseek(DeepSeekConfig deepseek) {
        this.deepseek = deepseek;
    }

    public QwenConfig getQwen() {
        return qwen;
    }

    public void setQwen(QwenConfig qwen) {
        this.qwen = qwen;
    }

    public GeminiConfig getGemini() {
        return gemini;
    }

    public void setGemini(GeminiConfig gemini) {
        this.gemini = gemini;
    }

    public static class OpenAiConfig {
        private String endpoint = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private double temperature = 0.7;
        private int maxTokens = 4096;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class DeepSeekConfig {
        private String endpoint = "https://api.deepseek.com/v1";
        private String apiKey = "";
        private String model = "deepseek-chat";
        private double temperature = 0.7;
        private int maxTokens = 4096;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class QwenConfig {
        private String endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey = "";
        private String model = "qwen-turbo";
        private double temperature = 0.7;
        private int maxTokens = 4096;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class GeminiConfig {
        private String endpoint = "https://generativelanguage.googleapis.com/v1beta/models";
        private String apiKey = "";
        private String model = "gemini-2.5-flash-lite";
        private int thinkingBudget = 0;
        private int maxToolRounds = 3;
        private int maxConversationMessages = 12;
        private double temperature = 0.7;
        private int maxTokens = 4096;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getThinkingBudget() {
            return thinkingBudget;
        }

        public void setThinkingBudget(int thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
        }

        public int getMaxToolRounds() {
            return maxToolRounds;
        }

        public void setMaxToolRounds(int maxToolRounds) {
            this.maxToolRounds = maxToolRounds;
        }

        public int getMaxConversationMessages() {
            return maxConversationMessages;
        }

        public void setMaxConversationMessages(int maxConversationMessages) {
            this.maxConversationMessages = maxConversationMessages;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}
