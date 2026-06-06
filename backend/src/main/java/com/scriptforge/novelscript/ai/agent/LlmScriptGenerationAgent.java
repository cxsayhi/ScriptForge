package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.ai.client.AiClient;
import com.scriptforge.novelscript.ai.prompt.PromptBuilder;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(name = "scriptforge.ai.enabled", havingValue = "true")
public class LlmScriptGenerationAgent implements ScriptGenerationAgent {

    private static final Logger log = LoggerFactory.getLogger(LlmScriptGenerationAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are ScriptForge's script adaptation engine.
            Return only valid YAML that matches the requested schema.
            Do not include Markdown fences, explanations, or comments outside YAML.
            """;

    private final AiClient aiClient;
    private final PromptBuilder promptBuilder;
    private final YamlScriptValidator validator;
    private final RuleBasedScriptGenerationAgent fallbackAgent;

    public LlmScriptGenerationAgent(AiClient aiClient,
                                    PromptBuilder promptBuilder,
                                    YamlScriptValidator validator,
                                    RuleBasedScriptGenerationAgent fallbackAgent) {
        this.aiClient = aiClient;
        this.promptBuilder = promptBuilder;
        this.validator = validator;
        this.fallbackAgent = fallbackAgent;
    }

    @Override
    public String generate(ProjectWorkspace project) {
        try {
            String prompt = promptBuilder.buildScriptGenerationPrompt(project);
            String rawResponse = aiClient.chatWithSystem(SYSTEM_PROMPT, prompt);
            String yaml = extractYaml(rawResponse);
            ValidationResult validation = validator.validate(yaml);
            if (validation.valid()) {
                return yaml;
            }
            log.warn("LLM script result failed validation, falling back to rule-based agent: {}", validation.errors());
        } catch (RuntimeException exception) {
            log.warn("LLM script generation failed, falling back to rule-based agent: {}", exception.getMessage());
            log.debug("LLM script generation failure details", exception);
        }
        return fallbackAgent.generate(project);
    }

    private String extractYaml(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("LLM response is empty");
        }

        String trimmed = response.trim();
        String fenced = extractFencedBlock(trimmed);
        if (fenced != null) {
            return validator.repair(fenced);
        }
        return validator.repair(trimmed);
    }

    private String extractFencedBlock(String response) {
        int opening = response.indexOf("```");
        if (opening < 0) {
            return null;
        }

        int contentStart = response.indexOf('\n', opening);
        if (contentStart < 0) {
            return null;
        }

        int closing = response.indexOf("```", contentStart + 1);
        if (closing < 0) {
            return null;
        }

        return response.substring(contentStart + 1, closing).trim();
    }
}
