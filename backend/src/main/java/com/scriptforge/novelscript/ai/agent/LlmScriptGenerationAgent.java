package com.scriptforge.novelscript.ai.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scriptforge.novelscript.ai.client.AiClient;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.FailedEpisode;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Primary
@Component
@ConditionalOnProperty(name = "scriptforge.ai.enabled", havingValue = "true")
public class LlmScriptGenerationAgent implements ScriptGenerationAgent {

    private static final Logger log = LoggerFactory.getLogger(LlmScriptGenerationAgent.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int CHAPTER_DIGEST_LIMIT = 900;
    private static final int EPISODE_CHAPTER_LIMIT = 2400;
    private static final int OUTLINE_DIGEST_MAX_CHARS = 12_000;

    private static final String JSON_SYSTEM_PROMPT = """
            You are ScriptForge's script adaptation engine.
            Return only valid JSON that matches the requested JSON schema.
            Do not include Markdown fences, explanations, comments, or text outside JSON.
            """;

    private static final String REPAIR_SYSTEM_PROMPT = """
            You are ScriptForge's strict JSON repair engine.
            Return only valid JSON that matches the requested JSON schema.
            Preserve the script content as much as possible, but fix syntax, types, missing wrappers, and schema shape.
            Do not include Markdown fences, explanations, comments, or text outside JSON.
            """;

    private final AiClient aiClient;
    private final YamlScriptValidator validator;

    public LlmScriptGenerationAgent(AiClient aiClient,
                                    YamlScriptValidator validator) {
        this.aiClient = aiClient;
        this.validator = validator;
    }

    @Override
    public ScriptGenerationResult generateResult(ProjectWorkspace project) {
        GenerationTrace trace = new GenerationTrace();
        try {
            Draft draft = generateSegmentedJsonDraft(project, trace);
            trace.setCandidateYaml(draft.yaml());
            if (trace.hasEpisodeFailures()) {
                return reviewRequired(project, trace, trace.failureMessage());
            }
            ValidationResult validation = validator.validate(draft.yaml());
            if (validation.valid()) {
                return ScriptGenerationResult.completed(draft.yaml());
            }

            Draft repaired = repairWithLlm(project, draft.yaml(), validation.errors(), trace);
            if (repaired != null) {
                trace.setCandidateYaml(repaired.yaml());
                ValidationResult repairedValidation = validator.validate(repaired.yaml());
                if (repairedValidation.valid()) {
                    return ScriptGenerationResult.completed(repaired.yaml());
                }
                log.warn(
                        "LLM script repair failed validation; preserving result for review: {}. raw={}, repaired={}",
                        repairedValidation.errors(),
                        preview(repaired.raw()),
                        preview(repaired.yaml())
                );
                return reviewRequired(project, trace, "完整剧本修复后仍未通过校验：" + String.join("；", repairedValidation.errors()));
            } else {
                log.warn(
                        "LLM script result failed validation and repair failed; preserving result for review: {}. raw={}, repaired={}",
                        validation.errors(),
                        preview(draft.raw()),
                        preview(draft.yaml())
                );
                return reviewRequired(project, trace, "完整剧本未通过校验，LLM 修复失败：" + String.join("；", validation.errors()));
            }
        } catch (RuntimeException exception) {
            if (exception instanceof StructuredResponseException structuredException) {
                log.warn(
                        "LLM script generation failed; preserving result for review: {}. raw={}",
                        structuredException.getMessage(),
                        preview(structuredException.raw())
                );
            } else {
                log.warn("LLM script generation failed; preserving result for review: {}", exception.getMessage());
            }
            log.debug("LLM script generation failure details", exception);
            return reviewRequired(project, trace, "LLM 生成或修复失败：" + safeMessage(exception));
        }
    }

    private Draft generateSegmentedJsonDraft(ProjectWorkspace project, GenerationTrace trace) {
        String outlineRaw = aiClient.chatJsonWithSystem(
                JSON_SYSTEM_PROMPT,
                buildOutlinePrompt(project),
                "script_outline",
                outlineSchema()
        );
        trace.appendRaw("outline", outlineRaw);
        Map<String, Object> outline = parseMapOrRepair(
                outlineRaw,
                project,
                "script_outline",
                outlineSchema(),
                "outline planning response",
                trace
        );
        int targetEpisodes = targetEpisodeCount(project, outline);
        if (containsCompleteScript(outline, targetEpisodes)) {
            Map<String, Object> completeScript = normalizeScriptRoot(outline, project, targetEpisodes);
            trace.setPartial(completeScript, listValue(completeScript.get("episodes")));
            return new Draft(toYaml(completeScript, project), outlineRaw);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("project", normalizedProjectBlock(outline.get("project"), project, targetEpisodes));
        root.put("characters", outline.getOrDefault("characters", List.of(defaultCharacter())));

        List<Object> episodeOutlines = listValue(firstPresent(outline, "episode_outlines", "episodeOutlines", "episodes"));
        List<List<Chapter>> chapterAssignments = chapterAssignments(project.getNovelContent().getChapters(), episodeOutlines, targetEpisodes);
        List<Object> episodes = new ArrayList<>();
        trace.setPartial(root, episodes);
        StringBuilder raw = new StringBuilder(outlineRaw);
        for (int episodeIndex = 1; episodeIndex <= targetEpisodes; episodeIndex++) {
            int rawStart = trace.rawLength();
            try {
                Object episodeOutline = episodeIndex <= episodeOutlines.size() ? episodeOutlines.get(episodeIndex - 1) : Map.of();
                String episodeRaw = aiClient.chatJsonWithSystem(
                        JSON_SYSTEM_PROMPT,
                        buildEpisodePrompt(project, root, episodeOutline, chapterAssignments.get(episodeIndex - 1), episodeIndex, targetEpisodes),
                        "script_episode",
                        episodeEnvelopeSchema()
                );
                trace.appendRaw("episode " + episodeIndex, episodeRaw);
                raw.append("\n\n--- episode ").append(episodeIndex).append(" raw ---\n").append(episodeRaw);
                Object parsedEpisode = parseValueOrRepair(
                        episodeRaw,
                        project,
                        "script_episode",
                        episodeEnvelopeSchema(),
                        "episode " + episodeIndex + " response",
                        trace
                );
                try {
                    episodes.add(extractEpisode(parsedEpisode, episodeIndex));
                } catch (RuntimeException exception) {
                    Object repairedEpisode = repairStructuredResponse(
                            episodeRaw,
                            project,
                            "script_episode",
                            episodeEnvelopeSchema(),
                            "episode " + episodeIndex + " response did not contain the requested episode: " + exception.getMessage(),
                            trace
                    );
                    episodes.add(extractEpisode(repairedEpisode, episodeIndex));
                }
            } catch (RuntimeException exception) {
                trace.addEpisodeFailure(episodeIndex, safeMessage(exception), trace.rawSince(rawStart));
            }
        }
        root.put("episodes", episodes);
        return new Draft(toYaml(root, project), raw.toString());
    }

    private Draft repairWithLlm(ProjectWorkspace project,
                                String invalidYaml,
                                List<String> errors,
                                GenerationTrace trace) {
        try {
            String repairRaw = aiClient.chatJsonWithSystem(
                    REPAIR_SYSTEM_PROMPT,
                    buildRepairPrompt(project, invalidYaml, errors),
                    "script_result",
                    scriptSchema()
            );
            trace.appendRaw("full script repair", repairRaw);
            Map<String, Object> repaired = parseStructuredMap(repairRaw, project);
            Map<String, Object> normalized = normalizeScriptRoot(repaired, project, targetEpisodeCount(project, repaired));
            return new Draft(toYaml(normalized, project), repairRaw);
        } catch (RuntimeException exception) {
            log.warn("LLM script repair retry failed: {}", exception.getMessage());
            log.debug("LLM script repair retry failure details", exception);
            return null;
        }
    }

    private Map<String, Object> parseMapOrRepair(String response,
                                                 ProjectWorkspace project,
                                                 String schemaName,
                                                 Map<String, Object> schema,
                                                 String context,
                                                 GenerationTrace trace) {
        try {
            return parseStructuredMap(response, project);
        } catch (RuntimeException exception) {
            Object repaired = repairStructuredResponse(response, project, schemaName, schema, context + ": " + exception.getMessage(), trace);
            if (repaired instanceof Map<?, ?> map) {
                return mutableMap(map);
            }
            throw new StructuredResponseException("LLM repaired response root is not an object", String.valueOf(repaired), exception);
        }
    }

    private Object parseValueOrRepair(String response,
                                      ProjectWorkspace project,
                                      String schemaName,
                                      Map<String, Object> schema,
                                      String context,
                                      GenerationTrace trace) {
        try {
            return parseStructuredValue(response, project);
        } catch (RuntimeException exception) {
            return repairStructuredResponse(response, project, schemaName, schema, context + ": " + exception.getMessage(), trace);
        }
    }

    private Object repairStructuredResponse(String response,
                                            ProjectWorkspace project,
                                            String schemaName,
                                            Map<String, Object> schema,
                                            String context,
                                            GenerationTrace trace) {
        String repairRaw = aiClient.chatJsonWithSystem(
                REPAIR_SYSTEM_PROMPT,
                buildRawResponseRepairPrompt(project, response, context),
                schemaName,
                schema
        );
        trace.appendRaw(schemaName + " repair", repairRaw);
        try {
            return parseStructuredValue(repairRaw, project);
        } catch (RuntimeException exception) {
            throw new StructuredResponseException(
                    "LLM response is not valid JSON or repairable YAML after repair retry: " + exception.getMessage(),
                    repairRaw,
                    exception
            );
        }
    }

    private String buildRawResponseRepairPrompt(ProjectWorkspace project, String rawResponse, String context) {
        return """
                The previous model response could not be parsed by the backend.
                Convert it into valid JSON matching the requested schema.

                Context:
                %s

                Project settings:
                %s

                Raw response:
                %s
                """.formatted(
                context,
                settingsBlock(project),
                rawResponse
        );
    }

    private String buildOutlinePrompt(ProjectWorkspace project) {
        return """
                Generate the first planning pass for a novel-to-script adaptation.

                Return JSON with exactly these root fields:
                - project: object matching the final script project schema.
                - characters: compact stable character list with ids like char_001.
                - episode_outlines: exactly %d outline objects with episode_id, title, summary, chapter_numbers, and key_events.

                Do not generate final scenes in this pass.

                Project settings:
                %s

                Chapter digest:
                %s
                """.formatted(
                targetEpisodeCount(project, null),
                settingsBlock(project),
                formatOutlineDigest(project.getNovelContent().getChapters())
        );
    }

    private String buildEpisodePrompt(ProjectWorkspace project,
                                      Map<String, Object> root,
                                      Object episodeOutline,
                                      List<Chapter> sourceChapters,
                                      int episodeIndex,
                                      int targetEpisodes) {
        return """
                Generate one episode script as JSON.

                Return JSON with exactly one root field `episode`.
                The episode must include episode_id, title, summary, and at least one shootable scene.
                Every scene must include scene_id, title, location, time, characters, action, and dialogues.
                Use only character ids from the supplied character list when possible.

                Episode number: %d of %d
                Project settings:
                %s

                Project object:
                %s

                Character list:
                %s

                Episode outline:
                %s

                Relevant source chapters:
                %s
                """.formatted(
                episodeIndex,
                targetEpisodes,
                settingsBlock(project),
                toJson(root.get("project")),
                toJson(root.get("characters")),
                toJson(episodeOutline),
                formatChapters(sourceChapters, EPISODE_CHAPTER_LIMIT)
        );
    }

    private String buildRepairPrompt(ProjectWorkspace project, String invalidYaml, List<String> errors) {
        return """
                The previous script draft failed validation. Repair it into one complete JSON script object.

                Validation errors:
                %s

                Project settings:
                %s

                Invalid draft:
                %s
                """.formatted(
                String.join("\n", errors),
                settingsBlock(project),
                invalidYaml
        );
    }

    private Map<String, Object> parseStructuredMap(String response, ProjectWorkspace project) {
        Object parsed = parseStructuredValue(response, project);
        if (parsed instanceof Map<?, ?> map) {
            return mutableMap(map);
        }
        throw new StructuredResponseException("LLM response root is not an object", response);
    }

    private Object parseStructuredValue(String response, ProjectWorkspace project) {
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("LLM response is empty");
        }

        Object parsedJson = parseJson(response);
        if (parsedJson != null) {
            return parsedJson;
        }

        String repairedYaml = validator.repair(response, project);
        try {
            return new Yaml().load(repairedYaml);
        } catch (YAMLException exception) {
            throw new StructuredResponseException("LLM response is not valid JSON or repairable YAML", response, exception);
        }
    }

    private Object parseJson(String response) {
        for (String candidate : jsonCandidates(response)) {
            try {
                Object parsed = OBJECT_MAPPER.readValue(candidate, new TypeReference<>() {
                });
                if (parsed instanceof String embedded && looksStructured(embedded)) {
                    Object embeddedParsed = parseJson(embedded);
                    return embeddedParsed == null ? parsed : embeddedParsed;
                }
                return parsed;
            } catch (JsonProcessingException ignored) {
                // Try the next candidate; LLMs often wrap JSON in fences or stray text.
            }
        }
        return null;
    }

    private List<String> jsonCandidates(String response) {
        String trimmed = response.trim();
        List<String> candidates = new ArrayList<>();
        candidates.add(trimmed);
        candidates.add(stripFenceLanguageMarker(trimmed));

        String fenced = extractFencedBlock(trimmed);
        if (fenced != null) {
            candidates.add(fenced);
            candidates.add(stripFenceLanguageMarker(fenced));
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            candidates.add(trimmed.substring(objectStart, objectEnd + 1));
        }

        int arrayStart = trimmed.indexOf('[');
        int arrayEnd = trimmed.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            candidates.add(trimmed.substring(arrayStart, arrayEnd + 1));
        }
        return candidates;
    }

