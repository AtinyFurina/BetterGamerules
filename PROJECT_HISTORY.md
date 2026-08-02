# BetterGamerules — 项目开发全记录

## 项目概述

为 Minecraft Java 版 1.20.1 Forge 端开发一个「快速游戏规则修改器」模组。
不写指令、不翻 wiki，按 **Ctrl+G** 可视化修改全部游戏规则。

---

## 开发历程

### 第一版：自定义滚动 UI（v1.0.0 — v1.0.7）

**架构**：`SimpleModeTab` + `AdvancedModeTab` 两个独立面板，手动计算滚动偏移。

**踩过的坑**：
| 版本 | Bug | 根因 | 修复 |
|------|-----|------|------|
| 1.0.1 | 数值滑块溢出 | `Integer.MIN~MAX` 范围导致算术溢出 | 每规则独立合理范围映射 |
| 1.0.1 | 标题与标签重叠 | 标题 Y 坐标和标签 Y 只差 1px | 上移标题 |
| 1.0.2~1.0.3 | 滚轮时输入框不跟随 | 手动 `setX/setY` 和 EditBox 渲染不同步 | 尝试换 ObjectSelectionList |

### 第二版：ObjectSelectionList 重构（v1.0.4 — v1.0.7）

**架构**：`GameruleListWidget` 继承 `ObjectSelectionList`，`GameruleListEntry` 管理每条规则。

**重大失败**：
1. `GameRules.Visitor` 找不到 → 改成 `GameRules.GameRuleTypeVisitor` → 泛型 `visit()` 覆盖
2. `value.toString()` 返回类名@哈希码 → 改用 `value.serialize()`
3. 控件通过 `addWidget()` 注册，Screen 自动分发事件和手动分发**双重触发** → 每次点击执行两次
4. 最终结论：`ObjectSelectionList` 和 `Screen` 的事件系统冲突，点击永远到不了 Entry

### 第三版：完全重写（v2.0.0 — v2.1.8）

**架构**：放弃 ObjectSelectionList。所有控件通过 `addRenderableWidget()` 注册，Minecraft 原版 Screen 负责事件分发，手动滚动偏移。

**核心设计决策**：
- 规则列表数据驱动 → `entries` 列表管理所有 `AbstractWidget`
- `super.render()` 渲染全部控件 → Minecraft 原版机制保证事件正确分发
- 滚动通过 `mouseScrolled` 更新偏移 → `render()` 中 `setX/setY` 定位
- 卡片溢出裁剪用 `Math.max/min` 边界检查

**踩过的坑**：
| 版本 | Bug | 根因 | 修复 |
|------|-----|------|------|
| 2.0.0 | 全部变滑块 | `GameruleHelper` 用了 `toString()` 而非 `serialize()` | 统一用 `serialize()` |
| 2.0.2 | 按钮不显示 | `RuleToggleButton` 里用了不存在的 `minecraft.font` | 改用 `Minecraft.getInstance().font` |
| 2.0.4 | 卡片背景溢出面板 | 裁剪用了硬编码值而非模式相关 `listTop/listBottom` | 改用动态边界 |
| 2.0.5 | 文字全部消失 | `enableScissor` 在 GUI 比例 3 下坐标错位 | 撤掉 scissor，换手动 Y 检查 |
| 2.0.6 | 文字+控件与按钮重叠 | 只裁剪了背景填充，没裁剪文字和控件渲染 | `widgetInView` + 文字 Y 边界检查 |
| 2.1.0 | 描述文字和滑块挤在一起 | 卡片 30px 塞了三样东西 | 去掉内联描述，改成 `?` help tooltip |
| 2.1.5 | 输入框数字左对齐 | EditBox 原生就是左对齐 | 非聚焦时手动 `drawString` 居中，聚焦时 EditBox 编辑 |
| 2.1.7 | 命令修改方块上限值溢出输入框 | 默认 32px 宽放不下 9 位数 | 动态输入框宽度，该规则专门 56px |

