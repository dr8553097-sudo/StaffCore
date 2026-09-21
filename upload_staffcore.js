const fs = require('fs');
const path = require('path');

const BASE_URL = 'https://gamedash.astrolnodes.net';
const API_KEY = 'ptlc_Zwd1thvzXmZ0CJBOplbOeHU3Z3yWF551yHAmqItMd7B';
const SERVER_ID = 'c0f9b14d';

const JAR_PATH = path.join(__dirname, 'target', 'StaffCore-2.0.0.jar');

async function uploadJar() {
  if (!fs.existsSync(JAR_PATH)) {
    throw new Error(`JAR not found at: ${JAR_PATH}`);
  }

  console.log('1. Getting signed upload URL from Pterodactyl...');
  const res = await fetch(`${BASE_URL}/api/client/servers/${SERVER_ID}/files/upload`, {
    headers: {
      Authorization: `Bearer ${API_KEY}`,
      Accept: 'application/json'
    }
  });

  if (!res.ok) {
    throw new Error(`Failed to get upload URL: ${res.status} ${await res.text()}`);
  }

  const data = await res.json();
  const uploadUrl = data.attributes.url;
  console.log('Upload URL obtained:', uploadUrl);

  console.log('2. Reading JAR file...');
  const fileBuffer = fs.readFileSync(JAR_PATH);
  const blob = new Blob([fileBuffer], { type: 'application/java-archive' });

  const formData = new FormData();
  formData.append('files', blob, 'StaffCore-2.0.0.jar');

  console.log('3. Uploading StaffCore-2.0.0.jar...');
  const uploadRes = await fetch(`${uploadUrl}&directory=/plugins`, {
    method: 'POST',
    body: formData
  });

  if (!uploadRes.ok) {
    throw new Error(`Upload failed: ${uploadRes.status} ${await uploadRes.text()}`);
  }

  console.log('Upload completed successfully!');
}

uploadJar().catch(err => {
  console.error('Upload failed:', err.message);
  process.exit(1);
});
