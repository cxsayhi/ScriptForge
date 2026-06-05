package com.scriptforge.novelscript.ai.client;

import org.springframework.stereotype.Component;

@Component
public class MockAiClient implements AiClient {

    @Override
    public String chat(String userPrompt) {
        return "Mock response for: " + userPrompt.substring(0, Math.min(50, userPrompt.length())) + "...";
    }

    @Override
    public String chatWithSystem(String systemPrompt, String userPrompt) {
        return "Mock response with system prompt: " + userPrompt.substring(0, Math.min(50, userPrompt.length())) + "...";
    }

}

