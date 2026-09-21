const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CHROME_PATH = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUTPUT_DIR = path.join(__dirname, 'assets', 'spigot_recommended');

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
  font-family: 'Plus Jakarta Sans', sans-serif;
  color: #e2e8f0;
}

.slice-container {
  width: 800px;
  position: relative;
  background: radial-gradient(circle at 50% 0%, rgba(0, 242, 254, 0.08) 0%, rgba(13, 17, 30, 1) 70%), #090d16;
  border-top: 1px solid rgba(0, 242, 254, 0.25);
  border-bottom: 1px solid rgba(123, 44, 191, 0.25);
  padding: 30px 36px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.slice-container::before {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
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
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #ffffff 30%, #a5b4fc 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.slice-subtitle {
  font-size: 12.5px;
  color: #94a3b8;
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

.pill-tag {
  font-size: 10.5px;
  font-family: 'JetBrains Mono', monospace;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(0, 242, 254, 0.12);
  border: 1px solid rgba(0, 242, 254, 0.3);
  color: #00f2fe;
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

const hudHtml = `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><style>${baseStyles}</style></head>
<body>
  <div class="slice-container" style="min-height:430px;">
    <div>
      <div class="slice-badge">IN-GAME LIVE HUD • REAL PREVIEW</div>
      <div class="slice-title" style="margin-top:6px;">LIVE BOSSBAR & ACTIONBAR TELEMETRY</div>
      <div class="slice-subtitle">Direct in-game capture showcasing real-time metrics and the 8 hotbar tools</div>
    </div>

    <div class="frame-card">
      <div class="frame-header">
        <div class="frame-title">📊 Live Dynamic BossBar HUD</div>
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
  </div>
</body>
</html>`;

const guisHtml = `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><style>${baseStyles}</style></head>
<body>
  <div class="slice-container" style="min-height:440px;">
    <div>
      <div class="slice-badge">INTERACTIVE MENUS • REAL PREVIEW</div>
      <div class="slice-title" style="margin-top:6px;">STAFF PANEL & TICKET REVIEW GUIS</div>
      <div class="slice-subtitle">Intuitive chest-menu interfaces for complete server surveillance and rapid dispatch</div>
    </div>

    <div class="grid-2col">
      <div class="frame-card">
        <div class="frame-header">
          <div class="frame-title">⭐ Master Staff Panel</div>
        </div>
        <div class="img-wrapper" style="min-height: 220px;">
          <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_staff_panel.png').replace(/\\/g, '/')}" style="width:100%; max-height: 240px; object-fit: contain;" />
        </div>
        <div style="font-size:12px; color:#94a3b8; line-height:1.4;">Acceso centralizado con un clic a teletransporte, congelación, inspección y visión nocturna.</div>
      </div>

      <div class="frame-card" style="border-color: rgba(168, 85, 247, 0.35);">
        <div class="frame-header">
          <div class="frame-title">📋 Report Review Ticket GUI</div>
        </div>
        <div class="img-wrapper" style="min-height: 220px;">
          <img src="file:///${path.join(__dirname, 'assets', 'screenshots', 'ingame_report_review.png').replace(/\\/g, '/')}" style="width:100%; max-height: 240px; object-fit: contain;" />
        </div>
        <div style="font-size:12px; color:#94a3b8; line-height:1.4;">Examen de tickets en vivo con resolución de UUIDs offline, salto a la escena y cierre de casos.</div>
      </div>
    </div>
  </div>
</body>
</html>`;

const tempDir = path.join(__dirname, 'temp_slices');
if (!fs.existsSync(tempDir)) fs.mkdirSync(tempDir, { recursive: true });

fs.writeFileSync(path.join(tempDir, 'showcase_hud.html'), hudHtml);
fs.writeFileSync(path.join(tempDir, 'showcase_guis.html'), guisHtml);

const hudOut = path.join(OUTPUT_DIR, 'Spigot_Showcase_InGame_HUDs.png');
const guisOut = path.join(OUTPUT_DIR, 'Spigot_Showcase_GUIs.png');

console.log('Rendering In-Game HUD Showcase...');
execSync(`"${CHROME_PATH}" --headless --disable-gpu --hide-scrollbars --window-size=800,430 --screenshot="${hudOut}" "file:///${path.join(tempDir, 'showcase_hud.html').replace(/\\/g, '/')}"`);

console.log('Rendering In-Game GUIs Showcase...');
execSync(`"${CHROME_PATH}" --headless --disable-gpu --hide-scrollbars --window-size=800,440 --screenshot="${guisOut}" "file:///${path.join(tempDir, 'showcase_guis.html').replace(/\\/g, '/')}"`);

console.log('Showcase slices rendered successfully!');
