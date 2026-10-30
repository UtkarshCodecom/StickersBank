/**
 * Firestore access — mirrors the Android app's AdminRepository EXACTLY.
 * Collections: categories → groups → collections → stickers
 *
 * Field names must match the Android models or the app won't read them.
 * (Android stores the doc id via @DocumentId, so we never write an `id` field.)
 */
import {
  collection,
  addDoc,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  orderBy,
  writeBatch,
} from 'firebase/firestore';
import { db } from './firebase';

const nowMs = () => Date.now();

// Android writes `order` as System.currentTimeMillis().toInt() (low 32 bits).
// `Date.now() | 0` produces the identical value, so ordering stays consistent.
const orderVal = () => Date.now() | 0;

const rows = (snap) => snap.docs.map((d) => ({ id: d.id, ...d.data() }));
const bySortOrder = (a, b) => (a.order || 0) - (b.order || 0);

/**
 * Sticker order inside a collection / group / category: stickers the admin has arranged carry a
 * `position` (0, 1, 2…) and come first in that order; the rest (newly uploaded since) follow in
 * upload order. The Android app uses the exact same rule (CatalogRepository.stickerOrder).
 */
const hasPos = (s) => typeof s.position === 'number' && s.position >= 0;
// Newly added stickers come FIRST (same as the app): not-yet-arranged ones lead, newest first,
// then the arranged ones by position.
export const byStickerOrder = (a, b) => {
  if (hasPos(a) !== hasPos(b)) return hasPos(a) ? 1 : -1;
  if (hasPos(a) && a.position !== b.position) return a.position - b.position;
  return (b.createdAt || 0) - (a.createdAt || 0);
};

/* ---------------- Categories (Hindi, English…) ---------------- */

export async function listCategories() {
  const snap = await getDocs(query(collection(db, 'categories'), orderBy('order')));
  return rows(snap);
}

/** Returns the new category's id, so callers can select it straight away. */
export async function addCategory(name) {
  const ref = await addDoc(collection(db, 'categories'), {
    name: name.trim(),
    order: orderVal(),
    createdAt: nowMs(),
  });
  return ref.id;
}

/**
 * Deleting a category cascades: every group/collection/sticker under it goes too. Every
 * sticker doc always carries its full lineage (categoryId, groupId, collectionId — see
 * addSticker below), so a single categoryId query finds all of them regardless of nesting
 * depth, no need to walk groups → collections first.
 */
export async function deleteCategory(id) {
  const [groupsSnap, collectionsSnap, stickersSnap] = await Promise.all([
    getDocs(query(collection(db, 'groups'), where('categoryId', '==', id))),
    getDocs(query(collection(db, 'collections'), where('categoryId', '==', id))),
    getDocs(query(collection(db, 'stickers'), where('categoryId', '==', id))),
  ]);
  await Promise.all([
    ...stickersSnap.docs.map((d) => deleteDoc(d.ref)),
    ...collectionsSnap.docs.map((d) => deleteDoc(d.ref)),
    ...groupsSnap.docs.map((d) => deleteDoc(d.ref)),
  ]);
  await deleteDoc(doc(db, 'categories', id));
}

/* ---------------- Groups / types (Actress, Comedian…) ---------------- */

export async function listGroups(categoryId) {
  const snap = await getDocs(query(collection(db, 'groups'), where('categoryId', '==', categoryId)));
  return rows(snap).sort(bySortOrder);
}

/** Returns the new group's id, so callers can select it straight away. */
export async function addGroup(categoryId, name) {
  const ref = await addDoc(collection(db, 'groups'), {
    categoryId,
    name: name.trim(),
    order: orderVal(),
    createdAt: nowMs(),
  });
  return ref.id;
}

/** Deleting a group cascades: its collections and every sticker under it (direct or nested
 * inside one of those collections — every sticker carries groupId regardless of depth). */
