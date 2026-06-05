# 系统设计

## MVP 边界

基础结构采用前后端分离。后端先使用内存存储跑通核心流程，保留 MySQL 表结构草案和 Agent 抽象；前端提供完整工作台，让用户可以从项目创建一路走到 YAML 导出。

```text
项目创建 -> 小说导入 -> 章节解析 -> 改编配置 -> 剧本生成 -> YAML 校验/编辑 -> 导出
```

## 后端分层

- `controller`：REST API 和请求参数校验。
- `service`：业务流程编排和当前内存存储。
- `ai/agent`：剧本生成入口，当前为规则型实现，后续接真实大模型。
- `util`：章节解析、YAML 校验、Markdown 导出。
- `entity`：MVP 聚合对象。
- `dto`：请求和响应对象。
- `resources/schema`：YAML Schema 与设计说明。
- `resources/prompts`：Prompt 模板。

## 前端分层

- `api`：Axios 请求封装和类型定义。
- `stores`：Pinia 项目列表状态。
- `views`：项目列表和项目工作区。
- `components`：项目表单、小说导入、配置面板、YAML 编辑器。
- `styles`：全局工作台视觉系统。

## 数据持久化演进

当前 `ProjectService` 使用 `ConcurrentHashMap` 作为演示存储。后续可以按 `backend/src/main/resources/sql/schema.sql` 接入 MySQL：

- `project`
- `novel_content`
- `adaptation_setting`
- `script_result`

人物、分集、场景在 MVP 中保存在 YAML 文本内，避免过早拆表。

## AI Agent 演进

`ScriptGenerationAgent` 是唯一生成接口。真实 AI 版本建议按以下步骤替换：

1. 新增模型客户端，读取环境变量中的 API Key。
2. 读取 `resources/prompts/script-generation.md`。
3. 组装小说章节、配置和 YAML Schema。
4. 调用模型并要求只返回 YAML。
5. 使用 `YamlScriptValidator` 校验。
6. 校验失败时进入自动修复或重新生成。
