package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class YamlScriptValidator {

    private static final Pattern FENCED_BLOCK = Pattern.compile("(?s)```(?:yaml|yml)?\\s*\\R?(.*?)\\R?```");
    private static final Pattern DASH_KEY_WITHOUT_SPACE = Pattern.compile("^(\\s*)-([A-Za-z_][\\w-]*:.*)$");
    private static final Pattern KEY_VALUE_WITHOUT_SPACE = Pattern.compile("^(\\s*(?:-\\s*)?[A-Za-z_][\\w-]*):([^\\s\"'].*)$");
    private static final Pattern FULL_WIDTH_KEY_SEPARATOR = Pattern.compile("^(\\s*(?:-\\s*)?[A-Za-z_][\\w-]*)：(.*)$");
    private static final Pattern SCALAR_KEY_VALUE = Pattern.compile("^(\\s*(?:-\\s*)?[A-Za-z_][\\w-]*:\\s*)(.*)$");
    private static final Set<String> SCRIPT_TYPES = Set.of("web_drama", "short_drama", "movie", "stage_play");
    private static final Set<String> LANGUAGES = Set.of("zh-CN", "en-US");
    private static final List<String> ROOT_KEYS = List.of("project:", "characters:", "episodes:");
    private static final Set<String> ROOT_FIELD_NAMES = Set.of("project", "characters", "episodes");
    private static final List<String> WRAPPER_KEYS = List.of(
            "script", "data", "result", "output", "yaml", "content", "response", "script_result"
    );

    public ValidationResult validate(String yamlText) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (yamlText == null || yamlText.isBlank()) {
            return new ValidationResult(false, List.of("YAML 内容不能为空"), List.of());
        }

        Object parsed;
        try {
            parsed = new Yaml().load(yamlText);
        } catch (YAMLException exception) {
            return new ValidationResult(false, List.of("YAML 解析失败: " + exception.getMessage()), List.of());
        }

        if (!(parsed instanceof Map<?, ?> root)) {
            return new ValidationResult(false, List.of("YAML 顶层必须是对象"), List.of());
        }

        validateProject(requireObject(root, "project", "project", errors), errors);
        validateCharacters(root.get("characters"), errors, warnings);
        validateEpisodes(root.get("episodes"), errors, warnings);

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    public String repair(String yamlText) {
        return repair(yamlText, null);
    }

    public String repair(String yamlText, ProjectWorkspace project) {
        if (yamlText == null) {
            return "";
        }
        String repaired = normalizeText(yamlText);
        String fencedBlock = extractFencedBlock(repaired);
        if (fencedBlock != null) {
            repaired = fencedBlock;
        } else {
            repaired = stripNarration(repaired);
        }

        List<String> lines = Arrays.stream(repaired.split("\n", -1))
                .map(this::repairLine)
                .toList();

        String joined = String.join("\n", lines).trim();
        if (joined.isBlank()) {
            return "";
        }
        return normalizeYamlShape(joined, project);
    }

    private void validateProject(Map<?, ?> project, List<String> errors) {
        if (project.isEmpty()) {
            return;
        }
        requireString(project, "title", "project.title", errors);
        requireEnum(project, "source_type", "project.source_type", Set.of("novel"), errors);
        requireEnum(project, "script_type", "project.script_type", SCRIPT_TYPES, errors);
        requireEnum(project, "language", "project.language", LANGUAGES, errors);
        requireInteger(project, "target_episodes", "project.target_episodes", 1, errors);
        optionalString(project, "summary", "project.summary", errors);
    }

    private void validateCharacters(Object charactersValue, List<String> errors, List<String> warnings) {
        if (!(charactersValue instanceof List<?> characters) || characters.isEmpty()) {
            errors.add("characters 必须是至少包含 1 个角色的数组");
            return;
        }

        for (int i = 0; i < characters.size(); i++) {
            String path = "characters[" + i + "]";
            Map<?, ?> character = requireObject(characters.get(i), path, errors);
            if (character.isEmpty()) {
                continue;
            }
            requireString(character, "id", path + ".id", errors);
            requireString(character, "name", path + ".name", errors);
            requireString(character, "role", path + ".role", errors);
            optionalString(character, "description", path + ".description", errors);
            optionalString(character, "motivation", path + ".motivation", errors);
            validateRelationships(character.get("relationships"), path, errors);
            if (!character.containsKey("motivation")) {
                warnings.add(path + ".motivation 建议填写人物动机");
            }
        }
    }

    private void validateEpisodes(Object episodesValue, List<String> errors, List<String> warnings) {
        if (!(episodesValue instanceof List<?> episodes) || episodes.isEmpty()) {
            errors.add("episodes 必须是至少包含 1 集的数组");
            return;
        }

        for (int i = 0; i < episodes.size(); i++) {
            String path = "episodes[" + i + "]";
            Map<?, ?> episode = requireObject(episodes.get(i), path, errors);
            if (episode.isEmpty()) {
                continue;
            }
            requireInteger(episode, "episode_id", path + ".episode_id", 1, errors);
            requireString(episode, "title", path + ".title", errors);
            requireString(episode, "summary", path + ".summary", errors);

            Object scenesValue = episode.get("scenes");
            if (!(scenesValue instanceof List<?> scenes) || scenes.isEmpty()) {
                errors.add(path + ".scenes 必须是至少包含 1 个场景的数组");
                continue;
            }
            validateScenes(scenes, path, errors, warnings);
        }
    }

    private void validateScenes(List<?> scenes, String episodePath, List<String> errors, List<String> warnings) {
        for (int i = 0; i < scenes.size(); i++) {
            String path = episodePath + ".scenes[" + i + "]";
            Map<?, ?> scene = requireObject(scenes.get(i), path, errors);
            if (scene.isEmpty()) {
                continue;
            }
            requireString(scene, "scene_id", path + ".scene_id", errors);
            requireString(scene, "title", path + ".title", errors);
            requireString(scene, "location", path + ".location", errors);
            requireString(scene, "time", path + ".time", errors);
            requireString(scene, "action", path + ".action", errors);
            validateStringArray(scene.get("characters"), path + ".characters", 1, errors);

            Object dialoguesValue = scene.get("dialogues");
            if (!(dialoguesValue instanceof List<?> dialogues)) {
                errors.add(path + ".dialogues 必须是数组");
            } else if (dialogues.isEmpty()) {
                errors.add(path + ".dialogues 必须至少包含 1 条对白");
            } else {
                validateDialogues(dialogues, path, errors);
            }
        }
    }

    private void validateDialogues(List<?> dialogues, String scenePath, List<String> errors) {
        for (int i = 0; i < dialogues.size(); i++) {
            String path = scenePath + ".dialogues[" + i + "]";
            Map<?, ?> dialogue = requireObject(dialogues.get(i), path, errors);
            if (dialogue.isEmpty()) {
                continue;
            }
            requireString(dialogue, "character", path + ".character", errors);
            requireString(dialogue, "line", path + ".line", errors);
        }
    }

    private void validateRelationships(Object relationshipsValue, String characterPath, List<String> errors) {
        if (relationshipsValue == null) {
            return;
        }
        if (!(relationshipsValue instanceof List<?> relationships)) {
            errors.add(characterPath + ".relationships 必须是数组");
            return;
        }
        for (int i = 0; i < relationships.size(); i++) {
            String path = characterPath + ".relationships[" + i + "]";
            Map<?, ?> relationship = requireObject(relationships.get(i), path, errors);
            if (relationship.isEmpty()) {
                continue;
            }
            requireString(relationship, "target", path + ".target", errors);
            requireString(relationship, "relation", path + ".relation", errors);
        }
    }

    private void validateStringArray(Object value, String path, int minItems, List<String> errors) {
        if (!(value instanceof List<?> values)) {
            errors.add(path + " 必须是数组");
            return;
        }
        if (values.size() < minItems) {
            errors.add(path + " 必须至少包含 " + minItems + " 项");
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            if (!(values.get(i) instanceof String item) || item.isBlank()) {
                errors.add(path + "[" + i + "] 必须是非空字符串");
            }
        }
    }

    private void requireString(Map<?, ?> map, String key, String path, List<String> errors) {
        Object value = map.get(key);
        if (value == null) {
            errors.add("缺少必填字段 " + path);
        } else if (!(value instanceof String text)) {
            errors.add(path + " 必须是字符串");
        } else if (text.isBlank()) {
            errors.add(path + " 不能为空");
        }
    }

    private void optionalString(Map<?, ?> map, String key, String path, List<String> errors) {
        Object value = map.get(key);
        if (value != null && !(value instanceof String)) {
            errors.add(path + " 必须是字符串");
        }
    }

    private void requireEnum(Map<?, ?> map, String key, String path, Set<String> allowedValues, List<String> errors) {
        Object value = map.get(key);
        if (value == null) {
            errors.add("缺少必填字段 " + path);
        } else if (!(value instanceof String text)) {
            errors.add(path + " 必须是字符串");
        } else if (text.isBlank()) {
            errors.add(path + " 不能为空");
        } else if (!allowedValues.contains(text)) {
            errors.add(path + " 必须是以下值之一: " + String.join(", ", allowedValues));
        }
    }

    private void requireInteger(Map<?, ?> map, String key, String path, int minValue, List<String> errors) {
        Object value = map.get(key);
        if (value == null) {
            errors.add("缺少必填字段 " + path);
            return;
        }
        if (!isIntegerNumber(value)) {
            errors.add(path + " 必须是整数");
            return;
        }
        Number number = (Number) value;
        if (number.longValue() < minValue) {
            errors.add(path + " 必须大于等于 " + minValue);
        }
    }

    private Map<?, ?> requireObject(Map<?, ?> parent, String key, String path, List<String> errors) {
        Object value = parent.get(key);
        if (value == null) {
            errors.add("缺少 " + path + " 对象");
            return Map.of();
        }
        return requireObject(value, path, errors);
    }

    private boolean isIntegerNumber(Object value) {
        return value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long
                || value instanceof BigInteger;
    }

    private Map<?, ?> requireObject(Object value, String path, List<String> errors) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        errors.add(path + " 必须是对象");
        return Map.of();
    }

    private String normalizeYamlShape(String yamlText) {
        return normalizeYamlShape(yamlText, null);
    }

    private String normalizeYamlShape(String yamlText, ProjectWorkspace project) {
        return normalizeYamlShape(yamlText, project, 0);
    }

    private String normalizeYamlShape(String yamlText, ProjectWorkspace project, int depth) {
        Object parsed;
        try {
            parsed = new Yaml().load(yamlText);
        } catch (YAMLException exception) {
            return yamlText.trim() + "\n";
        }

        Object unwrapped = unwrapRoot(parsed);
        if (unwrapped instanceof String embedded && depth < 2 && looksStructured(embedded)) {
            return normalizeYamlShape(normalizeText(embedded), project, depth + 1);
        }

        Map<String, Object> normalized;
        if (unwrapped instanceof Map<?, ?> root) {
            normalized = normalizeRoot(root, project);
        } else if (unwrapped instanceof List<?> list) {
            normalized = normalizeRootList(list, project);
        } else {
            return yamlText.trim() + "\n";
        }

        return dumpYaml(normalized);
    }

    private Object unwrapRoot(Object value) {
        Object current = value;
        for (int depth = 0; depth < 4; depth++) {
            if (!(current instanceof Map<?, ?> map) || hasRootField(map)) {
                return current;
            }

            Object wrapped = firstPresent(map, WRAPPER_KEYS.toArray(String[]::new));
            if (wrapped == null && map.size() == 1) {
                Map.Entry<?, ?> onlyEntry = map.entrySet().iterator().next();
                String key = text(onlyEntry.getKey());
                if (key != null && !ROOT_FIELD_NAMES.contains(key)) {
                    wrapped = onlyEntry.getValue();
                }
            }

            if (wrapped == null || wrapped == current) {
                return current;
            }
            current = wrapped;
        }
        return current;
    }

    private Map<String, Object> normalizeRoot(Map<?, ?> root, ProjectWorkspace project) {
        Map<String, Object> normalized = new LinkedHashMap<>();

        Object episodesValue = root.get("episodes");
        if (episodesValue == null && looksLikeEpisode(root)) {
            episodesValue = List.of(root);
        }

        List<Object> normalizedEpisodes = null;
        if (episodesValue != null) {
            normalizedEpisodes = normalizeEpisodes(episodesValue);
        }

        if (root.containsKey("project") || episodesValue != null || looksLikeProject(root)) {
            normalized.put("project", normalizeProject(
                    root.containsKey("project") ? root.get("project") : root,
                    project,
                    normalizedEpisodes == null ? null : normalizedEpisodes.size()
            ));
        }

        if (root.containsKey("characters")) {
            normalized.put("characters", normalizeCharacters(root.get("characters")));
        } else if (normalizedEpisodes != null) {
            normalized.put("characters", inferCharacters(normalizedEpisodes));
        }

        if (normalizedEpisodes != null) {
            normalized.put("episodes", normalizedEpisodes);
        }

        for (Map.Entry<?, ?> entry : root.entrySet()) {
            String key = text(entry.getKey());
            if (key != null && !normalized.containsKey(key)) {
                normalized.put(key, entry.getValue());
            }
        }

        return normalized;
    }

    private Map<String, Object> normalizeRootList(List<?> list, ProjectWorkspace project) {
        Map<String, Object> merged = new LinkedHashMap<>();
        boolean mergedRootFragments = false;

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            for (String rootKey : ROOT_FIELD_NAMES) {
                if (map.containsKey(rootKey)) {
                    merged.put(rootKey, map.get(rootKey));
                    mergedRootFragments = true;
                }
            }
        }

        if (mergedRootFragments) {
            return normalizeRoot(merged, project);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        if (looksLikeEpisodeList(list)) {
            List<Object> episodes = normalizeEpisodes(list);
            root.put("project", normalizeProject(Map.of(), project, episodes.size()));
            root.put("characters", inferCharacters(episodes));
            root.put("episodes", episodes);
            return root;
        }

        root.put("characters", normalizeCharacters(list));
        return root;
    }

    private Map<String, Object> normalizeProject(Object value, ProjectWorkspace workspace, Integer episodeCount) {
        Map<String, Object> project = new LinkedHashMap<>();
        Map<?, ?> map = value instanceof Map<?, ?> source ? source : Map.of();
        AdaptationSetting setting = workspace == null ? null : workspace.getSetting();

        putStringOrDefault(project, "title", map.get("title"), defaultTitle(workspace));
        project.put("source_type", "novel");
        project.put("script_type", normalizeEnum(
                firstPresent(map, "script_type", "scriptType", "type"),
                setting == null ? null : setting.getScriptType(),
                "web_drama",
                SCRIPT_TYPES
        ));
        project.put("language", normalizeEnum(
                map.get("language"),
                setting == null ? null : setting.getLanguage(),
                "zh-CN",
                LANGUAGES
        ));

        Integer targetEpisodes = integer(firstPresent(map, "target_episodes", "targetEpisodes"));
        if (targetEpisodes == null && setting != null && setting.getTargetEpisodes() > 0) {
            targetEpisodes = setting.getTargetEpisodes();
        }
        if (targetEpisodes == null && episodeCount != null && episodeCount > 0) {
            targetEpisodes = episodeCount;
        }
        project.put("target_episodes", targetEpisodes == null || targetEpisodes < 1 ? 1 : targetEpisodes);

        putString(project, "summary", map.get("summary"));
        copyUnknownFields(project, map);
        return project;
    }

    private String defaultTitle(ProjectWorkspace workspace) {
        if (workspace != null) {
            String title = text(workspace.getTitle());
            if (title != null) {
                return title;
            }
        }
        return "AI 生成剧本";
    }

    private List<Object> normalizeCharacters(Object value) {
        List<Object> characters = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                characters.add(normalizeCharacter(list.get(i), "char_" + padded(i + 1)));
            }
        } else if (value instanceof Map<?, ?> map) {
            if (hasAnyKey(map, "id", "name", "role")) {
                characters.add(normalizeCharacter(map, "char_001"));
            } else {
                int index = 1;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    characters.add(normalizeCharacterWithKey(entry.getKey(), entry.getValue(), "char_" + padded(index)));
                    index++;
                }
            }
        } else {
            for (String name : splitScalarList(value)) {
                characters.add(normalizeCharacter(name, "char_" + padded(characters.size() + 1)));
            }
        }
        return characters;
    }

    private Map<String, Object> normalizeCharacter(Object value, String fallbackId) {
        Map<String, Object> character = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            putStringOrDefault(character, "id", map.get("id"), fallbackId);
            putStringOrDefault(character, "name", map.get("name"), String.valueOf(character.get("id")));
            putStringOrDefault(character, "role", map.get("role"), "角色");
            putString(character, "description", map.get("description"));
            putString(character, "motivation", map.get("motivation"));
            if (map.containsKey("relationships")) {
                character.put("relationships", normalizeRelationships(map.get("relationships")));
            }
            copyUnknownFields(character, map);
        } else {
            String name = text(value);
            character.put("id", fallbackId);
            character.put("name", name == null ? fallbackId : name);
            character.put("role", "角色");
        }
        return character;
    }

    private Map<String, Object> normalizeCharacterWithKey(Object key, Object value, String fallbackId) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> character = normalizeCharacter(map, fallbackId);
            if (!character.containsKey("id")) {
                putStringOrDefault(character, "id", key, fallbackId);
            }
            return character;
        }
        Map<String, Object> character = new LinkedHashMap<>();
        String id = text(key);
        String name = text(value);
        character.put("id", id == null ? fallbackId : id);
        character.put("name", name == null ? character.get("id") : name);
        character.put("role", "角色");
        return character;
    }

    private List<Object> normalizeRelationships(Object value) {
        List<Object> relationships = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                relationships.add(normalizeRelationship(item));
            }
        } else if (value instanceof Map<?, ?> map && hasAnyKey(map, "target", "relation")) {
            relationships.add(normalizeRelationship(map));
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Map<String, Object> relationship = new LinkedHashMap<>();
                putStringOrDefault(relationship, "target", entry.getKey(), "");
                putStringOrDefault(relationship, "relation", entry.getValue(), "相关");
                relationships.add(relationship);
            }
        }
        return relationships;
    }

    private Map<String, Object> normalizeRelationship(Object value) {
        Map<String, Object> relationship = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            putString(relationship, "target", map.get("target"));
            putString(relationship, "relation", map.get("relation"));
            copyUnknownFields(relationship, map);
        } else {
            putStringOrDefault(relationship, "target", value, "");
            relationship.put("relation", "相关");
        }
        return relationship;
    }

    private List<Object> normalizeEpisodes(Object value) {
        List<Object> episodes = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                episodes.add(normalizeEpisode(list.get(i), i + 1));
            }
        } else if (value instanceof Map<?, ?> map) {
            if (hasAnyKey(map, "episode_id", "title", "summary", "scenes")) {
                episodes.add(normalizeEpisode(map, 1));
            } else {
                int index = 1;
                for (Object item : map.values()) {
                    episodes.add(normalizeEpisode(item, index));
                    index++;
                }
            }
        }
        return episodes;
    }

    private List<Object> inferCharacters(List<Object> episodes) {
        Map<String, Map<String, Object>> charactersById = new LinkedHashMap<>();
        for (Object episodeValue : episodes) {
            if (!(episodeValue instanceof Map<?, ?> episode)) {
                continue;
            }
            Object scenesValue = episode.get("scenes");
            if (!(scenesValue instanceof List<?> scenes)) {
                continue;
            }
            for (Object sceneValue : scenes) {
                if (!(sceneValue instanceof Map<?, ?> scene)) {
                    continue;
                }
                Object sceneCharacters = scene.get("characters");
                if (sceneCharacters instanceof List<?> characters) {
                    for (Object character : characters) {
                        addInferredCharacter(charactersById, character);
                    }
                }
                Object dialoguesValue = scene.get("dialogues");
                if (dialoguesValue instanceof List<?> dialogues) {
                    for (Object dialogueValue : dialogues) {
                        if (dialogueValue instanceof Map<?, ?> dialogue) {
                            addInferredCharacter(charactersById, dialogue.get("character"));
                        }
                    }
                }
            }
        }

        if (charactersById.isEmpty()) {
            addInferredCharacter(charactersById, "char_001");
        }
        return new ArrayList<>(charactersById.values());
    }

    private void addInferredCharacter(Map<String, Map<String, Object>> charactersById, Object value) {
        String id = text(value);
        if (id == null || charactersById.containsKey(id)) {
            return;
        }
        Map<String, Object> character = new LinkedHashMap<>();
        character.put("id", id);
        character.put("name", id);
        character.put("role", "角色");
        charactersById.put(id, character);
    }

    private Map<String, Object> normalizeEpisode(Object value, int fallbackIndex) {
        Map<String, Object> episode = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> map)) {
            return episode;
        }
        putIntegerOrDefault(episode, "episode_id", map.get("episode_id"), fallbackIndex);
        putString(episode, "title", map.get("title"));
        putString(episode, "summary", map.get("summary"));
        episode.put("scenes", normalizeScenes(map.get("scenes"), fallbackIndex));
        copyUnknownFields(episode, map);
        return episode;
    }

    private List<Object> normalizeScenes(Object value, int episodeIndex) {
        List<Object> scenes = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                scenes.add(normalizeScene(list.get(i), episodeIndex, i + 1, null));
            }
        } else if (value instanceof Map<?, ?> map) {
            if (hasAnyKey(map, "scene_id", "title", "location", "time", "characters", "action", "dialogues")) {
                scenes.add(normalizeScene(map, episodeIndex, 1, null));
            } else {
                int index = 1;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    scenes.add(normalizeScene(entry.getValue(), episodeIndex, index, text(entry.getKey())));
                    index++;
                }
            }
        }
        return scenes;
    }

    private Map<String, Object> normalizeScene(Object value, int episodeIndex, int sceneIndex, String fallbackSceneId) {
        Map<String, Object> scene = new LinkedHashMap<>();
        if (!(value instanceof Map<?, ?> map)) {
            return scene;
        }
        String defaultSceneId = fallbackSceneId == null || fallbackSceneId.isBlank()
                ? episodeIndex + "-" + sceneIndex
                : fallbackSceneId;
        putStringOrDefault(scene, "scene_id", map.get("scene_id"), defaultSceneId);
        putString(scene, "title", map.get("title"));
        putString(scene, "location", map.get("location"));
        putString(scene, "time", map.get("time"));
        scene.put("characters", normalizeStringArray(firstPresent(map, "characters", "character_ids", "cast")));
        putString(scene, "action", firstPresent(map, "action", "description", "stage_direction"));
        scene.put("dialogues", normalizeDialogues(firstPresent(map, "dialogues", "dialogue", "lines"), firstSceneCharacter(scene)));
        copyUnknownFields(scene, map);
        return scene;
    }

    private List<Object> normalizeDialogues(Object value, String fallbackCharacter) {
        List<Object> dialogues = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                dialogues.add(normalizeDialogue(item, fallbackCharacter));
            }
        } else if (value instanceof Map<?, ?> map) {
            if (hasAnyKey(map, "character", "speaker", "line", "text", "content")) {
                dialogues.add(normalizeDialogue(map, fallbackCharacter));
            } else {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    Map<String, Object> dialogue = new LinkedHashMap<>();
                    putStringOrDefault(dialogue, "character", entry.getKey(), fallbackCharacter);
                    putStringOrDefault(dialogue, "line", entry.getValue(), "");
                    dialogues.add(dialogue);
                }
            }
        } else if (text(value) != null) {
            Map<String, Object> dialogue = new LinkedHashMap<>();
            dialogue.put("character", fallbackCharacter);
            dialogue.put("line", text(value));
            dialogues.add(dialogue);
        }
        return dialogues;
    }

    private Map<String, Object> normalizeDialogue(Object value, String fallbackCharacter) {
        Map<String, Object> dialogue = new LinkedHashMap<>();
        if (value instanceof Map<?, ?> map) {
            putStringOrDefault(dialogue, "character", firstPresent(map, "character", "speaker", "role"), fallbackCharacter);
            putString(dialogue, "line", firstPresent(map, "line", "text", "content", "dialogue"));
            copyUnknownFields(dialogue, map);
        } else {
            dialogue.put("character", fallbackCharacter);
            putString(dialogue, "line", value);
        }
        return dialogue;
    }

    private String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Integer integer(Object value) {
        if (value instanceof Number number) {
            double doubleValue = number.doubleValue();
            if (doubleValue % 1 == 0 && doubleValue >= Integer.MIN_VALUE && doubleValue <= Integer.MAX_VALUE) {
                return number.intValue();
            }
            return null;
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            Matcher matcher = Pattern.compile("\\d+").matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }
            return null;
        }
    }

    private void putString(Map<String, Object> target, String key, Object value) {
        String text = text(value);
        if (text != null) {
            target.put(key, text);
        }
    }

    private void putStringOrDefault(Map<String, Object> target, String key, Object value, String fallback) {
        String text = text(value);
        target.put(key, text == null || text.isBlank() ? fallback : text);
    }

    private void putInteger(Map<String, Object> target, String key, Object value) {
        Integer integer = integer(value);
        if (integer != null) {
            target.put(key, integer);
        }
    }

    private void putIntegerOrDefault(Map<String, Object> target, String key, Object value, int fallback) {
        Integer integer = integer(value);
        target.put(key, integer == null ? fallback : integer);
    }

    private String normalizeEnum(Object primary, Object fallback, String defaultValue, Set<String> allowedValues) {
        String primaryValue = normalizeEnumValue(text(primary), allowedValues);
        if (primaryValue != null) {
            return primaryValue;
        }
        String fallbackValue = normalizeEnumValue(text(fallback), allowedValues);
        return fallbackValue == null ? defaultValue : fallbackValue;
    }

    private String normalizeEnumValue(String value, Set<String> allowedValues) {
        if (value == null) {
            return null;
        }
        if (allowedValues.contains(value)) {
            return value;
        }
        String normalized = switch (value) {
            case "网剧", "网络剧", "web drama", "web series" -> "web_drama";
            case "短剧", "微短剧", "short drama", "mini drama" -> "short_drama";
            case "电影", "film" -> "movie";
            case "舞台剧", "话剧", "stage play" -> "stage_play";
            case "中文", "简体中文", "zh", "zh_CN" -> "zh-CN";
            case "英文", "英语", "en", "en_US" -> "en-US";
            default -> null;
        };
        return normalized != null && allowedValues.contains(normalized) ? normalized : null;
    }

    private List<String> normalizeStringArray(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                addStringArrayItem(values, item);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Object item : map.values()) {
                addStringArrayItem(values, item);
            }
        } else {
            values.addAll(splitScalarList(value));
        }
        return values;
    }

    private void addStringArrayItem(List<String> values, Object item) {
        if (item instanceof Map<?, ?> map) {
            Object candidate = firstPresent(map, "id", "character", "name");
            String text = text(candidate);
            if (text != null) {
                values.add(text);
            }
            return;
        }
        values.addAll(splitScalarList(item));
    }

    private List<String> splitScalarList(Object value) {
        String text = text(value);
        if (text == null) {
            return List.of();
        }
        return Arrays.stream(text.split("[,，、/\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }
        return null;
    }

    private String firstSceneCharacter(Map<String, Object> scene) {
        Object characters = scene.get("characters");
        if (characters instanceof List<?> list && !list.isEmpty()) {
            String character = text(list.get(0));
            if (character != null) {
                return character;
            }
        }
        return "char_001";
    }

    private void copyUnknownFields(Map<String, Object> target, Map<?, ?> source) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            String key = text(entry.getKey());
            if (key != null && !target.containsKey(key)) {
                target.put(key, entry.getValue());
            }
        }
    }

    private boolean hasAnyKey(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRootField(Map<?, ?> map) {
        for (String key : ROOT_FIELD_NAMES) {
            if (map.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeProject(Map<?, ?> map) {
        return hasAnyKey(map, "source_type", "script_type", "scriptType", "language", "target_episodes", "targetEpisodes")
                && !looksLikeEpisode(map);
    }

    private boolean looksLikeEpisode(Map<?, ?> map) {
        return hasAnyKey(map, "episode_id", "episodeId", "scenes")
                || (map.containsKey("title") && map.containsKey("summary") && map.containsKey("scenes"));
    }

    private boolean looksLikeEpisodeList(List<?> list) {
        if (list.isEmpty()) {
            return false;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map && looksLikeEpisode(map)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksStructured(String value) {
        String text = normalizeText(value);
        return ROOT_KEYS.stream().anyMatch(text::contains)
                || text.startsWith("{")
                || text.startsWith("[")
                || text.startsWith("-");
    }

    private String padded(int value) {
        return String.format("%03d", value);
    }

    private String dumpYaml(Map<String, Object> root) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setWidth(120);
        return new Yaml(options).dump(root).trim() + "\n";
    }

    private String normalizeText(String text) {
        return text
                .replace("\uFEFF", "")
                .replace("\u00A0", " ")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\t", "  ")
                .replace('“', '"')
                .replace('”', '"')
                .replace('‘', '\'')
                .replace('’', '\'')
                .trim();
    }

    private String extractFencedBlock(String text) {
        Matcher matcher = FENCED_BLOCK.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private String stripNarration(String text) {
        String[] lines = text.split("\n", -1);
        int start = firstYamlLine(lines);
        if (start < 0) {
            return text;
        }

        List<String> yamlLines = new ArrayList<>();
        boolean sawContent = false;
        for (int i = start; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (sawContent) {
                    yamlLines.add(line);
                }
                continue;
            }
            if (sawContent && isLikelyTrailingNarration(line)) {
                break;
            }
            yamlLines.add(line);
            sawContent = true;
        }
        return String.join("\n", yamlLines);
    }

    private int firstYamlLine(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            String trimmed = repairLine(lines[i]).trim();
            if (isYamlRootStart(trimmed)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLikelyTrailingNarration(String line) {
        String trimmed = line.trim();
        return !line.startsWith(" ")
                && !line.startsWith("-")
                && !isYamlRootStart(trimmed)
                && !trimmed.contains(":");
    }

    private boolean isYamlRootStart(String trimmed) {
        return ROOT_KEYS.contains(trimmed)
                || WRAPPER_KEYS.stream().anyMatch(key -> trimmed.equals(key + ":"))
                || trimmed.startsWith("- episode_id:")
                || trimmed.startsWith("- episodeId:")
                || trimmed.startsWith("{")
                || trimmed.startsWith("[");
    }

    private String repairLine(String line) {
        String repaired = line.replace("\t", "  ");

        Matcher fullWidthColonMatcher = FULL_WIDTH_KEY_SEPARATOR.matcher(repaired);
        if (fullWidthColonMatcher.matches()) {
            repaired = fullWidthColonMatcher.group(1) + ":" + fullWidthColonMatcher.group(2);
        }

        Matcher dashMatcher = DASH_KEY_WITHOUT_SPACE.matcher(repaired);
        if (dashMatcher.matches()) {
            repaired = dashMatcher.group(1) + "- " + dashMatcher.group(2);
        }

        Matcher keyValueMatcher = KEY_VALUE_WITHOUT_SPACE.matcher(repaired);
        if (keyValueMatcher.matches()) {
            repaired = keyValueMatcher.group(1) + ": " + keyValueMatcher.group(2);
        }

        repaired = repairUnsafeDoubleQuotedScalar(repaired);

        return repaired.stripTrailing();
    }

    private String repairUnsafeDoubleQuotedScalar(String line) {
        Matcher matcher = SCALAR_KEY_VALUE.matcher(line);
        if (!matcher.matches()) {
            return line;
        }

        String value = matcher.group(2).stripTrailing();
        if (!value.startsWith("\"")) {
            return line;
        }

        List<Integer> quoteIndexes = unescapedQuoteIndexes(value);
        if (quoteIndexes.size() == 2 && hasOnlyTrailingCommentOrWhitespace(value, quoteIndexes.get(1))) {
            return line;
        }

        int contentEnd = value.length();
        if (quoteIndexes.size() >= 4 && quoteIndexes.size() % 2 == 0) {
            int lastQuote = quoteIndexes.get(quoteIndexes.size() - 1);
            if (hasOnlyTrailingCommentOrWhitespace(value, lastQuote)) {
                contentEnd = lastQuote;
            }
        }
        String content = value.substring(1, contentEnd);
        return matcher.group(1) + "'" + content.replace("'", "''") + "'";
    }

    private boolean hasOnlyTrailingCommentOrWhitespace(String value, int quoteIndex) {
        String tail = value.substring(quoteIndex + 1).trim();
        return tail.isEmpty() || tail.startsWith("#");
    }

    private List<Integer> unescapedQuoteIndexes(String value) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '"' && !isEscaped(value, i)) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private boolean isEscaped(String value, int quoteIndex) {
        int slashCount = 0;
        for (int i = quoteIndex - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }
}
