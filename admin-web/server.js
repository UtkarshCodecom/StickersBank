/**
 * Tiny local upload server.
 *
 * Why this exists: your R2 keys stay HERE (server-side) instead of being shipped to the
 * browser, and uploading server-side means you don't have to configure CORS on the bucket.
 *
 * It does exactly two things:
 *   1. Static stickers -> resize to 512x512 transparent WebP (<100KB, WhatsApp-compliant)
 *   2. Animated stickers -> a 512x512 *animated* WebP (<500KB) that WhatsApp plays inline.
 *      Source can be a video (mp4/mov/webm — transcoded with ffmpeg) OR an already-animated
 *      image (GIF / animated WebP / APNG — re-encoded with sharp).
 * ...then PUTs to R2 and returns the public URL.
 */
import express from 'express';
import multer from 'multer';
import sharp from 'sharp';
import dotenv from 'dotenv';
import ffmpegPath from 'ffmpeg-static';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { promises as fs } from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { S3Client, PutObjectCommand } from '@aws-sdk/client-s3';
import { NodeHttpHandler } from '@smithy/node-http-handler';
// v5 is CJS-only — the default export carries the actual named exports under ESM interop.
import httpsProxyAgentPkg from 'https-proxy-agent';
const { HttpsProxyAgent } = httpsProxyAgentPkg;

const execFileP = promisify(execFile);

dotenv.config();

const {
  R2_ENDPOINT,
  R2_BUCKET,
  R2_ACCESS_KEY_ID,
  R2_SECRET_ACCESS_KEY,
  R2_PUBLIC_BASE_URL,
} = process.env;

// Deliberately NOT the generic PORT: tooling that boots the dev server (and Vite itself)
// injects PORT, which had this upload server racing Vite for the same port. UPLOAD_PORT is
// ours alone; the Vite proxy in vite.config.js points at the same default.
const PORT = process.env.UPLOAD_PORT || 3001;

// R2 is only needed for image uploads. If it isn't configured, keep the server running so the
// rest of the admin (catalog editing via Firebase) still works — uploads just return a clear
// error until someone creates admin-web/.env (copy .env.example and fill in the R2 values).
const R2_READY = Boolean(R2_ENDPOINT && R2_BUCKET && R2_ACCESS_KEY_ID && R2_SECRET_ACCESS_KEY);
if (!R2_READY) {
  console.warn(
    '⚠  R2 not configured — image uploads are disabled.\n' +
    '   Create admin-web/.env (copy admin-web/.env.example) and add your Cloudflare R2 keys.\n' +
    '   Everything else (catalog editing) still works.'
  );
}

// The AWS SDK's default HTTP handler does NOT read HTTP_PROXY/HTTPS_PROXY the way curl and
// browsers do — on a machine that requires a proxy for outbound traffic, uploads would hang
// until ETIMEDOUT even though the network is otherwise fine. Route through it explicitly when set.
const proxyUrl = process.env.HTTPS_PROXY || process.env.https_proxy || process.env.HTTP_PROXY || process.env.http_proxy;
if (proxyUrl) console.log(`   Routing R2 uploads through proxy ${proxyUrl}`);

const s3 = R2_READY
  ? new S3Client({
      region: 'auto',
      endpoint: R2_ENDPOINT,
      credentials: {
        accessKeyId: R2_ACCESS_KEY_ID,
        secretAccessKey: R2_SECRET_ACCESS_KEY,
      },
      forcePathStyle: true,
      ...(proxyUrl
        ? { requestHandler: new NodeHttpHandler({ httpsAgent: new HttpsProxyAgent(proxyUrl) }) }
        : {}),
    })
  : null;

const app = express();
const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 60 * 1024 * 1024 }, // 60MB
});

/** Resize to a 512x512 transparent WebP, shrinking quality until it's under maxBytes. */
async function toStickerWebp(buffer, maxBytes = 99_000) {
  let quality = 90;
  let out = null;
  for (;;) {
    out = await sharp(buffer)
      .resize(512, 512, {
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 },
      })
      .webp({ quality })
      .toBuffer();
    if (out.length <= maxBytes || quality <= 20) break;
    quality -= 10;
  }
  return out;
}

