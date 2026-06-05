package com.scriptforge.novelscript.ai.client;

public interface AiClient {

    String chat(String userPrompt);

    String chatWithSystem(String systemPrompt, String userPrompt);

}