export async function deleteGroup(id) {
  const [collectionsSnap, stickersSnap] = await Promise.all([
    getDocs(query(collection(db, 'collections'), where('groupId', '==', id))),
    getDocs(query(collection(db, 'stickers'), where('groupId', '==', id))),
  ]);
  await Promise.all([
    ...stickersSnap.docs.map((d) => deleteDoc(d.ref)),
    ...collectionsSnap.docs.map((d) => deleteDoc(d.ref)),
  ]);
  await deleteDoc(doc(db, 'groups', id));
}

/* ---------------- Collections / names (Deepika, Animations…) ---------------- */

export async function listCollections(groupId) {
  const snap = await getDocs(query(collection(db, 'collections'), where('groupId', '==', groupId)));
  return rows(snap).sort(bySortOrder);
}

/** Returns the new collection's id, so callers that need to file stickers into it can. */
export async function addCollection(categoryId, groupId, name, animated) {
  const ref = await addDoc(collection(db, 'collections'), {
    categoryId,
    groupId,
    name: name.trim(),
    cover: '',
    animated: !!animated,
    order: orderVal(),
    createdAt: nowMs(),
  });
  return ref.id;
}

/** Deleting a collection cascades: every sticker placed inside it. */
export async function deleteCollection(id) {
  const stickersSnap = await getDocs(query(collection(db, 'stickers'), where('collectionId', '==', id)));
  await Promise.all(stickersSnap.docs.map((d) => deleteDoc(d.ref)));
  await deleteDoc(doc(db, 'collections', id));
}

/**
 * Cover photo for a collection (e.g. a photo of Kareena) — the app shows this on the
 * group page (Hindi → Actress). Pass '' to remove it.
 */
export const setCollectionCover = (collectionId, url) =>
  updateDoc(doc(db, 'collections', collectionId), { cover: url });

/* ---------------- Rename + reorder (categories / groups / collections) ---------------- */

/** coll = 'categories' | 'groups' | 'collections' */
export const updateName = (coll, id, name) =>
  updateDoc(doc(db, coll, id), { name: name.trim() });

/**
 * Persist the order the admin arranged items in. Writes order = index for each doc, so
 * the app (which sorts by `order`) shows them in exactly this order regardless of when
 * they were created.
 */
export async function saveOrder(coll, orderedItems) {
  await Promise.all(
    orderedItems.map((it, i) => updateDoc(doc(db, coll, it.id), { order: i }))
  );
}

/* ---------------- Stickers ---------------- */

export async function listStickers(collectionId) {
  const snap = await getDocs(
    query(collection(db, 'stickers'), where('collectionId', '==', collectionId))
  );
  return rows(snap).sort(byStickerOrder);
}

/** Every sticker in the whole catalog — used to build the "All tags" browser. */
export async function listAllStickers() {
  const snap = await getDocs(collection(db, 'stickers'));
  return rows(snap);
}

/**
 * Stickers placed directly on a Category or a Group — skipping Type and/or Name entirely.
 * Pass groupId to get stickers placed directly on that group (collectionId blank); omit it
 * to get stickers placed directly on the category (groupId AND collectionId both blank).
 * Single-field `where` (auto-indexed, no composite index needed) + a client-side filter,
 * so this never needs a Firestore index to be created first.
 */
export async function listDirectStickers({ categoryId, groupId }) {
  if (groupId) {
    const snap = await getDocs(query(collection(db, 'stickers'), where('groupId', '==', groupId)));
    return rows(snap).filter((s) => !s.collectionId).sort(byStickerOrder);
  }
  const snap = await getDocs(query(collection(db, 'stickers'), where('categoryId', '==', categoryId)));
  return rows(snap).filter((s) => !s.groupId && !s.collectionId).sort(byStickerOrder);
}

export async function addSticker({ categoryId, groupId, collectionId, imageUrl, tags, animated }) {
  await addDoc(collection(db, 'stickers'), {
    categoryId,
    groupId,
    collectionId,
    imageUrl,
    tags,
    caption: '',
    animated: !!animated,
    createdAt: nowMs(),
  });
}

/**
 * Persist an arranged sticker list: position = index. Only stickers whose position actually
 * changes are written (batched, Firestore's limit is 500 per batch).
 */
