package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.dto.response.ValidationResult;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final Set<String> SCRIPT_TYPES = Set.of("web_drama", "short_drama", "movie", "stage_play");
    private static final Set<String> LANGUAGES = Set.of("zh-CN", "en-US");
    private static final List<String> ROOT_KEYS = List.of("project:", "characters:", "episodes:");

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
        return joined + "\n";
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
            if (ROOT_KEYS.contains(trimmed)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isLikelyTrailingNarration(String line) {
        String trimmed = line.trim();
        return !line.startsWith(" ")
                && !line.startsWith("-")
                && ROOT_KEYS.stream().noneMatch(trimmed::startsWith)
                && !trimmed.contains(":");
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

        return repaired.stripTrailing();
    }
}
