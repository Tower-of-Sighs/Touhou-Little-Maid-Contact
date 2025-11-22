# Touhou Little Maid: Epistalove 玩家指南

面向玩家的说明：通过数据包或 KubeJS，让女仆在合适的时机写信并投递到你手中或邮筒。

---

## 重要概念
- 冷却以 `tick` 计（`20 tick ≈ 1 秒`）。
- 触发器类型：`once` 一次性（消费并持久记录），`repeat` 可重复（受冷却约束）。
- 成就触发只在获得成就当刻触发，不区分 `once/repeat`。

---

## 快速上手
- 数据包：在你的世界的 `datapacks` 中新增包，放置规则到 `data/touhou_little_maid_epistalove/maid_letters/*.json`，重进游戏或重载以生效。
- KubeJS：将脚本放到 `kubejs/server_scripts/*.js`，在 `LetterEvents.registerLetterRules` 中注册规则。

---

## 规则字段（通用）
- `type`: `preset` 或 `ai`
- `id`: 规则唯一 ID
- `triggers`: 触发器 ID 列表（如原版成就 `minecraft:story/mine_stone`，或自定义 `xxx:xxx`）
- `trigger_type`: `once`（一次性消费并持久化）或 `repeat`（数据包为 `persistent`）
- `min_affection` / `max_affection`: 好感度区间
- `cooldown`: 冷却（tick）
- `favorability_change` 与 `favorability_threshold`：每次送信的好感变化与阈值控制（达到阈值不再继续升/降）; KJS `.affectionChange()`/`.affectionThreshold()`
- 模型限制：数据包 `maid_id`（数组）；KJS  `.maidId()`/`.maidIds()`

---

## 生成器
- `preset`：固定标题与内容，指定 `parcel`（包裹物品）与 `postcard`（明信片样式）。ID 必须存在。
- `ai`：由提示词与可选语调生成。

---

## 触发与上下文
- 自定义触发：用 `LetterAPI.triggerEvent(player, id)` 或 `triggerEventWithContext(player, id, map)` 标记触发，`triggerEventWithContext` 可携带上下文供 AI 插值。
- 一次性消费记录（仅自定义触发）：
  - 查询：`LetterAPI.hasConsumedOnce(player, ruleId, triggerId)`
  - 清除：`LetterAPI.clearConsumedOnce(player, ruleId, triggerId)`
    - 由于此处过于底层，不再详细说明，有需要请翻阅源码。

---

## 投递与交付
- 跟随模式：靠近主人（≈3 格）直接交付。
- Home 模式：
  - 主人在家范围内：优先使用安全、可达邮筒，否则交付给主人。
  - 主人不在家：若找到可用邮筒则投递，否则暂缓。

---

## 常见问题
- 我触发了事件但没收到信件？
  - 冷却中：同一规则仍在冷却，请等待冷却结束。
  - AI 未启用或站点不可用。
  - AI 内容被质量过滤拒绝：提示词过短、过于通用，或内容长度不足，尝试改进提示词。（十分少见）
  - 一次性触发器被消耗但生成失败：请再次触发（once 类型会消耗触发器）。

- 预设信件没生成？
  - `postcard` 或 `parcel` ID 无效：检查数据包 JSON 的 ID 是否正确，是否存在对应的物品或样式。
  - `gifts` 数组长度错误：必须为 1，且仅一个礼物条目。

- 与女仆聊天无法触发送信？
  - 车万女仆 `Function Call 功能` 配置未启用。

- 邮筒投递不成功？
  - 邮筒是否在家范围内、可达且安全（模组会自动评估）。
  - 走位卡住：女仆可能重新规划路径，稍等一会儿。

- 某次重启游戏后发现触发不了 KJS/数据包的规则了？
  - 将女仆收进魂符再放出来，尽管这操作可能会导致之前某次的触发这时才生效。

---

## 示例
- 数据包（`data/touhou_little_maid_epistalove/maid_letters/first_gift.json`）：
```json5
{
  "type": "ai",
  "id": "first_gift",
  "triggers": ["minecraft:story/mine_stone"], // 当玩家获得成就 `minecraft:story/mine_stone` 也就是 `石器时代`时触发
  "trigger_type": "once", // 由于成就的特殊处理（成就每个存档只能获得一次），无需填写 trigger_type，此处仅做占位处理。
  "maidId": ["geckolib:zhiban", "geckolib:winefox_new_year"], // 指定女仆为纸板狐和新年酒狐，若不是这两个女仆则不会触发该规则。
  "min_affection": 20, // 要求最小好感度：20，小于此值不会触发此规则。
  "max_affection": 90, // 要求最大好感度：90，大于此值不会触发此规则。
  "cooldown": 1000, // 冷却 1000 tick（50s）， 触发一次后，1000 tick 过后方可再次触发，由于成就的特殊处理（成就每个存档只能获得一次），无需填写 cooldown，此处仅做占位处理。
  "favorability_change": 20, // 送信增加 20 好感度。
  "favorability_threshold": 100, // 当好感度达 100 时便不再增加。
  "ai": {
    "prompt": "当主人第一次在矿洞里拾起石头时，写一封富有氛围感的短笺。", // 给 AI 的提示词，可自由发挥。
    "tone": "lonesome" // 奠定全文的语气风格基调。
  }
}
```
- KubeJS（`kubejs/server_scripts/letters.js`）：
```js
LetterEvents.registerLetterRules(event => {
  event.createAI('first_gift_kjs', 'lonesome', '主人获得了成就:${str}，请为他写封信') // 此处引用该触发器的上下文，会自动填充 str 的内容。
    .trigger('touhou_little_maid_epistalove:advancement_gain') // 由于 advancement_gain 是在玩家获取成就的事件里触发的，所以只要玩家获取了成就，则会触发该触发器。
    .repeat() // 可重复被触发，若为 once，则获取一个成就之后不再被触发。
    .maidIds(["geckolib:zhiban", "geckolib:winefox_new_year"]) // 指定女仆为纸板狐和新年酒狐，若不是这两个女仆则不会触发该规则。
    .minAffection(20) // 要求最小好感度：20，小于此值不会触发此规则。
    .maxAffection(90) // 要求最大好感度：90，大于此值不会触发此规则。
    .affectionChange(-100) // 送信减少 100 好感度。
    .affectionThreshold(0) // 当好感度达 0 时便不再减少。
    .cooldown(1000) // 冷却 1000 tick（50s）， 触发一次后，1000 tick 过后方可再次触发。
    .register()
})

// 此处获取成就 ID 与描述，并添加到触发器 `advancement_gain` 的上下文中。
PlayerEvents.advancement(event => {
  const player = event.player;
  let advancement = event.advancement;
  if (advancement.description.empty) return;
  let str = `${advancement.displayText.getString()}:${advancement.description.getString()}(${advancement.description.getContents().getKey()})`;

  LetterAPI.triggerEventWithContext(player, 'touhou_little_maid_epistalove:advancement_gain', {
    str: str
  })
})
```


没什么好说的，祝你和女仆的感情更进一步！
