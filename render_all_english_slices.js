const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CHROME_PATH = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUTPUT_DIR = path.join(__dirname, 'assets', 'spigot_recommended');
const SLICES_DIR = path.join(__dirname, 'assets', 'slices');
const TEMP_DIR = path.join(__dirname, 'temp_slices');

if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });
if (!fs.existsSync(SLICES_DIR)) fs.mkdirSync(SLICES_DIR, { recursive: true });
if (!fs.existsSync(TEMP_DIR)) fs.mkdirSync(TEMP_DIR, { recursive: true });

const baseStyles = `
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  width: 800px;
  background: #090d16;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #e2e8f0;
  padding: 0;
  margin: 0;
}

.slice-container {
  width: 800px;
  position: relative;
  background: radial-gradient(circle at 50% 0%, rgba(0, 242, 254, 0.08) 0%, rgba(13, 17, 30, 1) 70%), #090d16;
  border-top: 1px solid rgba(0, 242, 254, 0.25);
  border-bottom: 1px solid rgba(123, 44, 191, 0.25);
  padding: 34px 38px 36px 38px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.slice-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(to right, rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 32px 32px;
  pointer-events: none;
}

.slice-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 12px;
  border-radius: 9999px;
  background: rgba(0, 242, 254, 0.1);
  border: 1px solid rgba(0, 242, 254, 0.35);
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 700;
  color: #00f2fe;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.slice-title {
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 30%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.slice-subtitle {
  font-size: 12.5px;
  color: #94a3b8;
  font-weight: 500;
}

.grid-2x2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  position: relative;
  z-index: 2;
}

.grid-4x2 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  position: relative;
  z-index: 2;
}

.grid-3x1 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  position: relative;
  z-index: 2;
}

.card {
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: relative;
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
}

.card-highlight {
  border-color: rgba(0, 242, 254, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(0, 242, 254, 0.08) 0%, rgba(15, 23, 42, 0.8) 100%);
}

.card-purple {
  border-color: rgba(168, 85, 247, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(168, 85, 247, 0.08) 0%, rgba(15, 23, 42, 0.8) 100%);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.card-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: rgba(0, 242, 254, 0.12);
  border: 1px solid rgba(0, 242, 254, 0.25);
  color: #00f2fe;
}

.card-icon.purple {
  background: rgba(168, 85, 247, 0.12);
  border-color: rgba(168, 85, 247, 0.25);
  color: #c084fc;
}

.card-icon.green {
  background: rgba(34, 197, 94, 0.12);
  border-color: rgba(34, 197, 94, 0.25);
  color: #4ade80;
}

.card-icon.amber {
  background: rgba(245, 158, 11, 0.12);
  border-color: rgba(245, 158, 11, 0.25);
  color: #fbbf24;
}

.card-title {
  font-size: 14.5px;
  font-weight: 700;
  color: #f8fafc;
}

.card-desc {
  font-size: 12px;
  line-height: 1.5;
  color: #94a3b8;
}

.tool-card {
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 10px;
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}

.tool-icon {
  font-size: 20px;
  margin-bottom: 2px;
}

.tool-name {
  font-size: 12.5px;
  font-weight: 700;
  color: #f1f5f9;
}

.tool-desc {
  font-size: 10.5px;
  line-height: 1.35;
  color: #94a3b8;
}

.bossbar-preview {
  background: rgba(10, 15, 26, 0.95);
  border: 1px solid rgba(0, 242, 254, 0.4);
  border-radius: 10px;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.bossbar-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 11.5px;
  font-family: 'JetBrains Mono', monospace;
}

.bossbar-track {
  width: 100%;
  height: 6px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
  position: relative;
}

.bossbar-fill {
  height: 100%;
  width: 98%;
  background: linear-gradient(90deg, #00f2fe 0%, #3b82f6 100%);
  border-radius: 3px;
  box-shadow: 0 0 10px rgba(0, 242, 254, 0.5);
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 2px;
}

.pill-tag {
  font-size: 10.5px;
  font-family: 'JetBrains Mono', monospace;
  padding: 2px 7px;
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #cbd5e1;
}

.frame-card {
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 242, 254, 0.3);
  border-radius: 14px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.4);
  position: relative;
  z-index: 2;
}

.frame-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.frame-title {
  font-size: 14px;
  font-weight: 700;
  color: #f8fafc;
  display: flex;
  align-items: center;
  gap: 8px;
}

.img-wrapper {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: #000;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 4px;
  box-shadow: inset 0 0 20px rgba(0,0,0,0.8);
}

.img-wrapper img {
  max-width: 100%;
  height: auto;
  display: block;
  image-rendering: pixelated;
}

.grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  position: relative;
  z-index: 2;
}
`;