export async function saveStickerOrder(orderedStickers) {
  const changed = orderedStickers
    .map((s, i) => ({ id: s.id, i, was: s.position }))
    .filter((x) => x.was !== x.i);
  for (let k = 0; k < changed.length; k += 450) {
    const batch = writeBatch(db);
    changed.slice(k, k + 450).forEach((x) => batch.update(doc(db, 'stickers', x.id), { position: x.i }));
    await batch.commit();
  }
}

export const deleteSticker = (id) => deleteDoc(doc(db, 'stickers', id));

/* ---------------- Organize (stickers imported without a collection) ---------------- */

/**
 * Stickers carried over from the old stickersbank.in site that still sit directly on a
 * group. The legacy database recorded no name for these (no sub_sub_category), so they
 * can only be sorted by a human who recognises who/what is in them.
 *
 * Each one carries `importBatch`: the upload burst it belonged to on the old site.
 * The old admin added a pack in one go (stickers seconds apart), so a batch is almost
 * always a single person or movie — which turns ~1,200 individual decisions into ~380.
 */
export async function listUnfiledStickers() {
  const snap = await getDocs(collection(db, 'stickers'));
  return rows(snap).filter((s) => !s.collectionId && s.groupId);
}

/** File a batch of stickers into a collection in one go. */
export async function assignStickersToCollection(ids, collectionId) {
  await Promise.all(ids.map((id) => updateDoc(doc(db, 'stickers', id), { collectionId })));
}

/**
 * Move stickers somewhere else in the catalog.
 *
 * All three ids are rewritten together on purpose: a sticker carries its FULL lineage
 * (see addSticker), and the app queries by categoryId/groupId as well as collectionId — so
 * updating only collectionId would leave the sticker still showing under its old category.
 * Pass '' for groupId/collectionId to place it directly on the level above.
 */
export async function moveStickers(ids, { categoryId, groupId = '', collectionId = '' }) {
  await Promise.all(
    ids.map((id) => updateDoc(doc(db, 'stickers', id), { categoryId, groupId, collectionId }))
  );
}

/** Replace a sticker's tags (used to edit tags after upload). */
export const updateStickerTags = (id, tags) =>
  updateDoc(doc(db, 'stickers', id), { tags });

/* ---------------- Emojis (shown in the app editor's emoji picker) ---------------- */

export async function listEmojis() {
  const snap = await getDocs(query(collection(db, 'emojis'), orderBy('order')));
  return rows(snap);
}

export async function addEmoji(value) {
  await addDoc(collection(db, 'emojis'), {
    value: value.trim(),
    order: orderVal(),
    createdAt: nowMs(),
  });
}

export const deleteEmoji = (id) => deleteDoc(doc(db, 'emojis', id));

/* ---------------- Custom fonts (shown in the app editor's font picker) ---------------- */

export async function listFonts() {
  const snap = await getDocs(query(collection(db, 'fonts'), orderBy('order')));
  return rows(snap);
}

export async function addFont(name, url) {
  await addDoc(collection(db, 'fonts'), {
    name: name.trim(),
    url,
    order: orderVal(),
    createdAt: nowMs(),
  });
}

export const renameFont = (id, name) => updateDoc(doc(db, 'fonts', id), { name: name.trim() });
export const deleteFont = (id) => deleteDoc(doc(db, 'fonts', id));

/**
 * Built-in fonts ship inside the app itself, so they can't actually be deleted from here —
 * instead, hiding one just excludes its asset path from the app's font picker. Restorable any
 * time, since the underlying font is still bundled in the app.
 */
export async function listHiddenFonts() {
  const snap = await getDocs(collection(db, 'hiddenFonts'));
  return rows(snap);
}
export const hideBuiltinFont = (path) =>
  addDoc(collection(db, 'hiddenFonts'), { path, createdAt: nowMs() });
export const unhideBuiltinFont = (id) => deleteDoc(doc(db, 'hiddenFonts', id));

/**
 * The app editor's font-picker order, first to last — one list covering built-in and uploaded
 * fonts alike. Keys: 'default', a built-in's asset path ('fonts/font_1.ttf'), or 'custom:<docId>'.
 * Fonts missing from it (new uploads, a just-restored built-in) follow, in their natural order.
 */