### 最终发布（release-1.0.0）

- 去掉标题文字
- 双语 README + LICENSE + .gitignore
- 上传 GitHub：https://github.com/AtinyFurina/BetterGamerules

---

## 最终架构

```
GameruleScreen (Screen)
├── 手动渲染：面板背景、标题、卡片背景、规则名、?图标
├── addRenderableWidget: 标签按钮、搜索框、自定义按钮、完成按钮
├── addRenderableWidget: 每个规则的 RuleToggleButton 或 RuleNumberWidget
├── super.render(): 渲染全部控件
├── 事件：Screen 原版分发到所有 addRenderableWidget 控件
└── 滚动：mouseScrolled → scroll 偏移 → render() 中 setX/setY
```

### 关键文件

| 文件 | 职责 |
|------|------|
| `BetterGamerules.java` | 模组入口，注册配置和网络 |
| `GameruleHelper.java` | 规则数据收集、翻译、范围映射、类型检测 |
| `GameruleScreen.java` | 主界面，整合所有 UI 逻辑 |
| `CustomizeRulesScreen.java` | 自定义简易模式规则列表 |
| `RuleToggleButton.java` | 布尔规则开关控件（56×16 原版风格按钮） |
| `RuleNumberWidget.java` | 数值规则滑块+输入框（116×18） |
| `SearchTextBox.java` | 高级模式搜索框 |
| `ClientConfig.java` | 简易模式规则列表持久化 |

---

## 版本演变

```
1.0.0  初始发布
1.0.1  修复数值溢出 + 标题重叠
1.0.2  ObjectSelectionList 重构
1.0.3  修复 updateSize 参数 + 渲染缺失
1.0.4  尝试修复数据加载（失败）
1.0.5  修复 key cache visitor（失败）
1.0.6  修复双重分发 + 类型判断（失败）
1.0.7  尝试独立事件路由（失败）
2.0.0  完全重写：放弃 ObjectSelectionList
2.0.1  修复 toString() → serialize()
2.0.2  修复按钮样式 + 输入框位置
2.0.3  修复卡片溢出 + EditBox 全高
2.0.4  滑块加粗 + 去 EditBox 暗底
2.0.5  尝试 scissor 裁剪（失败）
2.0.6  手动 Y 边界检查
2.0.7  暂存
2.1.0  去内联描述 → ? help tooltip
2.1.1  规则名截断 + 搜索框偏移
2.1.2  文字+? 图标的 Y 裁剪
2.1.3~2.1.4  输入框文字位置调整
2.1.5  非聚焦时居中显示数字
2.1.6  输入模式文字下移 3px
2.1.7  命令修改方块上限输入框加宽 + 标题调整
2.1.8  删除标题
release-1.0.0  正式发布：双语 README + MIT 协议 + GitHub 上传
```

---

## 经验教训

1. **Minecraft 的 `GameRules.Value.serialize()` ≠ `toString()`** — 用了错的序列化方法导致值全是垃圾字符
2. **ObjectSelectionList 和 Screen 的事件系统不兼容** — 花了 7 个版本才确认这个结论
3. **`enableScissor` 在不同 GUI 比例下坐标计算不同** — 不如手动 Y 检查可靠
4. **`addRenderableWidget` 让 Minecraft 原版管理事件是最稳妥的** — 不要手动和自动混用
5. **EditBox 原生左对齐无法改** — 不如自己画居中文字，聚焦时才用 EditBox

---

## v1.1.0：稳定性修复 + 性能优化 + 模组规则兼容（2026-08-02）

### 背景

发布 release-1.0.0 后从其他环境收集到错误报告，同时进行了全代码深度审查。

### 收集到的错误（来自外部环境，已修复）

