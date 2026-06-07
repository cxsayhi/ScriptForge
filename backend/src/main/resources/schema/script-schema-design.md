# Script YAML Schema Design

The script YAML is organized as `project -> characters -> episodes -> scenes -> dialogues`.

- `project` keeps high-level metadata for listing, display, generation settings, and export.
- `characters` is centralized so scenes can reference stable character IDs instead of repeating character descriptions.
- `episodes` matches the normal production structure of short dramas, web dramas, and serialized adaptations.
- `scenes` keeps each shootable unit explicit: location, time, characters, action, and dialogue.
- `dialogues` stores speaker and line separately, making later dialogue polishing and consistency checks easier.

This structure is intentionally small for the MVP. It is strict enough for validation and editing, but it does not force characters, episodes, or scenes into separate database tables before the core flow is proven.

# 剧本 YAML Schema 设计

剧本 YAML 按照 `project -> characters -> episodes -> scenes -> dialogues` 的结构组织。

- `project` 保存用于列表展示、页面显示、生成设置和导出的高层元数据。
- `characters` 采用集中管理的方式，使场景可以引用稳定的角色 ID，而不是重复编写角色描述。
- `episodes` 符合短剧、网剧和系列化改编的常见制作结构。
- `scenes` 明确保留每一个可拍摄单元：地点、时间、出场角色、动作和对白。
- `dialogues` 将说话人和台词分开存储，便于后续进行对白润色和一致性检查。

该结构是为 MVP 有意保持精简的设计。它足够严格，能够支持校验和编辑，但不会在核心流程被验证之前，强制将角色、剧集或场景拆分为独立的数据表。