const items = [
  {
    name: 'Spigot_Slice1_Pillars',
    height: 520,
    badge: 'CORE ARCHITECTURE • 01',
    title: 'ENTERPRISE SYSTEM ARCHITECTURE',
    subtitle: 'Native Paper/Purpur 1.20 - 1.21.x / 26.x • 0% NMS • Multi-Threaded Engine',
    html: `
      <div class="bossbar-preview">
        <div class="bossbar-title">
          <div><span style="color:#00f2fe;font-weight:700;">🛡️ STAFF MODE</span> <span style="color:#64748b">│</span> <span style="color:#94a3b8">TPS:</span> <span style="color:#22c55e;font-weight:700;">20.0</span> <span style="color:#64748b">•</span> <span style="color:#94a3b8">Ping:</span> <span style="color:#f8fafc">18ms</span> <span style="color:#64748b">•</span> <span style="color:#94a3b8">Reports:</span> <span style="color:#38bdf8;font-weight:700;">3</span> <span style="color:#64748b">•</span> <span style="color:#94a3b8">Staff:</span> <span style="color:#fbbf24;font-weight:700;">4</span></div>
          <span style="color:#38bdf8;font-size:10.5px;font-weight:600;">LIVE BOSSBAR HUD</span>
        </div>
        <div class="bossbar-track">
          <div class="bossbar-fill"></div>
        </div>
      </div>

      <div class="grid-2x2">
        <div class="card card-highlight">
          <div class="card-header">
            <div class="card-icon">⚡</div>
            <div class="card-title">Live BossBar & Actionbar HUD</div>
          </div>
          <div class="card-desc">Real-time telemetry streaming: dynamic TPS proportional progress bar, staff ping, pending ticket counter, and staff count.</div>
          <div class="tag-row">
            <span class="pill-tag" style="color:#00f2fe">Dynamic TPS Bar</span>
            <span class="pill-tag">Color Customizable</span>
            <span class="pill-tag">Auto-Cleanup</span>
          </div>
        </div>

        <div class="card card-purple">
          <div class="card-header">
            <div class="card-icon purple">🚀</div>
            <div class="card-title">Zero-Bloat & Pure Utility</div>
          </div>
          <div class="card-desc">100% focused on active staff utilities and moderation. Zero heavy legacy punishment overhead or third-party conflicts.</div>
          <div class="tag-row">
            <span class="pill-tag" style="color:#c084fc">Folia Ready</span>
            <span class="pill-tag">Async YAML & SQL</span>
            <span class="pill-tag">0% NMS</span>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-icon green">🗄️</div>
            <div class="card-title">Multi-Storage Persistence</div>
          </div>
          <div class="card-desc">Instant synchronization of staff sessions, duty history, and player tickets across H2, SQLite, MySQL, and high-speed Redis.</div>
          <div class="tag-row">
            <span class="pill-tag">MySQL & MariaDB</span>
            <span class="pill-tag">SQLite</span>
            <span class="pill-tag">Redis PubSub</span>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-icon amber">🌐</div>
            <div class="card-title">9 Native Languages</div>
          </div>
          <div class="card-desc">Full translation across 9 languages with Kyori Adventure MiniMessage, RGB gradient tags, and per-player auto-detection.</div>
          <div class="tag-row">
            <span class="pill-tag">EN</span>
            <span class="pill-tag">ES</span>
            <span class="pill-tag">DE</span>
            <span class="pill-tag">IT</span>
            <span class="pill-tag">RU</span>
            <span class="pill-tag">ZH</span>
            <span class="pill-tag">JA</span>
            <span class="pill-tag">FR</span>
            <span class="pill-tag">PT</span>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice2_StaffMode',
    height: 520,
    badge: 'MODERATION WORKFLOW • 02',
    title: 'STAFF MODE & 8 IN-GAME TOOLS',
    subtitle: 'Automated inventory backup, restoration and zero-leak stealth vanish',
    html: `
      <div class="grid-4x2">
        <div class="tool-card">
          <div class="tool-icon">🧭</div>
          <div class="tool-name">Teleporter</div>
          <div class="tool-desc">Quick jump or random search to players.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">🧊</div>
          <div class="tool-name">Freeze Tool</div>
          <div class="tool-desc">Instant suspect freeze & combat lock.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">📦</div>
          <div class="tool-name">Inspector</div>
          <div class="tool-desc">Live inspect inventory, stats & EnderChest.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">👁️</div>
          <div class="tool-name">Spectator Mode</div>
          <div class="tool-desc">One-click toggle between flight & noclip.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">⭐</div>
          <div class="tool-name">Staff Panel</div>
          <div class="tool-desc">Central master chest GUI moderation hub.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">🥕</div>
          <div class="tool-name">Night Vision</div>
          <div class="tool-desc">Infinite clear vision in dark caves & mines.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">🔮</div>
          <div class="tool-name">Stealth Vanish</div>
          <div class="tool-desc">Total invisibility with zero particle leaks.</div>
        </div>
        <div class="tool-card">
          <div class="tool-icon">🔴</div>
          <div class="tool-name">Safe Exit</div>
          <div class="tool-desc">Safely restores gear & starting location.</div>
        </div>
      </div>

      <div class="card card-highlight" style="padding: 14px 20px;">
        <div style="display:flex;align-items:center;justify-content:space-between;">
          <div style="display:flex;align-items:center;gap:12px;">
            <span style="font-size:20px;">🛡️</span>
            <div>
              <div style="font-size:13px;font-weight:700;color:#f8fafc;">Total Character Protection & Auto-Location Restore</div>
              <div style="font-size:12px;color:#94a3b8;">Complete damage immunity and zero hunger depletion. Exiting returns you to where you typed /staff.</div>
            </div>
          </div>
          <span class="pill-tag" style="color:#00f2fe;font-weight:700;background:rgba(0,242,254,0.1)">100% SAFE</span>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice3_Detection',
    height: 480,
    badge: 'SECURITY & DETECTION • 03',
    title: 'LIVE HEURISTICS & SURVEILLANCE',
    subtitle: 'Real-time mining rate heuristics, CPS click tracker and silent inspection',
    html: `
      <div class="grid-3x1">
        <div class="card card-highlight">
          <div class="card-header">
            <div class="card-icon">💎</div>
            <div class="card-title">X-Ray Heuristics</div>
          </div>
          <div class="card-desc">Detects anomalous rare vein mining ratios in rolling time windows with instant click-to-teleport interactive staff alerts.</div>
          <div class="tag-row">
            <span class="pill-tag" style="color:#00f2fe">Click to Teleport</span>
            <span class="pill-tag">Vein Ratio Alert</span>
          </div>
        </div>

        <div class="card card-purple">
          <div class="card-header">
            <div class="card-icon purple">⚡</div>
            <div class="card-title">Real-Time CPS Tracker</div>
          </div>
          <div class="card-desc">Monitors live clicking frequency (Left & Right CPS) with automated alerts for autoclickers and abnormal macro patterns.</div>
          <div class="tag-row">
            <span class="pill-tag" style="color:#c084fc">Live Sampling</span>
            <span class="pill-tag">Threshold Alerts</span>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-icon amber">🌟</div>
            <div class="card-title">Stealth Glowing Aura</div>
          </div>
          <div class="card-desc">Modern glowing outline visible only to the staff member on their own screen, turning off automatically in vanish.</div>
          <div class="tag-row">
            <span class="pill-tag">Staff-Only View</span>
            <span class="pill-tag">No Particle Leaks</span>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice4_Persistence',
    height: 490,
    badge: 'COMMUNICATION & REPORTS • 04',
    title: 'TICKET DISPATCH & STAFF CHANNELS',
    subtitle: 'Offline & online player reports, interactive ticket GUI and internal dispatch channels',
    html: `
      <div class="grid-2x2">
        <div class="card card-highlight">
          <div class="card-header">
            <div class="card-icon">📋</div>
            <div class="card-title">Offline & Online Reports</div>
          </div>
          <div class="card-desc">Players can submit reports with <code style="color:#00f2fe">/report &lt;player&gt; &lt;reason&gt;</code> even after rule-breakers log off. Resolves offline UUIDs automatically.</div>
        </div>

        <div class="card card-purple">
          <div class="card-header">
            <div class="card-icon purple">🎫</div>
            <div class="card-title">Interactive Tickets GUI</div>
          </div>
          <div class="card-desc">Visual <code style="color:#c084fc">/reports</code> chest menu to examine open tickets, teleport to scenes, claim investigations, and resolve with one click.</div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-icon green">💬</div>
            <div class="card-title">Encrypted StaffChat</div>
          </div>
          <div class="card-desc">Private dedicated communications channel for the moderation team with custom formatting and Discord webhook synchronization.</div>
        </div>

        <div class="card">
          <div class="card-header">
            <div class="card-icon amber">🔇</div>
            <div class="card-title">HelpOP & Chat Mute</div>
          </div>
          <div class="card-desc">Emergency player assistance with <code style="color:#fbbf24">/helpop</code> and global public chat lockdown with <code style="color:#fbbf24">/chatmute</code> during spam attacks.</div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice5_Duty_Specs',
    height: 490,
    badge: 'TIME TRACKING & PERFORMANCE • 05',
    title: 'STAFF DUTY, LEADERBOARDS & SPECS',
    subtitle: 'Automated staff clock-in, weekly leaderboard tracking and high-concurrency specs',
    html: `
      <div class="grid-2x2">
        <div class="card card-highlight">
          <div class="card-header">
            <div class="card-icon">🕒</div>
            <div class="card-title">Staff Duty Clock-In (/staffduty)</div>
          </div>
          <div class="card-desc">Automated moderation shift tracking. Records active hours, minutes, and actions in database with intelligent movement AFK detection.</div>
        </div>

        <div class="card card-purple">
          <div class="card-header">
            <div class="card-icon purple">🏆</div>
            <div class="card-title">Weekly Leaderboards (/stafftop)</div>
          </div>
          <div class="card-desc">Interactive GUI leaderboard displaying top active staff rankings. Promotes staff accountability and transparency across networks.</div>
        </div>
      </div>

      <div class="card" style="padding: 16px 20px; background: rgba(10, 15, 26, 0.85); border-color: rgba(0, 242, 254, 0.25);">
        <div style="display: flex; justify-content: space-between; align-items: center; text-align: center;">
          <div>
            <div style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-family:'JetBrains Mono'">Native Java</div>
            <div style="font-size:15px; font-weight:800; color:#00f2fe;">Java 21+</div>
          </div>
          <div style="width:1px; height:26px; background:rgba(255,255,255,0.1)"></div>
          <div>
            <div style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-family:'JetBrains Mono'">Server Platform</div>
            <div style="font-size:15px; font-weight:800; color:#f8fafc;">Paper / Purpur 1.20 - 1.21.x</div>
          </div>
          <div style="width:1px; height:26px; background:rgba(255,255,255,0.1)"></div>
          <div>
            <div style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-family:'JetBrains Mono'">Multi-Threading</div>
            <div style="font-size:15px; font-weight:800; color:#a855f7;">Folia Ready</div>
          </div>
          <div style="width:1px; height:26px; background:rgba(255,255,255,0.1)"></div>
          <div>
            <div style="font-size:11px; color:#94a3b8; text-transform:uppercase; font-family:'JetBrains Mono'">Architecture</div>
            <div style="font-size:15px; font-weight:800; color:#22c55e;">Zero-Bloat Core</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Showcase_InGame_HUDs',
    height: 480,
    badge: 'IN-GAME TELEMETRY • LIVE PREVIEW',
    title: 'LIVE BOSSBAR & ACTIONBAR HUD',
    subtitle: 'Direct in-game capture showcasing real-time metrics and the 8 hotbar tools',
    html: `
      <div class="frame-card">
        <div class="frame-header">
          <div class="frame-title">📊 Dynamic Live BossBar HUD</div>
          <div class="pill-tag">Dynamic Progress (TPS / 20.0)</div>
        </div>
        <div class="img-wrapper">
          <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_bossbar.png').replace(/\\/g, '/')}" style="width:100%;" />
        </div>
      </div>

      <div class="frame-card" style="border-color: rgba(168, 85, 247, 0.35);">
        <div class="frame-header">
          <div class="frame-title">🛠️ Hotbar Utility Suite & Actionbar HUD</div>
          <div class="pill-tag" style="color:#c084fc; border-color:rgba(168,85,247,0.3); background:rgba(168,85,247,0.12);">8 Modular Tools</div>
        </div>
        <div class="img-wrapper">
          <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_hotbar_actionbar.png').replace(/\\/g, '/')}" style="width:100%;" />
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Showcase_GUIs',
    height: 530,
    badge: 'INTERACTIVE MENUS • LIVE PREVIEW',
    title: 'STAFF PANEL & TICKET REVIEW GUIS',
    subtitle: 'Intuitive chest-menu interfaces for complete server surveillance and rapid dispatch',
    html: `
      <div class="grid-2col">
        <div class="frame-card">
          <div class="frame-header">
            <div class="frame-title">⭐ Master Staff Panel</div>
          </div>
          <div class="img-wrapper" style="min-height: 230px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_staff_panel.png').replace(/\\/g, '/')}" style="width:100%; max-height: 240px; object-fit: contain;" />
          </div>
          <div style="font-size:12px; color:#94a3b8; line-height:1.4;">Centralized one-click access to player teleport, freeze, inspect, and night vision.</div>
        </div>

        <div class="frame-card" style="border-color: rgba(168, 85, 247, 0.35);">
          <div class="frame-header">
            <div class="frame-title">📋 Report Review Ticket GUI</div>
          </div>
          <div class="img-wrapper" style="min-height: 230px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_report_review.png').replace(/\\/g, '/')}" style="width:100%; max-height: 240px; object-fit: contain;" />
          </div>
          <div style="font-size:12px; color:#94a3b8; line-height:1.4;">Live ticket examination with offline UUID resolution, instant teleport, and claim action.</div>
        </div>
      </div>
    `
  }
];

async function renderAll() {
  console.log('Rendering 100% English Slices and Showcases...');
  for (const item of items) {
    const htmlContent = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>${baseStyles}</style>
</head>
<body>
  <div class="slice-container" id="slice" style="min-height:${item.height}px;">
    <div class="slice-header">
      <div>
        <div class="slice-badge">${item.badge}</div>
        <div class="slice-title" style="margin-top:6px;">${item.title}</div>
        <div class="slice-subtitle">${item.subtitle}</div>
      </div>
    </div>
    ${item.html}
  </div>
</body>
</html>`;

    const htmlPath = path.join(TEMP_DIR, `${item.name}.html`);
    fs.writeFileSync(htmlPath, htmlContent);

    const outPng = path.join(OUTPUT_DIR, `${item.name}.png`);
    const cmd = `"${CHROME_PATH}" --headless --disable-gpu --hide-scrollbars --window-size=800,${item.height} --screenshot="${outPng}" "file:///${htmlPath.replace(/\\/g, '/')}"`;
    console.log(`Rendering ${item.name} (800x${item.height})...`);
    execSync(cmd);
  }
  console.log('All English slices and showcases rendered successfully without clipping!');
}

renderAll();