/**
 * Re-encode an animated source (GIF / animated WebP / APNG) into a 512x512 transparent
 * *animated* WebP that stays under WhatsApp's 500KB animated-sticker limit. Every frame is
 * kept ({ animated: true }); quality is stepped down until the whole clip fits.
 */
async function toAnimatedStickerWebp(buffer, maxBytes = 500_000) {
  let quality = 80;
  let out = null;
  for (;;) {
    out = await sharp(buffer, { animated: true })
      .resize(512, 512, {
        fit: 'contain',
        background: { r: 0, g: 0, b: 0, alpha: 0 },
      })
      .webp({ quality, effort: 4, loop: 0 })
      .toBuffer();
    if (out.length <= maxBytes || quality <= 20) break;
    quality -= 10;
  }
  return out;
}

/**
 * Transcode a VIDEO (mp4/mov/webm/…) to a 512x512 looping *animated* WebP under WhatsApp's
 * 500KB limit, using the bundled ffmpeg (ffmpeg-static — no system install needed). We step
 * fps / quality / duration down until it fits. Transparent letterbox padding keeps it square.
 */
async function videoToAnimatedWebp(buffer, srcExt = 'mp4', maxBytes = 500_000) {
  if (!ffmpegPath) throw new Error('ffmpeg is not available (ffmpeg-static failed to install)');
  const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'sbwebp-'));
  const inPath = path.join(dir, `in.${srcExt}`);
  const outPath = path.join(dir, 'out.webp');
  await fs.writeFile(inPath, buffer);

  const filter = (fps) =>
    `fps=${fps},scale=512:512:force_original_aspect_ratio=decrease,` +
    `pad=512:512:-1:-1:color=#00000000,format=rgba`;

  async function run(fps, quality, seconds) {
    await execFileP(ffmpegPath, [
      '-y', '-i', inPath,
      '-vcodec', 'libwebp', '-lossless', '0', '-compression_level', '5',
      '-q:v', String(quality), '-loop', '0', '-an', '-vsync', '0',
      '-t', String(seconds), '-vf', filter(fps), outPath,
    ]);
    return (await fs.stat(outPath)).size;
  }

  try {
    // Progressively cheaper encodes until the clip fits WhatsApp's 500KB cap.
    let size = await run(15, 60, 6);
    if (size > maxBytes) size = await run(12, 45, 5);
    if (size > maxBytes) size = await run(10, 32, 4);
    return await fs.readFile(outPath);
  } finally {
    await fs.rm(dir, { recursive: true, force: true });
  }
}

