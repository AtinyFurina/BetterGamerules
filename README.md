# BetterGamerules

> Minecraft Forge 1.20.1 mod — Edit every game rule through a clean in-game GUI. Press **Ctrl+G** and never type `/gamerule` again.

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.20.1-green?style=flat-square" alt="MC 1.20.1">
  <img src="https://img.shields.io/badge/Forge-47.3.0+-orange?style=flat-square" alt="Forge 47+">
  <img src="https://img.shields.io/badge/Java-17-blue?style=flat-square" alt="Java 17">
  <img src="https://img.shields.io/badge/License-MIT-yellow?style=flat-square" alt="MIT">
  <img src="https://img.shields.io/badge/Version-1.1.0-5b8731?style=flat-square" alt="v1.1.0">
</p>

---

## Features

- **Simple Mode** — 12 most-used rules at your fingertips. Toggle booleans with one click, adjust integers with both a **slider** (quick) and a **text input** (precise). The two stay synced in real time.
- **Advanced Mode** — All game rules in one scrollable list with real-time search. Filter by rule ID or display name.
- **Vanilla + Modded** — Works with **every vanilla gamerule** (47 in 1.20.1) **plus any gamerule added by other mods**. Unknown rules get sensible dynamic sliders and auto-formatted display names.
- **Vanilla-Style UI** — Semi-transparent dark panel, alternating card rows, `?` help tooltips, all matching Minecraft's modern (1.19.3+) visual language.
- **Customizable** — Pick exactly which rules appear in Simple Mode. Your list persists across restarts.
- **Multiplayer Ready** — Only operators (permission level 2+) can modify rules. Fully compatible with dedicated servers.
- **Full i18n** — Complete Chinese and English translations for every UI element and game rule description.

## Quick Start

| Action | Key |
|--------|-----|
| Open the GUI | **Ctrl+G** or `/bettergamerules` or `/bg` |
| Toggle a boolean rule | Click its button |
| Adjust an integer rule | Drag the slider or click the number to type |
| Search (Advanced Mode) | Type in the search bar |
| Switch Simple / Advanced | Click the tab buttons |
| Customize Simple Mode list | Click "Customize List" |
| Close | Click "Done" or press Esc |

## Installation

1. Install **Minecraft Forge 1.20.1** (47.3.0 or newer)
2. Download [bettergamerules-1.1.0.jar](https://github.com/AtinyFurina/BetterGamerules/releases/download/v1.1.0/bettergamerules-1.1.0.jar) from Releases
3. Place it in `.minecraft/mods/`
4. Launch the game and press **Ctrl+G**

## Build from Source

```bash
# Requirements: JDK 17
./gradlew build
# Output: build/libs/bettergamerules-1.1.0.jar
```

## License

MIT — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Made with ❤️ by <b>AtinyFurina</b> & <b>DeepSeek V4 Pro</b>
</p>
