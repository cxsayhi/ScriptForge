# API 设计

统一 JSON 响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

文件导出接口直接返回附件流。

## 项目

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/projects` | 创建项目 |
| GET | `/api/projects` | 项目列表 |
| GET | `/api/projects/{projectId}` | 项目详情 |
| PUT | `/api/projects/{projectId}` | 更新项目 |
| DELETE | `/api/projects/{projectId}` | 删除项目 |

## 小说输入

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/projects/{projectId}/novel/text` | 提交粘贴文本 |
| POST | `/api/projects/{projectId}/novel/file` | 上传 `.txt` / `.md` 文件 |
| GET | `/api/projects/{projectId}/novel` | 获取原文和章节 |

## 改编配置

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/projects/{projectId}/settings` | 获取配置 |
| PUT | `/api/projects/{projectId}/settings` | 保存配置 |

## AI 改编

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/projects/{projectId}/script/generate` | 生成剧本初稿 |
| POST | `/api/projects/{projectId}/script/regenerate` | 重新生成剧本 |
| GET | `/api/projects/{projectId}/script/status` | 获取生成状态 |

## YAML 剧本

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/projects/{projectId}/script` | 获取 YAML |
| PUT | `/api/projects/{projectId}/script` | 保存 YAML |
| POST | `/api/projects/{projectId}/script/validate` | 校验 YAML |
| POST | `/api/projects/{projectId}/script/repair` | 简单修复 YAML |

## Schema 与导出

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/schema/script` | 获取 YAML Schema |
| GET | `/api/schema/script/design` | 获取 Schema 设计说明 |
| GET | `/api/projects/{projectId}/export/yaml` | 导出 YAML |
| GET | `/api/projects/{projectId}/export/markdown` | 导出 Markdown |