app.post('/api/upload', upload.single('file'), async (req, res) => {
  try {
    if (!R2_READY) return res.status(503).json({ error: 'R2 not configured on this machine — add admin-web/.env to enable uploads.' });
    const { collectionId, groupId, categoryId } = req.body;
    // kind: 'sticker' (default) | 'animated' | 'cover' | 'banner' | 'square'
    const kind = req.body.kind || 'sticker';
    const noCollKinds = [
      'banner', 'bannerVideo', 'square', 'trendingImage', 'canvasTemplate', 'font', 'decoration',
      'pageBackground', 'categoryBg', 'trendingBg',
    ];
    // Storage-path folder only — stickers/animated can be uploaded directly on a Category or a
    // Group (skipping deeper levels), so fall back to whichever id the client actually has.
    const scopeId = collectionId || groupId || categoryId || '';
    if (!req.file) return res.status(400).json({ error: 'No file' });
    if (!noCollKinds.includes(kind) && !scopeId) return res.status(400).json({ error: 'No destination selected' });

    const stamp = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;

    let body;
    let key;
    let contentType;

    if (kind === 'banner') {
      // A promo banner shown on Featured — cropped to a 5:1 strip (1200x240) webp.
      body = await sharp(req.file.buffer)
        .resize(1200, 240, { fit: 'cover', position: 'attention' })
        .webp({ quality: 85 })
        .toBuffer();
      key = `banners/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'bannerVideo') {
      // A video banner — uploaded as-is (no re-encode) into the same rotation as image
      // banners; the app fits it into the 5:1 slot (not cropped) and plays it back directly.
      body = req.file.buffer;
      const ext = (req.file.originalname?.split('.').pop() || 'mp4').toLowerCase();
      key = `banners/${stamp}.${ext}`;
      contentType = req.file.mimetype || 'video/mp4';
    } else if (kind === 'canvasTemplate') {
      // A canvas template's art — portrait 4:5, matching the Templates tab card.
      body = await sharp(req.file.buffer)
        .resize(720, 900, { fit: 'cover', position: 'attention' })
        .webp({ quality: 88 })
        .toBuffer();
      key = `templates/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'trendingImage') {
      // A Trending card's own background — portrait 4:5 (width:height), cropped to fill.
      body = await sharp(req.file.buffer)
        .resize(480, 600, { fit: 'cover', position: 'attention' })
        .webp({ quality: 85 })
        .toBuffer();
      key = `trending/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'square') {
      // A square (1:1) image used for a category card in the app's grid view.
      body = await sharp(req.file.buffer)
        .resize(600, 600, { fit: 'cover', position: 'attention' })
        .webp({ quality: 85 })
        .toBuffer();
      key = `categories/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'animated') {
      // Accept either a video (mp4/mov/webm — transcoded with ffmpeg) or an already-animated
      // image (GIF / animated WebP / APNG — re-encoded with sharp). Either way the output is
      // a 512x512 animated WebP under 500KB, which WhatsApp plays as an animated sticker.
      const isVideo =
        (req.file.mimetype || '').startsWith('video/') ||
        /\.(mp4|mov|webm|mkv|avi|m4v)$/i.test(req.file.originalname || '');
      if (isVideo) {
        const ext = (req.file.originalname?.split('.').pop() || 'mp4').toLowerCase();
        body = await videoToAnimatedWebp(req.file.buffer, ext);
      } else {
        body = await toAnimatedStickerWebp(req.file.buffer);
      }
      key = `animations/${scopeId}/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'font') {
      // A custom .ttf/.otf uploaded as-is — the app downloads and caches it locally to use
      // like any bundled font.
      body = req.file.buffer;
      const ext = (req.file.originalname?.split('.').pop() || 'ttf').toLowerCase();
      key = `fonts/${stamp}.${ext}`;
      contentType = req.file.mimetype || 'font/ttf';
    } else if (kind === 'pageBackground' || kind === 'categoryBg' || kind === 'trendingBg') {
      // A full-screen backdrop behind a whole tab, a category's page, or a trending card's
      // page — displayed behind flexible-height content, so it's capped to a sane size (not
      // cropped to any fixed shape) and re-encoded to webp.
      body = await sharp(req.file.buffer)
        .resize(1080, 1920, { fit: 'inside', withoutEnlargement: true })
        .webp({ quality: 85 })
        .toBuffer();
      key = `backgrounds/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'decoration') {
      // A transparent overlay (glasses, cap, mustache…) droppable onto a photo from the
      // editor's Decorate tab — same treatment as a sticker (512 square, transparent pad) but
      // stored under its own prefix since it isn't tied to any collection.
      body = await toStickerWebp(req.file.buffer);
      key = `decorations/${stamp}.webp`;
      contentType = 'image/webp';
    } else if (kind === 'cover') {
      // A cover photo (e.g. a picture of Kareena) shown on the Actress page.
      // Square crop, focused on the most interesting region (usually the face).
      body = await sharp(req.file.buffer)
        .resize(600, 600, { fit: 'cover', position: 'attention' })
        .webp({ quality: 85 })
        .toBuffer();
      key = `covers/${collectionId}/${stamp}.webp`;
      contentType = 'image/webp';
    } else {
      body = await toStickerWebp(req.file.buffer);
      key = `stickers/${scopeId}/${stamp}.webp`;
      contentType = 'image/webp';
    }

    await s3.send(
      new PutObjectCommand({
        Bucket: R2_BUCKET,
        Key: key,
        Body: body,
        ContentType: contentType,
      })
    );

    res.json({ url: `${R2_PUBLIC_BASE_URL}/${key}`, key, bytes: body.length });
  } catch (err) {
    console.error('Upload failed:', err);
    res.status(500).json({ error: err.message || 'Upload failed' });
  }
});

app.get('/api/health', (_req, res) => res.json({ ok: true, bucket: R2_BUCKET }));

app.listen(PORT, () => {
  console.log(
    R2_READY
      ? `✓ Upload server ready  →  http://localhost:${PORT}  (bucket: ${R2_BUCKET})`
      : `✓ Server up (uploads OFF — no R2)  →  http://localhost:${PORT}`
  );
});
