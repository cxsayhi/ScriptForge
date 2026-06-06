package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.ai.client.AiClient;
import com.scriptforge.novelscript.ai.prompt.PromptBuilder;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmScriptGenerationAgentTests {

    private final YamlScriptValidator validator = new YamlScriptValidator();

    @Test
    void generateReturnsValidYamlFromMockClient() {
        RecordingAiClient client = new RecordingAiClient(validYaml("LLM 版剧本"));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: LLM 版剧本");
        assertThat(client.systemPrompt).contains("Return only valid YAML");
        assertThat(client.userPrompt).contains("Novel title: 雨夜来信");
    }

    @Test
    void generateExtractsYamlFromMarkdownFence() {
        RecordingAiClient client = new RecordingAiClient("""
                下面是剧本：
                ```yaml
                %s
                ```
                """.formatted(validYaml("Fence 版剧本")));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: Fence 版剧本");
        assertThat(yaml).doesNotContain("```");
    }

    @Test
    void generateFallsBackWhenMockClientReturnsInvalidYaml() {
        RecordingAiClient client = new RecordingAiClient("project:\n  title: 缺少必要结构\n");
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: 雨夜来信");
        assertThat(yaml).contains("episodes:");
    }

    @Test
    void generateFallsBackWhenMockClientThrows() {
        RecordingAiClient client = new RecordingAiClient(new RuntimeException("network timeout"));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: 雨夜来信");
        assertThat(yaml).contains("episodes:");
    }

    private LlmScriptGenerationAgent createAgent(AiClient client) {
        return new LlmScriptGenerationAgent(
                client,
                new PromptBuilder(),
                validator,
                new RuleBasedScriptGenerationAgent()
        );
    }

    private ProjectWorkspace sampleProject() {
        ProjectWorkspace project = new ProjectWorkspace();
        project.setTitle("雨夜来信");
        project.getNovelContent().setChapters(List.of(
                new Chapter(1, "第一章 雨夜", "林秋收到一封没有署名的信。"),
                new Chapter(2, "第二章 旧剧院", "旧剧院里出现一张旧照片。"),
                new Chapter(3, "第三章 回声", "脚步声揭开更大的局。")
        ));
        project.getSetting().setScriptType("short_drama");
        project.getSetting().setTargetEpisodes(3);
        project.getSetting().setStyle("悬疑");
        return project;
    }

    private String validYaml(String title) {
        return """
                project:
                  title: %s
                  source_type: novel
                  script_type: short_drama
                  language: zh-CN
                  target_episodes: 3
                  summary: 模型生成的剧本初稿
                characters:
                  - id: char_001
                    name: 林秋
                    role: 主角
                    motivation: 找到信件真相
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
                """.formatted(title);
    }

    private static class RecordingAiClient implements AiClient {

        private final String response;
        private final RuntimeException exception;
        private String systemPrompt;
        private String userPrompt;

        RecordingAiClient(String response) {
            this.response = response;
            this.exception = null;
        }

        RecordingAiClient(RuntimeException exception) {
            this.response = null;
            this.exception = exception;
        }

        @Override
        public String chat(String userPrompt) {
            return chatWithSystem(null, userPrompt);
        }

        @Override
        public String chatWithSystem(String systemPrompt, String userPrompt) {
            this.systemPrompt = systemPrompt;
            this.userPrompt = userPrompt;
            if (exception != null) {
                throw exception;
            }
            return response;
        }
    }
}
