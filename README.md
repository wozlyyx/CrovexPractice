# ⚔️ CrovexPractice

<p align="center">
  <img src="https://img.shields.io/badge/Author-wozly__v2-blue?style=for-the-badge" alt="Author">
  <img src="https://img.shields.io/badge/Minecraft-1.20.4+-brightgreen?style=for-the-badge&logo=minecraft" alt="Minecraft Version">
  <img src="https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/License-MIT-purple?style=for-the-badge" alt="License">
</p>

<p align="center">
  <b>A modern, high-performance, and feature-rich Minecraft Practice Core built for Paper 1.20.4+.</b><br>
  Engineered by <b>wozly_v2</b> for competitive PvP networks.
</p>

---

## 🌟 Key Features

### ⚔️ Matchmaking & Queue System
- **Ranked & Unranked Queues:** Full matchmaking support with custom ELO rating calculations.
- **Dynamic ELO Expansion:** Automatically expands matchmaking range over time to optimize queue wait times.
- **Boxing & Custom Modes:** Real-time hit counters, combo tracking, and customizable win conditions.
- **Match Post-Game Snapshots:** View end-game inventories, remaining health, potions, and hit statistics.

### 🛡️ Arena & FFA Management
- **In-Game Arena Setup:** Intuitive setup workflow with visual boundary selection and multi-spawn configuration.
- **Permanent FFA Arenas:** Dedicated Free-For-All zones with instant respawn options, killstreak milestones, and global announcements.
- **Arena Bounds Protection:** Automatically detects and manages players exiting arena borders during combat.

### 🎒 Kit System & In-Game Editor
- **Custom Kits:** Create, modify, and delete PvP kits in real time.
- **Kit Customizer / Editor:** Interactive GUI enabling players to save custom hotbar and inventory layouts.
- **Default & Ranked Kits:** Assign specific kits to queues, duels, or FFA arenas.

### 👥 Party System
- **Party Duels:** 
  - **Party FFA:** Free-for-all brawl among party members.
  - **Party Split:** Evenly split team battles (Team Red vs Team Blue).
- **Party Management:** Party chat channel, invites, leader controls, and duel challenges.

### ⚡ Performance & Visibility Optimization
- **HikariCP Connection Pool:** High-throughput database architecture supporting **SQLite**, **MySQL**, **MariaDB**, and **PostgreSQL**.
- **Packet-Level Entity Visibility:** ProtocolLib integration for packet-level player hiding in lobbies to eliminate client render lag and save bandwidth.
- **Clean Architecture:** Non-blocking async queries and cache-backed player data.

### 🌐 Multi-Language & Integrations
- **Multi-Language Engine:** Built-in localization files for English (`en`), Turkish (`tr`), Spanish (`es`), and French (`fr`).
- **Discord Webhooks:** Automatic notifications for match conclusions, FFA winstreaks, and administrative actions.
- **PlaceholderAPI Hook:** Rich set of placeholders for scoreboards, tablists, and chat formatting.

---

## 📋 Commands & Permissions

| Command | Aliases | Description | Permission |
| :--- | :--- | :--- | :--- |
| `/cpractice` | `/practice`, `/crovexpractice` | Main administration command and setup GUI | `crovexpractice.admin` |
| `/queue` | `/q` | Open queue selection menu | None (Default) |
| `/party` | `/p` | Party creation and management | None (Default) |
| `/duel <player>` | `/d`, `/challenge` | Challenge a player to a 1v1 duel | None (Default) |
| `/ffa` | `/joinffa` | Join the active FFA arena | None (Default) |
| `/leaveffa` | `/quitffa` | Safely leave the FFA arena | None (Default) |
| `/spectate <player>`| `/spec` | Spectate an active match | None (Default) |
| `/stats [player]` | `/stat` | View personal or target player statistics | None (Default) |
| `/leaderboard` | `/top`, `/lb` | View global rankings (ELO, Kills, Streaks) | None (Default) |

---

## 🛠️ Installation & Building

### Requirements
- **Java 21** or higher
- **Paper / Purpur 1.20.4+**
- *(Optional)* **ProtocolLib** & **PlaceholderAPI**

### Compiling from Source
```bash
# Clone the repository
git clone https://github.com/wozly_v2/CrovexPractice.git

# Enter the project directory
cd CrovexPractice

# Build with Gradle
./gradlew build
```
The compiled `.jar` file will be located in `build/libs/`.

---

## ⚙️ Configuration

CrovexPractice is fully customizable. Modify `config.yml` to adjust:
- Database settings (SQLite / MySQL / MariaDB / PostgreSQL)
- ELO calculations and queue expansion timings
- Lobby hotbar item slots
- Discord Webhook URLs and events
- Match countdown and transition delays

---

## 👤 Author & Credits

Developed with passion by **wozly_v2**.

- **GitHub:** [@wozly_v2](https://github.com/wozly_v2)
- **Project:** CrovexPractice

---

## 📜 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.
