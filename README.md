# ScriptForge

ScriptForge 是一个 AI 小说转剧本工具 MVP，用于把 3 个章节以上的小说文本改编为结构化 YAML 剧本初稿。

## 当前基础结构

```text
ScriptForge/
├── frontend/                  # Vue 3 + Vite + Element Plus 工作台
├── backend/                   # Spring Boot REST API
├── docs/                      # 需求、Schema、接口和设计文档
├── storage/                   # 后续文件存储目录
└── docker/                    # 本地基础设施配置
```

## 已实现的 MVP 骨架

- 项目 CRUD：`/api/projects`
- 小说导入：粘贴文本和 `.txt` / `.md` 文件上传
- 章节解析：校验至少 3 个章节
- 改编配置：剧本类型、集数、风格、语言、对白保留等
- 剧本生成：内置规则型 Agent 生成合法 YAML 初稿
- YAML 编辑：获取、保存、校验、简单修复
- Schema：后端提供 YAML Schema 和设计说明
- 导出：YAML 和 Markdown 文件

## 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认运行在 `http://localhost:8080`。

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，并通过 Vite proxy 转发 `/api` 到后端。

## 后续替换点

- 将 `RuleBasedScriptGenerationAgent` 替换为 OpenAI Compatible / Qwen / DeepSeek 客户端。
- 将 `ProjectService` 中的内存存储替换为 MySQL + MyBatis。
- 将普通 textarea 替换为 Monaco Editor 或 CodeMirror。
- 增加质量检查、单场重写、版本管理等扩展模块。
