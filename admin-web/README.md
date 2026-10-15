# Stickers Bank — Web Admin

A React admin panel for building the sticker catalog. It writes to the **same Firebase
Firestore** and the **same Cloudflare R2 bucket** as the Android app, so anything you upload
here shows up in the app.

## Run it

```bash
cd admin-web
npm install        # first time only
npm run dev
```

Then open **http://localhost:5173**

That's it. `npm run dev` starts two things:

| Part | Port | What it does |
|------|------|--------------|
| Web UI (Vite) | 5173 | The admin panel you click around in |
| Upload server (Node) | 3001 | Uploads to R2 + converts images to sticker WebP |

## How to upload stickers

1. **Categories** → add `Hindi`
2. Click Hindi → **Types** → add `Actress`
3. Click Actress → **Names** → add `Deepika`
   - Tick **Animated (GIF / .mp4)** if this collection holds video clips instead of stickers
     (e.g. a name called `Animations`)
4. Click Deepika → **drag & drop all the images at once**, type tags
   (e.g. `bollywood, funny`), hit **Upload**

Images are automatically converted to **512×512 transparent WebP under 100KB**
(WhatsApp's sticker requirements). Animated collections upload `.mp4` files as-is —
the app shares those to WhatsApp's **GIF** section.

Tags you type are applied to **every** file in that batch, plus the category / type /
name are added automatically (so `hindi`, `actress`, `deepika`).

## Notes

- **Your R2 keys live in `.env`** and are only used by `server.js` (Node). They are never
  sent to the browser. Don't commit `.env`.
- If Firestore errors with an API-key/referrer problem, register a **Web** app in the
  Firebase console (Project settings → Your apps → Add app → Web) and paste the `apiKey`
  and `appId` into `src/firebase.js`.
- Firestore rules must allow writes (currently open test-mode rules).

## Files

```
server.js        Node upload server (R2 + sharp image conversion)
src/firebase.js  Firebase project config
src/db.js        Firestore CRUD — must match the Android models exactly
src/api.js       Calls the upload server
src/App.jsx      The whole UI (categories → types → names → upload)
```