| 错误 | 根因 | 修复 |
|------|------|------|
| `InvalidModFileException: Illegal version number specified release-1.0.0` | `gradle.properties` 版本号为 `release-1.0.0`，Forge 不认非标准格式 | 改为 `1.0.0` → `1.1.0` |
| 英文客户端显示原始规则 ID | `en_us.json` 完全缺少 `gamerule.*` 翻译条目 | 补齐 50 条英文翻译 |
| 开关按钮始终显示中文 | `RuleToggleButton` 硬编码 `"✓ 开启"/"✗ 关闭"` | 改为 `Component.translatable()` |
| 高级模式搜索框不生效 | 过滤器设 `w.visible = false` 被 render 循环强制覆盖为 `true` | 改为 `continue` 跳过，不加入 entries |
| 自定义规则列表重启后丢失 | (1) `defineList` validator 对 String 元素检查 `instanceof List`；(2) `setSimpleModeRules()` 缺少 `SPEC.save()` | 修复 validator + 添加 save() |
| 滑块单击不同步到服务器 | `onClick` 更新本地值但 `sendUpdatePacket()` 只在 `onRelease` + `dragging` 时调用 | `onClick` 滑块分支直接发包 |

### 性能优化

| 优化项 | 改动 | 效果 |
|--------|------|------|
| 搜索不再重建 widget | 过滤逻辑从 `rebuildEntries` 移到 `render` | 每按键 50 次 add/remove → 0 |
| 翻译缓存 | 显示名称/描述在数据更新时预计算 | 每帧 5-10 次 translatable 查找 → 每数据更新 1 次 |
| O(1) 选中查找 | `CustomizeRulesScreen` 用 `HashSet` 代替 `List.contains()` | O(n) → O(1) |

### 新功能：模组规则兼容

- **动态滑块范围**：未知模组规则不再固定 `[0, 1000]`，根据当前值动态计算 `[0, max(val×2, 1000)]`
- **降级显示名**：翻译缺失时自动格式化规则 ID（`doFireTick` → "Do Fire Tick"）
- **降级描述**：翻译缺失时显示 "No description available"
- **动态输入框宽度**：根据数值位数决定宽度（≤4 位 32px，≤6 位 40px，≤8 位 48px，>8 位 56px）
- **类型安全应用**：`applyGamerule` 改用 `value instanceof BooleanValue/IntegerValue` 代替信任客户端 `ruleType`

### 代码精简

| 项目 | 改动 |
|------|------|
| clamp() 去重 | 5 份（`GameruleScreen`、`CustomizeRulesScreen`、`RuleNumberWidget`×3）→ `GameruleHelper` 3 个静态方法 |
| 颜色常量统一 | 分散在 4 个文件 → `GameruleHelper` 集中定义 |
| 滚动条去重 | `GameruleScreen` + `CustomizeRulesScreen` → 共享 `drawScrollbar()` |
| 死代码删除 | ~50 行：`getAllRuleIds`、`parseBooleanValue`、`getValue/setValue`、`displayOrder`、`buildContext`、`MinecraftForge.EVENT_BUS.register` |

### 兼容性增强

| 增强项 | 改动 |
|--------|------|
| GameRules Visitor | 仅覆盖泛型 `visit()` → 同时覆盖 `visitBoolean()`/`visitInteger()` |
| 跨服缓存污染 | 静态 `KEY_CACHE` 永不重建 → 每次 `collectAllGamerules` 重建 |
| 网络包安全 | `readUtf()` 无限制 → 加长度上限（id 128, value 32, type 16）+ count 上限 512 |
| 数据丢失修复 | `CustomizeRulesScreen` 关闭时 `new GameruleScreen()` → 复用 `parentScreen` |
| 焦点泄漏修复 | `RuleNumberWidget.inputFocused` 失焦不清理 → 覆盖 `setFocused()` 清理 |

### 发布

- 版本号：`1.0.0` → `1.1.0`
- GitHub Release：[v1.1.0](https://github.com/AtinyFurina/BetterGamerules/releases/tag/v1.1.0)
- 上传 JAR：`bettergamerules-1.1.0.jar`（51 KB）
- 更新 README.md、CHANGELOG.md、MOD_DESCRIPTION.md
