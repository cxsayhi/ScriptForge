package com.scriptforge.novelscript.util;

import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

@Component
public class MarkdownExporter {

    public String toMarkdown(String yamlText) {
        Object parsed = new Yaml().load(yamlText);
        if (!(parsed instanceof Map<?, ?> root)) {
            return "# Script\n\n```yaml\n" + yamlText + "\n```\n";
        }

        StringBuilder builder = new StringBuilder();
        Map<?, ?> project = asMap(root.get("project"));
        builder.append("# ").append(value(project, "title", "Untitled Script")).append("\n\n");
        appendIfPresent(builder, "Summary", project.get("summary"));

        builder.append("## Characters\n\n");
        for (Object characterObject : asList(root.get("characters"))) {
            Map<?, ?> character = asMap(characterObject);
            builder.append("- **").append(value(character, "name", "Unknown")).append("**");
            builder.append(" (").append(value(character, "role", "Role")).append(")");
            appendInline(builder, character.get("description"));
            builder.append("\n");
        }

        builder.append("\n## Episodes\n\n");
        for (Object episodeObject : asList(root.get("episodes"))) {
            Map<?, ?> episode = asMap(episodeObject);
            builder.append("### Episode ").append(value(episode, "episode_id", "?"));
            builder.append(": ").append(value(episode, "title", "Untitled")).append("\n\n");
            appendIfPresent(builder, "Summary", episode.get("summary"));

            for (Object sceneObject : asList(episode.get("scenes"))) {
                Map<?, ?> scene = asMap(sceneObject);
                builder.append("#### ").append(value(scene, "scene_id", "?"));
                builder.append(" ").append(value(scene, "title", "Untitled Scene")).append("\n\n");
                builder.append("- Location: ").append(value(scene, "location", "-")).append("\n");
                builder.append("- Time: ").append(value(scene, "time", "-")).append("\n\n");
                appendIfPresent(builder, "Action", scene.get("action"));
                for (Object dialogueObject : asList(scene.get("dialogues"))) {
                    Map<?, ?> dialogue = asMap(dialogueObject);
                    builder.append("> **").append(value(dialogue, "character", "character"));
                    builder.append("**: ").append(value(dialogue, "line", "")).append("\n\n");
                }
            }
        }
        return builder.toString();
    }

    private void appendIfPresent(StringBuilder builder, String label, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            builder.append("**").append(label).append(":** ").append(value).append("\n\n");
        }
    }

    private void appendInline(StringBuilder builder, Object value) {
        if (value != null && !String.valueOf(value).isBlank()) {
            builder.append(": ").append(value);
        }
    }

    private String value(Map<?, ?> map, String key, String fallback) {
        Object value = map.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return fallback;
        }
        return String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }
}