export async function getFontOrder() {
  const snap = await getDoc(doc(db, 'appSettings', 'fonts'));
  return (snap.exists() && Array.isArray(snap.data().order) && snap.data().order) || [];
}
export const setFontOrder = (order) => setDoc(doc(db, 'appSettings', 'fonts'), { order }, { merge: true });

/* ---------------- Banners (5:1 promo images shown on the app's Featured tab) ---------------- */

export async function listBanners() {
  const snap = await getDocs(query(collection(db, 'banners'), orderBy('order')));
  return rows(snap);
}

export async function addBanner(image, video = false, link = '') {
  await addDoc(collection(db, 'banners'), {
    image,
    video: !!video,
    link: (link || '').trim(),
    order: orderVal(),
    createdAt: nowMs(),
  });
}

/** The URL a tap on this banner opens in the app. Empty string = not tappable. */
export const setBannerLink = (id, link) =>
  updateDoc(doc(db, 'banners', id), { link: (link || '').trim() });

export const deleteBanner = (id) => deleteDoc(doc(db, 'banners', id));

/**
 * Featured's banner rotation style — a single app-wide setting, not per-banner:
 * 'carousel' (current: dots, slide-style auto-advance) or 'fade' (no dots, plain crossfade).
 * A video banner always holds until it finishes playing before advancing, in either mode.
 */
export async function getBannerDisplayMode() {
  const snap = await getDoc(doc(db, 'appSettings', 'banners'));
  return snap.exists() ? (snap.data().displayMode || 'carousel') : 'carousel';
}
export const setBannerDisplayMode = (mode) =>
  setDoc(doc(db, 'appSettings', 'banners'), { displayMode: mode }, { merge: true });

/**
 * Per-page custom backgrounds — a solid color and/or an image behind the Featured tab and the
 * other main tabs (Templates, New Arrival, Bank, Favorites). One doc, flat fields (`<page>Color`
 * / `<page>Image`) so a page with nothing set just falls back to the app's normal flat bg.
 */
export const PAGE_BACKGROUND_KEYS = ['featured', 'templates', 'newArrival', 'bank', 'favorites', 'trendingAll', 'trending'];

export async function getPageBackgrounds() {
  const snap = await getDoc(doc(db, 'appSettings', 'pageBackgrounds'));
  const data = snap.exists() ? snap.data() : {};
  const out = {};
  for (const key of PAGE_BACKGROUND_KEYS) {
    out[key] = { color: data[`${key}Color`] || '', image: data[`${key}Image`] || '' };
  }
  return out;
}

export const setPageBackground = (key, { color, image }) =>
  setDoc(
    doc(db, 'appSettings', 'pageBackgrounds'),
    { [`${key}Color`]: color || '', [`${key}Image`]: image || '' },
    { merge: true },
  );

/**
 * Trending — standalone cards on Featured, NOT tied to a real category/collection. Each card
 * has its own title + portrait (4:5) image + a hand-picked list of stickerIds, shown when tapped.
 */

export async function listTrending() {
  const snap = await getDocs(query(collection(db, 'trending'), orderBy('order')));
  return rows(snap);
}
export async function addTrendingCard({ title, image, badge, stickerIds, bgColor }) {
  await addDoc(collection(db, 'trending'), {
    title: title.trim(),
    image: image || '',
    badge: badge || 'none',
    stickerIds: stickerIds || [],
    bgColor: bgColor || '',
    order: orderVal(),
    createdAt: nowMs(),
  });
}
export const updateTrendingCard = (id, data) => updateDoc(doc(db, 'trending', id), data);
export const setTrendingBadge = (id, badge) => updateDoc(doc(db, 'trending', id), { badge });
export const deleteTrending = (id) => deleteDoc(doc(db, 'trending', id));

/**
 * Packs — admin-curated packs in the app (Featured → Packs). Each is all-still or all-animated
 * ({ animated }), with a hand-picked, ordered list of stickerIds.
 */
