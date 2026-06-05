package com.scriptforge.novelscript.ai.prompt;

import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromptBuilder {

    private final String scriptGenerationPrompt;
    private final String scriptSchema;

    public PromptBuilder() {
        this.scriptGenerationPrompt = loadResource("prompts/script-generation.md");
        this.scriptSchema = loadResource("schema/script-schema.yaml");
    }

    public String buildScriptGenerationPrompt(ProjectWorkspace project) {
        AdaptationSetting setting = project.getSetting();
        Map<String, String> variables = new HashMap<>();
        variables.put("{{novelTitle}}", project.getTitle());
        variables.put("{{chapters}}", formatChapters(project.getNovelContent().getChapters()));
        variables.put("{{scriptType}}", setting.getScriptType());
        variables.put("{{targetEpisodes}}", String.valueOf(setting.getTargetEpisodes()));
        variables.put("{{episodeDurationMinutes}}", String.valueOf(setting.getEpisodeDurationMinutes()));
        variables.put("{{style}}", setting.getStyle());
        variables.put("{{language}}", setting.getLanguage());
        variables.put("{{adaptationIntensity}}", setting.getAdaptationIntensity());
        variables.put("{{dialogueStyle}}", setting.getDialogueStyle());
        variables.put("{{budgetPreference}}", setting.getBudgetPreference());
        variables.put("{{keepOriginalDialogues}}", setting.isKeepOriginalDialogues() ? "是" : "否");
        variables.put("{{yamlSchema}}", scriptSchema);

        String prompt = scriptGenerationPrompt;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            prompt = prompt.replace(entry.getKey(), entry.getValue());
        }

        return prompt;
    }

    private String formatChapters(List<Chapter> chapters) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            sb.append("--- Chapter ").append(i + 1).append(": ").append(chapter.title()).append(" ---\n");
            sb.append(chapter.content()).append("\n\n");
        }
        return sb.toString();
    }

    private String loadResource(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load prompt resource: " + path, e);
        }
    }
}
