# 剧本 YAML Schema 结构

## 1. 顶层结构

剧本 YAML 的顶层必须包含三个核心部分：

```yaml
project:
  # 项目信息

characters:
  # 人物表

episodes:
  # 分集剧本
```

完整结构如下：

```yaml
project:
  title: "小说改编剧本"
  source_type: "novel"
  script_type: "web_drama"
  language: "zh-CN"
  target_episodes: 3
  summary: "这是根据小说自动生成的剧本初稿。"

characters:
  - id: "char_001"
    name: "林秋"
    role: "主角"
    description: "故事的核心人物。"
    motivation: "寻找真相。"
    relationships:
      - target: "char_002"
        relation: "朋友"

episodes:
  - episode_id: 1
    title: "第一集"
    summary: "主角进入故事核心冲突。"
    scenes:
      - scene_id: "1-1"
        title: "雨夜相遇"
        location: "城市街道"
        time: "夜晚"
        characters:
          - "char_001"
          - "char_002"
        action: "林秋在雨夜中发现了一封神秘信件。"
        dialogues:
          - character: "char_001"
            line: "这封信为什么会出现在这里？"
          - character: "char_002"
            line: "也许有人一直在等你发现它。"
```

---

## 2. YAML Schema 定义

```yaml
type: object
required:
  - project
  - characters
  - episodes

properties:
  project:
    type: object
    required:
      - title
      - source_type
      - script_type
      - language
      - target_episodes
    properties:
      title:
        type: string
        description: "剧本标题"
      source_type:
        type: string
        enum:
          - novel
        description: "原始文本类型，当前项目固定为 novel"
      script_type:
        type: string
        enum:
          - web_drama
          - short_drama
          - movie
          - stage_play
        description: "剧本类型"
      language:
        type: string
        enum:
          - zh-CN
          - en-US
        description: "剧本语言"
      target_episodes:
        type: integer
        minimum: 1
        description: "目标集数"
      summary:
        type: string
        description: "剧本整体简介"

  characters:
    type: array
    minItems: 1
    description: "人物表"
    items:
      type: object
      required:
        - id
        - name
        - role
      properties:
        id:
          type: string
          description: "人物唯一 ID，例如 char_001"
        name:
          type: string
          description: "人物名称"
        role:
          type: string
          description: "人物角色，例如主角、配角、反派"
        description:
          type: string
          description: "人物简介"
        motivation:
          type: string
          description: "人物目标或动机"
        relationships:
          type: array
          description: "人物关系"
          items:
            type: object
            required:
              - target
              - relation
            properties:
              target:
                type: string
                description: "关联人物 ID"
              relation:
                type: string
                description: "与目标人物的关系"

  episodes:
    type: array
    minItems: 1
    description: "分集剧本列表"
    items:
      type: object
      required:
        - episode_id
        - title
        - summary
        - scenes
      properties:
        episode_id:
          type: integer
          minimum: 1
          description: "集数编号"
        title:
          type: string
          description: "单集标题"
        summary:
          type: string
          description: "单集剧情简介"
        scenes:
          type: array
          minItems: 1
          description: "单集中的场景列表"
          items:
            type: object
            required:
              - scene_id
              - title
              - location
              - time
              - characters
              - action
              - dialogues
            properties:
              scene_id:
                type: string
                description: "场景编号，例如 1-1"
              title:
                type: string
                description: "场景标题"
              location:
                type: string
                description: "场景地点"
              time:
                type: string
                description: "场景时间，例如 白天、夜晚、清晨"
              characters:
                type: array
                minItems: 1
                description: "本场景出场人物 ID 列表"
                items:
                  type: string
              action:
                type: string
                description: "动作描写或场景说明"
              dialogues:
                type: array
                description: "对白列表"
                items:
                  type: object
                  required:
                    - character
                    - line
                  properties:
                    character:
                      type: string
                      description: "说话人物 ID"
                    line:
                      type: string
                      description: "对白内容"
```

---

## 3. 字段说明

### 3.1 project

`project` 用于描述整个剧本项目的基本信息。

| 字段              | 类型      | 是否必填 | 说明                 |
| --------------- | ------- | ---- | ------------------ |
| title           | string  | 是    | 剧本标题               |
| source_type     | string  | 是    | 原始文本类型，当前固定为 novel |
| script_type     | string  | 是    | 剧本类型               |
| language        | string  | 是    | 剧本语言               |
| target_episodes | integer | 是    | 目标集数               |
| summary         | string  | 否    | 剧本整体简介             |

