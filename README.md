# ScriptForge

ScriptForge 是一个 AI 小说转剧本工具 MVP，用于把 3 个章节以上的小说文本改编为结构化 YAML 剧本初稿。项目提供从项目创建、小说导入、改编配置、AI 生成、YAML 编辑校验到 YAML/Markdown 导出的完整闭环。
## 项目视频地址
https://www.bilibili.com/video/BV11dEx6vEd8/
## 核心功能

- 项目管理：创建、查看、更新、删除小说改编项目。
- 小说输入：支持粘贴文本和上传 `.txt` / `.md` 文件。
- 章节解析：自动识别章节，并要求生成前至少包含 3 个章节。
- 改编配置：设置剧本类型、目标集数、单集时长、语言、风格、改编强度、对白风格、制作倾向和是否保留原文对白。
- AI 剧本生成：根据小说章节和改编配置生成 YAML 剧本初稿。
- 兼容性保护：LLM 内部优先生成 JSON，并按分集逐步生成；后端合并后转换为 YAML，校验失败时会先进行一次 LLM 修复重试，再降级到规则生成。
- YAML 编辑：支持查看、保存、校验和自动修复 YAML。
- Schema 文档：提供 YAML Schema 和 Schema 设计说明。
- 文件导出：支持导出 YAML 和 Markdown。

## 技术栈

后端：

- Java 17
- Spring Boot 3.2
- MyBatis Plus
- MySQL 8
- SnakeYAML
- OpenAI Compatible / Gemini / DeepSeek / Qwen API

前端：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios

## 项目结构

```text
ScriptForge/
├── backend/                   # Spring Boot REST API 和 AI Agent
│   ├── src/main/java/...       # controller / service / ai / mapper / entity / dto / util
│   └── src/main/resources/     # application.yml、schema、prompt、SQL
├── frontend/                  # Vue 3 + Vite 前端工作台
├── docs/                      # API、设计、YAML Schema 等文档
├── docker/                    # MySQL Docker Compose 配置
└── storage/                   # 导出和临时文件目录
```

## 快速启动

### 1. 准备环境

需要安装：

- JDK 17
- Maven 3.8+
- Node.js 18+
- npm
- MySQL 8，或 Docker

### 2. 启动 MySQL

可以使用仓库内的 Docker Compose：

```bash
cd docker
docker compose up -d
```

初始化数据库表：

```bash
cd ..
mysql -h 127.0.0.1 -P 3306 -uroot -pscriptforge < backend/src/main/resources/sql/schema.sql
```

如果使用自己的 MySQL，只需执行同一个 `schema.sql`，并按实际连接信息设置后端环境变量。

### 3. 启动后端

本地演示可以关闭真实 AI 调用，使用规则生成器跑通流程：

```bash
cd backend
AI_ENABLED=false \
DB_USERNAME=root \
DB_PASSWORD=scriptforge \
mvn spring-boot:run
```

后端默认运行在：

```text
http://localhost:8080
```

如果要启用真实大模型：

```bash
cd backend
AI_ENABLED=true \
AI_PROVIDER=gemini \
GEMINI_API_KEY=你的_API_KEY \
DB_USERNAME=root \
DB_PASSWORD=scriptforge \
mvn spring-boot:run
```

可选 provider：

- `gemini`
- `openai`
- `deepseek`
- `qwen`

常用 AI 环境变量：

```bash
AI_ENABLED=true
AI_PROVIDER=gemini
GEMINI_API_KEY=...
OPENAI_API_KEY=...
DEEPSEEK_API_KEY=...
QWEN_API_KEY=...
```

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在：

```text
http://localhost:5173
```

Vite 会将 `/api` 请求代理到后端。

## 使用流程

1. 在前端创建小说改编项目。
2. 上传 `.txt` / `.md` 小说文件，或粘贴小说正文。
3. 系统自动解析章节，确认章节数量不少于 3。
4. 填写改编配置，例如目标集数、剧本类型、语言和风格。
5. 点击生成剧本。
6. 查看生成的 YAML 剧本初稿。
7. 在编辑器中修改 YAML，并执行校验或修复。
8. 导出 YAML 或 Markdown 文件。

## AI 生成策略

真实 AI 模式下，后端使用 `LlmScriptGenerationAgent` 生成剧本。为了降低 LLM 输出格式漂移，当前实现包含三层保护：