    private boolean looksStructured(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.startsWith("{")
                || trimmed.startsWith("[")
                || trimmed.startsWith("```")
                || trimmed.startsWith("project:")
                || trimmed.startsWith("characters:")
                || trimmed.startsWith("episodes:")
                || trimmed.startsWith("- episode_id:");
    }

    private String extractFencedBlock(String response) {
        int opening = response.indexOf("```");
        if (opening < 0) {
            return null;
        }

        int contentStart = opening + 3;
        int closing = response.indexOf("```", contentStart);
        if (closing < 0) {
            return null;
        }

        return stripFenceLanguageMarker(response.substring(contentStart, closing).trim());
    }

    private String stripFenceLanguageMarker(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.startsWith("json\n") || trimmed.startsWith("yaml\n") || trimmed.startsWith("yml\n")) {
            return trimmed.substring(trimmed.indexOf('\n') + 1).trim();
        }
        if (trimmed.startsWith("json ")) {
            return trimmed.substring(5).trim();
        }
        if (trimmed.startsWith("yaml ")) {
            return trimmed.substring(5).trim();
        }
        if (trimmed.startsWith("yml ")) {
            return trimmed.substring(4).trim();
        }
        return trimmed;
    }

    private String toYaml(Object value, ProjectWorkspace project) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setWidth(120);
        return validator.repair(new Yaml(options).dump(value), project);
    }

    private Object extractEpisode(Object parsed, int episodeIndex) {
        if (parsed instanceof Map<?, ?> map) {
            Object episode = firstPresent(map, "episode");
            if (episode != null) {
                return requireExpectedEpisode(episode, episodeIndex);
            }
            Object episodesValue = firstPresent(map, "episodes");
            Object selected = selectEpisode(listValue(episodesValue), episodeIndex);
            if (selected != null) {
                return requireExpectedEpisode(selected, episodeIndex);
            }
            if (looksLikeEpisode(map)) {
                return requireExpectedEpisode(map, episodeIndex);
            }
        }
        if (parsed instanceof List<?> list) {
            Object selected = selectEpisode(list, episodeIndex);
            if (selected != null) {
                return requireExpectedEpisode(selected, episodeIndex);
            }
        }
        throw new IllegalStateException("LLM episode response does not contain episode " + episodeIndex);
    }

    private Object selectEpisode(List<?> episodes, int episodeIndex) {
        for (Object episode : episodes) {
            if (episode instanceof Map<?, ?> map && integer(map.get("episode_id")) == episodeIndex) {
                return episode;
            }
        }
        return null;
    }

    private Object requireExpectedEpisode(Object value, int expectedEpisodeId) {
        if (!(value instanceof Map<?, ?> episode)) {
            throw new IllegalStateException("LLM episode response is not an episode object");
        }
        int actualEpisodeId = integer(firstPresent(episode, "episode_id", "episodeId"));
        if (actualEpisodeId != expectedEpisodeId) {
            throw new IllegalStateException(
                    "LLM episode response id %d does not match requested episode %d"
                            .formatted(actualEpisodeId, expectedEpisodeId)
            );
        }
        return episode;
    }

    private boolean containsCompleteScript(Map<String, Object> root, int targetEpisodes) {
        Object episodesValue = root.get("episodes");
        if (!(episodesValue instanceof List<?> episodes) || !hasExpectedEpisodeSequence(episodes, targetEpisodes)) {
            return false;
        }
        for (Object episode : episodes) {
            if (!(episode instanceof Map<?, ?> map) || !looksLikeEpisode(map) || !map.containsKey("scenes")) {
                return false;
            }
        }
        return root.containsKey("project") && root.containsKey("characters");
    }

    private boolean hasExpectedEpisodeSequence(List<?> episodes, int targetEpisodes) {
        if (episodes.size() != targetEpisodes) {
            return false;
        }
        for (int index = 0; index < episodes.size(); index++) {
            if (!(episodes.get(index) instanceof Map<?, ?> episode)
                    || integer(firstPresent(episode, "episode_id", "episodeId")) != index + 1) {
                return false;
            }
        }
        return true;
    }

    private boolean looksLikeEpisode(Map<?, ?> map) {
        return map.containsKey("episode_id")
                || map.containsKey("episodeId")
                || (map.containsKey("title") && map.containsKey("summary") && map.containsKey("scenes"));
    }

    private int targetEpisodeCount(ProjectWorkspace project, Map<String, Object> outline) {
        int targetEpisodes = project.getSetting().getTargetEpisodes();
        if (targetEpisodes > 0) {
            return targetEpisodes;
        }
        if (outline != null) {
            List<Object> outlines = listValue(firstPresent(outline, "episode_outlines", "episodeOutlines", "episodes"));
            if (!outlines.isEmpty()) {
                return outlines.size();
            }
        }
        return 1;
    }

    private List<List<Chapter>> chapterAssignments(List<Chapter> chapters,
                                                    List<Object> episodeOutlines,
                                                    int targetEpisodes) {
        List<List<Chapter>> plannedAssignments = chapterAssignmentsFromOutline(chapters, episodeOutlines, targetEpisodes);
        return plannedAssignments != null ? plannedAssignments : proportionalChapterAssignments(chapters, targetEpisodes);
    }

    private List<List<Chapter>> chapterAssignmentsFromOutline(List<Chapter> chapters,
                                                               List<Object> episodeOutlines,
                                                               int targetEpisodes) {
        if (chapters.isEmpty() || episodeOutlines.size() != targetEpisodes) {
            return null;
        }

        Map<Integer, Chapter> chaptersByNumber = new LinkedHashMap<>();
        Map<Integer, Integer> chapterPositions = new LinkedHashMap<>();
        for (int index = 0; index < chapters.size(); index++) {
            Chapter chapter = chapters.get(index);
            chaptersByNumber.put(chapter.index(), chapter);
            chapterPositions.put(chapter.index(), index);
        }

        List<List<Chapter>> assignments = new ArrayList<>();
        int previousChapterPosition = -1;
        for (int episodeIndex = 0; episodeIndex < targetEpisodes; episodeIndex++) {
            if (!(episodeOutlines.get(episodeIndex) instanceof Map<?, ?> outline)
                    || integer(firstPresent(outline, "episode_id", "episodeId")) != episodeIndex + 1) {
                return null;
            }
            Object chapterNumbersValue = firstPresent(outline, "chapter_numbers", "chapterNumbers");
            if (!(chapterNumbersValue instanceof List<?> chapterNumbers) || chapterNumbers.isEmpty()) {
                return null;
            }

            List<Chapter> assignment = new ArrayList<>();
            int previousInEpisode = -1;
            for (Object chapterNumberValue : chapterNumbers) {
                int chapterNumber = integer(chapterNumberValue);
                Chapter chapter = chaptersByNumber.get(chapterNumber);
                Integer chapterPosition = chapterPositions.get(chapterNumber);
                if (chapter == null || chapterPosition == null
                        || chapterPosition <= previousInEpisode
                        || chapterPosition < previousChapterPosition) {
                    return null;
                }
                assignment.add(chapter);
                previousInEpisode = chapterPosition;
            }
            previousChapterPosition = previousInEpisode;
            assignments.add(assignment);
        }
        return assignments;
    }

    private List<List<Chapter>> proportionalChapterAssignments(List<Chapter> chapters, int targetEpisodes) {
        int safeTarget = Math.max(1, targetEpisodes);
        List<List<Chapter>> assignments = new ArrayList<>();
        if (chapters.isEmpty()) {
            for (int index = 0; index < safeTarget; index++) {
                assignments.add(List.of());
            }
            return assignments;
        }

        for (int episodeIndex = 1; episodeIndex <= safeTarget; episodeIndex++) {
            int start = (episodeIndex - 1) * chapters.size() / safeTarget;
            int end = episodeIndex * chapters.size() / safeTarget;
            if (end <= start) {
                end = Math.min(start + 1, chapters.size());
            }
            assignments.add(chapters.subList(start, end));
        }
        return assignments;
    }

    private String formatOutlineDigest(List<Chapter> chapters) {
        if (chapters.isEmpty()) {
            return "No source chapters are available.";
        }

        int headingChars = 0;
        for (Chapter chapter : chapters) {
            headingChars += ("--- Chapter " + chapter.index() + ": " + chapter.title() + " ---\n\n").length();
        }
        if (headingChars >= OUTLINE_DIGEST_MAX_CHARS) {
            return "The novel contains %d chapters. Chapter headings exceed the planning budget; use the per-episode source excerpts for details."
                    .formatted(chapters.size());
        }

        int contentBudget = OUTLINE_DIGEST_MAX_CHARS - headingChars;
        int perChapterLimit = Math.min(
                CHAPTER_DIGEST_LIMIT,
                Math.max(0, contentBudget / chapters.size() - 3)
        );
        return formatChapters(chapters, perChapterLimit);
    }

    private String formatChapters(List<Chapter> chapters, int maxCharsPerChapter) {
        StringBuilder builder = new StringBuilder();
        for (Chapter chapter : chapters) {
            builder.append("--- Chapter ")
                    .append(chapter.index())
                    .append(": ")
                    .append(chapter.title())
                    .append(" ---\n")
                    .append(excerpt(chapter.content(), maxCharsPerChapter))
                    .append("\n\n");
        }
        return builder.toString();
    }

    private String settingsBlock(ProjectWorkspace project) {
        AdaptationSetting setting = project.getSetting();
        return """
                Novel title: %s
                Script type: %s
                Target episodes: %d
                Episode duration minutes: %d
                Style: %s
                Language: %s
                Adaptation intensity: %s
                Dialogue style: %s
                Budget preference: %s
                Keep original dialogues: %s
                """.formatted(
                text(project.getTitle(), "未命名项目"),
                setting.getScriptType(),
                setting.getTargetEpisodes(),
                setting.getEpisodeDurationMinutes(),
                setting.getStyle(),
                setting.getLanguage(),
                setting.getAdaptationIntensity(),
                setting.getDialogueStyle(),
                setting.getBudgetPreference(),
                setting.isKeepOriginalDialogues() ? "是" : "否"
        );
    }

    private Map<String, Object> defaultProject(ProjectWorkspace project, int targetEpisodes) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("title", text(project.getTitle(), "AI 生成剧本"));
        block.put("source_type", "novel");
        block.put("script_type", project.getSetting().getScriptType());
        block.put("language", project.getSetting().getLanguage());
        block.put("target_episodes", targetEpisodes);
        return block;
    }

    private Map<String, Object> normalizeScriptRoot(Map<String, Object> root,
                                                     ProjectWorkspace project,
                                                     int targetEpisodes) {
        Map<String, Object> normalized = new LinkedHashMap<>(root);
        normalized.put("project", normalizedProjectBlock(root.get("project"), project, targetEpisodes));
        return normalized;
    }

    private Map<String, Object> normalizedProjectBlock(Object source,
                                                        ProjectWorkspace project,
                                                        int targetEpisodes) {
        Map<String, Object> normalized = defaultProject(project, targetEpisodes);
        if (!(source instanceof Map<?, ?> sourceMap)) {
            return normalized;
        }

        Object title = sourceMap.get("title");
        if (title instanceof String titleText && !titleText.isBlank()) {
            normalized.put("title", titleText);
        }
        Object summary = sourceMap.get("summary");
        if (summary instanceof String summaryText && !summaryText.isBlank()) {
            normalized.put("summary", summaryText);
        }
        return normalized;
    }

    private Map<String, Object> defaultCharacter() {
        Map<String, Object> character = new LinkedHashMap<>();
        character.put("id", "char_001");
        character.put("name", "主角");
        character.put("role", "主角");
        return character;
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private List<Object> listValue(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        if (value == null) {
            return List.of();
        }
        return List.of(value);
    }

    private Map<String, Object> mutableMap(Map<?, ?> source) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                target.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return target;
    }

    private int integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        try {
            return Integer.parseInt(String.valueOf(value).replaceAll("\\D+", ""));
        } catch (NumberFormatException exception) {
            return Integer.MIN_VALUE;
        }
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String excerpt(String value, int maxLength) {
        String compact = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (maxLength <= 0) {
            return "";
        }
        if (compact.length() <= maxLength) {
            return compact;
        }
        return compact.substring(0, maxLength) + "...";
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }
        String compact = value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .trim();
        if (compact.length() <= 800) {
            return compact;
        }
        return compact.substring(0, 800) + "...";
    }

    private ScriptGenerationResult reviewRequired(ProjectWorkspace project,
                                                  GenerationTrace trace,
                                                  String message) {
        return ScriptGenerationResult.needsReview(
                bestAvailableYaml(project, trace),
                trace.rawLlmResponse(),
                message,
                trace.failedEpisodes()
        );
    }

    private String bestAvailableYaml(ProjectWorkspace project, GenerationTrace trace) {
        if (trace.candidateYaml() != null && !trace.candidateYaml().isBlank()) {
            return trace.candidateYaml();
        }
        if (trace.partialRoot() == null) {
            return "";
        }
        try {
            Map<String, Object> partial = new LinkedHashMap<>(trace.partialRoot());
            partial.put("episodes", new ArrayList<>(trace.partialEpisodes()));
            return toYaml(partial, project);
        } catch (RuntimeException exception) {
            log.warn("Unable to serialize partial LLM draft for review: {}", exception.getMessage());
            return "";
        }
    }

    private String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private Map<String, Object> scriptSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("project", projectSchema());
        properties.put("characters", arraySchema(characterSchema(), 1));
        properties.put("episodes", arraySchema(episodeSchema(), 1));
        return objectSchema(List.of("project", "characters", "episodes"), properties);
    }

    private Map<String, Object> outlineSchema() {
        Map<String, Object> outlineProperties = new LinkedHashMap<>();
        outlineProperties.put("episode_id", integerSchema(1));
        outlineProperties.put("title", stringSchema());
        outlineProperties.put("summary", stringSchema());
        outlineProperties.put("chapter_numbers", arraySchema(integerSchema(1), 0));
        outlineProperties.put("key_events", arraySchema(stringSchema(), 0));

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("project", projectSchema());
        properties.put("characters", arraySchema(characterSchema(), 1));
        properties.put("episode_outlines", arraySchema(objectSchema(
                List.of("episode_id", "title", "summary"),
                outlineProperties
        ), 1));
        return objectSchema(List.of("project", "characters", "episode_outlines"), properties);
    }

    private Map<String, Object> episodeEnvelopeSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("episode", episodeSchema());
        return objectSchema(List.of("episode"), properties);
    }

    private Map<String, Object> projectSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("title", stringSchema());
        properties.put("source_type", enumSchema("novel"));
        properties.put("script_type", enumSchema("web_drama", "short_drama", "movie", "stage_play"));
        properties.put("language", enumSchema("zh-CN", "en-US"));
        properties.put("target_episodes", integerSchema(1));
        properties.put("summary", stringSchema());
        return objectSchema(List.of("title", "source_type", "script_type", "language", "target_episodes"), properties);
    }

    private Map<String, Object> characterSchema() {
        Map<String, Object> relationshipProperties = new LinkedHashMap<>();
        relationshipProperties.put("target", stringSchema());
        relationshipProperties.put("relation", stringSchema());

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", stringSchema());
        properties.put("name", stringSchema());
        properties.put("role", stringSchema());
        properties.put("description", stringSchema());
        properties.put("motivation", stringSchema());
        properties.put("relationships", arraySchema(objectSchema(List.of("target", "relation"), relationshipProperties), 0));
        return objectSchema(List.of("id", "name", "role"), properties);
    }

    private Map<String, Object> episodeSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("episode_id", integerSchema(1));
        properties.put("title", stringSchema());
        properties.put("summary", stringSchema());
        properties.put("scenes", arraySchema(sceneSchema(), 1));
        return objectSchema(List.of("episode_id", "title", "summary", "scenes"), properties);
    }

    private Map<String, Object> sceneSchema() {
        Map<String, Object> dialogueProperties = new LinkedHashMap<>();
        dialogueProperties.put("character", stringSchema());
        dialogueProperties.put("line", stringSchema());

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("scene_id", stringSchema());
        properties.put("title", stringSchema());
        properties.put("location", stringSchema());
        properties.put("time", stringSchema());
        properties.put("characters", arraySchema(stringSchema(), 1));
        properties.put("action", stringSchema());
        properties.put("dialogues", arraySchema(objectSchema(List.of("character", "line"), dialogueProperties), 1));
        return objectSchema(
                List.of("scene_id", "title", "location", "time", "characters", "action", "dialogues"),
                properties
        );
    }

    private Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("required", required);
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> arraySchema(Map<String, Object> itemSchema, int minItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", itemSchema);
        if (minItems > 0) {
            schema.put("minItems", minItems);
        }
        return schema;
    }

    private Map<String, Object> stringSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        return schema;
    }

    private Map<String, Object> integerSchema(int minimum) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        schema.put("minimum", minimum);
        return schema;
    }

    private Map<String, Object> enumSchema(String... values) {
        Map<String, Object> schema = stringSchema();
        schema.put("enum", List.of(values));
        return schema;
    }

    private record Draft(String yaml, String raw) {
    }

    private static class GenerationTrace {

        private final StringBuilder rawResponses = new StringBuilder();
        private Map<String, Object> partialRoot;
        private List<Object> partialEpisodes = List.of();
        private String candidateYaml;
        private final List<FailedEpisode> episodeFailures = new ArrayList<>();

        void appendRaw(String stage, String rawResponse) {
            if (rawResponse == null || rawResponse.isBlank()) {
                return;
            }
            if (rawResponses.length() > 0) {
                rawResponses.append("\n\n");
            }
            rawResponses.append("--- ").append(stage).append(" ---\n").append(rawResponse);
        }

        void setPartial(Map<String, Object> root, List<Object> episodes) {
            this.partialRoot = root;
            this.partialEpisodes = episodes;
        }

        void setCandidateYaml(String candidateYaml) {
            this.candidateYaml = candidateYaml;
        }

        int rawLength() {
            return rawResponses.length();
        }

        String rawSince(int start) {
            if (start < 0 || start >= rawResponses.length()) {
                return "";
            }
            return rawResponses.substring(start).trim();
        }

        void addEpisodeFailure(int episodeIndex, String reason, String rawResponse) {
            episodeFailures.add(FailedEpisode.needsReview(
                    episodeIndex,
                    "第 " + episodeIndex + " 集生成失败：" + reason,
                    rawResponse
            ));
        }

        boolean hasEpisodeFailures() {
            return !episodeFailures.isEmpty();
        }

        String failureMessage() {
            return episodeFailures.stream()
                    .map(FailedEpisode::reason)
                    .reduce((first, second) -> first + "；" + second)
                    .orElse("LLM 剧集生成失败")
                    + "。原始 LLM 回复和已完成剧集已保留，请人工审核。";
        }

        String rawLlmResponse() {
            return rawResponses.toString();
        }

        Map<String, Object> partialRoot() {
            return partialRoot;
        }

        List<Object> partialEpisodes() {
            return partialEpisodes;
        }

        String candidateYaml() {
            return candidateYaml;
        }

        List<FailedEpisode> failedEpisodes() {
            return List.copyOf(episodeFailures);
        }
    }

    private static class StructuredResponseException extends RuntimeException {

        private final String raw;

        StructuredResponseException(String message, String raw) {
            super(message);
            this.raw = raw;
        }

        StructuredResponseException(String message, String raw, Throwable cause) {
            super(message, cause);
            this.raw = raw;
        }

        String raw() {
            return raw;
        }
    }
}
