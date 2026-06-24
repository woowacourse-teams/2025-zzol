import sharp from 'sharp';
import { readFileSync, mkdirSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const SVG_PATH = path.resolve(__dirname, '../src/assets/profile-red.svg');
const OUT_DIR = path.resolve(__dirname, '../public/icons');

mkdirSync(OUT_DIR, { recursive: true });

const svgBuffer = readFileSync(SVG_PATH);

await sharp(svgBuffer).resize(192, 192).png().toFile(`${OUT_DIR}/icon-192.png`);
await sharp(svgBuffer).resize(512, 512).png().toFile(`${OUT_DIR}/icon-512.png`);
await sharp(svgBuffer).resize(180, 180).png().toFile(`${OUT_DIR}/apple-touch-icon.png`);

// maskable: 아이콘을 72% 크기로 흰 배경 위에 중앙 배치
const iconSize = Math.round(512 * 0.72);
const padding = Math.round((512 - iconSize) / 2);
const iconBuffer = await sharp(svgBuffer).resize(iconSize, iconSize).png().toBuffer();

await sharp({
  create: {
    width: 512,
    height: 512,
    channels: 4,
    background: { r: 255, g: 255, b: 255, alpha: 1 },
  },
})
  .composite([{ input: iconBuffer, left: padding, top: padding }])
  .png()
  .toFile(`${OUT_DIR}/icon-512-maskable.png`);

console.log('Icons generated in public/icons/');
