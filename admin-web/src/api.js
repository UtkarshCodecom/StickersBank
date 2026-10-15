/**
 * Uploads one file to R2 via the local server (server.js), returns its public URL.
 * kind: 'sticker' (512 webp) | 'animated' (.mp4 as-is) | 'cover' (600x600 photo)
 */
export async function uploadToR2(file, { collectionId, kind = 'sticker' }) {
  const form = new FormData();
  form.append('file', file);
  form.append('collectionId', collectionId);
  form.append('kind', kind);

  let res;
  try {
    // Without a deadline a wedged upload just sits there: the button says "Uploading…" for
    // half a minute with nothing to act on, which reads as a dead button rather than a
    // failure. Fail fast and say why instead.
    res = await fetch('/api/upload', { method: 'POST', body: form, signal: AbortSignal.timeout(45_000) });
  } catch (e) {
    if (e.name === 'TimeoutError' || e.name === 'AbortError') {
      throw new Error('Upload timed out — the upload server (npm run server) is not reaching R2. Check that it is running and online.');
    }
    throw new Error('Upload failed — the upload server (npm run server) is not reachable.');
  }
  if (!res.ok) {
    throw new Error(`Upload failed: ${await readError(res)}`);
  }
  const { url } = await res.json();
  return url;
}

/** The server answers with JSON on failure; show its message, not the raw envelope. */
async function readError(res) {
  const text = await res.text();
  try {
    const j = JSON.parse(text);
    const msg = j.error || text;
    // Connection-level failures from the S3 client are meaningless on their own.
    if (/ETIMEDOUT|ECONNRESET|ENOTFOUND|EAI_AGAIN|fetch failed/i.test(msg)) {
      return `${msg} — the upload server can't reach R2 (network, VPN or R2 credentials).`;
    }
    return msg;
  } catch {
    return text || `HTTP ${res.status}`;
  }
}
