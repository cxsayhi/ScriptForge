package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class RuleBasedScriptGenerationAgent implements ScriptGenerationAgent {

    @Override
    public String generate(ProjectWorkspace project) {
        AdaptationSetting setting = project.getSetting();
        List<Chapter> chapters = project.getNovelContent().getChapters();
        int targetEpisodes = Math.max(1, setting.getTargetEpisodes());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("project", projectBlock(project, setting, chapters));
        root.put("characters", characterBlocks());
        root.put("episodes", episodeBlocks(chapters, targetEpisodes, setting));

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        options.setWidth(100);
        return new Yaml(options).dump(root);
    }

    private Map<String, Object> projectBlock(ProjectWorkspace project, AdaptationSetting setting, List<Chapter> chapters) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("title", project.getTitle());
        block.put("source_type", "novel");
        block.put("script_type", setting.getScriptType());
        block.put("language", setting.getLanguage());
        block.put("target_episodes", setting.getTargetEpisodes());
        block.put("summary", "根据 " + chapters.size() + " 个章节生成的" + setting.getStyle() + "风格剧本初稿。");
        return block;
    }

    private List<Map<String, Object>> characterBlocks() {
        List<Map<String, Object>> characters = new ArrayList<>();
        characters.add(character("char_001", "主角", "主角", "承担主要叙事视角的人物。", "推动真相浮出水面。"));
        characters.add(character("char_002", "关键关系人", "配角", "与主角关系密切，提供线索和情感压力。", "保护秘密并完成自己的选择。"));
        characters.add(character("char_003", "阻力制造者", "反派", "不断制造误导和外部冲突的人物。", "阻止主角接近核心答案。"));
        return characters;
    }

    private Map<String, Object> character(String id, String name, String role, String description, String motivation) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("id", id);
        block.put("name", name);
        block.put("role", role);
        block.put("description", description);
        block.put("motivation", motivation);
        block.put("relationships", new ArrayList<>());
        return block;
    }

    private List<Map<String, Object>> episodeBlocks(List<Chapter> chapters, int targetEpisodes, AdaptationSetting setting) {
        List<Map<String, Object>> episodes = new ArrayList<>();
        for (int i = 1; i <= targetEpisodes; i++) {
            Chapter source = chapters.get(Math.min(i - 1, chapters.size() - 1));
            Map<String, Object> episode = new LinkedHashMap<>();
            episode.put("episode_id", i);
            episode.put("title", "第 " + i + " 集：" + source.title());
            episode.put("summary", summary(source.content(), setting.getAdaptationIntensity()));
            episode.put("scenes", sceneBlocks(i, source, setting));
            episodes.add(episode);
        }
        return episodes;
    }

    private List<Map<String, Object>> sceneBlocks(int episodeIndex, Chapter source, AdaptationSetting setting) {
        List<Map<String, Object>> scenes = new ArrayList<>();
        scenes.add(scene(episodeIndex, 1, "引入冲突", "主要场景", "白天", "主角从原文章节中提炼出新的目标。", source, setting));
        scenes.add(scene(episodeIndex, 2, "关系推进", "转折空间", "夜晚", "人物关系出现裂缝，关键信息被重新解释。", source, setting));
        return scenes;
    }

    private Map<String, Object> scene(int episodeIndex, int sceneIndex, String title, String location, String time,
                                      String action, Chapter source, AdaptationSetting setting) {
        Map<String, Object> scene = new LinkedHashMap<>();
        scene.put("scene_id", episodeIndex + "-" + sceneIndex);
        scene.put("title", title);
        scene.put("location", location);
        scene.put("time", time);
        scene.put("characters", List.of("char_001", "char_002"));
        scene.put("action", action + " 改编参考：" + excerpt(source.content()));
        scene.put("dialogues", List.of(
                dialogue("char_001", setting.isKeepOriginalDialogues() ? "这件事和原来的描述不一样。" : "我们得重新判断眼前的局面。"),
                dialogue("char_002", "如果继续追下去，答案可能比想象中更难接受。")
        ));
        return scene;
    }

    private Map<String, Object> dialogue(String character, String line) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("character", character);
        block.put("line", line);
        return block;
    }

    private String summary(String text, String intensity) {
        return intensity + "：围绕“" + excerpt(text) + "”展开本集核心冲突。";
    }

    private String excerpt(String text) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (compact.isBlank()) {
            return "原文关键情节";
        }
        return compact.length() > 52 ? compact.substring(0, 52) + "..." : compact;
    }
}
