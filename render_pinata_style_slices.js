const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CHROME_PATH = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUTPUT_DIR = path.join(__dirname, 'assets', 'v2');
const TEMP_DIR = path.join(__dirname, 'temp_slices');

if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });
if (!fs.existsSync(TEMP_DIR)) fs.mkdirSync(TEMP_DIR, { recursive: true });

const baseStyles = `
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  width: 850px;
  height: 600px;
  background: #0b0e14;
  font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  color: #e2e8f0;
  overflow: hidden;
}

.slice-canvas {
  width: 850px;
  height: 600px;
  position: relative;
  background: radial-gradient(circle at 50% 0%, rgba(0, 242, 254, 0.09) 0%, rgba(11, 14, 20, 1) 75%), #0b0e14;
  padding: 30px 36px 26px 36px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border-top: 1px solid rgba(0, 242, 254, 0.25);
  border-bottom: 1px solid rgba(123, 44, 191, 0.25);
}

.slice-canvas::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: 
    linear-gradient(to right, rgba(255,255,255,0.025) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(255,255,255,0.025) 1px, transparent 1px);
  background-size: 24px 24px;
  pointer-events: none;
}

.header-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
  position: relative;
  z-index: 2;
}

.pill-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border-radius: 9999px;
  background: rgba(0, 242, 254, 0.1);
  border: 1px solid rgba(0, 242, 254, 0.35);
  box-shadow: 0 0 16px rgba(0, 242, 254, 0.15);
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  font-weight: 700;
  color: #00f2fe;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.pill-badge.purple {
  background: rgba(168, 85, 247, 0.1);
  border-color: rgba(168, 85, 247, 0.35);
  box-shadow: 0 0 16px rgba(168, 85, 247, 0.15);
  color: #c084fc;
}

.slice-title {
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 40%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.slice-subtitle {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}

.cards-grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  position: relative;
  z-index: 2;
}

.cards-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  position: relative;
  z-index: 2;
}

.card-pillar {
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
  border-radius: 14px;
  padding: 16px 16px 18px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  box-shadow: 0 10px 28px rgba(0,0,0,0.35);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.card-pillar.cyan {
  border-color: rgba(0, 242, 254, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(0, 242, 254, 0.08) 0%, rgba(15, 23, 42, 0.85) 100%);
}

.card-pillar.blue {
  border-color: rgba(56, 189, 248, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(56, 189, 248, 0.08) 0%, rgba(15, 23, 42, 0.85) 100%);
}

.card-pillar.purple {
  border-color: rgba(168, 85, 247, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(168, 85, 247, 0.08) 0%, rgba(15, 23, 42, 0.85) 100%);
}

.card-pillar.amber {
  border-color: rgba(245, 158, 11, 0.35);
  background: radial-gradient(circle at 100% 0%, rgba(245, 158, 11, 0.08) 0%, rgba(15, 23, 42, 0.85) 100%);
}

.card-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag-badge {
  font-size: 10px;
  font-family: 'JetBrains Mono', monospace;
  font-weight: 700;
  padding: 3px 8px;
  border-radius: 6px;
  text-transform: uppercase;
}

.tag-cyan { background: rgba(0, 242, 254, 0.15); color: #00f2fe; border: 1px solid rgba(0, 242, 254, 0.3); }
.tag-blue { background: rgba(56, 189, 248, 0.15); color: #38bdf8; border: 1px solid rgba(56, 189, 248, 0.3); }
.tag-purple { background: rgba(168, 85, 247, 0.15); color: #c084fc; border: 1px solid rgba(168, 85, 247, 0.3); }
.tag-amber { background: rgba(245, 158, 11, 0.15); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3); }
.tag-gray { background: rgba(255, 255, 255, 0.06); color: #94a3b8; border: 1px solid rgba(255, 255, 255, 0.1); }

.card-title {
  font-size: 15.5px;
  font-weight: 800;
  color: #ffffff;
}

.graphic-box {
  height: 90px;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 10px;
  border: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px 0 4px 0;
  overflow: hidden;
  position: relative;
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 11.5px;
  color: #cbd5e1;
  line-height: 1.35;
}

.check-icon {
  font-weight: 800;
  font-size: 12px;
}

.check-icon.cyan { color: #00f2fe; }
.check-icon.blue { color: #38bdf8; }
.check-icon.purple { color: #c084fc; }
.check-icon.amber { color: #fbbf24; }

.bottom-wide-card {
  background: rgba(10, 15, 26, 0.85);
  backdrop-filter: blur(12px);
  border: 1px solid rgba(0, 242, 254, 0.25);
  border-radius: 12px;
  padding: 12px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
}

.spec-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.spec-icon-box {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(0, 242, 254, 0.12);
  border: 1px solid rgba(0, 242, 254, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  color: #00f2fe;
}

.spec-text-title {
  font-size: 12.5px;
  font-weight: 800;
  color: #00f2fe;
}

.spec-text-desc {
  font-size: 11px;
  color: #94a3b8;
}

.spec-sep {
  width: 1px;
  height: 28px;
  background: rgba(255, 255, 255, 0.1);
}

.img-frame {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.15);
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}
`;

