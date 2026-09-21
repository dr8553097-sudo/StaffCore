const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const CHROME_PATH = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";
const OUTPUT_DIR = path.join(__dirname, 'assets', 'v2');
const TEMP_DIR = path.join(__dirname, 'temp_slices');

if (!fs.existsSync(OUTPUT_DIR)) fs.mkdirSync(OUTPUT_DIR, { recursive: true });
if (!fs.existsSync(TEMP_DIR)) fs.mkdirSync(TEMP_DIR, { recursive: true });

const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <style>
    @import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@700;800;900&family=JetBrains+Mono:wght@700&display=swap');
    * { margin:0; padding:0; box-sizing:border-box; }
    body {
      width: 850px;
      height: 220px;
      background: transparent;
      font-family: 'Plus Jakarta Sans', sans-serif;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
    }
    .banner {
      width: 850px;
      height: 220px;
      position: relative;
      background: linear-gradient(135deg, rgba(15,23,42,0.95) 0%, rgba(11,14,20,0.98) 100%);
      border: 1px solid rgba(56,189,248,0.3);
      border-radius: 16px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 10px 40px -10px rgba(0,242,254,0.25), inset 0 1px 0 rgba(255,255,255,0.1);
      padding: 20px;
      text-align: center;
    }
    .badge-row {
      display: flex;
      gap: 10px;
      margin-bottom: 8px;
    }
    .badge {
      background: rgba(56,189,248,0.12);
      border: 1px solid rgba(56,189,248,0.4);
      color: #38bdf8;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 1px;
      padding: 4px 14px;
      border-radius: 9999px;
      text-transform: uppercase;
      box-shadow: 0 0 15px rgba(56,189,248,0.2);
    }
    .badge-ver {
      background: linear-gradient(135deg, #00f2fe, #4facfe);
      color: #0b0e14;
      font-weight: 800;
      border: none;
    }
    .title {
      font-size: 42px;
      font-weight: 900;
      letter-spacing: -0.5px;
      background: linear-gradient(135deg, #ffffff 0%, #38bdf8 60%, #00f2fe 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      line-height: 1.1;
      margin-bottom: 6px;
    }
    .sub {
      color: #94a3b8;
      font-size: 14px;
      font-weight: 600;
      letter-spacing: 0.5px;
    }
    .sub span {
      color: #38bdf8;
      font-weight: 700;
    }
  </style>
</head>
<body>
  <div class="banner">
    <div class="badge-row">
      <div class="badge">MODERN STAFF SUITE</div>
      <div class="badge badge-ver">v2.0.0 ENTERPRISE</div>
      <div class="badge">FOLIA & PAPER READY</div>
    </div>
    <div class="title">⚡ STAFFCORE ⚡</div>
    <div class="sub">Next-Generation Moderation Architecture • <span>Zero-Bloat</span> • Pure Native Async</div>
  </div>
</body>
</html>`;

const tempHtml = path.join(TEMP_DIR, 'header_850.html');
fs.writeFileSync(tempHtml, html);
const outPng = path.join(OUTPUT_DIR, 'Spigot_Header_850px.png');
const cmd = `"${CHROME_PATH}" --headless --disable-gpu --hide-scrollbars --window-size=850,220 --screenshot="${outPng}" "file:///${tempHtml.replace(/\\/g, '/')}"`;
execSync(cmd);
console.log('Header rendered at 850x220 successfully!');
