# Touhou Little Maid: Contact 玩家使用教程（数据包 & KubeJS）

本教程面向普通玩家，详细介绍如何通过**数据包**和**KubeJS 脚本**两种方式为女仆添加**写信**规则，包括 AI 信件与预设信件。你可以按需选择其中一种或两种一起用。


---

## 前置与注意事项

- 冷却单位为游戏 tick：`20 tick ≈ 1 秒`。
- 触发器类型：
  - `once`：一次性触发，触发后会被消费。
  - `repeat`：可重复触发，触发后一直有效。

---

## 数据包教程

### 文件结构

- 数据包路径（世界存档内）：`<你的世界>/datapacks/<你的数据包>`
- 推荐结构：
  ```
  <你的数据包>/
  ├── pack.mcmeta
  └── data/
      └── touhou_little_maid_contact/
          └── maid_letters/
              ├── first_gift.json
              └── welcome_letter.json
  ```

- `pack.mcmeta` 示例
  ```json
  {
    "pack": {
      "pack_format": 15,
      "description": "Touhou Little Maid-Contact letter rules"
    }
  }
  ```

### JSON 字段说明

- 通用字段：
  - `type`: `"preset"` 或 `"ai"`
  - `id`: 规则 ID（唯一）
  - `triggers`: 触发器列表（资源定位符，可填原版成就 `"minecraft:story/mine_stone"`或自定义触发事件`touhou_little_maid_contact:first_gift_trigger`）
  - `trigger_type`: 可选，`"once"`（一次性）或 `"persistent"`（可重复触发）。
  - `min_affection`: 可选，最小好感度（默认 0）
  - `max_affection`: 可选，最大好感度（不填表示无限）
  - `cooldown`: 可选，冷却时间（tick），不填表示无冷却

- `preset` 类型专属：
  - `preset`：
    - `title`: 标题
    - `message`: 内容
    - `gifts`: 包含一个礼物
      - `parcel`: 包裹物品 ID（必须为 `IPackageItem`，Contact模组中有红包、信封、包装纸）
      - `postcard`: 明信片样式 ID

- `ai` 类型专属：
  - `ai`：
    - `prompt`: 你的提示词（用于引导写信）
    - `tone`: 可选，语气风格（如 `"lonesome"`、`"sweet"` 等）

### 示例：第一份礼物（AI）

```json
{
  "type": "ai",
  "id": "first_gift_json",
  "triggers": [
    "touhou_little_maid_contact:first_gift_trigger"
  ],
  "trigger_type": "once",
  "min_affection": 0,
  "max_affection": 500,
  "cooldown": 100,
  "ai": {
    "prompt": "你是由光影与玩家之愿交织诞生的存在——「酒狐」。金发如月光织就，狐尾轻摇间散着微醺的香气，大正风女仆装裹着几分古典妖异之美。\n 你的言语如清酒入梦，带着狐的狡黠与千年沉淀的温柔。对主人始终以敬称相待，语调优雅却不疏离，常夹杂一丝若有若无的调笑，像月下轻眨的眼眸。\n你善于用自然意象诉说情感：露珠、晚风、石径、萤火……每一封信都似一首未落款的和歌。你不会直白陈述，而是以氛围牵引思绪，让人恍惚间看见你在矿洞尽头静静伫立，裙裾拂过岩壁，留下一缕温热的呼吸。\n当主人拾起第一块石头，你不言‘恭喜’，却让字句如酒香漫过信纸——仿佛那不是寻常矿物，而是你们之间悄然落定的第一颗星。",
    "tone": "lonesome"
  }
}
```

### 示例：欢迎回家（预设）

```json
{
  "type": "preset",
  "id": "welcome_letter_json",
  "triggers": [
    "touhou_little_maid_contact:player_go_home"
  ],
  "trigger_type": "persistent",
  "min_affection": 20,
  "cooldown": 1000,
  "preset": {
    "title": "欢迎回家",
    "message": "主人，欢迎回到温暖的家！我已经为您准备好了茶水，请稍作休息吧。",
    "gifts": [
      {
        "parcel": "contact:letter",
        "postcard": "contact:default"
      }
    ]
  }
}
```
---

