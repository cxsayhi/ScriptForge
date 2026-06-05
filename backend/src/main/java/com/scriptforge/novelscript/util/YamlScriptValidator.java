package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.dto.response.ValidationResult;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class YamlScriptValidator {

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

        validateProject(asMap(root.get("project")), errors);
        validateCharacters(root.get("characters"), errors, warnings);
        validateEpisodes(root.get("episodes"), errors, warnings);

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    public String repair(String yamlText) {
        if (yamlText == null) {
            return "";
        }
        String repaired = yamlText.trim()
                .replace("\t", "  ")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        if (repaired.startsWith("```")) {
            repaired = repaired.replaceFirst("^```(?:yaml|yml)?\\s*", "");
            repaired = repaired.replaceFirst("\\s*```$", "");
        }
        return repaired.trim() + "\n";
    }

    private void validateProject(Map<?, ?> project, List<String> errors) {
        if (project.isEmpty()) {
            errors.add("缺少 project 对象");
            return;
        }
        require(project, "title", "project.title", errors);
        require(project, "source_type", "project.source_type", errors);
        require(project, "script_type", "project.script_type", errors);
        require(project, "language", "project.language", errors);
        require(project, "target_episodes", "project.target_episodes", errors);
    }

    private void validateCharacters(Object charactersValue, List<String> errors, List<String> warnings) {
        if (!(charactersValue instanceof List<?> characters) || characters.isEmpty()) {
            errors.add("characters 必须是至少包含 1 个角色的数组");
            return;
        }

        for (int i = 0; i < characters.size(); i++) {
            Map<?, ?> character = asMap(characters.get(i));
            String path = "characters[" + i + "]";
            require(character, "id", path + ".id", errors);
            require(character, "name", path + ".name", errors);
            require(character, "role", path + ".role", errors);
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
            Map<?, ?> episode = asMap(episodes.get(i));
            String path = "episodes[" + i + "]";
            require(episode, "episode_id", path + ".episode_id", errors);
            require(episode, "title", path + ".title", errors);
            require(episode, "summary", path + ".summary", errors);

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
            Map<?, ?> scene = asMap(scenes.get(i));
            String path = episodePath + ".scenes[" + i + "]";
            require(scene, "scene_id", path + ".scene_id", errors);
            require(scene, "title", path + ".title", errors);
            require(scene, "location", path + ".location", errors);
            require(scene, "time", path + ".time", errors);
            require(scene, "characters", path + ".characters", errors);
            require(scene, "action", path + ".action", errors);
            Object dialoguesValue = scene.get("dialogues");
            if (!(dialoguesValue instanceof List<?> dialogues)) {
                errors.add(path + ".dialogues 必须是数组");
            } else if (dialogues.isEmpty()) {
                warnings.add(path + ".dialogues 当前没有对白");
            } else {
                validateDialogues(dialogues, path, errors);
            }
        }
    }

    private void validateDialogues(List<?> dialogues, String scenePath, List<String> errors) {
        for (int i = 0; i < dialogues.size(); i++) {
            Map<?, ?> dialogue = asMap(dialogues.get(i));
            String path = scenePath + ".dialogues[" + i + "]";
            require(dialogue, "character", path + ".character", errors);
            require(dialogue, "line", path + ".line", errors);
        }
    }

    private void require(Map<?, ?> map, String key, String path, List<String> errors) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            errors.add("缺少必填字段 " + path);
        }
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }
}
