package com.scriptforge.novelscript.util;

import com.scriptforge.novelscript.dto.response.ValidationResult;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class YamlScriptValidatorTests {

    private final YamlScriptValidator validator = new YamlScriptValidator();

    @Test
    void validateAcceptsCompleteScriptYaml() {
        ValidationResult result = validator.validate(validYaml());

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validateRejectsWrongFieldTypesAndEnumValues() {
        String yaml = validYaml()
                .replace("script_type: short_drama", "script_type: series")
                .replace("target_episodes: 3", "target_episodes: \"3\"")
                .replace("episode_id: 1", "episode_id: one")
                .replace("characters:\n          - char_001", "characters: char_001");

        ValidationResult result = validator.validate(yaml);

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .anyMatch(error -> error.contains("project.script_type 必须是以下值之一"))
                .contains("project.target_episodes 必须是整数")
                .contains("episodes[0].episode_id 必须是整数")
                .contains("episodes[0].scenes[0].characters 必须是数组");
    }

    @Test
    void validateRejectsEmptyScenesAndDialogues() {
        String emptyScenes = validYaml().replace("""
                    scenes:
                      - scene_id: 1-1
                        title: 匿名信
                        location: 林秋家
                        time: 夜晚
                        characters:
                          - char_001
                        action: 林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                    scenes: []
                """);
        String emptyDialogues = validYaml().replace("""
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                        dialogues: []
                """);

        assertThat(validator.validate(emptyScenes).errors())
                .contains("episodes[0].scenes 必须是至少包含 1 个场景的数组");
        assertThat(validator.validate(emptyDialogues).errors())
                .contains("episodes[0].scenes[0].dialogues 必须至少包含 1 条对白");
    }

    @Test
    void repairExtractsYamlFromMarkdownCodeFence() {
        String repaired = validator.repair("""
                下面是生成的 YAML：
                ```yaml
                %s
                ```
                以上内容仅供参考。
                """.formatted(validYaml()));

        assertThat(repaired).doesNotContain("```");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairNormalizesCommonFormattingIssues() {
        String repaired = validator.repair("""
                project：
                  title：雨夜来信
                  source_type:novel
                  script_type:short_drama
                  language:zh-CN
                  target_episodes:3
                characters:
                  -id:char_001
                    name：林秋
                    role:主角
                episodes:
                  -episode_id:1
                    title:第一集：雨夜来信
                    summary:匿名信把林秋引向旧剧院。
                    scenes:
                      -scene_id:1-1
                        title:匿名信
                        location:林秋家
                        time:夜晚
                        characters:
                          - char_001
                        action:林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          -character:char_001
                            line:我必须弄清楚这封信是谁寄来的。
                """);

        assertThat(repaired).contains("project:", "source_type: novel", "- id: char_001");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairConvertsUnsafeDoubleQuotedScalarsToSingleQuotedScalars() {
        String repaired = validator.repair(validYaml().replace(
                "summary: 林秋收到匿名信并前往旧剧院。",
                "summary: \"在一个雨夜，林秋收到一封神秘的匿名信，信中只提到了\"旧剧院\"和\"午夜\"。\""
        ));

        assertThat(repaired)
                .contains("summary: 在一个雨夜")
                .contains("\"旧剧院\"和\"午夜\"");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairConvertsLlmProseWithBareInnerQuotesToValidYaml() {
        String repaired = validator.repair(validYaml().replace(
                "summary: 林秋收到匿名信并前往旧剧院。",
                "summary: \"天梦冰蚕向霍雨浩展示了\"智慧魂环\"的独特封印，并赋予他能够无限进化的第一魂环和第二武魂。\""
        ));

        assertThat(repaired)
                .contains("天梦冰蚕向霍雨浩展示了")
                .contains("\"智慧魂环\"");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairConvertsUnbalancedDoubleQuotedScalarsToSingleQuotedScalars() {
        String repaired = validator.repair(validYaml().replace(
                "summary: 林秋收到匿名信并前往旧剧院。",
                "summary: \"天梦冰蚕向霍雨浩展示了\"智慧魂环\"的独特封印，并赋予他能够无限进化的第一魂环和第二武魂。"
        ));

        assertThat(repaired)
                .contains("天梦冰蚕向霍雨浩展示了")
                .contains("\"智慧魂环\"");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairKeepsSafeDoubleQuotedScalarsWithTrailingComments() {
        String repaired = validator.repair(validYaml().replace(
                "summary: 林秋收到匿名信并前往旧剧院。",
                "summary: \"林秋收到匿名信并前往旧剧院。\" # 模型多余注释"
        ));

        assertThat(repaired)
                .contains("summary: 林秋收到匿名信并前往旧剧院。")
                .doesNotContain("模型多余注释");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairNormalizesCommonLlmShapeDrift() {
        String yaml = validYaml()
                .replace("target_episodes: 3", "target_episodes: \"3\"")
                .replace("episode_id: 1", "episode_id: \"第1集\"")
                .replace("""
                        characters:
                          - char_001
                """, """
                        characters: char_001
                """)
                .replace("""
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                        dialogues:
                          character: char_001
                          line: 我必须弄清楚这封信是谁寄来的。
                """);

        String repaired = validator.repair(yaml);

        assertThat(repaired)
                .contains("target_episodes: 3")
                .contains("episode_id: 1")
                .contains("characters:")
                .contains("- char_001")
                .contains("dialogues:")
                .contains("- character: char_001");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairUnwrapsCommonTopLevelContainer() {
        String repaired = validator.repair("script:\n" + indent(validYaml()));

        assertThat(repaired).startsWith("project:");
        assertThat(repaired).doesNotContain("script:");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairWrapsTopLevelEpisodeArrayWithContextDefaults() {
        ProjectWorkspace project = new ProjectWorkspace();
        project.setTitle("雨夜来信");
        project.getSetting().setScriptType("short_drama");
        project.getSetting().setTargetEpisodes(1);

        String repaired = validator.repair("""
                - episode_id: "第1集"
                  title: 只返回剧集
                  summary: 模型漏掉顶层对象，只返回了剧集数组。
                  scenes:
                    - scene_id: 1-1
                      title: 异常输出
                      location: 旧剧院
                      time: 午夜
                      characters: char_001
                      action: 林秋和周然发现输出结构需要被兼容处理。
                      dialogues:
                        character: char_001
                        line: 这次模型只返回了数组。
                """, project);

        assertThat(repaired)
                .startsWith("project:")
                .contains("title: 雨夜来信")
                .contains("script_type: short_drama")
                .contains("episode_id: 1")
                .contains("characters:")
                .contains("- id: char_001")
                .contains("episodes:");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    @Test
    void repairStripsLeadingAndTrailingNarrationWithoutCodeFence() {
        String repaired = validator.repair("""
                我将直接给出 YAML：
                %s
                以上是修复后的版本。
                """.formatted(validYaml()));

        assertThat(repaired).startsWith("project:");
        assertThat(repaired).doesNotContain("我将直接给出", "以上是修复后的版本");
        assertThat(validator.validate(repaired).valid()).isTrue();
    }

    private String validYaml() {
        return """
                project:
                  title: 雨夜来信
                  source_type: novel
                  script_type: short_drama
                  language: zh-CN
                  target_episodes: 3
                  summary: 林秋收到匿名信并前往旧剧院。
                characters:
                  - id: char_001
                    name: 林秋
                    role: 主角
                    description: 收到匿名信的调查者。
                    motivation: 找到信件真相
                    relationships:
                      - target: char_002
                        relation: 朋友
                  - id: char_002
                    name: 朋友
                    role: 配角
                episodes:
                  - episode_id: 1
                    title: 第一集：雨夜来信
                    summary: 林秋收到匿名信并前往旧剧院。
                    scenes:
                      - scene_id: 1-1
                        title: 匿名信
                        location: 林秋家
                        time: 夜晚
                        characters:
                          - char_001
                        action: 林秋读完匿名信，决定前往旧剧院。
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """;
    }

    private String indent(String yaml) {
        return yaml.lines()
                .map(line -> "  " + line)
                .collect(Collectors.joining("\n"));
    }
}