export async function listPacks() {
  const snap = await getDocs(query(collection(db, 'packs'), orderBy('order')));
  return rows(snap);
}
export async function addPack({ name, animated, stickerIds }) {
  await addDoc(collection(db, 'packs'), {
    name: name.trim(),
    animated: !!animated,
    stickerIds: stickerIds || [],
    order: orderVal(),
    createdAt: nowMs(),
  });
}
export const updatePack = (id, data) => updateDoc(doc(db, 'packs', id), data);
export const deletePack = (id) => deleteDoc(doc(db, 'packs', id));

/**
 * Search suggestions — the scrollable "try these" chips under the app's search bar. Tapping one
 * searches it. The app shows a built-in starter set until at least one is added here.
 */
export async function listSearchSuggestions() {
  const snap = await getDocs(query(collection(db, 'searchSuggestions'), orderBy('order')));
  return rows(snap);
}
export async function addSearchSuggestion(term, order) {
  await addDoc(collection(db, 'searchSuggestions'), { term: term.trim(), order: order ?? orderVal() });
}
export const deleteSearchSuggestion = (id) => deleteDoc(doc(db, 'searchSuggestions', id));

/* ---------------- Popular tags (pills on Featured) ---------------- */

export async function listPopularTags() {
  const snap = await getDocs(query(collection(db, 'popularTags'), orderBy('order')));
  return rows(snap);
}
export async function addPopularTag(tag, color) {
  await addDoc(collection(db, 'popularTags'), { tag: tag.trim().toLowerCase(), color: color || '', order: orderVal(), createdAt: nowMs() });
}
export const deletePopularTag = (id) => deleteDoc(doc(db, 'popularTags', id));
export const setPopularTagColor = (id, color) => updateDoc(doc(db, 'popularTags', id), { color: color || '' });
export const renamePopularTag = (id, tag) => updateDoc(doc(db, 'popularTags', id), { tag });

/**
 * Decorations — transparent overlay art (glasses, a cap, etc.) for the app editor's Decorator.
 * `categoryId` files one under a decoration category ('' = uncategorized; still under "All").
 */
export async function listDecorations() {
  const snap = await getDocs(query(collection(db, 'decorations'), orderBy('order')));
  return rows(snap);
}
export async function addDecoration(image, categoryId = '') {
  await addDoc(collection(db, 'decorations'), { image, categoryId, order: orderVal(), createdAt: nowMs() });
}
export const deleteDecoration = (id) => deleteDoc(doc(db, 'decorations', id));
export const setDecorationCategory = (id, categoryId) =>
  updateDoc(doc(db, 'decorations', id), { categoryId: categoryId || '' });

/** Decoration categories — the toggles along the bottom of the app's Decorator, in `order`. */
export async function listDecorationCategories() {
  const snap = await getDocs(query(collection(db, 'decorationCategories'), orderBy('order')));
  return rows(snap);
}
export async function addDecorationCategory(name) {
  await addDoc(collection(db, 'decorationCategories'), { name: name.trim(), order: orderVal(), createdAt: nowMs() });
}
/** Deleting a category keeps its decorations — they just drop back to uncategorized. */
export async function deleteDecorationCategory(id) {
  const snap = await getDocs(query(collection(db, 'decorations'), where('categoryId', '==', id)));
  await Promise.all(snap.docs.map((d) => updateDoc(d.ref, { categoryId: '' })));
  await deleteDoc(doc(db, 'decorationCategories', id));
}

/* ---------------- Canvas templates (Templates tab) ---------------- */

/**
 * A ready-made canvas. `title` / `image` / `textColor` / `bgColor` are only the THUMBNAIL —
 * the 4:5 card shown in the app's Templates tab. `config` is the actual saved editor canvas
 * ({ bgColor, baseImage, layers[] }) that the app's editor rebuilds when the card is tapped.
 */