---

### 3.2 characters

`characters` 用于保存人物表。

| 字段            | 类型     | 是否必填 | 说明      |
| ------------- | ------ | ---- | ------- |
| id            | string | 是    | 人物唯一 ID |
| name          | string | 是    | 人物名称    |
| role          | string | 是    | 人物角色    |
| description   | string | 否    | 人物简介    |
| motivation    | string | 否    | 人物动机    |
| relationships | array  | 否    | 人物关系    |

---

### 3.3 episodes

`episodes` 用于保存分集剧本。

| 字段         | 类型      | 是否必填 | 说明     |
| ---------- | ------- | ---- | ------ |
| episode_id | integer | 是    | 集数编号   |
| title      | string  | 是    | 单集标题   |
| summary    | string  | 是    | 单集剧情简介 |
| scenes     | array   | 是    | 场景列表   |

---

### 3.4 scenes

`scenes` 用于保存每一集中的具体场景。

| 字段         | 类型     | 是否必填 | 说明         |
| ---------- | ------ | ---- | ---------- |
| scene_id   | string | 是    | 场景编号       |
| title      | string | 是    | 场景标题       |
| location   | string | 是    | 场景地点       |
| time       | string | 是    | 场景时间       |
| characters | array  | 是    | 出场人物 ID 列表 |
| action     | string | 是    | 动作描写       |
| dialogues  | array  | 是    | 对白列表       |

---

### 3.5 dialogues

`dialogues` 用于保存场景中的人物对白。

| 字段        | 类型     | 是否必填 | 说明      |
| --------- | ------ | ---- | ------- |
| character | string | 是    | 说话人物 ID |
| line      | string | 是    | 对白内容    |

---

## 4. Schema 设计原因

### 4.1 使用 project 保存整体信息

`project` 用于记录剧本标题、剧本类型、语言和目标集数，方便前端展示和后端导出。

---

### 4.2 使用 characters 管理人物信息

剧本创作中人物是核心元素。
将人物单独放入 `characters`，可以避免在每个场景中重复写人物介绍。

场景中只需要通过人物 ID 引用人物，例如：

```yaml
characters:
  - "char_001"
```

这样可以减少冗余，也方便后续检查人物一致性。

---

### 4.3 使用 episodes 表达分集结构

题目要求将小说改编为剧本初稿，而剧本通常需要分集或分段组织。

因此使用：

```yaml
episodes:
  - episode_id: 1
    scenes:
      - scene_id: "1-1"
```

这种结构能够清楚表达：

```txt
剧本 → 集数 → 场景 → 对白
```

---

### 4.4 使用 scenes 表达分场剧本

剧本不是普通小说文本，而是由多个场景组成。

每个场景包含：

1. 地点
2. 时间
3. 出场人物
4. 动作描写
5. 对白

这符合基本剧本写作逻辑，也方便用户后续逐场修改。

---

### 4.5 使用 dialogues 表达人物对白

对白是剧本的核心内容之一。

每条对白包含：

```yaml
character: "char_001"
line: "这封信为什么会出现在这里？"
```

这样可以明确“谁说了什么”，方便前端展示，也方便后续进行对白优化。

---

## 5. 最小合法 YAML 示例

```yaml
project:
  title: "示例剧本"
  source_type: "novel"
  script_type: "web_drama"
  language: "zh-CN"
  target_episodes: 3
  summary: "这是一个由小说改编的剧本初稿。"

characters:
  - id: "char_001"
    name: "林秋"
    role: "主角"
    description: "故事主人公。"
    motivation: "寻找真相。"
    relationships: []

episodes:
  - episode_id: 1
    title: "第一集"
    summary: "故事开始，主角发现异常。"
    scenes:
      - scene_id: "1-1"
        title: "开端"
        location: "房间"
        time: "夜晚"
        characters:
          - "char_001"
        action: "林秋独自坐在桌前，发现了一张陌生纸条。"
        dialogues:
          - character: "char_001"
            line: "这到底是谁留下的？"
```

---

## 6. 推荐文件名

建议将 Schema 文件保存为：

```txt
backend/src/main/resources/schema/script-schema.yaml
```

建议将说明文档保存为：

```txt
docs/yaml-schema.md
```
