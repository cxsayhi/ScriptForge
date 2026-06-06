package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.dto.response.ScriptQualityCheckResult;
import com.scriptforge.novelscript.dto.response.ScriptQualityResponse;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScriptQualityServiceTests {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ScriptQualityService service = new ScriptQualityService(projectService, new YamlScriptValidator());

    @Test
    void checkReturnsPassedForStableScript() {
        ScriptQualityResponse response = service.check(1L, stableYaml());

        assertThat(response.passed()).isTrue();
        assertThat(response.overallScore()).isEqualTo(100);
        assertThat(response.validationResult().valid()).isTrue();
        assertThat(response.checks())
                .extracting(ScriptQualityCheckResult::category)
                .containsExactly("character_consistency", "plot_continuity", "dialogue_naturalness");
        assertThat(response.checks()).allMatch(check -> "passed".equals(check.status()));
    }

    @Test
    void checkDetectsCharacterConsistencyIssues() {
        String yaml = stableYaml()
                .replace("""
                          - char_002
                        action: 林秋读完匿名信准备出门，周然赶到门口拦住她。
                """, """
                          - char_999
                        action: 林秋读完匿名信准备出门，周然赶到门口拦住她。
                """)
                .replace("""
                          - character: char_002
                            line: 你不能一个人去。
                """, """
                          - character: char_404
                            line: 你不能一个人去。
                """);

        ScriptQualityResponse response = service.check(1L, yaml);
        ScriptQualityCheckResult check = check(response, "character_consistency");

        assertThat(response.passed()).isFalse();
        assertThat(check.status()).isEqualTo("failed");
        assertThat(check.issues())
                .anyMatch(issue -> issue.message().contains("场景引用了未定义角色: char_999"))
                .anyMatch(issue -> issue.message().contains("对白使用了未定义角色: char_404"));
    }

    @Test
    void checkDetectsPlotContinuityIssues() {
        String yaml = stableYaml()
                .replace("target_episodes: 2", "target_episodes: 3")
                .replace("episode_id: 2", "episode_id: 3")
                .replace("scene_id: 2-1", "scene_id: 9-2")
                .replace("朋友指向舞台中央的木盒，林秋意识到信件与旧照片有关。", "发现木盒。");

        ScriptQualityResponse response = service.check(1L, yaml);
        ScriptQualityCheckResult check = check(response, "plot_continuity");

        assertThat(check.status()).isEqualTo("warning");
        assertThat(check.issues())
                .anyMatch(issue -> issue.message().contains("实际集数与 project.target_episodes 不一致"))
                .anyMatch(issue -> issue.message().contains("集编号不连续"))
                .anyMatch(issue -> issue.message().contains("场景 ID 与所属集编号不一致"))
                .anyMatch(issue -> issue.message().contains("场景动作描述偏弱"));
    }

    @Test
    void checkDetectsDialogueNaturalnessIssues() {
        String yaml = stableYaml().replace("""
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                          - character: char_001
                            line: 嗯
                          - character: char_001
                            line: 嗯
                          - character: char_002
                            line: 镜头推进到她的脸上。
                """);

        ScriptQualityResponse response = service.check(1L, yaml);
        ScriptQualityCheckResult check = check(response, "dialogue_naturalness");

        assertThat(check.status()).isEqualTo("warning");
        assertThat(check.issues())
                .anyMatch(issue -> issue.message().contains("对白过短"))
                .anyMatch(issue -> issue.message().contains("重复对白"))
                .anyMatch(issue -> issue.message().contains("镜头或动作说明"));
    }

    @Test
    void checkUsesPersistedScriptWhenRequestYamlIsBlank() {
        ProjectWorkspace project = new ProjectWorkspace();
        project.getScriptResult().setYaml(stableYaml());
        when(projectService.get(7L)).thenReturn(project);

        ScriptQualityResponse response = service.check(7L, null);

        assertThat(response.projectId()).isEqualTo(7L);
        assertThat(response.passed()).isTrue();
    }

    @Test
    void checkThrowsWhenProjectHasNoScript() {
        when(projectService.get(7L)).thenReturn(new ProjectWorkspace());

        assertThatThrownBy(() -> service.check(7L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("当前项目还没有可检查的剧本。");
    }

    private ScriptQualityCheckResult check(ScriptQualityResponse response, String category) {
        return response.checks().stream()
                .filter(check -> category.equals(check.category()))
                .findFirst()
                .orElseThrow();
    }

    private String stableYaml() {
        return """
                project:
                  title: 雨夜来信
                  source_type: novel
                  script_type: short_drama
                  language: zh-CN
                  target_episodes: 2
                  summary: 林秋收到匿名信后，与朋友一起追查旧剧院的秘密。
                characters:
                  - id: char_001
                    name: 林秋
                    role: 主角
                    motivation: 找到匿名信背后的真相
                  - id: char_002
                    name: 周然
                    role: 配角
                    motivation: 保护林秋并查清旧剧院线索
                episodes:
                  - episode_id: 1
                    title: 第一集：雨夜来信
                    summary: 林秋收到匿名信，周然赶来阻止她独自前往。
                    scenes:
                      - scene_id: 1-1
                        title: 匿名信
                        location: 林秋家
                        time: 夜晚
                        characters:
                          - char_001
                          - char_002
                        action: 林秋读完匿名信准备出门，周然赶到门口拦住她。
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                          - character: char_002
                            line: 你不能一个人去。
                  - episode_id: 2
                    title: 第二集：旧剧院
                    summary: 两人在旧剧院发现木盒和旧照片，线索指向陌生人。
                    scenes:
                      - scene_id: 2-1
                        title: 木盒
                        location: 旧剧院
                        time: 午夜
                        characters:
                          - char_001
                          - char_002
                        action: 朋友指向舞台中央的木盒，林秋意识到信件与旧照片有关。
                        dialogues:
                          - character: char_002
                            line: 盒子像是故意放在这里等我们。
                          - character: char_001
                            line: 照片背后的名字，我从来没有见过。
                """;
    }
}
