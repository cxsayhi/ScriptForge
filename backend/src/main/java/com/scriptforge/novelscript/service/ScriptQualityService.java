package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.ScriptQualityCheckResult;
import com.scriptforge.novelscript.dto.response.ScriptQualityIssue;
import com.scriptforge.novelscript.dto.response.ScriptQualityResponse;
import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScriptQualityService {

    private static final String CHARACTER_CONSISTENCY = "character_consistency";
    private static final String PLOT_CONTINUITY = "plot_continuity";
    private static final String DIALOGUE_NATURALNESS = "dialogue_naturalness";

    private final ProjectService projectService;
    private final YamlScriptValidator validator;

    public ScriptQualityService(ProjectService projectService, YamlScriptValidator validator) {
        this.projectService = projectService;
        this.validator = validator;
    }

    public ScriptQualityResponse check(Long projectId, String yaml) {
        String source = resolveYaml(projectId, yaml);
        ValidationResult validation = validator.validate(source);
        Map<?, ?> root = parseRoot(source);

        List<ScriptQualityCheckResult> checks = root.isEmpty()
                ? unavailableChecks()
                : List.of(
                        buildCheck(CHARACTER_CONSISTENCY, "人物一致性", checkCharacterConsistency(root)),
                        buildCheck(PLOT_CONTINUITY, "剧情连续性", checkPlotContinuity(root)),
                        buildCheck(DIALOGUE_NATURALNESS, "对白自然度", checkDialogueNaturalness(root))
                );

        int overallScore = overallScore(checks, validation);
        boolean passed = validation.valid() && checks.stream().noneMatch(check -> "failed".equals(check.status()));
        return new ScriptQualityResponse(
                projectId,
                overallScore,
                passed,
                summary(checks, validation),
                validation,
                checks
        );
    }

    private String resolveYaml(Long projectId, String yaml) {
        if (yaml != null && !yaml.isBlank()) {
            return yaml;
        }
        ProjectWorkspace project = projectService.get(projectId);
        if (!project.getScriptResult().hasYaml()) {
            throw new BusinessException("当前项目还没有可检查的剧本。");
        }
        return project.getScriptResult().getYaml();
    }

    private List<ScriptQualityIssue> checkCharacterConsistency(Map<?, ?> root) {
        List<ScriptQualityIssue> issues = new ArrayList<>();
        List<?> characters = asList(root.get("characters"));
        Set<String> characterIds = new LinkedHashSet<>();
        Map<String, Integer> usageCounts = new HashMap<>();

        if (characters.isEmpty()) {
            issues.add(error("characters", "剧本缺少角色表，无法检查人物一致性。", "补充 characters 角色列表。"));
        }

        for (int i = 0; i < characters.size(); i++) {
            Map<?, ?> character = asMap(characters.get(i));
            String id = text(character.get("id"));
            if (id == null) {
                continue;
            }
            if (!characterIds.add(id)) {
                issues.add(error("characters[" + i + "].id", "角色 ID 重复: " + id, "为每个角色分配唯一 ID。"));
            }
            usageCounts.putIfAbsent(id, 0);
        }

        List<?> episodes = asList(root.get("episodes"));
        for (int episodeIndex = 0; episodeIndex < episodes.size(); episodeIndex++) {
            Map<?, ?> episode = asMap(episodes.get(episodeIndex));
            List<?> scenes = asList(episode.get("scenes"));
            for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
                Map<?, ?> scene = asMap(scenes.get(sceneIndex));
                String scenePath = "episodes[" + episodeIndex + "].scenes[" + sceneIndex + "]";

                List<?> sceneCharacters = asList(scene.get("characters"));
                for (int characterIndex = 0; characterIndex < sceneCharacters.size(); characterIndex++) {
                    String characterId = text(sceneCharacters.get(characterIndex));
                    if (characterId == null) {
                        continue;
                    }
                    if (!characterIds.contains(characterId)) {
                        issues.add(error(
                                scenePath + ".characters[" + characterIndex + "]",
                                "场景引用了未定义角色: " + characterId,
                                "在 characters 中补充该角色，或修正场景角色 ID。"
                        ));
                    } else {
                        usageCounts.merge(characterId, 1, Integer::sum);
                    }
                }

                List<?> dialogues = asList(scene.get("dialogues"));
                for (int dialogueIndex = 0; dialogueIndex < dialogues.size(); dialogueIndex++) {
                    Map<?, ?> dialogue = asMap(dialogues.get(dialogueIndex));
                    String characterId = text(dialogue.get("character"));
                    if (characterId == null) {
                        continue;
                    }
                    if (!characterIds.contains(characterId)) {
                        issues.add(error(
                                scenePath + ".dialogues[" + dialogueIndex + "].character",
                                "对白使用了未定义角色: " + characterId,
                                "将对白角色改为已定义角色 ID，或补充角色定义。"
                        ));
                    } else {
                        usageCounts.merge(characterId, 1, Integer::sum);
                    }
                }
            }
        }

        for (String characterId : characterIds) {
            if (usageCounts.getOrDefault(characterId, 0) == 0) {
                issues.add(warning(
                        "characters." + characterId,
                        "角色未在任何场景或对白中出现: " + characterId,
                        "删除未使用角色，或安排角色在相关场景中出场。"
                ));
            }
        }
        return issues;
    }

    private List<ScriptQualityIssue> checkPlotContinuity(Map<?, ?> root) {
        List<ScriptQualityIssue> issues = new ArrayList<>();
        Map<?, ?> project = asMap(root.get("project"));
        Integer targetEpisodes = integer(project.get("target_episodes"));
        List<?> episodes = asList(root.get("episodes"));

        if (targetEpisodes != null && !episodes.isEmpty() && episodes.size() != targetEpisodes) {
            issues.add(warning(
                    "episodes",
                    "实际集数与 project.target_episodes 不一致。",
                    "调整 episodes 数量，或同步修改 target_episodes。"
            ));
        }

        if (episodes.isEmpty()) {
            issues.add(error("episodes", "剧本没有任何剧集。", "至少补充 1 集，并为每集添加场景。"));
            return issues;
        }

        Set<Integer> episodeIds = new HashSet<>();
        for (int episodeIndex = 0; episodeIndex < episodes.size(); episodeIndex++) {
            Map<?, ?> episode = asMap(episodes.get(episodeIndex));
            String episodePath = "episodes[" + episodeIndex + "]";
            Integer episodeId = integer(episode.get("episode_id"));
            if (episodeId != null) {
                if (!episodeIds.add(episodeId)) {
                    issues.add(error(episodePath + ".episode_id", "集编号重复: " + episodeId, "保持每集 episode_id 唯一。"));
                }
                if (episodeId != episodeIndex + 1) {
                    issues.add(warning(
                            episodePath + ".episode_id",
                            "集编号不连续，期望为 " + (episodeIndex + 1) + "。",
                            "按剧情顺序重新编号 episode_id。"
                    ));
                }
            }

            if (isBlank(episode.get("summary"))) {
                issues.add(warning(episodePath + ".summary", "本集缺少剧情摘要。", "补充本集起因、转折和结果。"));
            }

            List<?> scenes = asList(episode.get("scenes"));
            if (scenes.isEmpty()) {
                issues.add(error(episodePath + ".scenes", "本集没有可拍摄场景。", "至少补充 1 个场景。"));
                continue;
            }

            Set<String> sceneIds = new HashSet<>();
            for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
                Map<?, ?> scene = asMap(scenes.get(sceneIndex));
                String scenePath = episodePath + ".scenes[" + sceneIndex + "]";
                String sceneId = text(scene.get("scene_id"));
                if (sceneId != null) {
                    if (!sceneIds.add(sceneId)) {
                        issues.add(error(scenePath + ".scene_id", "同一集中场景 ID 重复: " + sceneId, "保持场景 ID 唯一。"));
                    }
                    if (episodeId != null && !sceneId.startsWith(episodeId + "-")) {
                        issues.add(warning(
                                scenePath + ".scene_id",
                                "场景 ID 与所属集编号不一致。",
                                "建议使用类似 " + episodeId + "-" + (sceneIndex + 1) + " 的场景 ID。"
                        ));
                    }
                    Integer sceneOrder = sceneOrder(sceneId);
                    if (sceneOrder != null && sceneOrder != sceneIndex + 1) {
                        issues.add(warning(
                                scenePath + ".scene_id",
                                "场景编号不连续，期望场次为 " + (sceneIndex + 1) + "。",
                                "按场景出现顺序重新编号 scene_id。"
                        ));
                    }
                }

                String action = text(scene.get("action"));
                if (action == null || action.length() < 12) {
                    issues.add(warning(scenePath + ".action", "场景动作描述偏弱。", "补充人物目标、冲突或行动结果。"));
                }
            }
        }
        return issues;
    }

    private List<ScriptQualityIssue> checkDialogueNaturalness(Map<?, ?> root) {
        List<ScriptQualityIssue> issues = new ArrayList<>();
        List<?> episodes = asList(root.get("episodes"));
        for (int episodeIndex = 0; episodeIndex < episodes.size(); episodeIndex++) {
            Map<?, ?> episode = asMap(episodes.get(episodeIndex));
            List<?> scenes = asList(episode.get("scenes"));
            for (int sceneIndex = 0; sceneIndex < scenes.size(); sceneIndex++) {
                Map<?, ?> scene = asMap(scenes.get(sceneIndex));
                String scenePath = "episodes[" + episodeIndex + "].scenes[" + sceneIndex + "]";
                List<?> dialogues = asList(scene.get("dialogues"));
                if (dialogues.isEmpty()) {
                    issues.add(error(scenePath + ".dialogues", "场景没有对白。", "至少补充 1 条符合人物身份的对白。"));
                    continue;
                }

                Set<String> sceneLines = new HashSet<>();
                for (int dialogueIndex = 0; dialogueIndex < dialogues.size(); dialogueIndex++) {
                    Map<?, ?> dialogue = asMap(dialogues.get(dialogueIndex));
                    String line = text(dialogue.get("line"));
                    String dialoguePath = scenePath + ".dialogues[" + dialogueIndex + "].line";
                    if (line == null) {
                        issues.add(error(dialoguePath, "对白内容为空。", "补充角色要表达的具体台词。"));
                        continue;
                    }
                    if (line.length() < 4) {
                        issues.add(warning(dialoguePath, "对白过短，可能缺少真实语境。", "补充情绪、态度或信息量。"));
                    }
                    if (line.length() > 120) {
                        issues.add(warning(dialoguePath, "对白过长，可能不够口语化。", "拆分成长短交替的多句对白。"));
                    }
                    if (!sceneLines.add(line)) {
                        issues.add(warning(dialoguePath, "同一场景内出现重复对白。", "删除重复句，或改写为递进表达。"));
                    }
                    if (looksLikeDirection(line)) {
                        issues.add(warning(dialoguePath, "对白疑似包含镜头或动作说明。", "将动作说明移到 action 字段，台词只保留角色说出口的话。"));
                    }
                }
            }
        }
        return issues;
    }

    private ScriptQualityCheckResult buildCheck(String category, String label, List<ScriptQualityIssue> issues) {
        return new ScriptQualityCheckResult(category, label, status(issues), score(issues), List.copyOf(issues));
    }

    private List<ScriptQualityCheckResult> unavailableChecks() {
        ScriptQualityIssue issue = error("yaml", "YAML 基础结构无效，无法完成质量检查。", "先使用修复功能生成有效 YAML。");
        return List.of(
                buildCheck(CHARACTER_CONSISTENCY, "人物一致性", List.of(issue)),
                buildCheck(PLOT_CONTINUITY, "剧情连续性", List.of(issue)),
                buildCheck(DIALOGUE_NATURALNESS, "对白自然度", List.of(issue))
        );
    }

    private int score(List<ScriptQualityIssue> issues) {
        int penalty = 0;
        for (ScriptQualityIssue issue : issues) {
            penalty += "error".equals(issue.severity()) ? 20 : 8;
        }
        return Math.max(0, 100 - penalty);
    }

    private String status(List<ScriptQualityIssue> issues) {
        if (issues.stream().anyMatch(issue -> "error".equals(issue.severity()))) {
            return "failed";
        }
        if (!issues.isEmpty()) {
            return "warning";
        }
        return "passed";
    }

    private int overallScore(List<ScriptQualityCheckResult> checks, ValidationResult validation) {
        int average = checks.stream()
                .mapToInt(ScriptQualityCheckResult::score)
                .sum() / Math.max(1, checks.size());
        int validationPenalty = validation.errors().size() * 10 + validation.warnings().size() * 3;
        return Math.max(0, average - validationPenalty);
    }

    private String summary(List<ScriptQualityCheckResult> checks, ValidationResult validation) {
        long errors = checks.stream()
                .flatMap(check -> check.issues().stream())
                .filter(issue -> "error".equals(issue.severity()))
                .count() + validation.errors().size();
        long warnings = checks.stream()
                .flatMap(check -> check.issues().stream())
                .filter(issue -> "warning".equals(issue.severity()))
                .count() + validation.warnings().size();
        if (errors == 0 && warnings == 0) {
            return "剧本质量检查通过，人物、剧情和对白结构稳定。";
        }
        return "发现 " + errors + " 个严重问题，" + warnings + " 个优化提醒。";
    }

    private Map<?, ?> parseRoot(String yaml) {
        try {
            Object parsed = new Yaml().load(yaml);
            if (parsed instanceof Map<?, ?> root) {
                return root;
            }
        } catch (YAMLException ignored) {
            return Map.of();
        }
        return Map.of();
    }

    private ScriptQualityIssue error(String path, String message, String suggestion) {
        return new ScriptQualityIssue("error", path, message, suggestion);
    }

    private ScriptQualityIssue warning(String path, String message, String suggestion) {
        return new ScriptQualityIssue("warning", path, message, suggestion);
    }

    private List<?> asList(Object value) {
        if (value instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private Map<?, ?> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return map;
        }
        return Map.of();
    }

    private String text(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            return text.trim();
        }
        return null;
    }

    private Integer integer(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue && longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
            return longValue.intValue();
        }
        return null;
    }

    private boolean isBlank(Object value) {
        return !(value instanceof String text) || text.isBlank();
    }

    private Integer sceneOrder(String sceneId) {
        int separator = sceneId.lastIndexOf('-');
        if (separator < 0 || separator == sceneId.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(sceneId.substring(separator + 1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private boolean looksLikeDirection(String line) {
        return line.contains("镜头")
                || line.contains("画面")
                || line.contains("动作")
                || line.contains("场景")
                || line.contains("旁白");
    }
}