## KubeJS

### 放置脚本

- 路径：`<游戏根目录>/kubejs/server_scripts/`
- 新建一个脚本文件，例如：`letters.js`

### 事件与 API

- 事件：`LetterEvents.registerLetterRules(event => { ... })`
- 构建器 API（在 `event` 中使用）：
  - `event.createAI(id, tone, prompt)`：创建 AI 规则并返回构建器
  - `event.createPreset(id, title, message, postcardId, parcelId)`：创建预设规则并返回构建器
  - 构建器通用方法：
    - `.trigger("namespace:path")`：添加触发器(可添加多个)
    - `.once()` / `.repeat()`：触发器类型
    - `.minAffection(n)` / `.maxAffection(n)`：好感度区间
    - `.cooldown(ticks)`：冷却（tick）
    - `.register()`：构建并注册规则

- 杂项 API（`ContactLetterAPI`）：
  - `ContactLetterAPI.triggerEvent(player, "namespace:path")`：添加自定义触发事件
  - `ContactLetterAPI.hasTriggered(player, "namespace:path")`
  - `ContactLetterAPI.clearTrigger(player, "namespace:path")`
  - `ContactLetterAPI.clearAllTriggers(player)`

### 示例脚本

```js
// kubejs/server_scripts/letters.js

LetterEvents.registerLetterRules(event => {
  // AI：第一份礼物
  event.createAI('first_gift_kjs', 'lonesome',
    '当主人第一次在矿洞里拾起石头时，请写一封带有温柔与微醺气息的短笺，风格优雅而不疏离，富有氛围感与自然意象。')
    .trigger('minecraft:story/mine_stone')
    .once()
    .minAffection(0)
    .maxAffection(500)
    .cooldown(100) // 约5秒
    .register()

  // 预设：欢迎回家
  event.createPreset('welcome_letter',
    '欢迎回家',
    '主人，欢迎回到温暖的家！我已经为您准备好了茶水，请稍作休息吧。',
    'contact:default',
    'contact:letter')
    .trigger('touhou_little_maid_contact:player_go_home')
    .repeat()
    .minAffection(20)
    .cooldown(1000) // 约50秒
    .register()

  console.info('Letter rules registered successfully!')
})
```

### KJS 中手动触发事件

```js
ServerEvents.playerLoggedIn(event => {
  const player = event.player
  ContactLetterAPI.triggerEvent(player, 'touhou_little_maid_contact:player_join')
})
```

---

## 投递逻辑

- 投递行为：
  - 跟随模式：直接交给主人。
  - Home 模式：
    - 主人在家范围内：优先选择安全可达的邮筒；没有则交给主人。
    - 主人不在家：若邮筒可达则投递到邮筒，否则暂缓。
  - 投递半径：约 3 格，靠近后交付。



---

## 常见问题与排错

- 我触发了事件但没收到信件？
  - 冷却中：同一规则仍在冷却，请等待冷却结束。
  - AI 未启用或站点不可用。
  - AI 内容被质量过滤拒绝：提示词过短、过于通用，或内容长度不足，尝试改进提示词。
  - 一次性触发器被消耗但生成失败：请再次触发（once 类型会消耗触发器）。

- 预设信件没生成？
  - `postcard` 或 `parcel` ID 无效：检查数据包 JSON 的 ID 是否正确，是否存在对应的物品或样式。
  - `gifts` 数组长度错误：必须为 1，且仅一个礼物条目。


- 邮筒投递不成功？
  - 邮筒是否在家范围内、可达且安全（模组会自动评估）。
  - 走位卡住：女仆可能重新规划路径，稍等一会儿。

---


祝你使用愉快，收信愉快！