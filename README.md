<div align="center">

# 🛡️ StaffCore

### Advanced Moderation & Staff Management Suite for Paper / Spigot

[![Modrinth](https://img.shields.io/badge/Modrinth-StaffCore-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/staffcore-dafealru)
[![SpigotMC](https://img.shields.io/badge/SpigotMC-Resource-orange?style=for-the-badge&logo=spigotmc&logoColor=white)](https://www.spigotmc.org/resources/staffcore-advanced-staff-moderation-suite.134819/)
[![Version](https://img.shields.io/badge/Version-1.3.0-brightgreen?style=for-the-badge)](https://github.com/dr8553097-sudo/StaffCore/releases)
[![Platform](https://img.shields.io/badge/Paper-1.21.x-blue?style=for-the-badge&logo=papermc&logoColor=white)](https://papermc.io)
[![License](https://img.shields.io/badge/License-GPL--3.0-purple?style=for-the-badge)](LICENSE)

---

</div>

## 📖 Overview

**StaffCore** is an enterprise-grade moderation ecosystem engineered for modern Paper and Spigot servers. Built for networks and server administrators that demand **absolute control, visual clarity, strict anti-abuse protections, and seamless staff workflow**.

---

## ✨ Features

- 🕵️ **Staff Mode:** Seamlessly switch into staff mode with automated inventory backup and restore.
- 🌟 **Glowing Silhouette Aura:** Outlines the staff character model with glowing borders and orbital particle helixes.
- 🛠️ **8 Hotbar Tools:**
  - 🧭 **Teleporter:** Jump directly to players.
  - 🧊 **Freeze Tool:** Instantly freeze/unfreeze suspects.
  - 📦 **Inspector:** View inventory, armor, enderchest & stats in real-time.
  - 👁️ **Spectator Tool:** One-click toggle between flight and spectator noclip mode.
  - ⭐ **Staff Panel:** GUI moderation menu.
  - 🥕 **Night Vision:** Infinite clear vision toggle.
  - 🔮 **Vanish:** Complete silent invisibility.
  - 🔴 **Safe Exit:** Safely restore player mode.
- 🔨 **IP Moderation:** `/scbanip`, `/sctempbanip`, `/scunbanip`.
- 🧊 **Anti-Bypass Freeze:** Blocks vehicles, elytras, tridents, drops, and re-freezes on reconnect.
- 💎 **Heuristic X-Ray Alerts:** Monitors suspicious vein mining rates in real time.
- 📋 **Ticket System:** Player reports with `/report` and emergency `/helpop`.
- 🌐 **Multi-Language:** English (`en`), Spanish (`es`), French (`fr`), Portuguese (`pt`).

---

## 💻 Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/staff` | Toggle staff mode | `staffcore.staff` |
| `/staff menu` | Open master GUI panel | `staffcore.staff` |
| `/staff speed <0-10>` | Adjust walk speed | `staffcore.staff` |
| `/staff flyspeed <0-10>` | Adjust fly speed | `staffcore.staff` |
| `/staff version` | View version and server compatibility | `staffcore.staff` |
| `/vanish` | Toggle silent invisibility | `staffcore.vanish` |
| `/freeze <player> [reason]` | Freeze a player | `staffcore.freeze` |
| `/freeze claim <player>` | Claim freeze investigation | `staffcore.freeze` |
| `/staffchat <msg>` | Private staff chat | `staffcore.chat` |
| `/chatmute` | Toggle global server chat lock | `staffcore.chatmute` |
| `/helpop <msg>` | Priority help request | `staffcore.helpop` |
| `/report <player> <reason>` | Submit a player report | None |
| `/reports` | Manage reports tickets | `staffcore.reports` |
| `/notes <player> [add\|remove\|list]` | Internal staff notes | `staffcore.notes` |
| `/history <player>` | View punishment history | `staffcore.history` |
| `/warn, /mute, /sckick, /scban, /sctempban, /scunban` | Punishment suite | `staffcore.punish.*` |
| `/scbanip, /sctempbanip, /scunbanip` | IP Ban suite | `staffcore.punish.banip` |
| `/stafflogs [player\|clear]` | Audit staff action history | `staffcore.logs` |
| `/stafflang <en\|es\|fr\|pt>` | Change player language | `staffcore.lang` |
| `/xrayalerts` | Manage X-Ray alerts | `staffcore.xray` |

---

## 🚀 Installation

1. Download **`StaffCore-1.3.0.jar`** from [Modrinth](https://modrinth.com/plugin/staffcore-dafealru) or [SpigotMC](https://www.spigotmc.org/resources/staffcore-advanced-staff-moderation-suite.134819/).
2. Place the `.jar` in your server's `plugins/` directory.
3. Start or reload your server (**Paper 1.21+ / Java 21+** recommended).
4. Configure permissions in **LuckPerms** (`staffcore.admin` or `staffcore.staff`).

---

## 🤝 Contributing & Issues

- 🐛 **Report Bugs:** [GitHub Issues](https://github.com/dr8553097-sudo/StaffCore/issues)
- 💡 **Feature Requests:** [Submit Idea](https://github.com/dr8553097-sudo/StaffCore/issues/new/choose)
- 📜 **Contribution Guide:** [CONTRIBUTING.md](CONTRIBUTING.md)

---

<div align="center">

Developed with ❤️ by **Dafealru** for the Minecraft community.

</div>
