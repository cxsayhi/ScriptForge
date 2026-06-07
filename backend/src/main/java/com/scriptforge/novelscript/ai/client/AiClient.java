package com.scriptforge.novelscript.ai.client;

import java.util.Map;

public interface AiClient {

    String chat(String userPrompt);

    String chatWithSystem(String systemPrompt, String userPrompt);

    default String chatJsonWithSystem(String systemPrompt,
                                      String userPrompt,
                                      String schemaName,
                                      Map<String, Object> jsonSchema) {
        return chatWithSystem(systemPrompt, userPrompt);
    }

}
