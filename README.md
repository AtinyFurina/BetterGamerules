# BetterGamerules

[English](#english) | [中文](#chinese)

---

<a name="english"></a>
## English

BetterGamerules is a **Minecraft Forge 1.20.1** mod that lets you quickly edit game rules (`/gamerule`) through an in-game GUI. No more typing long commands — just press **Ctrl+G** and adjust everything visually.

### Features

- **Simple Mode** — Your most-used 12 game rules at a glance. One-click toggle for booleans, slider + precise input for numbers.
- **Advanced Mode** — All 47 vanilla game rules with real-time search. Filter by rule ID, Chinese name, or description.
- **Customizable** — Pick which rules appear in Simple Mode. Your list is saved and persists across restarts.
- **Vanilla-Style UI** — Modern Minecraft aesthetic (1.19.3+ era). Semi-transparent dark panel, scrollable card layout, native tooltips.
- **Dual Input** — Integer rules get both a **slider** (quick drag) and a **text input** (precise typing). Bidirectionally synced.
- **Multiplayer Ready** — Works on servers. Only operators (permission level 2+) can modify rules.
- **Full i18n** — Complete Chinese translation for every game rule. English fallback included.
- **Help Tooltips** — Hover the `?` icon next to any rule name to see what it does.

### How to Use

| Action | Key / Command |
|--------|--------------|
| Open GUI | **Ctrl+G** or `/bettergamerules` or `/bg` |
| Toggle boolean rule | Click the button |
| Adjust integer rule | Drag the slider OR click the input box and type a number |
| Search (Advanced Mode) | Type in the search bar |
| Switch modes | Click tab buttons at the top |
| Customize Simple Mode | Click "Customize List" button |
| Close | Click "Done" or press Esc |

### Installation

1. Install **Minecraft Forge 1.20.1** (47.3.0+)
2. Download `bettergamerules-release-1.0.0.jar` from [Releases](../../releases)
3. Place the jar in your `.minecraft/mods/` folder
4. Launch the game and press **Ctrl+G**!

### Building from Source

```bash
# Requirements: JDK 17
./gradlew build
# Output: build/libs/bettergamerules-release-1.0.0.jar
```

---

<a name="chinese"></a>
## 中文

BetterGamerules 是一个 **Minecraft Forge 1.20.1** 模组，让你通过游戏内图形界面快速修改游戏规则（`/gamerule`）。不用再输入长指令——只需按下 **Ctrl+G**，全部可视化操作。

### 功能

- **简易模式** — 你最常用的 12 条规则一目了然。布尔规则一键开关，数值规则滑块 + 精确输入双模式。
- **高级模式** — 全部 47 条原版游戏规则，支持实时搜索。可按规则 ID、中文名或描述筛选。
- **可自定义** — 自由选择简易模式显示哪些规则，列表保存并跨重启持久化。
- **原版风格 UI** — 现代 Minecraft 美学（1.19.3+ 时代）。半透明深色面板、可滚动卡片布局、原生 tooltip。
- **双模输入** — 数值规则同时提供**滑块**（快速拖动）和**文本框**（精确输入），双向实时同步。
- **支持多人** — 可在服务器使用。仅管理员（权限等级 2+）可修改规则。
- **完整汉化** — 每条游戏规则都有中文翻译和详细说明。附带英文备选。
- **帮助提示** — 鼠标悬停在规则名旁的 `?` 图标上查看规则说明。

### 使用方法

| 操作 | 按键 / 指令 |
|------|------------|
| 打开界面 | **Ctrl+G** 或 `/bettergamerules` 或 `/bg` |
| 切换布尔规则 | 点击按钮 |
| 调整数值规则 | 拖动滑块 或 点击输入框输入数字 |
| 搜索（高级模式） | 在搜索栏输入 |
| 切换模式 | 点击顶部标签按钮 |
| 自定义简易模式列表 | 点击「自定义列表」按钮 |
| 关闭 | 点击「完成」或按 Esc |

### 安装

1. 安装 **Minecraft Forge 1.20.1**（47.3.0+）
2. 从 [Releases](../../releases) 下载 `bettergamerules-release-1.0.0.jar`
3. 将 jar 文件放入 `.minecraft/mods/` 文件夹
4. 启动游戏，按 **Ctrl+G**！

### 从源码构建

```bash
# 环境要求：JDK 17
./gradlew build
# 输出：build/libs/bettergamerules-release-1.0.0.jar
```

### 许可证

MIT License — 详见 [LICENSE](LICENSE)

### 致谢

由 **AtinyFurina-Deepseek V4 Pro** 开发。