export async function listCanvasTemplates() {
  const snap = await getDocs(query(collection(db, 'canvasTemplates'), orderBy('order')));
  return rows(snap);
}
export async function addCanvasTemplate({ title, image, textColor, bgColor, config, category, tags, ratio }) {
  await addDoc(collection(db, 'canvasTemplates'), {
    title: title.trim(),
    image: image || '',
    textColor: textColor || '',
    bgColor: bgColor || '',
    config: config || null,
    // Groups templates into named rows on the app's Templates tab (blank = the "More" row).
    category: (category || '').trim(),
    // Searchable from the app's Featured search bar, alongside title and category.
    tags: (tags || []).map((t) => t.trim().toLowerCase()).filter(Boolean),
    // Canvas shape (width/height) for this template's category — see setCategoryRatio.
    ratio: Number(ratio) || 0,
    order: orderVal(),
    createdAt: nowMs(),
  });
}

/**
 * Aspect ratio is a property of the CATEGORY, not one template: a row only lines up if every
 * card in it is the same shape. It's stored on each template too (so the app can read it
 * without a second collection), which means changing it has to write to the whole category
 * at once — this is the shared primitive both the category manager below and legacy callers
 * use to do that.
 */
export async function setCategoryRatio(category, ratio) {
  const snap = await getDocs(query(collection(db, 'canvasTemplates'), where('category', '==', category)));
  await Promise.all(snap.docs.map((d) => updateDoc(d.ref, { ratio: Number(ratio) || 0 })));
  return snap.size;
}
export const updateCanvasTemplate = (id, data) => updateDoc(doc(db, 'canvasTemplates', id), data);
export const deleteCanvasTemplate = (id) => deleteDoc(doc(db, 'canvasTemplates', id));

/* ---------------- Canvas template categories (own row, own locked-in aspect ratio) ---------------- */

export async function listCanvasTemplateCategories() {
  const snap = await getDocs(query(collection(db, 'canvasTemplateCategories'), orderBy('order')));
  return rows(snap);
}

/**
 * Create a template category with its shape decided up front — mirrors the main Categories
 * manager's "type a name, hit Add" flow, plus the one extra choice that a plain category
 * doesn't need. Idempotent by name: retyping an existing category's name just returns its
 * existing id instead of creating a duplicate with a possibly-different ratio.
 *
 * Also syncs the ratio onto any templates already using this name (harmless no-op for a
 * genuinely new category, but heals older templates whose category was just a typed string
 * with no managed row behind it).
 */
export async function addCanvasTemplateCategory(name, ratio) {
  const clean = name.trim();
  const existing = (await getDocs(collection(db, 'canvasTemplateCategories'))).docs
    .find((d) => (d.data().name || '').trim().toLowerCase() === clean.toLowerCase());
  let id;
  if (existing) {
    id = existing.id;
    await updateDoc(existing.ref, { ratio: Number(ratio) || 0 });
  } else {
    const ref = await addDoc(collection(db, 'canvasTemplateCategories'), {
      name: clean,
      ratio: Number(ratio) || 0,
      order: orderVal(),
      createdAt: nowMs(),
    });
    id = ref.id;
  }
  await setCategoryRatio(clean, ratio);
  return id;
}

/** Change a managed category's shape — rewrites every template already in it too. */
export async function updateCanvasTemplateCategoryRatio(id, name, ratio) {
  await updateDoc(doc(db, 'canvasTemplateCategories', id), { ratio: Number(ratio) || 0 });
  await setCategoryRatio(name, ratio);
}

/**
 * Rename a category. Templates reference their category by NAME, not by id, so this has to
 * cascade or every template in it would be orphaned into an unmanaged category under the old
 * name. Refuses a name another category already uses, which would silently merge the two.
 */
export async function renameCanvasTemplateCategory(id, oldName, newName) {
  const clean = newName.trim();
  if (!clean) throw new Error('Category name cannot be empty.');
  if (clean === oldName) return 0;
  const clash = (await getDocs(collection(db, 'canvasTemplateCategories'))).docs
    .find((d) => d.id !== id && (d.data().name || '').trim().toLowerCase() === clean.toLowerCase());
  if (clash) throw new Error(`A category named "${clash.data().name}" already exists.`);
  await updateDoc(doc(db, 'canvasTemplateCategories', id), { name: clean });
  const snap = await getDocs(query(collection(db, 'canvasTemplates'), where('category', '==', oldName)));
  await Promise.all(snap.docs.map((d) => updateDoc(d.ref, { category: clean })));
  return snap.size;
}

