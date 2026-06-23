package com.scriptforge.novelscript.ai.agent;

import com.scriptforge.novelscript.ai.client.AiClient;
import com.scriptforge.novelscript.entity.Chapter;
import com.scriptforge.novelscript.entity.ProjectWorkspace;
import com.scriptforge.novelscript.util.YamlScriptValidator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmScriptGenerationAgentTests {

    private final YamlScriptValidator validator = new YamlScriptValidator();

    @Test
    void generateReturnsValidYamlFromMockClient() {
        RecordingAiClient client = new RecordingAiClient(validYaml("LLM 版剧本"));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: LLM 版剧本");
        assertThat(client.systemPrompt).contains("Return only valid JSON");
        assertThat(client.userPrompt).contains("Novel title: 雨夜来信");
        assertThat(client.schemaNames).contains("script_outline");
    }

    @Test
    void generateBuildsScriptFromOutlineAndEpisodeJsonCalls() {
        RecordingAiClient client = new RecordingAiClient(
                outlineJson(),
                episodeJson(1, "雨夜来信", "林秋收到匿名信并决定出门。"),
                episodeJson(2, "旧剧院", "林秋在旧剧院发现旧照片。"),
                episodeJson(3, "回声", "林秋用回声线索锁定真相。")
        );
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: JSON 分段剧本")
                .contains("title: 第1集：雨夜来信")
                .contains("title: 第2集：旧剧院")
                .contains("title: 第3集：回声")
                .doesNotContain("name: 主角");
        assertThat(client.schemaNames)
                .containsExactly("script_outline", "script_episode", "script_episode", "script_episode");
    }

    @Test
    void generateKeepsChapterAssignmentsChronologicalWhenTargetEpisodesExceedChapterCount() {
        RecordingAiClient client = new RecordingAiClient(
                outlineJson(),
                episodeJson(1, "雨夜来信", "林秋收到匿名信并决定出门。"),
                episodeJson(2, "雨夜余波", "林秋继续追查匿名信。"),
                episodeJson(3, "旧剧院", "林秋在旧剧院发现旧照片。"),
                episodeJson(4, "剧院暗门", "林秋追查旧照片的来源。"),
                episodeJson(5, "回声", "林秋用回声线索锁定真相。")
        );
        LlmScriptGenerationAgent agent = createAgent(client);
        ProjectWorkspace project = sampleProject();
        project.getSetting().setTargetEpisodes(5);

        String yaml = agent.generate(project);

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("target_episodes: 5");
        assertThat(client.userPrompts).hasSize(6);
        assertThat(client.userPrompts.get(1)).contains("--- Chapter 1: 第一章 雨夜 ---");
        assertThat(client.userPrompts.get(2)).contains("--- Chapter 1: 第一章 雨夜 ---");
        assertThat(client.userPrompts.get(3)).contains("--- Chapter 2: 第二章 旧剧院 ---");
        assertThat(client.userPrompts.get(4)).contains("--- Chapter 2: 第二章 旧剧院 ---");
        assertThat(client.userPrompts.get(5)).contains("--- Chapter 3: 第三章 回声 ---");
    }

    @Test
    void generatePreservesPartialDraftWhenEpisodeIdDoesNotMatchRequestedEpisode() {
        RecordingAiClient client = new RecordingAiClient(
                outlineJson(),
                episodeJson(1, "雨夜来信", "林秋收到匿名信并决定出门。"),
                episodeJson(1, "重复集号", "模型错误地再次返回第一集。"),
                episodeJson(1, "重复集号", "修复仍然错误地返回第一集。"),
                episodeJson(3, "回声", "林秋用回声线索锁定真相。")
        );
        LlmScriptGenerationAgent agent = createAgent(client);

        ScriptGenerationResult result = agent.generateResult(sampleProject());

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.yaml()).contains("title: JSON 分段剧本", "episode_id: 1", "episode_id: 3");
        assertThat(result.rawLlmResponse()).contains("--- outline ---", "--- episode 2 ---", "--- episode 3 ---", "重复集号");
        assertThat(result.failedEpisodes()).singleElement()
                .satisfies(failedEpisode -> {
                    assertThat(failedEpisode.episodeId()).isEqualTo(2);
                    assertThat(failedEpisode.status()).isEqualTo("needs_review");
                    assertThat(failedEpisode.reason()).contains("第 2 集生成失败");
                    assertThat(failedEpisode.rawResponse()).contains("重复集号");
                });
        assertThat(client.schemaNames)
                .containsExactly("script_outline", "script_episode", "script_episode", "script_episode", "script_episode");
    }

    @Test
    void generateKeepsOutlinePromptWithinGlobalDigestBudget() {
        RecordingAiClient client = new RecordingAiClient(validYaml("长文本预算测试"));
        LlmScriptGenerationAgent agent = createAgent(client);
        ProjectWorkspace project = singleEpisodeProject();
        List<Chapter> chapters = new ArrayList<>();
        for (int index = 1; index <= 40; index++) {
            chapters.add(new Chapter(index, "第" + index + "章", "原文内容".repeat(1_000)));
        }
        project.getNovelContent().setChapters(chapters);

        String yaml = agent.generate(project);

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(client.userPrompts.get(0).length()).isLessThanOrEqualTo(13_000);
    }

    @Test
    void generateParsesJsonWithLanguageMarkerWithoutFences() {
        RecordingAiClient client = new RecordingAiClient(
                "json\n" + outlineJson(),
                "json\n" + episodeJson(1, "雨夜来信", "林秋收到匿名信并决定出门。"),
                "json\n" + episodeJson(2, "旧剧院", "林秋在旧剧院发现旧照片。"),
                "json\n" + episodeJson(3, "回声", "林秋用回声线索锁定真相。")
        );
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: JSON 分段剧本")
                .contains("title: 第3集：回声")
                .doesNotContain("name: 主角");
    }

    @Test
    void generateRepairsUnparseableOutlineBeforeMarkingResultForReview() {
        RecordingAiClient client = new RecordingAiClient(
                "我将先解释生成思路，但这里没有返回可解析结构。",
                outlineJson(),
                episodeJson(1, "雨夜来信", "林秋收到匿名信并决定出门。"),
                episodeJson(2, "旧剧院", "林秋在旧剧院发现旧照片。"),
                episodeJson(3, "回声", "林秋用回声线索锁定真相。")
        );
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(sampleProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: JSON 分段剧本")
                .doesNotContain("name: 主角")
                .doesNotContain("根据 3 个章节");
        assertThat(client.schemaNames)
                .containsExactly("script_outline", "script_outline", "script_episode", "script_episode", "script_episode");
        assertThat(client.userPrompts.get(1)).contains("could not be parsed");
    }

    @Test
    void generateRetriesWithLlmRepairBeforeReturningCompletedResult() {
        RecordingAiClient client = new RecordingAiClient(
                scriptJson("缺角色的 JSON 剧本", false),
                scriptJson("LLM 修复后的 JSON 剧本", true)
        );
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: LLM 修复后的 JSON 剧本")
                .doesNotContain("title: 雨夜来信\n");
        assertThat(client.schemaNames).containsExactly("script_outline", "script_result");
        assertThat(client.userPrompts.get(1)).contains("Validation errors");
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

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml).contains("title: Fence 版剧本");
        assertThat(yaml).doesNotContain("```");
    }

    @Test
    void generateRepairsUnsafeDoubleQuotesFromMockClient() {
        RecordingAiClient client = new RecordingAiClient(validYaml("Gemini 版剧本").replace(
                "summary: 模型生成的剧本初稿",
                "summary: \"在一个雨夜，林秋收到一封神秘的匿名信，信中只提到了\"旧剧院\"和\"午夜\"。\""
        ));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: Gemini 版剧本")
                .contains("\"旧剧院\"和\"午夜\"")
                .doesNotContain("name: 主角");
    }

    @Test
    void generateNormalizesDialogueObjectFromMockClient() {
        RecordingAiClient client = new RecordingAiClient(validYaml("Gemini 对白兼容测试").replace("""
                        dialogues:
                          - character: char_001
                            line: 我必须弄清楚这封信是谁寄来的。
                """, """
                        dialogues:
                          character: char_001
                          line: 我必须弄清楚这封信是谁寄来的。
                """));
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: Gemini 对白兼容测试")
                .contains("dialogues:")
                .contains("- character: char_001")
                .doesNotContain("name: 主角");
    }

    @Test
    void generateWrapsTopLevelEpisodeArrayFromMockClient() {
        RecordingAiClient client = new RecordingAiClient("""
                - episode_id: "第1集"
                  title: Gemini 顶层数组兼容测试
                  summary: 模型漏掉 project 和 characters，只返回 episodes 数组。
                  scenes:
                    - scene_id: 1-1
                      title: 顶层数组
                      location: 旧剧院
                      time: 午夜
                      characters: char_001
                      action: 林秋发现模型输出不是标准根对象。
                      dialogues:
                        character: char_001
                        line: 这次返回的是数组，但仍然应该被兼容。
                """);
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: 雨夜来信")
                .contains("title: Gemini 顶层数组兼容测试")
                .contains("- id: char_001")
                .doesNotContain("name: 主角");
    }

    @Test
    void generateUnwrapsScriptContainerFromMockClient() {
        RecordingAiClient client = new RecordingAiClient("""
                script:
                  project:
                    title: Gemini 外层容器兼容测试
                    source_type: novel
                    script_type: short_drama
                    language: zh-CN
                    target_episodes: 1
                  characters:
                    - id: char_001
                      name: 林秋
                      role: 主角
                  episodes:
                    - episode_id: 1
                      title: 第一集
                      summary: 模型把标准 YAML 包在 script 字段下。
                      scenes:
                        - scene_id: 1-1
                          title: 外层容器
                          location: 旧剧院
                          time: 午夜
                          characters:
                            - char_001
                          action: 林秋拆开外层容器。
                          dialogues:
                            - character: char_001
                              line: 这次多了一层 script。
                """);
        LlmScriptGenerationAgent agent = createAgent(client);

        String yaml = agent.generate(singleEpisodeProject());

        assertThat(validator.validate(yaml).valid()).isTrue();
        assertThat(yaml)
                .contains("title: Gemini 外层容器兼容测试")
                .doesNotContain("script:")
                .doesNotContain("name: 主角");
    }

    @Test
    void generatePreservesRawResponseWhenMockClientReturnsInvalidYaml() {
        RecordingAiClient client = new RecordingAiClient("project:\n  title: 缺少必要结构\n");
        LlmScriptGenerationAgent agent = createAgent(client);

        ScriptGenerationResult result = agent.generateResult(sampleProject());

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.rawLlmResponse()).contains("缺少必要结构");
        assertThat(validator.validate(result.yaml()).valid()).isFalse();
    }

    @Test
    void generateMarksReviewRequiredWhenMockClientThrows() {
        RecordingAiClient client = new RecordingAiClient(new RuntimeException("network timeout"));
        LlmScriptGenerationAgent agent = createAgent(client);

        ScriptGenerationResult result = agent.generateResult(sampleProject());

        assertThat(result.requiresReview()).isTrue();
        assertThat(result.rawLlmResponse()).isBlank();
        assertThat(result.message()).contains("network timeout");
    }

    private LlmScriptGenerationAgent createAgent(AiClient client) {
        return new LlmScriptGenerationAgent(
                client,
                validator
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

    private ProjectWorkspace singleEpisodeProject() {
        ProjectWorkspace project = sampleProject();
        project.getSetting().setTargetEpisodes(1);
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

    private String outlineJson() {
        return """
                {
                  "project": {
                    "title": "JSON 分段剧本",
                    "source_type": "novel",
                    "script_type": "short_drama",
                    "language": "zh-CN",
                    "target_episodes": 3,
                    "summary": "林秋循着匿名信追查旧剧院真相。"
                  },
                  "characters": [
                    {
                      "id": "char_001",
                      "name": "林秋",
                      "role": "主角",
                      "motivation": "找到匿名信背后的真相"
                    },
                    {
                      "id": "char_002",
                      "name": "周然",
                      "role": "配角",
                      "motivation": "保护林秋"
                    }
                  ],
                  "episode_outlines": [
                    {
                      "episode_id": 1,
                      "title": "雨夜来信",
                      "summary": "林秋收到匿名信。",
                      "chapter_numbers": [1],
                      "key_events": ["匿名信出现"]
                    },
                    {
                      "episode_id": 2,
                      "title": "旧剧院",
                      "summary": "林秋前往旧剧院。",
                      "chapter_numbers": [2],
                      "key_events": ["发现旧照片"]
                    },
                    {
                      "episode_id": 3,
                      "title": "回声",
                      "summary": "线索揭开真相。",
                      "chapter_numbers": [3],
                      "key_events": ["回声暴露布局"]
                    }
                  ]
                }
                """;
    }

    private String episodeJson(int episodeId, String title, String summary) {
        return """
                {
                  "episode": {
                    "episode_id": %d,
                    "title": "第%d集：%s",
                    "summary": "%s",
                    "scenes": [
                      {
                        "scene_id": "%d-1",
                        "title": "%s",
                        "location": "旧剧院",
                        "time": "夜晚",
                        "characters": ["char_001"],
                        "action": "林秋追查线索并推进真相。",
                        "dialogues": [
                          {
                            "character": "char_001",
                            "line": "我必须弄清楚这封信是谁寄来的。"
                          }
                        ]
                      }
                    ]
                  }
                }
                """.formatted(episodeId, episodeId, title, summary, episodeId, title);
    }

    private String scriptJson(String title, boolean includeCharacters) {
        String characters = includeCharacters
                ? """
                    [
                      {
                        "id": "char_001",
                        "name": "林秋",
                        "role": "主角",
                        "motivation": "找到匿名信真相"
                      }
                    ]
                """
                : "[]";
        return """
                {
                  "project": {
                    "title": "%s",
                    "source_type": "novel",
                    "script_type": "short_drama",
                    "language": "zh-CN",
                    "target_episodes": 3,
                    "summary": "模型生成的 JSON 剧本初稿"
                  },
                  "characters": %s,
                  "episodes": [
                    {
                      "episode_id": 1,
                      "title": "第一集：雨夜来信",
                      "summary": "林秋收到匿名信并前往旧剧院。",
                      "scenes": [
                        {
                          "scene_id": "1-1",
                          "title": "匿名信",
                          "location": "林秋家",
                          "time": "夜晚",
                          "characters": ["char_001"],
                          "action": "林秋读完匿名信，决定前往旧剧院。",
                          "dialogues": [
                            {
                              "character": "char_001",
                              "line": "我必须弄清楚这封信是谁寄来的。"
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """.formatted(title, characters);
    }

    private static class RecordingAiClient implements AiClient {

        private final List<String> responses;
        private final RuntimeException exception;
        private int responseIndex;
        private String systemPrompt;
        private String userPrompt;
        private final List<String> userPrompts = new ArrayList<>();
        private final List<String> schemaNames = new ArrayList<>();

        RecordingAiClient(String... responses) {
            this.responses = List.of(responses);
            this.exception = null;
        }

        RecordingAiClient(RuntimeException exception) {
            this.responses = List.of();
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
            this.userPrompts.add(userPrompt);
            if (exception != null) {
                throw exception;
            }
            if (responses.isEmpty()) {
                return "";
            }
            String response = responses.get(Math.min(responseIndex, responses.size() - 1));
            responseIndex++;
            return response;
        }

        @Override
        public String chatJsonWithSystem(String systemPrompt,
                                         String userPrompt,
                                         String schemaName,
                                         Map<String, Object> jsonSchema) {
            this.schemaNames.add(schemaName);
            return chatWithSystem(systemPrompt, userPrompt);
        }
    }
}