const slices = [
  {
    name: 'Spigot_Slice1_Pillars',
    badge: '🛡️ CORE ARCHITECTURE',
    title: 'ENTERPRISE ARCHITECTURE & PERFORMANCE',
    subtitle: 'Native Paper / Purpur 1.20 - 1.21.x / 26.x • 0% NMS • Multi-Threaded Folia Ready',
    html: `
      <div class="cards-grid-3">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">TELEMETRY</span>
            <span class="tag-badge tag-gray">REAL-TIME</span>
          </div>
          <div class="card-title" style="color:#00f2fe;">Live BossBar HUD</div>
          <div class="graphic-box" style="padding: 10px;">
            <div style="width:100%; display:flex; flex-direction:column; gap:6px;">
              <div style="font-size:10px; font-family:'JetBrains Mono'; display:flex; justify-content:space-between;">
                <span style="color:#00f2fe;font-weight:700;">STAFF</span>
                <span style="color:#22c55e;">TPS: 20.00</span>
                <span style="color:#f8fafc;">81ms</span>
              </div>
              <div style="width:100%; height:6px; background:rgba(255,255,255,0.1); border-radius:3px; overflow:hidden;">
                <div style="width:100%; height:100%; background:linear-gradient(90deg,#00f2fe,#3b82f6); box-shadow:0 0 8px #00f2fe;"></div>
              </div>
            </div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Dynamic TPS progress bar tracking</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Staff ping, pending reports & count</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>13+ customizable placeholders</span></div>
          </div>
        </div>

        <div class="card-pillar blue">
          <div class="card-top-row">
            <span class="tag-badge tag-blue">ENGINE</span>
            <span class="tag-badge tag-gray">&lt;300MS BOOT</span>
          </div>
          <div class="card-title" style="color:#38bdf8;">Zero-Bloat Core</div>
          <div class="graphic-box">
            <div style="font-size:28px;">⚡</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Zero heavy punishment overhead</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>0% NMS safe cross-version design</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Multi-threaded Folia ready</span></div>
          </div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">DATABASE</span>
            <span class="tag-badge tag-gray">ASYNC POOL</span>
          </div>
          <div class="card-title" style="color:#c084fc;">Multi-Storage Sync</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🗄️</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon purple">✔</span> <span>SQLite, H2, MySQL & MariaDB</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>High-throughput Redis Pub/Sub</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Non-blocking async persistence</span></div>
          </div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">⚡</div>
          <div>
            <div class="spec-text-title">0% NMS ENGINE</div>
            <div class="spec-text-desc">Safe for all future Paper updates.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">🚀</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">HIGH TPS EFFICIENCY</div>
            <div class="spec-text-desc">Loads fully in under 300ms.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">🌐</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">FOLIA & BEDROCK</div>
            <div class="spec-text-desc">Cross-platform network ready.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice2_StaffMode',
    badge: '🛠️ MODERATION WORKFLOW',
    title: 'STAFF MODE & 8 IN-GAME TOOLS',
    subtitle: 'Automated inventory backup, restoration and zero-leak stealth vanish',
    html: `
      <div class="cards-grid-3">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">STEALTH</span>
            <span class="tag-badge tag-gray">0 LEAKS</span>
          </div>
          <div class="card-title" style="color:#00f2fe;">Silent Vanish & Vision</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🔮</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Zero particle leaks in vanish</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Staff-only glowing outline aura</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Infinite night vision tool</span></div>
          </div>
        </div>

        <div class="card-pillar blue">
          <div class="card-top-row">
            <span class="tag-badge tag-blue">SURVEILLANCE</span>
            <span class="tag-badge tag-gray">8 TOOLS</span>
          </div>
          <div class="card-title" style="color:#38bdf8;">Investigation Suite</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🧭</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Instant player teleporter tool</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Freeze tool with anti-bypass</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Live inventory & EnderChest inspect</span></div>
          </div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">PROTECTION</span>
            <span class="tag-badge tag-gray">AUTO-RESTORE</span>
          </div>
          <div class="card-title" style="color:#c084fc;">Control & Safe Exit</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🔴</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon purple">✔</span> <span>One-click spectator noclip mode</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Master chest GUI staff panel</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Safe return to starting location</span></div>
          </div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">🛡️</div>
          <div>
            <div class="spec-text-title">100% DAMAGE IMMUNITY</div>
            <div class="spec-text-desc">Invulnerable while in staff mode.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">📍</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">AUTO-LOCATION RETURN</div>
            <div class="spec-text-desc">Teleports back to where /staff started.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">📦</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">INVENTORY BACKUP</div>
            <div class="spec-text-desc">Safe armor and hotbar preservation.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice3_Detection',
    badge: '💎 SECURITY & HEURISTICS',
    title: 'X-RAY HEURISTICS & LIVE CPS TRACKER',
    subtitle: 'Continuous sampling, anomalous mining ratio analytics, and instant staff dispatch',
    html: `
      <div class="cards-grid-3">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">HEURISTICS</span>
            <span class="tag-badge tag-gray">ANALYTICS</span>
          </div>
          <div class="card-title" style="color:#00f2fe;">X-Ray Detection</div>
          <div class="graphic-box">
            <div style="font-size:28px;">💎</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Rolling window ore-to-stone ratio</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Click-to-teleport interactive alerts</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Diamond & ancient debris filters</span></div>
          </div>
        </div>

        <div class="card-pillar blue">
          <div class="card-top-row">
            <span class="tag-badge tag-blue">PVP SECURITY</span>
            <span class="tag-badge tag-gray">REAL-TIME</span>
          </div>
          <div class="card-title" style="color:#38bdf8;">Live CPS Tracker</div>
          <div class="graphic-box">
            <div style="font-size:28px;">⚡</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Left & Right click sampling</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Autoclicker & macro alerts</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Configurable threshold cooldowns</span></div>
          </div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">DISPATCH</span>
            <span class="tag-badge tag-gray">ENCRYPTED</span>
          </div>
          <div class="card-title" style="color:#c084fc;">Communications</div>
          <div class="graphic-box">
            <div style="font-size:28px;">💬</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Encrypted private StaffChat</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Emergency priority /helpop</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Global /chatmute public lock</span></div>
          </div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">⚡</div>
          <div>
            <div class="spec-text-title">CLICK-TO-TELEPORT</div>
            <div class="spec-text-desc">Jump to suspects directly from chat.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">🤖</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">DISCORD WEBHOOKS</div>
            <div class="spec-text-desc">Broadcast embeds for X-Ray alerts.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">🔒</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">FREEZE ANTI-BYPASS</div>
            <div class="spec-text-desc">Re-freezes players upon reconnect.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice4_Persistence',
    badge: '📋 TICKET DISPATCH',
    title: 'OFFLINE REPORTS & TICKET SYSTEM',
    subtitle: 'Offline & online player reports, interactive ticket GUI and internal dispatch channels',
    html: `
      <div class="cards-grid-3">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">REPORTING</span>
            <span class="tag-badge tag-gray">OFFLINE</span>
          </div>
          <div class="card-title" style="color:#00f2fe;">Offline Player Reports</div>
          <div class="graphic-box">
            <div style="font-size:28px;">📋</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Report players who disconnected</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Automated offline UUID resolver</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Anti-spam player cooldowns</span></div>
          </div>
        </div>

        <div class="card-pillar blue">
          <div class="card-top-row">
            <span class="tag-badge tag-blue">DISPATCH</span>
            <span class="tag-badge tag-gray">CHEST GUI</span>
          </div>
          <div class="card-title" style="color:#38bdf8;">Ticket Management GUI</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🎫</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Examine active tickets with /reports</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Teleport to scene of incident</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Claim case & resolve with 1 click</span></div>
          </div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">AUDITING</span>
            <span class="tag-badge tag-gray">STAFF LOGS</span>
          </div>
          <div class="card-title" style="color:#c084fc;">Notes & Action Logs</div>
          <div class="graphic-box">
            <div style="font-size:28px;">📜</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Internal staff player notes (/notes)</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Action history audit trail</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Persistent across server reboots</span></div>
          </div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">👥</div>
          <div>
            <div class="spec-text-title">OFFLINE UUID RESOLVER</div>
            <div class="spec-text-desc">Rule-breakers cannot evade reports.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">🎫</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">ONE-CLICK GUI ACTIONS</div>
            <div class="spec-text-desc">Accept, teleport, and close tickets.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">🤖</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">DISCORD EMBED SYNC</div>
            <div class="spec-text-desc">Instant webhook ticket dispatch.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Slice5_Duty_Specs',
    badge: '🕒 SHIFT TRACKING & SPECS',
    title: 'STAFF DUTY & GLOBAL LOCALIZATION',
    subtitle: 'Automated shift clock-in, weekly leaderboard tracking, and 9 native languages',
    html: `
      <div class="cards-grid-3">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">CLOCK-IN</span>
            <span class="tag-badge tag-gray">DATABASE</span>
          </div>
          <div class="card-title" style="color:#00f2fe;">Staff Duty (/staffduty)</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🕒</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Automated shift time logging</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Intelligent movement AFK pause</span></div>
            <div class="check-item"><span class="check-icon cyan">✔</span> <span>Weekly & monthly hour tracking</span></div>
          </div>
        </div>

        <div class="card-pillar blue">
          <div class="card-top-row">
            <span class="tag-badge tag-blue">RANKINGS</span>
            <span class="tag-badge tag-gray">GUI MENU</span>
          </div>
          <div class="card-title" style="color:#38bdf8;">Top Weekly Leaderboard</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🏆</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Interactive /stafftop rankings GUI</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Promotes staff transparency</span></div>
            <div class="check-item"><span class="check-icon blue">✔</span> <span>Auto-resets weekly statistics</span></div>
          </div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">LANGUAGES</span>
            <span class="tag-badge tag-gray">9 PACKS</span>
          </div>
          <div class="card-title" style="color:#c084fc;">9 Native Languages</div>
          <div class="graphic-box">
            <div style="font-size:28px;">🌐</div>
          </div>
          <div class="checklist">
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Instant /stafflang hot-swap</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>EN, ES, DE, IT, RU, ZH, JA, FR, PT</span></div>
            <div class="check-item"><span class="check-icon purple">✔</span> <span>Kyori MiniMessage RGB & gradients</span></div>
          </div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">☕</div>
          <div>
            <div class="spec-text-title">NATIVE JAVA 21+</div>
            <div class="spec-text-desc">Engineered for modern performance.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">🎮</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">PAPER / PURPUR 1.20 - 1.21.x</div>
            <div class="spec-text-desc">Tested across modern releases.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">🌐</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">9 NATIVE LANGUAGES</div>
            <div class="spec-text-desc">Global network localization ready.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Showcase_InGame_HUDs',
    badge: '📊 IN-GAME TELEMETRY',
    title: 'REAL-TIME BOSSBAR & ACTIONBAR HUD',
    subtitle: 'Direct in-game capture showcasing live metrics and the 8 hotbar tools',
    html: `
      <div style="display:flex; flex-direction:column; gap:12px;">
        <div class="card-pillar cyan" style="padding: 12px 16px;">
          <div class="card-top-row" style="margin-bottom: 8px;">
            <span class="card-title" style="font-size:14px; color:#00f2fe;">📊 Live Dynamic BossBar HUD</span>
            <span class="tag-badge tag-cyan">DYNAMIC PROGRESS (TPS / 20.0)</span>
          </div>
          <div class="img-frame" style="height: 52px; padding: 2px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_bossbar.png').replace(/\\/g, '/')}" style="width:100%; height:100%; object-fit: cover;" />
          </div>
        </div>

        <div class="card-pillar purple" style="padding: 12px 16px;">
          <div class="card-top-row" style="margin-bottom: 8px;">
            <span class="card-title" style="font-size:14px; color:#c084fc;">🛠️ Hotbar Utility Suite & Actionbar HUD</span>
            <span class="tag-badge tag-purple">8 MODULAR TOOLS</span>
          </div>
          <div class="img-frame" style="height: 180px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_hotbar_actionbar.png').replace(/\\/g, '/')}" style="width:100%; height:100%; object-fit: contain;" />
          </div>
        </div>
      </div>

      <div class="bottom-wide-card" style="margin-top: 4px;">
        <div class="spec-item">
          <div class="spec-icon-box">📊</div>
          <div>
            <div class="spec-text-title">13+ PLACEHOLDERS</div>
            <div class="spec-text-desc">TPS, Ping, Reports, Duty, Staff.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">🎨</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">RGB GRADIENTS</div>
            <div class="spec-text-desc">Custom colors & styles in ui.yml.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">🧹</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">AUTO-CLEANUP</div>
            <div class="spec-text-desc">Zero ghost BossBars upon quit.</div>
          </div>
        </div>
      </div>
    `
  },
  {
    name: 'Spigot_Showcase_GUIs',
    badge: '📋 INTERACTIVE MENUS',
    title: 'STAFF PANEL & TICKET REVIEW GUIS',
    subtitle: 'Intuitive chest-menu interfaces for complete server surveillance and rapid dispatch',
    html: `
      <div class="cards-grid-2">
        <div class="card-pillar cyan">
          <div class="card-top-row">
            <span class="tag-badge tag-cyan">MASTER MENU</span>
            <span class="tag-badge tag-gray">/STAFF MENU</span>
          </div>
          <div class="card-title" style="color:#00f2fe; font-size:14px;">⭐ Master Staff Panel</div>
          <div class="img-frame" style="height: 200px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_staff_panel.png').replace(/\\/g, '/')}" style="width:100%; height:100%; object-fit: contain;" />
          </div>
          <div style="font-size:11.5px; color:#94a3b8; line-height:1.35;">Centralized one-click access to player teleport, freeze, inspect, and night vision.</div>
        </div>

        <div class="card-pillar purple">
          <div class="card-top-row">
            <span class="tag-badge tag-purple">TICKETS</span>
            <span class="tag-badge tag-gray">/REPORTS</span>
          </div>
          <div class="card-title" style="color:#c084fc; font-size:14px;">📋 Report Review Ticket GUI</div>
          <div class="img-frame" style="height: 200px;">
            <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_report_review.png').replace(/\\/g, '/')}" style="width:100%; height:100%; object-fit: contain;" />
          </div>
          <div style="font-size:11.5px; color:#94a3b8; line-height:1.35;">Live ticket examination with offline UUID resolution, instant teleport, and claim action.</div>
        </div>
      </div>

      <div class="bottom-wide-card">
        <div class="spec-item">
          <div class="spec-icon-box">👥</div>
          <div>
            <div class="spec-text-title">OFFLINE LOOKUP</div>
            <div class="spec-text-desc">Review disconnected player tickets.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#38bdf8; background:rgba(56,189,248,0.12); border-color:rgba(56,189,248,0.3);">📍</div>
          <div>
            <div class="spec-text-title" style="color:#38bdf8;">CLICK-TO-TELEPORT</div>
            <div class="spec-text-desc">Jump straight to the report scene.</div>
          </div>
        </div>
        <div class="spec-sep"></div>
        <div class="spec-item">
          <div class="spec-icon-box" style="color:#c084fc; background:rgba(168,85,247,0.12); border-color:rgba(168,85,247,0.3);">✔</div>
          <div>
            <div class="spec-text-title" style="color:#c084fc;">ONE-CLICK RESOLVE</div>
            <div class="spec-text-desc">Claim, investigate, and close cases.</div>
          </div>
        </div>
      </div>
    `
  }
];

async function renderPinataStyle() {
  console.log('Rendering 850x600 PinataSpectra style slices for StaffCore...');
  for (const s of slices) {
    const htmlContent = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>${baseStyles}</style>
</head>
<body>
  <div class="slice-canvas">
    <div class="header-center">
      <div class="pill-badge">${s.badge}</div>
      <div class="slice-title">${s.title}</div>
      <div class="slice-subtitle">${s.subtitle}</div>
    </div>
    ${s.html}
  </div>
</body>
</html>`;

    const htmlPath = path.join(TEMP_DIR, `${s.name}.html`);
    fs.writeFileSync(htmlPath, htmlContent);

    const outPng = path.join(OUTPUT_DIR, `${s.name}.png`);
    const cmd = `"${CHROME_PATH}" --headless --disable-gpu --hide-scrollbars --window-size=850,600 --screenshot="${outPng}" "file:///${htmlPath.replace(/\\/g, '/')}"`;
    console.log(`Rendering ${s.name} (850x600)...`);
    execSync(cmd);
  }
  console.log('All 850x600 slices generated successfully!');
}

renderPinataStyle();