1. JSON-first：请求模型输出符合 JSON Schema 的结构化内容。
2. 分段生成：先生成 `project`、`characters` 和分集大纲，再逐集生成 episode，减少长文本输出被截断或局部 YAML 损坏的概率。
3. 修复重试：最终 YAML 校验失败时，先把错误和草稿交给模型修复一次；仍失败才降级为规则生成器。

最终保存和展示给用户的结果始终是 YAML。

## YAML 剧本结构

生成结果顶层包含三个部分：

```yaml
project:
  title: 示例小说改编剧本
  source_type: novel
  script_type: web_drama
  language: zh-CN
  target_episodes: 3
  summary: 剧本整体简介

characters:
  - id: char_001
    name: 主角
    role: 主角
    description: 人物简介
    motivation: 人物动机

episodes:
  - episode_id: 1
    title: 第一集
    summary: 本集剧情摘要
    scenes:
      - scene_id: 1-1
        title: 场景标题
        location: 场景地点
        time: 时间
        characters:
          - char_001
        action: 动作描写
        dialogues:
          - character: char_001
            line: 对白内容
```

完整字段说明见 [docs/yaml-schema.md](docs/yaml-schema.md)，后端 Schema 文件位于 [backend/src/main/resources/schema/script-schema.yaml](backend/src/main/resources/schema/script-schema.yaml)。

## API 概览

统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

主要接口：

| 模块 | 方法 | 路径 | 说明 |
|---|---|---|---|
| 项目 | POST | `/api/projects` | 创建项目 |
| 项目 | GET | `/api/projects` | 项目列表 |
| 项目 | GET | `/api/projects/{projectId}` | 项目详情 |
| 项目 | PUT | `/api/projects/{projectId}` | 更新项目 |
| 项目 | DELETE | `/api/projects/{projectId}` | 删除项目 |
| 小说 | POST | `/api/projects/{projectId}/novel/text` | 提交小说文本 |
| 小说 | POST | `/api/projects/{projectId}/novel/file` | 上传小说文件 |
| 小说 | GET | `/api/projects/{projectId}/novel` | 获取小说内容 |
| 配置 | GET | `/api/projects/{projectId}/settings` | 获取改编配置 |
| 配置 | PUT | `/api/projects/{projectId}/settings` | 保存改编配置 |
| 剧本 | POST | `/api/projects/{projectId}/script/generate` | 生成剧本 |
| 剧本 | POST | `/api/projects/{projectId}/script/regenerate` | 重新生成 |
| 剧本 | GET | `/api/projects/{projectId}/script/status` | 生成状态 |
| 剧本 | GET | `/api/projects/{projectId}/script` | 获取 YAML |
| 剧本 | PUT | `/api/projects/{projectId}/script` | 保存 YAML |
| 剧本 | POST | `/api/projects/{projectId}/script/validate` | 校验 YAML |
| 剧本 | POST | `/api/projects/{projectId}/script/repair` | 修复 YAML |
| Schema | GET | `/api/schema/script` | 获取 YAML Schema |
| Schema | GET | `/api/schema/script/design` | 获取设计说明 |
| 导出 | GET | `/api/projects/{projectId}/export/yaml` | 导出 YAML |
| 导出 | GET | `/api/projects/{projectId}/export/markdown` | 导出 Markdown |

更多接口说明见 [docs/api.md](docs/api.md)。

## 常用命令

后端测试：

```bash
cd backend
mvn test
```

前端构建：

```bash
cd frontend
npm run build
```

停止 Docker MySQL：

```bash
cd docker
docker compose down
```

## 配置说明

后端主要配置位于 [backend/src/main/resources/application.yml](backend/src/main/resources/application.yml)。

常用环境变量：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/novel_script...` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | 空 | 数据库密码 |
| `AI_ENABLED` | `true` | 是否启用真实 AI Agent |
| `AI_PROVIDER` | `gemini` | AI provider |
| `GEMINI_API_KEY` | 空 | Gemini API Key |
| `OPENAI_API_KEY` | 空 | OpenAI API Key |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key |
| `QWEN_API_KEY` | 空 | Qwen API Key |

## 文档索引

- [API 设计](docs/api.md)
- [系统设计](docs/design.md)
- [YAML Schema 说明](docs/yaml-schema.md)
- [需求文档](docs/AI小说转剧本工具_需求文档.md)

## MVP 边界

当前版本聚焦小说到 YAML 剧本初稿的核心闭环，暂不包含用户权限、多人协作、版本管理、支付系统、复杂 RAG、DOCX/PDF 高级导出等商业级功能。