/**
 * Removes the managed row only — existing templates keep their `category` string and current
 * `ratio` as-is (no cascade). The category just goes back to being an unmanaged, typed-in name
 * until someone recreates it here.
 */
export const deleteCanvasTemplateCategory = (id) => deleteDoc(doc(db, 'canvasTemplateCategories', id));

/* ---------------- Canvas themes (admin-created text/bg color pairs) ---------------- */

export async function listCanvasThemes() {
  const snap = await getDocs(query(collection(db, 'canvasThemes'), orderBy('order')));
  return rows(snap);
}
export async function addCanvasTheme({ name, textColor, bgColor }) {
  await addDoc(collection(db, 'canvasThemes'), {
    name: name.trim(),
    textColor,
    bgColor,
    order: orderVal(),
    createdAt: nowMs(),
  });
}
export const deleteCanvasTheme = (id) => deleteDoc(doc(db, 'canvasThemes', id));

/* ---------------- Category meta (description + card images) ---------------- */

export const updateCategoryMeta = (id, data) => updateDoc(doc(db, 'categories', id), data);

/* ---------------- Group meta (description + card images, same shape as category meta) ----------- */

export const updateGroupMeta = (id, data) => updateDoc(doc(db, 'groups', id), data);

/* ---------------- Analytics (counted by the app — see Analytics.kt) ---------------- */

/**
 * The app's usage counters for each day in [days] (yyyy-MM-dd strings, oldest first). Two docs
 * per day: `analyticsDaily/{day}` (totals + tab/screen/category/template/search maps) and
 * `analyticsStickers/{day}` (per-sticker `views` / `dumps` maps). A day with no activity just
 * comes back as empty objects.
 */
export async function getAnalytics(days) {
  const [daily, stickers] = await Promise.all([
    Promise.all(days.map((d) => getDoc(doc(db, 'analyticsDaily', d)))),
    Promise.all(days.map((d) => getDoc(doc(db, 'analyticsStickers', d)))),
  ]);
  return days.map((day, i) => ({
    day,
    daily: daily[i].exists() ? daily[i].data() : {},
    stickers: stickers[i].exists() ? stickers[i].data() : {},
  }));
}

let insightsCache = null; // { at, promise }

/**
 * All-time per-sticker insights — every day's `analyticsStickers` doc added up:
 * `views` (detail sheet opened) and `downloads` (sent out to WhatsApp/Telegram/share), each a
 * { stickerId: count } map. One read per recorded day, so cached for a few minutes.
 */
export function getStickerInsights() {
  if (!insightsCache || Date.now() - insightsCache.at > 5 * 60 * 1000) {
    insightsCache = {
      at: Date.now(),
      promise: getDocs(collection(db, 'analyticsStickers'))
        .then((snap) => {
          const views = {};
          const downloads = {};
          const add = (into, map) => { for (const [id, n] of Object.entries(map || {})) into[id] = (into[id] || 0) + (Number(n) || 0); };
          snap.forEach((d) => { add(views, d.data().views); add(downloads, d.data().dumps); });
          return { views, downloads };
        })
        .catch(() => ({ views: {}, downloads: {} })),
    };
  }
  return insightsCache.promise;
}

/** Every recorded day's analytics, oldest first — the Analytics page's "All time" range. */
export async function getAllAnalytics() {
  const [daily, stickers] = await Promise.all([
    getDocs(collection(db, 'analyticsDaily')),
    getDocs(collection(db, 'analyticsStickers')),
  ]);
  const byDay = {};
  daily.forEach((d) => { byDay[d.id] = { day: d.id, daily: d.data(), stickers: {} }; });
  stickers.forEach((d) => { (byDay[d.id] ||= { day: d.id, daily: {}, stickers: {} }).stickers = d.data(); });
  return Object.values(byDay).sort((a, b) => a.day.localeCompare(b.day));
}
