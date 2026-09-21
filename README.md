<div align="center">

<img src="assets/v2/staffcore_header_transparent.png" alt="StaffCore Banner" width="800"/>

# 🛡️ StaffCore v2.0.0

### Next-Gen Enterprise Moderation & Staff Management Suite for Paper / Purpur

[![Modrinth](https://img.shields.io/badge/Modrinth-StaffCore-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/plugin/staffcore-dafealru)
[![SpigotMC](https://img.shields.io/badge/SpigotMC-Resource-orange?style=for-the-badge&logo=spigotmc&logoColor=white)](https://www.spigotmc.org/resources/staffcore-advanced-staff-moderation-suite.134819/)
[![Version](https://img.shields.io/badge/Version-2.0.0-brightgreen?style=for-the-badge)](https://github.com/dr8553097-sudo/StaffCore/releases)
[![Platform](https://img.shields.io/badge/Paper-1.21.x-blue?style=for-the-badge&logo=papermc&logoColor=white)](https://papermc.io)
[![License](https://img.shields.io/badge/License-GPL--3.0-purple?style=for-the-badge)](LICENSE)

---

### 🎥 Official Showcase Video
[![StaffCore Showcase Video](https://img.youtube.com/vi/0pvHOOajb1I/maxresdefault.jpg)](https://youtu.be/0pvHOOajb1I)
*(Click above to watch the full in-game showcase on YouTube)*

---

<img src="assets/v2/Spigot_Slice1_Pillars.png" alt="Core Pillars" width="800"/>

<br/>

<img src="assets/v2/Spigot_Showcase_InGame_HUDs.png" alt="Live In-Game HUDs" width="800"/>

<br/>

<img src="assets/v2/Spigot_Showcase_GUIs.png" alt="In-Game GUIs" width="800"/>

<br/>

<img src="assets/v2/Spigot_Slice2_StaffMode.png" alt="Staff Mode Tools" width="800"/>

<br/>

<img src="assets/v2/Spigot_Slice3_Detection.png" alt="Detection & Spectate" width="800"/>

<br/>

<img src="assets/v2/Spigot_Slice4_Persistence.png" alt="Persistence SQL Redis" width="800"/>

<br/>

<img src="assets/v2/Spigot_Slice5_Duty_Specs.png" alt="Staff Duty & Specs" width="800"/>

---

</div>

## 📖 Overview

**StaffCore v2.0.0** is an enterprise-grade moderation ecosystem engineered for modern Paper, Purpur, and Folia servers. Built for networks and server administrators that demand **absolute control, visual clarity, live HUD BossBar metrics, strict anti-abuse protections, and seamless staff workflow** without heavy bloated punishment dependencies.

---

## ✨ Features

- 🕵️ **Staff Mode:** Seamlessly switch into staff mode with automated inventory, armor, and location restoration.
- 📊 **Staff Live BossBar HUD:** Real-time TPS (dynamic progress bar), Staff Ping, Open Reports counter, and active online staff counter right on the BossBar.
- 🌟 **Glowing Silhouette Aura:** Outlines the staff character model with glowing aura (completely hidden in Vanish).
- 🛠️ **8 Hotbar Tools:**
  - 🧭 **Teleporter:** Jump directly or randomly to players.
  - 🧊 **Freeze Tool:** Instantly freeze/unfreeze suspects with anti-disconnect re-freeze.
  - 📦 **Inspector:** View inventory, armor, enderchest & stats in real-time.
  - 👁️ **Spectator Tool:** One-click toggle between flight and spectator noclip mode.
  - ⭐ **Staff Panel:** GUI moderation menu.
  - 🥕 **Night Vision:** Infinite clear vision toggle.
  - 🔮 **Vanish:** Complete silent invisibility with zero particle leaks.
  - 🔴 **Safe Exit:** Safely restore player mode and original location.
- 📋 **Report System:** Offline & Online player reports with `/report <player> <reason>` and GUI tickets with `/reports`.
- 🕒 **Staff Duty & Leaderboards:** Clock-in system (`/staffduty`) with movement AFK detection and weekly leaderboard (`/stafftop`).
- 💎 **Heuristic X-Ray Alerts:** Rolling-window ore-to-stone ratio analytics with instant click-to-teleport alerts.
- ⚡ **Real-Time CPS Tracker:** Left & Right click speed sampling and automated macro detection.
- 🌐 **9 Native Languages:** English, Spanish, German, Italian, Russian, Chinese, Japanese, French, Portuguese (`/stafflang`).

---

## 💻 Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/staff` | Toggle staff mode (saves & restores inventory & location) | `staffcore.staff` |
| `/staff menu` | Open master GUI panel | `staffcore.staff` |
| `/staff speed <0-10>` | Adjust walk speed | `staffcore.staff` |
| `/staff flyspeed <0-10>` | Adjust fly speed | `staffcore.staff` |
| `/vanish` | Toggle silent invisibility | `staffcore.vanish` |
| `/freeze <player> [reason]` | Freeze a suspect | `staffcore.freeze` |
| `/freeze claim <player>` | Claim freeze investigation | `staffcore.freeze` |
| `/report <player> <reason>` | Submit a report (online or offline player) | None |
| `/reports` | Manage reports tickets GUI | `staffcore.reports` |
| `/staffduty` | Toggle staff duty time tracking | `staffcore.staff` |
| `/stafftop` | View staff duty leaderboard | `staffcore.staff` |
| `/staffchat <msg>` | Private staff chat channel | `staffcore.chat` |
| `/chatmute` | Toggle global server chat lock | `staffcore.chatmute` |
| `/helpop <msg>` | Priority help request | `staffcore.helpop` |
| `/notes <player> [add\|remove\|list]` | Internal staff notes | `staffcore.notes` |
| `/stafflogs [player\|clear]` | Audit staff action history | `staffcore.logs` |
| `/stafflang <code>` | Change player language (9 languages) | `staffcore.lang` |
| `/xrayalerts` | Manage X-Ray alerts | `staffcore.xray` |
| `/staff reload` | Reload configurations and languages | `staffcore.admin` |

---

## 🚀 Installation

1. Download **`StaffCore-2.0.0.jar`** from [SpigotMC](https://www.spigotmc.org/resources/staffcore-advanced-staff-moderation-suite.134819/) or [Modrinth](https://modrinth.com/plugin/staffcore-dafealru).
2. Place the `.jar` in your server's `plugins/` directory.
3. Start or reload your server (**Paper / Purpur 1.20 - 1.21+ / Java 21+** recommended).
4. Configure permissions in **LuckPerms** (`staffcore.admin` or `staffcore.staff`).

---

<div align="center">

Developed with ❤️ by **Dafealru** for the Minecraft community.

</div>
