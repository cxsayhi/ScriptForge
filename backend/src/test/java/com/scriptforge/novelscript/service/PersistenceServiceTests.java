package com.scriptforge.novelscript.service;

import com.scriptforge.novelscript.common.BusinessException;
import com.scriptforge.novelscript.ai.agent.RuleBasedScriptGenerationAgent;
import com.scriptforge.novelscript.ai.agent.ScriptGenerationAgent;
import com.scriptforge.novelscript.ai.agent.ScriptGenerationResult;
import com.scriptforge.novelscript.dto.request.AdaptationSettingRequest;
import com.scriptforge.novelscript.dto.request.ProjectCreateRequest;
import com.scriptforge.novelscript.dto.response.NovelResponse;
import com.scriptforge.novelscript.dto.response.ProjectResponse;
import com.scriptforge.novelscript.dto.response.RepairResponse;
import com.scriptforge.novelscript.dto.response.ScriptResponse;
import com.scriptforge.novelscript.entity.AdaptationSetting;
import com.scriptforge.novelscript.entity.FailedEpisode;
import com.scriptforge.novelscript.mapper.AdaptationSettingMapper;
import com.scriptforge.novelscript.mapper.NovelContentMapper;
import com.scriptforge.novelscript.mapper.ScriptResultMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.url=jdbc:h2:mem:scriptforge;MODE=MySQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "scriptforge.ai.enabled=false"
})
class PersistenceServiceTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private NovelService novelService;

    @Autowired
    private SettingService settingService;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private ExportService exportService;

    @Autowired
    private NovelContentMapper novelContentMapper;

    @Autowired
    private AdaptationSettingMapper adaptationSettingMapper;

    @Autowired
    private ScriptResultMapper scriptResultMapper;

    @MockBean
    private ScriptGenerationAgent scriptGenerationAgent;

    private final RuleBasedScriptGenerationAgent ruleBasedAgent = new RuleBasedScriptGenerationAgent();

    @BeforeEach
    void setUpDatabase() {
        given(scriptGenerationAgent.generateResult(any()))
                .willAnswer(invocation -> ruleBasedAgent.generateResult(invocation.getArgument(0)));
        jdbcTemplate.execute("DROP TABLE IF EXISTS script_result");
        jdbcTemplate.execute("DROP TABLE IF EXISTS adaptation_setting");
        jdbcTemplate.execute("DROP TABLE IF EXISTS novel_content");
        jdbcTemplate.execute("DROP TABLE IF EXISTS project");
        jdbcTemplate.execute("""
                CREATE TABLE project (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(80) NOT NULL,
                    description VARCHAR(500),
                    status VARCHAR(40) NOT NULL DEFAULT 'draft',
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE novel_content (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    project_id BIGINT NOT NULL,
                    original_text LONGTEXT NOT NULL,
                    chapters_json LONGTEXT NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_novel_project FOREIGN KEY (project_id) REFERENCES project(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE adaptation_setting (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    project_id BIGINT NOT NULL,
                    script_type VARCHAR(40) NOT NULL,
                    target_episodes INT NOT NULL,
                    episode_duration_minutes INT NOT NULL,
                    style VARCHAR(80) NOT NULL,
                    language VARCHAR(20) NOT NULL,
                    adaptation_intensity VARCHAR(40) NOT NULL,
                    dialogue_style VARCHAR(40) NOT NULL,
                    budget_preference VARCHAR(40) NOT NULL,
                    keep_original_dialogues BOOLEAN NOT NULL,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_setting_project FOREIGN KEY (project_id) REFERENCES project(id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE script_result (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    project_id BIGINT NOT NULL,
                    yaml LONGTEXT NOT NULL,
                    validation_status VARCHAR(40) NOT NULL,
                    raw_llm_response LONGTEXT,
                    generation_status VARCHAR(40) NOT NULL DEFAULT 'completed',
                    generation_message VARCHAR(1000),
                    failed_episodes_json LONGTEXT,
                    updated_at DATETIME NOT NULL,
                    CONSTRAINT fk_script_project FOREIGN KEY (project_id) REFERENCES project(id)
                )
                """);
    }

    @Test
    void createProjectPersistsProjectAndDefaultSetting() {
        ProjectResponse created = projectService.create(new ProjectCreateRequest("雨夜来信", "联调项目"));

        ProjectResponse fetched = projectService.getResponse(created.id());
        AdaptationSetting setting = settingService.get(created.id());

        assertThat(fetched.title()).isEqualTo("雨夜来信");
        assertThat(fetched.description()).isEqualTo("联调项目");
        assertThat(fetched.status()).isEqualTo("draft");
        assertThat(fetched.hasNovel()).isFalse();
        assertThat(setting.getScriptType()).isEqualTo("web_drama");
        assertThat(setting.getTargetEpisodes()).isEqualTo(3);
        assertThat(adaptationSettingMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void saveNovelTextPersistsOriginalTextChaptersAndProjectStatus() {
        ProjectResponse project = projectService.create(new ProjectCreateRequest("雨夜来信", "联调项目"));

        NovelResponse saved = novelService.saveText(project.id(), sampleNovelText());
        NovelResponse fetchedNovel = novelService.get(project.id());
        ProjectResponse fetchedProject = projectService.getResponse(project.id());

        assertThat(saved.chapterCount()).isEqualTo(3);
        assertThat(fetchedNovel.chapters()).hasSize(3);
        assertThat(fetchedNovel.chapters().get(0).title()).isEqualTo("第一章 雨夜");
        assertThat(fetchedNovel.originalText()).contains("旧剧院");
        assertThat(fetchedProject.status()).isEqualTo("novel_ready");
        assertThat(fetchedProject.hasNovel()).isTrue();
        assertThat(fetchedProject.chapterCount()).isEqualTo(3);
        assertThat(novelContentMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void saveSettingsUpdatesPersistedSettingWithoutChangingApiShape() {
        ProjectResponse project = projectService.create(new ProjectCreateRequest("雨夜来信", "联调项目"));

        AdaptationSetting saved = settingService.save(project.id(), new AdaptationSettingRequest(
                "short_drama",
                12,
                6,
                "都市悬疑",
                "zh-CN",
                "大幅改编",
                "口语化",
                "少场景",
                false
        ));
        AdaptationSetting fetched = settingService.get(project.id());

        assertThat(saved.getScriptType()).isEqualTo("short_drama");
        assertThat(fetched.getTargetEpisodes()).isEqualTo(12);
        assertThat(fetched.getStyle()).isEqualTo("都市悬疑");
        assertThat(fetched.isKeepOriginalDialogues()).isFalse();
        assertThat(adaptationSettingMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void deleteProjectRemovesProjectNovelAndSettingRows() {
        ProjectResponse project = projectService.create(new ProjectCreateRequest("雨夜来信", "联调项目"));
        novelService.saveText(project.id(), sampleNovelText());
        scriptService.generate(project.id());

        projectService.delete(project.id());

        assertThatThrownBy(() -> projectService.get(project.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目不存在");
        assertThat(novelContentMapper.selectCount(null)).isZero();
        assertThat(adaptationSettingMapper.selectCount(null)).isZero();
        assertThat(scriptResultMapper.selectCount(null)).isZero();
    }

    @Test
    void generateGetAndExportScriptUsePersistedResult() {
        ProjectResponse project = projectService.create(new ProjectCreateRequest("雨夜来信", "联调项目"));
        novelService.saveText(project.id(), sampleNovelText());

        ScriptResponse generated = scriptService.generate(project.id());
        ScriptResponse fetched = scriptService.get(project.id());
        ProjectResponse fetchedProject = projectService.getResponse(project.id());

        assertThat(generated.validationResult().valid()).isTrue();
        assertThat(fetched.yaml()).isEqualTo(generated.yaml());
        assertThat(fetched.yaml()).contains("project:", "episodes:");
        assertThat(fetchedProject.status()).isEqualTo("script_ready");
        assertThat(fetchedProject.hasScript()).isTrue();
        assertThat(exportService.yaml(project.id())).isEqualTo(generated.yaml());
        assertThat(exportService.markdown(project.id())).contains("# 雨夜来信", "## Episodes");
        assertThat(scriptResultMapper.selectCount(null)).isEqualTo(1);
    }

    @Test
    void generatePreservesReviewRequiredLlmResultInsteadOfReplacingItWithRuleBasedScript() {
        ProjectResponse project = projectService.create(new ProjectCreateRequest("雨夜来信", "LLM 审核联调项目"));
        novelService.saveText(project.id(), sampleNovelText());
        willReturn(ScriptGenerationResult.needsReview(
                "",
                "--- episode 2 ---\n模型返回了无法解析的剧情文本。",
                "第 2 集修复失败，原始 LLM 回复已保留",
                List.of(FailedEpisode.needsReview(2, "第 2 集无法解析", "第 2 集原始回复"))
        )).given(scriptGenerationAgent).generateResult(any());

        ScriptResponse generated = scriptService.generate(project.id());
        ProjectResponse fetchedProject = projectService.getResponse(project.id());

        assertThat(generated.generationStatus()).isEqualTo("needs_review");
        assertThat(generated.generationMessage()).contains("第 2 集修复失败");
        assertThat(generated.rawLlmResponse()).contains("无法解析的剧情文本");
        assertThat(generated.failedEpisodes()).singleElement()
                .satisfies(failedEpisode -> {
                    assertThat(failedEpisode.episodeId()).isEqualTo(2);
                    assertThat(failedEpisode.rawResponse()).isEqualTo("第 2 集原始回复");
                });
        assertThat(generated.validationResult().valid()).isFalse();
        assertThat(fetchedProject.status()).isEqualTo("script_review");
        assertThat(fetchedProject.hasScript()).isTrue();
        assertThat(scriptService.status(project.id()).getStatus()).isEqualTo("needs_review");

        ScriptResponse updated = scriptService.updateFailedEpisode(project.id(), 2, "用户补充后的第 2 集内容");

        assertThat(updated.generationStatus()).isEqualTo("needs_review");
        assertThat(updated.failedEpisodes()).singleElement()
                .satisfies(failedEpisode -> {
                    assertThat(failedEpisode.status()).isEqualTo("edited");
                    assertThat(failedEpisode.rawResponse()).isEqualTo("用户补充后的第 2 集内容");
                });

        String validYaml = ruleBasedAgent.generate(projectService.get(project.id()));
        RepairResponse repaired = scriptService.repair(project.id(), validYaml);

        assertThat(repaired.validationResult().valid()).isTrue();
        assertThat(repaired.generationStatus()).isEqualTo("needs_review");
        assertThat(repaired.failedEpisodes()).hasSize(1);
        assertThat(projectService.getResponse(project.id()).status()).isEqualTo("script_review");
    }

    private String sampleNovelText() {
        return """
                第一章 雨夜
                雨夜里，林秋收到一封没有署名的信。信里只写着旧剧院和午夜两个词。她决定独自前往，却发现朋友也在门口等待。

                第二章 旧剧院
                旧剧院的灯忽明忽暗，舞台中央放着一只木盒。盒子里是一张旧照片，照片背后写着一个陌生人的名字。

                第三章 回声
                当两人准备离开时，剧院深处传来脚步声。林秋意识到这不是一次偶然的邀请，而是有人精心设计的局。
                """;
    }
}
