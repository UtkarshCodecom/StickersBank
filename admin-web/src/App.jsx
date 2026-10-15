import { useEffect, useState, useCallback, useMemo, useRef } from 'react';
import * as api from './db';
import { uploadToR2 } from './api';
import Editor from './Editor';
import AnalyticsDashboard from './Analytics';

/**
 * Tags are split on comma OR any run of whitespace — "funny cute" and "funny, cute" both become
 * two tags, never one tag with a space in it. A space inside a tag would silently break matching
 * everywhere tags are compared (search, array-contains filters, the app's tag chips) since those
 * all assume single-word tags — splitting on input is what keeps that invariant true everywhere
 * instead of relying on every caller to remember it.
 */
function splitTags(raw) {
  return raw.split(/[,\s]+/).map((t) => t.trim().toLowerCase()).filter(Boolean);
}

export default function App() {
  // Drill-down: categories → groups → collections → upload
  const [category, setCategory] = useState(null);
  const [group, setGroup] = useState(null);
  const [coll, setColl] = useState(null);
  const [showEmojis, setShowEmojis] = useState(false);
  const [showFonts, setShowFonts] = useState(false);
  const [showBanners, setShowBanners] = useState(false);
  const [showTrending, setShowTrending] = useState(false);
  const [showTags, setShowTags] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false);
  const [showPageBackgrounds, setShowPageBackgrounds] = useState(false);
  const [showDecorations, setShowDecorations] = useState(false);
  const [showAnalytics, setShowAnalytics] = useState(false);
  const [showAllTags, setShowAllTags] = useState(false);
  const [showEditor, setShowEditor] = useState(false);
  const [showOrganize, setShowOrganize] = useState(false);
  const [showBrowse, setShowBrowse] = useState(false);
  const [editingCategory, setEditingCategory] = useState(null);
  const [editingGroup, setEditingGroup] = useState(null);
  // Incrementing this key remounts NameList so it re-fetches categories after a meta save.
  const [catKey, setCatKey] = useState(0);

  const view = coll ? 'upload' : group ? 'collections' : category ? 'groups' : 'categories';

  if (showEmojis) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — emoji picker</div>
          </div>
        </header>
        <EmojiManager onClose={() => setShowEmojis(false)} />
      </div>
    );
  }

  if (showBanners) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Featured banners</div>
          </div>
        </header>
        <BannerManager onClose={() => setShowBanners(false)} />
      </div>
    );
  }

  if (showTrending) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Trending</div>
          </div>
        </header>
        <TrendingManager onClose={() => setShowTrending(false)} />
      </div>
    );
  }

  if (showTags) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Popular tags</div>
          </div>
        </header>
        <PopularTagsManager onClose={() => setShowTags(false)} />
      </div>
    );
  }

  if (showTemplates) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Canvas templates</div>
          </div>
        </header>
        <CanvasTemplateManager onClose={() => setShowTemplates(false)} />
      </div>
    );
  }

  if (showPageBackgrounds) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Page backgrounds</div>
          </div>
        </header>
        <PageBackgroundsManager onClose={() => setShowPageBackgrounds(false)} />
      </div>
    );
  }

  if (showAnalytics) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Analytics</div>
          </div>
        </header>
        <AnalyticsDashboard onClose={() => setShowAnalytics(false)} />
      </div>
    );
  }

  if (showDecorations) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Decorations</div>
          </div>
        </header>
        <DecorationsManager onClose={() => setShowDecorations(false)} />
      </div>
    );
  }

  if (showAllTags) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — All tags</div>
          </div>
        </header>
        <AllTagsManager onClose={() => setShowAllTags(false)} />
      </div>
    );
  }

  if (showFonts) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — Fonts</div>
          </div>
        </header>
        <FontManager onClose={() => setShowFonts(false)} />
      </div>
    );
  }

  if (showBrowse) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — browse &amp; move stickers</div>
          </div>
        </header>
        <BrowseMoveManager onClose={() => setShowBrowse(false)} />
      </div>
    );
  }

  if (showOrganize) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — organize imported stickers</div>
          </div>
        </header>
        <OrganizeManager onClose={() => setShowOrganize(false)} />
      </div>
    );
  }

  if (showEditor) {
    return (
      <div className="app">
        <header>
          <div className="header-title">
            <h1>STICKERS <span>BANK</span></h1>
            <div className="sub">Admin — sticker editor</div>
          </div>
        </header>
        <Editor onClose={() => setShowEditor(false)} />
      </div>
    );
  }

  return (
    <div className="app">
      <header>
        <div className="header-title">
          <h1>
            STICKERS <span>BANK</span>
          </h1>
          <div className="sub">Admin — build the catalog & upload stickers</div>
        </div>
        {/* One right-justified block that wraps onto as many rows as the width needs — in
            normal flow (not absolutely positioned) so the header's own height always accounts
            for however tall this block actually is, instead of silently overlapping whatever
            renders below it. See the .admin-nav comment in styles.css for the history here. */}
        <div className="admin-nav">
          <button className="primary" onClick={() => setShowBrowse(true)}>🔀 Browse &amp; Move</button>
          <button className="primary" onClick={() => setShowOrganize(true)}>🗃 Organize</button>
          <button className="primary" onClick={() => setShowTemplates(true)}>🎨 Canvas Templates</button>
          <button className="primary" onClick={() => setShowAllTags(true)}>🗂 All Tags</button>
          <button className="primary" onClick={() => setShowEmojis(true)}>😀 Emojis</button>
          <button className="primary" onClick={() => setShowFonts(true)}>🔤 Fonts</button>
          <button className="primary" onClick={() => setShowEditor(true)}>🎨 Editor</button>
          <button className="primary" onClick={() => setShowBanners(true)}>🖼 Banners</button>
          <button className="primary" onClick={() => setShowTrending(true)}>🔥 Trending</button>
          <button className="primary" onClick={() => setShowTags(true)}>🏷 Popular Tags</button>
          <button className="primary" onClick={() => setShowPageBackgrounds(true)}>🎨 Page Backgrounds</button>
          <button className="primary" onClick={() => setShowDecorations(true)}>🕶 Decorations</button>
          <button className="primary" onClick={() => setShowAnalytics(true)}>📊 Analytics</button>
        </div>
      </header>

      <Breadcrumb
        category={category}
        group={group}
        coll={coll}
        go={(lvl) => {
          if (lvl === 'root') { setCategory(null); setGroup(null); setColl(null); }
          if (lvl === 'category') { setGroup(null); setColl(null); }
          if (lvl === 'group') { setColl(null); }
        }}
      />

      {view === 'categories' && (
        <NameList
          key={catKey}
          title="Categories"
          hint="Main sections — e.g. Hindi, English, Telugu. Use ↑ ↓ to set the order shown in the app. ⚙ sets the card image & description."
          collName="categories"
          load={api.listCategories}
          onAdd={(name) => api.addCategory(name)}
          onDelete={api.deleteCategory}
          onOpen={setCategory}
          onEdit={setEditingCategory}
        />
      )}

      {editingCategory && (
        <CategoryMetaModal
          category={editingCategory}
          onClose={() => setEditingCategory(null)}
          onSaved={() => { setEditingCategory(null); setCatKey((k) => k + 1); }}
        />
      )}

      {editingGroup && (
        <GroupMetaModal
          group={editingGroup}
          onClose={() => setEditingGroup(null)}
          onSaved={() => setEditingGroup(null)}
        />
      )}

      {view === 'groups' && (
        <>
          <NameList
            title={`${category.name} — Types`}
            hint="e.g. Actress, Comedian. Use ↑ ↓ to set the order. ⚙ sets the card image & description."
            collName="groups"
            load={() => api.listGroups(category.id)}
            onAdd={(name) => api.addGroup(category.id, name)}
            onDelete={api.deleteGroup}
            onOpen={setGroup}
            onEdit={setEditingGroup}
          />
          <DirectStickers
            key={`cat-${category.id}`}
            title={`Stickers directly on "${category.name}"`}
            hint={`Skip Type & Name entirely — these show right under ${category.name} in the app, alongside any Types you add above.`}
            categoryId={category.id}
            autoTags={[category.name]}
          />
        </>
      )}

      {view === 'collections' && (
        <>
          <NameList
            title={`${group.name} — Names`}
            hint="e.g. Deepika, Kareena, Animations. Use ↑ ↓ to set the order."
            collName="collections"
            load={() => api.listCollections(group.id)}
            onAdd={(name, animated) => api.addCollection(category.id, group.id, name, animated)}
            onDelete={api.deleteCollection}
            onOpen={setColl}
            allowAnimated
          />
          <DirectStickers
            key={`grp-${group.id}`}
            title={`Stickers directly on "${group.name}"`}
            hint={`Skip Name entirely — these show right under ${group.name} in the app, alongside any Names you add above.`}
            categoryId={category.id}
            groupId={group.id}
            autoTags={[category.name, group.name]}
          />
        </>
      )}

      {view === 'upload' && (
        <Upload category={category} group={group} coll={coll} />
      )}
    </div>
  );
}

function BannerManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [displayMode, setDisplayMode] = useState('carousel');
  const [modeBusy, setModeBusy] = useState(false);

  const load = useCallback(async () => setItems(await api.listBanners()), []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { api.getBannerDisplayMode().then(setDisplayMode); }, []);

  function pick(video) {
    const inp = document.createElement('input');
    inp.type = 'file';
    inp.accept = video ? 'video/*' : 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0];
      if (!f) return;
      setBusy(true);
      setMsg('');
      try {
        const url = await uploadToR2(f, { collectionId: '', kind: video ? 'bannerVideo' : 'banner' });
        await api.addBanner(url, video);
        await load();
        setMsg(`✓ ${video ? 'Video' : ''} banner added — it rotates at the top of the app’s Featured tab.`);
      } catch (e) {
        setMsg(`✗ ${e.message}`);
      }
      setBusy(false);
    };
    inp.click();
  }

  async function changeMode(mode) {
    setModeBusy(true);
    try { await api.setBannerDisplayMode(mode); setDisplayMode(mode); }
    catch (e) { setMsg(`✗ ${e.message}`); }
    setModeBusy(false);
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Featured banners</h2>
          <p className="hint">
            Wide 5:1 promos that auto-rotate at the top of the app’s Featured tab. Images are cropped to 5:1;
            videos play as-is (fitted into the slot, not cropped — a roughly 5:1 clip looks best), and the
            rotation waits for a video to finish before moving on. Add as many as you like.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <div className="med-label" style={{ marginBottom: 6 }}>Rotation style</div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
        <button
          className={displayMode === 'carousel' ? 'primary' : ''}
          disabled={modeBusy}
          onClick={() => changeMode('carousel')}
        >
          Carousel (dots)
        </button>
        <button
          className={displayMode === 'fade' ? 'primary' : ''}
          disabled={modeBusy}
          onClick={() => changeMode('fade')}
        >
          Fade (no dots)
        </button>
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <button className="primary" disabled={busy} onClick={() => pick(false)}>{busy ? 'Uploading…' : '+ Add banner image'}</button>
        <button className="primary" disabled={busy} onClick={() => pick(true)}>{busy ? 'Uploading…' : '+ Add banner video'}</button>
      </div>
      {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 6 }}>{msg}</p>}

      {items.length === 0 ? (
        <p className="muted" style={{ marginTop: 12 }}>No banners yet — the Featured tab just shows categories.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10, marginTop: 12 }}>
          {items.map((b) => (
            <div key={b.id} style={{ position: 'relative' }}>
              {b.video ? (
                <video src={b.image} muted controls style={{ width: '100%', aspectRatio: '5 / 1', objectFit: 'cover', borderRadius: 10, display: 'block', background: '#000' }} />
              ) : (
                <img src={b.image} alt="" style={{ width: '100%', aspectRatio: '5 / 1', objectFit: 'cover', borderRadius: 10, display: 'block' }} />
              )}
              {b.video && <span className="muted small" style={{ position: 'absolute', top: 8, left: 8, background: 'rgba(0,0,0,0.6)', borderRadius: 6, padding: '2px 8px' }}>🎬 video</span>}
              <button
                className="del"
                title="Delete"
                style={{ position: 'absolute', top: 8, right: 8 }}
                onClick={async () => { await api.deleteBanner(b.id); load(); }}
              >
                ✕
              </button>
              {/* Tapping the banner in the app opens this. Blank = the banner isn't tappable. */}
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 6 }}>
                <span className="muted small" style={{ flexShrink: 0 }}>🔗 Link</span>
                <input
                  key={`link-${b.id}-${b.link || ''}`}
                  defaultValue={b.link || ''}
                  placeholder="https://…  (leave empty for no link)"
                  style={{ flex: 1, minWidth: 180 }}
                  onKeyDown={(e) => { if (e.key === 'Enter') e.target.blur(); if (e.key === 'Escape') { e.target.value = b.link || ''; e.target.blur(); } }}
                  onBlur={async (e) => {
                    const next = e.target.value.trim();
                    if (next === (b.link || '')) return;
                    try { await api.setBannerLink(b.id, next); await load(); setMsg('✓ Link saved.'); }
                    catch (err) { setMsg(`✗ ${err.message}`); e.target.value = b.link || ''; }
                  }}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

/** Transparent overlay art (glasses, a cap, etc.) for the app editor's Decorator, filed under
 * admin-managed categories that show as toggles along the bottom of it. Upload a transparent
 * PNG/WebP; anything with a solid background just won't look like an overlay on a photo. */
function DecorationsManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [cats, setCats] = useState([]);
  const [filter, setFilter] = useState('all'); // 'all' | '' (uncategorized) | a category id
  const [newCat, setNewCat] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => {
    const [d, c] = await Promise.all([api.listDecorations(), api.listDecorationCategories()]);
    setItems(d);
    setCats(c);
  }, []);
  useEffect(() => { load().catch((e) => setMsg(`✗ ${e.message}`)); }, [load]);

  // A categoryId pointing at a category that no longer exists counts as uncategorized — same as
  // the app, which only ever shows it under "All".
  const catOf = (d) => (d.categoryId && cats.some((c) => c.id === d.categoryId) ? d.categoryId : '');
  const count = (id) => items.filter((d) => catOf(d) === id).length;
  const catName = (id) => cats.find((c) => c.id === id)?.name;
  const shown = filter === 'all' ? items : items.filter((d) => catOf(d) === filter);
  // Uploads land in whichever category is being viewed; from All / Uncategorized they're unfiled.
  const uploadTarget = filter && filter !== 'all' && catName(filter) ? filter : '';

  function pick() {
    const inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*'; inp.multiple = true;
    inp.onchange = async () => {
      const files = [...inp.files];
      if (!files.length) return;
      setBusy(true); setMsg('');
      try {
        for (const f of files) {
          const url = await uploadToR2(f, { collectionId: '', kind: 'decoration' });
          await api.addDecoration(url, uploadTarget);
        }
        await load();
        setMsg(`✓ ${files.length} decoration${files.length > 1 ? 's' : ''} added${uploadTarget ? ` to ${catName(uploadTarget)}` : ''}.`);
      } catch (e) { setMsg(`✗ ${e.message}`); }
      setBusy(false);
    };
    inp.click();
  }

  async function addCat(e) {
    e.preventDefault();
    if (!newCat.trim()) return;
    try { await api.addDecorationCategory(newCat); setNewCat(''); await load(); }
    catch (err) { setMsg(`✗ ${err.message}`); }
  }
  async function renameCat(c) {
    const next = window.prompt('Rename category to:', c.name);
    if (!next || !next.trim() || next.trim() === c.name) return;
    try { await api.updateName('decorationCategories', c.id, next.trim()); await load(); }
    catch (err) { setMsg(`✗ ${err.message}`); }
  }
  async function deleteCat(c) {
    if (!confirm(`Delete category "${c.name}"? Its ${count(c.id)} decoration(s) stay — they just become uncategorized.`)) return;
    try { await api.deleteDecorationCategory(c.id); if (filter === c.id) setFilter('all'); await load(); }
    catch (err) { setMsg(`✗ ${err.message}`); }
  }
  async function moveCat(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= cats.length) return;
    const arr = [...cats];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    setCats(arr); // optimistic
    try { await api.saveOrder('decorationCategories', arr); }
    catch (err) { setMsg(`✗ ${err.message}`); load(); }
  }
  async function assign(d, categoryId) {
    setItems((all) => all.map((x) => (x.id === d.id ? { ...x, categoryId } : x))); // optimistic
    try { await api.setDecorationCategory(d.id, categoryId); }
    catch (err) { setMsg(`✗ ${err.message}`); load(); }
  }

  const filters = [
    ['all', `All · ${items.length}`],
    ...cats.map((c) => [c.id, `${c.name} · ${count(c.id)}`]),
    ['', `Uncategorized · ${count('')}`],
  ];

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Decorations</h2>
          <p className="hint">
            Transparent PNG/WebP overlays (glasses, a cap, a mustache…) users can drop straight
            onto a photo from the editor's Decorator. Use a transparent background — a solid
            one defeats the point of an overlay.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <h3 style={{ margin: '4px 0' }}>Categories</h3>
      <p className="hint">
        Shown as toggles along the bottom of the app's Decorator, after "All", in this order (▲▼).
        A category only shows in the app once it holds at least one decoration. Click a name to
        view it and upload straight into it.
      </p>
      <form className="addrow" onSubmit={addCat}>
        <input value={newCat} onChange={(e) => setNewCat(e.target.value)} placeholder="New category (e.g. Glasses)" />
        <button className="primary" disabled={!newCat.trim()}>+ Add</button>
      </form>
      {cats.length > 0 && (
        <ul className="list">
          {cats.map((c, i) => (
            <li key={c.id}>
              <div className="reorder">
                <button className="mv" title="Move up" disabled={i === 0} onClick={() => moveCat(i, -1)}>▲</button>
                <button className="mv" title="Move down" disabled={i === cats.length - 1} onClick={() => moveCat(i, 1)}>▼</button>
              </div>
              <button className="open" onClick={() => setFilter(c.id)} style={filter === c.id ? { fontWeight: 800 } : undefined}>
                {c.name} <span className="muted small">· {count(c.id)}</span>
              </button>
              <button className="icon" title="Rename" onClick={() => renameCat(c)}>✎</button>
              <button className="del" title="Delete" onClick={() => deleteCat(c)}>✕</button>
            </li>
          ))}
        </ul>
      )}

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, margin: '16px 0 10px' }}>
        {filters.map(([id, label]) => (
          <button key={id || '_none'} className={filter === id ? 'primary' : ''} onClick={() => setFilter(id)}>{label}</button>
        ))}
      </div>

      <button className="primary" disabled={busy} onClick={pick}>
        {busy ? 'Uploading…' : uploadTarget ? `+ Upload to ${catName(uploadTarget)}` : '+ Upload decoration'}
      </button>
      {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 6 }}>{msg}</p>}

      {shown.length === 0 ? (
        <p className="muted" style={{ marginTop: 12 }}>None here yet — upload one above.</p>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, marginTop: 12 }}>
          {shown.map((d) => (
            <div key={d.id} style={{ position: 'relative', width: 84 }}>
              <div
                style={{
                  width: 84, height: 84, borderRadius: 10, overflow: 'hidden',
                  backgroundColor: '#2a2a30',
                  backgroundImage: 'repeating-conic-gradient(#3a3a40 0% 25%, #2a2a30 0% 50%)',
                  backgroundSize: '12px 12px',
                }}
              >
                <img src={d.image} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain', display: 'block' }} />
              </div>
              <select
                value={catOf(d)}
                onChange={(e) => assign(d, e.target.value)}
                title="Category"
                style={{ width: 84, marginTop: 4, fontSize: 11, padding: '2px 0' }}
              >
                <option value="">Uncategorized</option>
                {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
              <button
                className="del"
                title="Delete"
                style={{ position: 'absolute', top: -6, right: -6, width: 22, height: 22, borderRadius: '50%', padding: 0, lineHeight: '22px' }}
                onClick={async () => { await api.deleteDecoration(d.id); load(); }}
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

const PAGE_LABELS = {
  featured: 'Featured',
  templates: 'Templates',
  newArrival: 'New Arrival',
  bank: 'Bank',
  favorites: 'Favorites',
  trendingAll: 'Trending — see all',
  trending: 'Trending — inside a card',
};

/** A solid color and/or image behind a whole tab, in place of the app's plain flat background. */
function PageBackgroundsManager({ onClose }) {
  const [data, setData] = useState(null); // { [key]: {color, image} }
  const [busyKey, setBusyKey] = useState('');
  const [msg, setMsg] = useState('');

  useEffect(() => { api.getPageBackgrounds().then(setData); }, []);

  async function save(key, next) {
    setBusyKey(key);
    setMsg('');
    try {
      await api.setPageBackground(key, next);
      setData((d) => ({ ...d, [key]: next }));
    } catch (e) {
      setMsg(`✗ ${e.message}`);
    }
    setBusyKey('');
  }

  function pickImage(key, current) {
    const inp = document.createElement('input');
    inp.type = 'file';
    inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0];
      if (!f) return;
      setBusyKey(key);
      setMsg('');
      try {
        const url = await uploadToR2(f, { collectionId: '', kind: 'pageBackground' });
        await save(key, { ...current, image: url });
      } catch (e) {
        setMsg(`✗ ${e.message}`);
        setBusyKey('');
      }
    };
    inp.click();
  }

  if (!data) return <section className="card"><p className="hint">Loading…</p></section>;

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Page backgrounds</h2>
          <p className="hint">
            A solid color and/or an image behind a whole tab, instead of the app's plain flat
            background. The image sits on top of the color (the color shows through as a fallback
            edge), and either can be left unset. Category pages get their own background from
            that category's ⚙ card editor, not from here.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      {msg && <p className="error" style={{ marginBottom: 10 }}>{msg}</p>}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {api.PAGE_BACKGROUND_KEYS.map((key) => {
          const entry = data[key] || { color: '', image: '' };
          const busy = busyKey === key;
          return (
            <div key={key} style={{ display: 'flex', gap: 14, alignItems: 'center', padding: 12, background: '#1c1c22', borderRadius: 10 }}>
              {entry.image ? (
                <img src={entry.image} alt="" style={{ width: 64, height: 64, objectFit: 'cover', borderRadius: 8, background: entry.color || '#222', flexShrink: 0 }} />
              ) : (
                <div style={{ width: 64, height: 64, borderRadius: 8, background: entry.color || '#2a2a30', flexShrink: 0 }} />
              )}
              <div style={{ flex: 1, minWidth: 220 }}>
                <div className="med-label" style={{ marginBottom: 6 }}>{PAGE_LABELS[key]}</div>
                <ColorSwatchPicker value={entry.color} onChange={(color) => save(key, { ...entry, color })} />
                <div style={{ display: 'flex', gap: 8, marginTop: 8, alignItems: 'center' }}>
                  <button className="primary" disabled={busy} onClick={() => pickImage(key, entry)}>
                    {busy ? 'Uploading…' : entry.image ? 'Replace image' : '+ Add image'}
                  </button>
                  {entry.image && <button className="link" disabled={busy} onClick={() => save(key, { ...entry, image: '' })}>Remove image</button>}
                  {(entry.color || entry.image) && (
                    <button className="link" disabled={busy} onClick={() => save(key, { color: '', image: '' })}>Clear all</button>
                  )}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}

// Panel color swatches for the trending card's title/count strip (admin-picked, not the image).
const SWATCHES = ['#7C3AED', '#FF8A00', '#14B8A6', '#FF4D8D', '#3B82F6', '#00C853', '#EF4444', '#FFC400'];

const HEX_RE = /^#[0-9a-fA-F]{6}$/;

function ColorSwatchPicker({ value, onChange }) {
  const [hexInput, setHexInput] = useState(value || '');
  useEffect(() => { setHexInput(value || ''); }, [value]);

  function commitHex(raw) {
    const hex = raw.trim().startsWith('#') ? raw.trim() : `#${raw.trim()}`;
    if (HEX_RE.test(hex)) onChange(hex);
  }

  const isCustom = value && !SWATCHES.includes(value);

  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
      {SWATCHES.map((c) => (
        <button
          key={c}
          type="button"
          onClick={() => onChange(c)}
          title={c}
          style={{
            width: 20, height: 20, borderRadius: '50%', background: c, cursor: 'pointer', padding: 0,
            border: value === c ? '2px solid #fff' : '2px solid transparent', outline: value === c ? '1px solid ' + c : 'none',
          }}
        />
      ))}
      {/* Any color, via the OS color picker — shown as a wheel until a custom color is chosen. */}
      <label
        title="Pick any color"
        style={{
          width: 20, height: 20, borderRadius: '50%', cursor: 'pointer', position: 'relative', overflow: 'hidden', display: 'inline-block',
          border: isCustom ? '2px solid #fff' : '2px solid #555',
          background: isCustom ? value : 'conic-gradient(red, yellow, lime, cyan, blue, magenta, red)',
        }}
      >
        <input
          type="color"
          value={HEX_RE.test(value || '') ? value : '#7c3aed'}
          onChange={(e) => onChange(e.target.value)}
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer', border: 'none', padding: 0 }}
        />
      </label>
      {/* Or type a hex code directly. */}
      <input
        type="text"
        value={hexInput}
        onChange={(e) => { setHexInput(e.target.value); commitHex(e.target.value); }}
        onBlur={() => { const h = hexInput.trim().startsWith('#') ? hexInput.trim() : `#${hexInput.trim()}`; if (!HEX_RE.test(h)) setHexInput(value || ''); }}
        placeholder="#RRGGBB"
        style={{ width: 84, fontSize: 12, padding: '3px 6px' }}
      />
    </div>
  );
}

// The same fixed theme palette the app itself uses for tag pills (see TagPalette in
// FeaturedScreen.kt) — picking one here just pins a tag to that theme instead of letting the
// app's hash-based fallback choose it.
const TAG_THEMES = [
  { name: 'Purple', color: '#9B5DE5' },
  { name: 'Green', color: '#00C853' },
  { name: 'Gold', color: '#FFC400' },
  { name: 'Pink', color: '#FF4D8D' },
  { name: 'Blue', color: '#3B82F6' },
  { name: 'Orange', color: '#FF8A00' },
  { name: 'Teal', color: '#14B8A6' },
  { name: 'Red', color: '#EF4444' },
];

function TagThemePicker({ value, onChange }) {
  // The browser's picker reports every drag step; hold the colour locally and only pass it on
  // once dragging pauses, so a tag's colour is saved once instead of dozens of times.
  const [draft, setDraft] = useState(null);
  const timer = useRef(null);
  useEffect(() => () => clearTimeout(timer.current), []);
  const shown = draft ?? value;
  // Anything not one of the presets came from the free colour picker below.
  const isCustom = !!shown && !TAG_THEMES.some((t) => t.color.toLowerCase() === shown.toLowerCase());
  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center', flexWrap: 'wrap' }}>
      {TAG_THEMES.map((t) => (
        <button
          key={t.color}
          type="button"
          onClick={() => onChange(t.color)}
          title={t.name}
          style={{
            width: 22, height: 22, borderRadius: '50%', background: t.color, cursor: 'pointer', padding: 0,
            border: value === t.color ? '2px solid #fff' : '2px solid transparent', outline: value === t.color ? '1px solid ' + t.color : 'none',
          }}
        />
      ))}
      {/* Any colour at all: a rainbow swatch until one's picked, then that colour. The real
          <input type="color"> sits invisibly on top so a click opens the browser's picker. */}
      <label
        title="Pick any colour"
        style={{
          width: 22, height: 22, borderRadius: '50%', cursor: 'pointer', position: 'relative', overflow: 'hidden', display: 'inline-block',
          background: isCustom ? shown : 'conic-gradient(#ef4444, #f59e0b, #22c55e, #3b82f6, #a855f7, #ef4444)',
          border: isCustom ? '2px solid #fff' : '2px solid transparent', outline: isCustom ? '1px solid ' + shown : 'none',
        }}
      >
        <input
          type="color"
          value={isCustom ? shown : '#9b5de5'}
          onChange={(e) => {
            const c = e.target.value;
            setDraft(c);
            clearTimeout(timer.current);
            timer.current = setTimeout(() => { setDraft(null); onChange(c); }, 400);
          }}
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', opacity: 0, cursor: 'pointer', border: 'none', padding: 0 }}
        />
      </label>
    </div>
  );
}

function TrendingManager({ onClose }) {
  const [items, setItems] = useState([]); // currently-trending cards, in order
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');

  // Create/edit form state — editingId set means the form below is editing that card
  // in place instead of building a new one.
  const [editingId, setEditingId] = useState(null);
  const [title, setTitle] = useState('');
  const [image, setImage] = useState('');
  const [imageBusy, setImageBusy] = useState(false);
  const [badge, setBadge] = useState('none');
  const [bgColor, setBgColor] = useState(SWATCHES[0]);
  const [selected, setSelected] = useState(new Set());
  const [creating, setCreating] = useState(false);
  const [formMsg, setFormMsg] = useState('');
  const formRef = useRef(null);

  // Sticker picker
  const [allStickers, setAllStickers] = useState([]);
  const [pickerQuery, setPickerQuery] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try { setItems(await api.listTrending()); }
    catch (e) { setError(e.message); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);
  useEffect(() => { api.listAllStickers().then(setAllStickers); }, []);

  const filteredStickers = pickerQuery.trim()
    ? allStickers.filter((s) => (s.tags || []).some((t) => t.includes(pickerQuery.trim().toLowerCase())))
    : allStickers;

  function toggleSticker(id) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  function pickImage() {
    const inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0]; if (!f) return;
      setImageBusy(true); setFormMsg('');
      try { setImage(await uploadToR2(f, { collectionId: '', kind: 'trendingImage' })); }
      catch (e) { setFormMsg(`✗ ${e.message}`); }
      setImageBusy(false);
    };
    inp.click();
  }

  function resetForm() {
    setEditingId(null);
    setTitle(''); setImage(''); setBadge('none'); setBgColor(SWATCHES[0]); setSelected(new Set()); setPickerQuery('');
  }

  function startEdit(t) {
    setEditingId(t.id);
    setTitle(t.title || '');
    setImage(t.image || '');
    setBadge(t.badge || 'none');
    setBgColor(t.bgColor || SWATCHES[0]);
    setSelected(new Set(t.stickerIds || []));
    setPickerQuery('');
    setFormMsg('');
    formRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  async function saveCard() {
    if (!title.trim() || selected.size === 0) return;
    setCreating(true); setFormMsg('');
    try {
      const data = { title, image, badge, stickerIds: [...selected], bgColor };
      if (editingId) await api.updateTrendingCard(editingId, data);
      else await api.addTrendingCard(data);
      resetForm();
      await load();
      setFormMsg(editingId ? '✓ Trending card updated.' : '✓ Trending card created.');
    } catch (e) { setFormMsg(`✗ ${e.message}`); }
    setCreating(false);
  }

  async function removeOne(t) {
    setBusyId(t.id);
    try { await api.deleteTrending(t.id); await load(); }
    catch (e) { setError(e.message); }
    setBusyId('');
  }

  async function move(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= items.length) return;
    const arr = [...items];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    setItems(arr); // optimistic
    try { await api.saveOrder('trending', arr); }
    catch (e) { setError(e.message); load(); }
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Trending</h2>
          <p className="hint">Create a card with its own title, portrait (4:5) image, panel color, and a hand-picked set of stickers — shown on Featured. Tapping it opens just those stickers.</p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>
      {error && <div className="error">{error}</div>}

      <h3 className="section">Currently trending ({items.length})</h3>
      {items.length === 0 ? (
        <p className="muted">Nothing created yet — build one below.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 18 }}>
          {items.map((t, i) => (
            <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#1c1c20', borderRadius: 10, padding: '9px 12px' }}>
              <div className="reorder">
                <button className="mv" title="Show earlier" disabled={i === 0} onClick={() => move(i, -1)}>▲</button>
                <button className="mv" title="Show later" disabled={i === items.length - 1} onClick={() => move(i, 1)}>▼</button>
              </div>
              {t.image ? (
                <img src={t.image} alt="" style={{ width: 28, height: 35, objectFit: 'cover', borderRadius: 5 }} />
              ) : (
                <div style={{ width: 28, height: 35, borderRadius: 5, background: '#2a2a30' }} />
              )}
              <strong>{t.title}</strong>
              <span className="muted small">{(t.stickerIds || []).length} stickers</span>
              <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginLeft: 'auto' }}>
                <ColorSwatchPicker
                  value={t.bgColor || ''}
                  onChange={async (c) => { await api.updateTrendingCard(t.id, { bgColor: c }); load(); }}
                />
                <select
                  value={t.badge || 'none'}
                  onChange={async (e) => { await api.setTrendingBadge(t.id, e.target.value); load(); }}
                >
                  <option value="none">No badge</option><option value="new">NEW</option><option value="hot">HOT</option>
                </select>
                <button className="link" onClick={() => startEdit(t)}>✎ Edit</button>
                {/* Backdrop for the page this card opens into — set per card, so every
                    trending page can have its own artwork instead of sharing one. */}
                <TrendingPageBg card={t} onSaved={load} />
                <button className="del" disabled={busyId === t.id} onClick={() => removeOne(t)}>✕</button>
              </div>
            </div>
          ))}
        </div>
      )}

      <h3 className="section" ref={formRef}>{editingId ? `Editing "${title}"` : 'Create a trending card'}</h3>
      {editingId && <button className="link" style={{ marginBottom: 10 }} onClick={resetForm}>✕ Cancel edit, start a new card</button>}
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
            {image ? (
              <img src={image} alt="" style={{ width: 78, height: 98, objectFit: 'cover', borderRadius: 10 }} />
            ) : (
              <div style={{ width: 78, height: 98, borderRadius: 10, background: '#1c1c20', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#888', textAlign: 'center', padding: 4 }}>
                4:5 image
              </div>
            )}
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <button className="primary" disabled={imageBusy} onClick={pickImage}>{imageBusy ? 'Uploading…' : image ? 'Replace image' : '+ Upload image'}</button>
              <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Card title (e.g. Marvel)" />
              <select value={badge} onChange={(e) => setBadge(e.target.value)}>
                <option value="none">No badge</option><option value="new">NEW</option><option value="hot">HOT</option>
              </select>
              <div>
                <p className="muted small" style={{ margin: '0 0 4px' }}>Panel color (behind title/count)</p>
                <ColorSwatchPicker value={bgColor} onChange={setBgColor} />
              </div>
            </div>
          </div>
        </div>
      </div>

      <p className="muted small" style={{ marginTop: 12 }}>Pick which stickers this card opens — {selected.size} selected</p>
      <input value={pickerQuery} onChange={(e) => setPickerQuery(e.target.value)} placeholder="Filter stickers by tag…" style={{ width: '100%', marginBottom: 8 }} />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(70px, 1fr))', gap: 8, maxHeight: 320, overflowY: 'auto', background: '#111114', borderRadius: 10, padding: 8 }}>
        {filteredStickers.map((s) => {
          const isSel = selected.has(s.id);
          return (
            <div
              key={s.id}
              onClick={() => toggleSticker(s.id)}
              style={{
                position: 'relative', aspectRatio: 1, borderRadius: 8, overflow: 'hidden', cursor: 'pointer',
                background: '#1c1c20', outline: isSel ? '2px solid var(--gold)' : 'none',
              }}
            >
              {s.imageUrl?.endsWith('.mp4') ? (
                <video src={s.imageUrl} muted style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
              ) : (
                <img src={s.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
              )}
              {isSel && (
                <div style={{ position: 'absolute', inset: 0, background: 'rgba(245,179,1,0.28)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, color: '#fff' }}>✓</div>
              )}
            </div>
          );
        })}
      </div>

      {formMsg && <p className={formMsg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 8 }}>{formMsg}</p>}
      <button className="primary" style={{ marginTop: 12 }} disabled={!title.trim() || selected.size === 0 || creating} onClick={saveCard}>
        {creating
          ? (editingId ? 'Saving…' : 'Creating…')
          : editingId
            ? `Save changes (${selected.size} sticker${selected.size === 1 ? '' : 's'})`
            : `Create trending card (${selected.size} sticker${selected.size === 1 ? '' : 's'})`}
      </button>
    </section>
  );
}

// Built-in canvas themes: a light background + a matching text color, like the reference art.
// Admins can add their own on top of these (stored in the `canvasThemes` collection).
const BUILTIN_CANVAS_THEMES = [
  { name: 'Purple', bgColor: '#EDE3FF', textColor: '#7C3AED' },
  { name: 'Sunshine', bgColor: '#FFF3CC', textColor: '#B45309' },
  { name: 'Sky', bgColor: '#DCEBFF', textColor: '#1D4ED8' },
  { name: 'Blush', bgColor: '#FFE1EC', textColor: '#DB2777' },
  { name: 'Mint', bgColor: '#D8F5E6', textColor: '#047857' },
  { name: 'Peach', bgColor: '#FFE6D5', textColor: '#C2410C' },
];

function ThemeSwatch({ theme, selected, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title={theme.name}
      style={{
        display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', borderRadius: 10,
        background: theme.bgColor, color: theme.textColor, fontWeight: 800, fontSize: 11,
        padding: '7px 11px', border: selected ? '2px solid #fff' : '2px solid transparent',
      }}
    >
      {theme.name}
    </button>
  );
}

/** Canvas shapes offered per template category (width / height). Chosen once, when the
 * category is created — every template placed in it shares this shape. */
const CANVAS_RATIOS = [
  { label: 'Default — 3.2:5', value: 0 },
  { label: 'Square — 1:1', value: 1 },
  { label: 'Portrait — 4:5', value: 0.8 },
  { label: 'Portrait — 3:4', value: 0.75 },
  { label: 'Portrait — 2:3', value: 0.6667 },
  { label: 'Portrait — 1:2', value: 0.5 },
  { label: 'Story / Reel — 9:16', value: 0.5625 },
  { label: 'Landscape — 16:9', value: 1.7778 },
  { label: 'Landscape — 4:3', value: 1.3333 },
  { label: 'Landscape — 3:2', value: 1.5 },
  { label: 'Landscape — 2:1', value: 2 },
  { label: 'Wide — 21:9', value: 2.3333 },
];

// Sentinel for the "＋ New category…" option in the category <select> below.
const NEW = '__NEW__';

function CanvasTemplateManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [themes, setThemes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState('');
  const [error, setError] = useState('');

  // Thumbnail form — the 4:5 card in the app's Templates tab (NOT the canvas itself).
  const [title, setTitle] = useState('');
  const [image, setImage] = useState('');
  const [imageBusy, setImageBusy] = useState(false);
  const [theme, setTheme] = useState(BUILTIN_CANVAS_THEMES[0]);
  const [formMsg, setFormMsg] = useState('');
  // Once the thumbnail is filled in, the admin builds the actual canvas in the editor.
  const [category, setCategory] = useState('');
  const [tplTags, setTplTags] = useState('');
  const [buildFor, setBuildFor] = useState(null);

  // Template categories — each one is created with its shape decided up front (see
  // CANVAS_RATIOS), the same way the main Categories manager works plus that one extra choice.
  const [tplCats, setTplCats] = useState([]); // [{id, name, ratio}]
  const [catBusy, setCatBusy] = useState('');
  const [creatingCat, setCreatingCat] = useState(false);
  const [newCatName, setNewCatName] = useState('');
  const [newCatRatio, setNewCatRatio] = useState(0);

  // New-theme form
  const [themeName, setThemeName] = useState('');
  const [themeBg, setThemeBg] = useState('#EDE3FF');
  const [themeFg, setThemeFg] = useState('#7C3AED');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [t, th, cats] = await Promise.all([
        api.listCanvasTemplates(), api.listCanvasThemes(), api.listCanvasTemplateCategories(),
      ]);
      setItems(t);
      setThemes(th);
      // Merge in any category name already used by a template but with no managed row yet
      // (e.g. templates made before this screen existed) — so it stays pickable and its
      // existing ratio is visible, without silently dropping it from the list.
      const managedNames = new Set(cats.map((c) => c.name.toLowerCase()));
      const unmanaged = [...new Set(t.map((x) => (x.category || '').trim()).filter(Boolean))]
        .filter((n) => !managedNames.has(n.toLowerCase()))
        .map((n) => ({ id: null, name: n, ratio: t.find((x) => (x.category || '').trim() === n)?.ratio || 0 }));
      setTplCats([...cats, ...unmanaged]);
    } catch (e) { setError(e.message); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  const allThemes = [...BUILTIN_CANVAS_THEMES, ...themes];

  function pickImage() {
    const inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0]; if (!f) return;
      setImageBusy(true); setFormMsg('');
      try { setImage(await uploadToR2(f, { collectionId: '', kind: 'canvasTemplate' })); }
      catch (e) { setFormMsg(`✗ ${e.message}`); }
      setImageBusy(false);
    };
    inp.click();
  }

  function openBuilder() {
    if (!title.trim()) return;
    setFormMsg('');
    setBuildFor({
      title, image, category,
      tags: splitTags(tplTags),
      // Sourced from the category itself (not scanned off existing templates) — the FIRST
      // template in a brand-new category gets its ratio correctly this way; scanning would've
      // found nothing yet and silently fallen back to the default shape.
      ratio: tplCats.find((c) => c.name === category)?.ratio || 0,
      textColor: theme.textColor, bgColor: theme.bgColor,
    });
  }

  async function onTemplateSaved(wasEdit) {
    setBuildFor(null);
    // Only the "new template" form gets cleared — editing an existing one never filled it in.
    if (!wasEdit) {
      setTitle(''); setImage(''); setCategory(''); setTplTags(''); setTheme(BUILTIN_CANVAS_THEMES[0]);
    }
    await load();
    setFormMsg(wasEdit ? '✓ Template updated.' : '✓ Template saved.');
  }

  async function createTheme() {
    if (!themeName.trim()) return;
    try {
      await api.addCanvasTheme({ name: themeName, textColor: themeFg, bgColor: themeBg });
      setThemeName('');
      await load();
    } catch (e) { setError(e.message); }
  }

  async function removeOne(t) {
    setBusyId(t.id);
    try { await api.deleteCanvasTemplate(t.id); await load(); }
    catch (e) { setError(e.message); }
    setBusyId('');
  }

  async function move(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= items.length) return;
    const arr = [...items];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    setItems(arr); // optimistic
    try { await api.saveOrder('canvasTemplates', arr); }
    catch (e) { setError(e.message); load(); }
  }

  // Step 2: build the actual canvas in the editor, then save it as this template.
  if (buildFor) {
    return (
      <Editor
        template={buildFor}
        onTemplateSaved={onTemplateSaved}
        onClose={() => setBuildFor(null)}
      />
    );
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Canvas templates</h2>
          <p className="hint">
            Two steps: fill in the <strong>thumbnail</strong> (the 4:5 card users see in the
            Templates tab), then build the actual <strong>canvas</strong> in the editor and save it.
            Tapping the card in the app reopens that canvas so the user can swap the picture and
            the words.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>
      {error && <div className="error">{error}</div>}

      {/* Template categories — created the same way as a real category (name + Add), plus the
          one extra choice a template category needs: its shape, decided up front. Every
          template placed in a category shares that shape, so a row always lines up. */}
      <div style={{ background: '#1c1c20', borderRadius: 10, padding: 12, marginTop: 12 }}>
        <div className="muted small" style={{ marginBottom: 8 }}>
          Template categories — each row on the Templates tab is one of these, all cards in it the same shape
        </div>
        {tplCats.length === 0 ? (
          <p className="muted small" style={{ margin: '0 0 8px' }}>None yet — create one below.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 10 }}>
            {tplCats.map((c) => (
              <div key={c.id || c.name} style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                {c.id ? (
                  // Editable in place. Commits on blur/Enter and cascades onto every template in
                  // this category (they reference it by name) — see renameCanvasTemplateCategory.
                  <input
                    defaultValue={c.name}
                    key={`name-${c.id}-${c.name}`}
                    disabled={catBusy === c.name}
                    title="Rename this category — templates in it follow the new name"
                    style={{ minWidth: 130, fontWeight: 700 }}
                    onKeyDown={(e) => { if (e.key === 'Enter') e.target.blur(); if (e.key === 'Escape') { e.target.value = c.name; e.target.blur(); } }}
                    onBlur={async (e) => {
                      const next = e.target.value.trim();
                      if (!next || next === c.name) { e.target.value = c.name; return; }
                      setCatBusy(c.name);
                      try {
                        await api.renameCanvasTemplateCategory(c.id, c.name, next);
                        // The picker above may be pointing at the old name.
                        setCategory((cur) => (cur === c.name ? next : cur));
                        await load();
                      } catch (err) { setError(err.message); e.target.value = c.name; }
                      setCatBusy('');
                    }}
                  />
                ) : (
                  <strong style={{ minWidth: 130 }}>{c.name}</strong>
                )}
                {!c.id && <span className="muted small">unmanaged — pick a shape to adopt it</span>}
                <select
                  value={c.ratio || 0}
                  disabled={catBusy === c.name}
                  style={{ marginLeft: 'auto' }}
                  onChange={async (e) => {
                    setCatBusy(c.name);
                    try {
                      if (c.id) await api.updateCanvasTemplateCategoryRatio(c.id, c.name, e.target.value);
                      else await api.addCanvasTemplateCategory(c.name, e.target.value);
                      await load();
                    } catch (err) { setError(err.message); }
                    setCatBusy('');
                  }}
                >
                  {CANVAS_RATIOS.map((r) => <option key={r.label} value={r.value}>{r.label}</option>)}
                </select>
                {c.id ? (
                  <button
                    className="del"
                    disabled={catBusy === c.name}
                    title="Remove this managed category (templates keep their name & shape)"
                    onClick={async () => {
                      setCatBusy(c.name);
                      try { await api.deleteCanvasTemplateCategory(c.id); await load(); }
                      catch (err) { setError(err.message); }
                      setCatBusy('');
                    }}
                  >
                    ✕
                  </button>
                ) : null}
              </div>
            ))}
          </div>
        )}
        {creatingCat ? (
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <input
              autoFocus
              value={newCatName}
              onChange={(e) => setNewCatName(e.target.value)}
              placeholder="Category name (e.g. Birthday)"
              style={{ minWidth: 200 }}
            />
            <select value={newCatRatio} onChange={(e) => setNewCatRatio(e.target.value)}>
              {CANVAS_RATIOS.map((r) => <option key={r.label} value={r.value}>{r.label}</option>)}
            </select>
            <button
              className="primary"
              disabled={!newCatName.trim() || catBusy === '__new__'}
              onClick={async () => {
                setCatBusy('__new__');
                try {
                  await api.addCanvasTemplateCategory(newCatName, newCatRatio);
                  setCategory(newCatName.trim());
                  setCreatingCat(false); setNewCatName(''); setNewCatRatio(0);
                  await load();
                } catch (err) { setError(err.message); }
                setCatBusy('');
              }}
            >
              Create
            </button>
            <button className="link" onClick={() => { setCreatingCat(false); setNewCatName(''); }}>Cancel</button>
          </div>
        ) : (
          <button className="link" onClick={() => setCreatingCat(true)}>+ New template category…</button>
        )}
      </div>

      <h3 className="section">Templates ({items.length})</h3>
      {loading ? (
        <p className="muted">Loading…</p>
      ) : items.length === 0 ? (
        <p className="muted">Nothing created yet — build one below.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 18 }}>
          {items.map((t, i) => (
            <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#1c1c20', borderRadius: 10, padding: '9px 12px' }}>
              <div className="reorder">
                <button className="mv" title="Show earlier" disabled={i === 0} onClick={() => move(i, -1)}>▲</button>
                <button className="mv" title="Show later" disabled={i === items.length - 1} onClick={() => move(i, 1)}>▼</button>
              </div>
              {t.image ? (
                <img src={t.image} alt="" style={{ width: 32, height: 40, objectFit: 'cover', borderRadius: 5 }} />
              ) : (
                <div style={{ width: 32, height: 40, borderRadius: 5, background: t.bgColor || '#2a2a30' }} />
              )}
              <div>
                <strong>{t.title}</strong>
                {t.category ? <span className="muted small"> · {t.category}</span> : null}
                {(t.tags || []).length ? <span className="muted small"> · {(t.tags || []).join(', ')}</span> : null}
                <div className="muted small">
                  {t.config?.layers?.length
                    ? `${t.config.layers.length} layer${t.config.layers.length === 1 ? '' : 's'}`
                    : 'no canvas saved'}
                </div>
              </div>
              <span
                style={{
                  marginLeft: 'auto', borderRadius: 8, fontSize: 11, fontWeight: 800, padding: '5px 10px',
                  background: t.bgColor || '#2a2a30', color: t.textColor || '#fff',
                }}
              >
                Aa
              </span>
              <button
                className="icon"
                title="Reopen this template in the editor and change it"
                disabled={busyId === t.id}
                onClick={() => setBuildFor({
                  id: t.id,
                  title: t.title, image: t.image, category: t.category || '',
                  tags: t.tags || [], ratio: t.ratio || 0,
                  textColor: t.textColor, bgColor: t.bgColor,
                  config: t.config || null,
                })}
              >
                ✎
              </button>
              <button className="del" disabled={busyId === t.id} onClick={() => removeOne(t)}>✕</button>
            </div>
          ))}
        </div>
      )}

      <h3 className="section">New template — step 1: the thumbnail card</h3>
      {(() => {
        // Same shape the card actually renders as in the app — the category's own ratio
        // (width/height), falling back to the same default the "Default" ratio option means
        // elsewhere (DEFAULT_TEMPLATE_RATIO = 3.2/5 on the app side). Longer side pinned to
        // 120px so the preview stays a similar overall size across very wide/tall ratios.
        const previewRatio = tplCats.find((c) => c.name === category)?.ratio || 3.2 / 5;
        const previewW = previewRatio >= 1 ? 120 : Math.round(120 * previewRatio);
        const previewH = previewRatio >= 1 ? Math.round(120 / previewRatio) : 120;
        return (
          <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            {image ? (
              <img src={image} alt="" style={{ width: previewW, height: previewH, objectFit: 'cover', borderRadius: 10 }} />
            ) : (
              <div style={{ width: previewW, height: previewH, borderRadius: 10, background: theme.bgColor, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: theme.textColor, textAlign: 'center', padding: 4, fontWeight: 800 }}>
                art
              </div>
            )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, minWidth: 260, flex: 1 }}>
          <button className="primary" disabled={imageBusy} onClick={pickImage}>
            {imageBusy ? 'Uploading…' : image ? 'Replace art' : '+ Upload card art'}
          </button>
          <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Card label (e.g. Meme)" />
          {/* Groups templates into named rows on the app's Templates tab, the same way
              Trending rows work on Featured. A picked category locks in its shape (see the
              "Template categories" manager above); "— no category —" lands in the "More" row. */}
          <select
            value={category}
            onChange={(e) => {
              if (e.target.value === NEW) { setCreatingCat(true); return; }
              setCategory(e.target.value);
            }}
          >
            <option value="">— no category —</option>
            {tplCats.map((c) => <option key={c.id || c.name} value={c.name}>{c.name}</option>)}
            <option value={NEW}>＋ New category…</option>
          </select>
          <input
            value={tplTags}
            onChange={(e) => setTplTags(e.target.value)}
            placeholder="Tags, comma separated (birthday, love)"
          />
          <div>
            <p className="muted small" style={{ margin: '0 0 4px' }}>Card theme</p>
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              {allThemes.map((t) => (
                <ThemeSwatch
                  key={t.id || t.name}
                  theme={t}
                  selected={theme.bgColor === t.bgColor && theme.textColor === t.textColor}
                  onClick={() => setTheme(t)}
                />
              ))}
            </div>
          </div>
        </div>
      </div>
        );
      })()}
      {formMsg && <p className={formMsg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 8 }}>{formMsg}</p>}
      <button className="primary" style={{ marginTop: 12 }} disabled={!title.trim()} onClick={openBuilder}>
        Step 2: build the canvas in the editor →
      </button>

      <h3 className="section">Make your own theme</h3>
      <p className="hint">Pick a text color and a background color — it joins the theme list above.</p>
      <div style={{ display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
        <input value={themeName} onChange={(e) => setThemeName(e.target.value)} placeholder="Theme name" style={{ width: 150 }} />
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          Text
          <input type="color" value={themeFg} onChange={(e) => setThemeFg(e.target.value)} style={{ width: 34, height: 26, padding: 0, border: 'none', background: 'none', cursor: 'pointer' }} />
        </label>
        <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 12 }}>
          Background
          <input type="color" value={themeBg} onChange={(e) => setThemeBg(e.target.value)} style={{ width: 34, height: 26, padding: 0, border: 'none', background: 'none', cursor: 'pointer' }} />
        </label>
        <span style={{ borderRadius: 8, fontSize: 11, fontWeight: 800, padding: '7px 11px', background: themeBg, color: themeFg }}>
          {themeName.trim() || 'Preview'}
        </span>
        <button className="primary" disabled={!themeName.trim()} onClick={createTheme}>+ Add theme</button>
      </div>
      {themes.length > 0 && (
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 10 }}>
          {themes.map((t) => (
            <span key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 6, borderRadius: 10, background: t.bgColor, color: t.textColor, fontWeight: 800, fontSize: 11, padding: '7px 11px' }}>
              {t.name}
              <button
                className="del"
                title="Delete theme"
                style={{ padding: '0 4px' }}
                onClick={async () => { await api.deleteCanvasTheme(t.id); load(); }}
              >
                ✕
              </button>
            </span>
          ))}
        </div>
      )}
    </section>
  );
}

function PopularTagsManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [val, setVal] = useState('');
  const [color, setColor] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const load = useCallback(async () => setItems(await api.listPopularTags()), []);
  useEffect(() => { load(); }, [load]);

  async function add() {
    const toAdd = splitTags(val);
    if (!toAdd.length) return;
    setBusy(true); setMsg('');
    try { for (const t of toAdd) await api.addPopularTag(t, color); setVal(''); setColor(''); await load(); setMsg('✓ Added.'); }
    catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  // Deselect / undo — removes a tag that was already added as popular.
  async function remove(id) {
    try { await api.deletePopularTag(id); await load(); }
    catch (e) { setMsg(`✗ ${e.message}`); }
  }

  async function move(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= items.length) return;
    const arr = [...items];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    setItems(arr); // optimistic
    try { await api.saveOrder('popularTags', arr); }
    catch (e) { setMsg(`✗ ${e.message}`); load(); }
  }

  // ---- Assign a tag to a hand-picked set of stickers (browse everything, tag in bulk) ----
  const [allStickers, setAllStickers] = useState([]);
  const [pickerQuery, setPickerQuery] = useState('');
  const [pickedIds, setPickedIds] = useState(new Set());
  const [assignTag, setAssignTag] = useState('');
  const [assigning, setAssigning] = useState(false);
  const [assignMsg, setAssignMsg] = useState('');

  useEffect(() => { api.listAllStickers().then(setAllStickers); }, []);

  const allKnownTags = useMemo(() => {
    const set = new Set();
    for (const s of allStickers) for (const t of s.tags || []) if (t.trim()) set.add(t.trim().toLowerCase());
    for (const t of items) set.add(t.tag);
    return [...set].sort();
  }, [allStickers, items]);

  // ---- Per-tag sticker count + "view stickers, remove the tag from one" ----
  const [expandedTag, setExpandedTag] = useState(null);
  const stickersByTag = useMemo(() => {
    const map = new Map();
    for (const s of allStickers) {
      for (const t of s.tags || []) {
        if (!map.has(t)) map.set(t, []);
        map.get(t).push(s);
      }
    }
    return map;
  }, [allStickers]);

  // Removes just this ONE tag from the sticker — the sticker itself stays, with its other tags.
  async function removeTagFromSticker(sticker, tag) {
    const next = (sticker.tags || []).filter((t) => t !== tag);
    try {
      await api.updateStickerTags(sticker.id, next);
      setAllStickers((prev) => prev.map((s) => (s.id === sticker.id ? { ...s, tags: next } : s)));
    } catch (e) { setMsg(`✗ ${e.message}`); }
  }

  // Edit a tag's text. A pill finds its stickers by their own tags, so the stickers carrying the
  // old tag are offered the rename too — otherwise the renamed pill would open an empty page.
  async function rename(t) {
    const input = window.prompt('Edit tag:', t.tag);
    if (input == null) return;
    const next = splitTags(input.replace(/#/g, ''))[0];
    if (!next || next === t.tag) return;
    if (items.some((x) => x.id !== t.id && x.tag === next)) { setMsg(`✗ #${next} is already a popular tag.`); return; }
    const carriers = stickersByTag.get(t.tag) || [];
    const cascade = carriers.length > 0 && confirm(
      `Also change #${t.tag} → #${next} on the ${carriers.length} sticker${carriers.length === 1 ? '' : 's'} that carry it?\n\n` +
      `OK = rename it on those stickers too, so the pill keeps showing them.\n` +
      `Cancel = rename only the pill (it will show stickers already tagged #${next}).`,
    );
    setBusy(true); setMsg('');
    try {
      await api.renamePopularTag(t.id, next);
      if (cascade) {
        const swap = (tags) => [...new Set((tags || []).map((x) => (x === t.tag ? next : x)))];
        for (const st of carriers) await api.updateStickerTags(st.id, swap(st.tags));
        setAllStickers((prev) => prev.map((st) => ((st.tags || []).includes(t.tag) ? { ...st, tags: swap(st.tags) } : st)));
      }
      if (expandedTag === t.tag) setExpandedTag(next);
      await load();
      setMsg(`✓ Renamed to #${next}${cascade ? ` (and on ${carriers.length} sticker${carriers.length === 1 ? '' : 's'})` : ''}.`);
    } catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  const filteredPicker = pickerQuery.trim()
    ? allStickers.filter((s) => (s.tags || []).some((t) => t.includes(pickerQuery.trim().toLowerCase())))
    : allStickers;

  function togglePicked(id) {
    setPickedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  async function applyTag() {
    const tag = assignTag.trim().toLowerCase();
    if (!tag || pickedIds.size === 0) return;
    setAssigning(true); setAssignMsg('');
    try {
      const chosen = allStickers.filter((s) => pickedIds.has(s.id));
      for (const s of chosen) {
        const next = [...new Set([...(s.tags || []), tag])];
        await api.updateStickerTags(s.id, next);
      }
      setAllStickers((prev) => prev.map((s) => (pickedIds.has(s.id) ? { ...s, tags: [...new Set([...(s.tags || []), tag])] } : s)));
      setAssignMsg(`✓ Tagged ${chosen.length} sticker${chosen.length === 1 ? '' : 's'} with #${tag}.`);
      setPickedIds(new Set());
    } catch (e) { setAssignMsg(`✗ ${e.message}`); }
    setAssigning(false);
  }

  return (
    <section className="card">
      <div className="card-head">
        <div><h2>Popular tags</h2><p className="hint">Shown as pills on Featured, in this order. Tapping one opens every sticker with that tag. Use ▲▼ to set which shows first, second… Click ✎ to edit a tag's text, ✕ to undo/deselect it. Pick a theme per tag (the rainbow swatch picks any colour), or leave it unset for an automatic theme. Click the sticker count to see which stickers carry a tag and remove it from any of them individually.</p></div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <input value={val} onChange={(e) => setVal(e.target.value)} placeholder="funny, love, meme…" style={{ flex: 1, minWidth: 160 }} onKeyDown={(e) => { if (e.key === 'Enter') add(); }} />
        <TagThemePicker value={color} onChange={setColor} />
        {color && <button className="link" onClick={() => setColor('')}>auto</button>}
        <button className="primary" disabled={!val.trim() || busy} onClick={add}>+ Add</button>
      </div>
      {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 6 }}>{msg}</p>}
      {items.length === 0 ? (<p className="muted" style={{ marginTop: 12 }}>No tags yet.</p>) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 12 }}>
          {items.map((t, i) => {
            const tagStickers = stickersByTag.get(t.tag) || [];
            const open = expandedTag === t.tag;
            return (
              <div key={t.id} style={{ background: '#1c1c20', borderRadius: 10, padding: '8px 12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
                  <div className="reorder">
                    <button className="mv" title="Show earlier" disabled={i === 0} onClick={() => move(i, -1)}>▲</button>
                    <button className="mv" title="Show later" disabled={i === items.length - 1} onClick={() => move(i, 1)}>▼</button>
                  </div>
                  <strong>#{t.tag}</strong>
                  <button className="icon" title="Edit tag" disabled={busy} onClick={() => rename(t)}>✎</button>
                  <button className="link" onClick={() => setExpandedTag(open ? null : t.tag)}>
                    {open ? '▴ Hide' : '▾'} {tagStickers.length} sticker{tagStickers.length === 1 ? '' : 's'}
                  </button>
                  <TagThemePicker
                    value={t.color || ''}
                    onChange={async (c) => { await api.setPopularTagColor(t.id, c); load(); }}
                  />
                  {t.color && (
                    <button className="link" onClick={async () => { await api.setPopularTagColor(t.id, ''); load(); }}>auto</button>
                  )}
                  <button className="del" title="Undo / deselect" style={{ marginLeft: 'auto' }} onClick={() => remove(t.id)}>✕</button>
                </div>
                {open && (
                  tagStickers.length === 0 ? (
                    <p className="muted small" style={{ marginTop: 8 }}>No stickers carry this tag.</p>
                  ) : (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(64px, 1fr))', gap: 6, marginTop: 10 }}>
                      {tagStickers.map((s) => (
                        <div key={s.id} style={{ position: 'relative', aspectRatio: 1, borderRadius: 8, overflow: 'hidden', background: '#111114' }}>
                          {s.imageUrl?.endsWith('.mp4') ? (
                            <video src={s.imageUrl} muted style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                          ) : (
                            <img src={s.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
                          )}
                          <button
                            className="del"
                            title={`Remove #${t.tag} from this sticker`}
                            style={{ position: 'absolute', top: 2, right: 2, width: 20, height: 20, padding: 0, lineHeight: '20px' }}
                            onClick={() => removeTagFromSticker(s, t.tag)}
                          >
                            ✕
                          </button>
                        </div>
                      ))}
                    </div>
                  )
                )}
              </div>
            );
          })}
        </div>
      )}

      <h3 className="section">Assign a tag to stickers</h3>
      <p className="hint">Pick stickers below, then choose an existing tag (any tag in use — not just the popular ones above) or type a new one, and apply it to everything selected.</p>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginBottom: 8 }}>
        <input
          list="all-known-tags"
          value={assignTag}
          onChange={(e) => setAssignTag(e.target.value)}
          placeholder="Existing tag or type a new one…"
          style={{ flex: 1, minWidth: 200 }}
        />
        <datalist id="all-known-tags">
          {allKnownTags.map((t) => <option key={t} value={t} />)}
        </datalist>
        <button className="primary" disabled={!assignTag.trim() || pickedIds.size === 0 || assigning} onClick={applyTag}>
          {assigning ? 'Applying…' : `Apply to ${pickedIds.size} sticker${pickedIds.size === 1 ? '' : 's'}`}
        </button>
      </div>
      {assignMsg && <p className={assignMsg.startsWith('✗') ? 'error' : 'hint'} style={{ marginBottom: 8 }}>{assignMsg}</p>}
      <input value={pickerQuery} onChange={(e) => setPickerQuery(e.target.value)} placeholder="Filter stickers by tag…" style={{ width: '100%', marginBottom: 8 }} />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(70px, 1fr))', gap: 8, maxHeight: 320, overflowY: 'auto', background: '#111114', borderRadius: 10, padding: 8 }}>
        {filteredPicker.map((s) => {
          const isSel = pickedIds.has(s.id);
          return (
            <div
              key={s.id}
              onClick={() => togglePicked(s.id)}
              style={{
                position: 'relative', aspectRatio: 1, borderRadius: 8, overflow: 'hidden', cursor: 'pointer',
                background: '#1c1c20', outline: isSel ? '2px solid var(--gold)' : 'none',
              }}
            >
              {s.imageUrl?.endsWith('.mp4') ? (
                <video src={s.imageUrl} muted style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
              ) : (
                <img src={s.imageUrl} alt="" style={{ width: '100%', height: '100%', objectFit: 'contain' }} />
              )}
              {isSel && (
                <div style={{ position: 'absolute', inset: 0, background: 'rgba(245,179,1,0.28)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20, color: '#fff' }}>✓</div>
              )}
            </div>
          );
        })}
      </div>
    </section>
  );
}

function CategoryMetaModal({ category, onClose, onSaved }) {
  const [desc, setDesc] = useState(category.description || '');
  const [banners, setBanners] = useState(
    category.bannerImages?.length ? category.bannerImages : [category.bannerImage].filter(Boolean),
  );
  const [squares, setSquares] = useState(
    category.squareImages?.length ? category.squareImages : [category.squareImage].filter(Boolean),
  );
  const [bgColor, setBgColor] = useState(category.bgColor || '');
  const [bgImage, setBgImage] = useState(category.bgImage || '');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  function pick(kind, onAdd) {
    const inp = document.createElement('input');
    inp.type = 'file';
    inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0];
      if (!f) return;
      setBusy(true); setMsg('');
      try { const url = await uploadToR2(f, { collectionId: '', kind }); onAdd(url); setMsg('✓ Uploaded — click Save to keep it.'); }
      catch (e) { setMsg(`✗ ${e.message}`); }
      setBusy(false);
    };
    inp.click();
  }

  async function save() {
    setBusy(true); setMsg('');
    try {
      await api.updateCategoryMeta(category.id, {
        description: desc.trim(),
        bannerImages: banners, bannerImage: banners[0] || '', // singular kept in sync for older readers
        squareImages: squares, squareImage: squares[0] || '',
        bgColor, bgImage,
      });
      (onSaved || onClose)();
    } catch (e) { setMsg(`✗ ${e.message}`); setBusy(false); }
  }

  const overlay = { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: 16 };
  const box = { background: '#17171b', borderRadius: 14, padding: 20, width: 'min(540px, 96vw)', maxHeight: '90vh', overflowY: 'auto' };
  const label = { display: 'block', color: '#f0c040', fontSize: 12, fontWeight: 700, letterSpacing: 0.5, margin: '14px 0 6px' };

  return (
    <div style={overlay} onClick={onClose}>
      <div style={box} onClick={(e) => e.stopPropagation()}>
        <div className="card-head"><div><h2 style={{ margin: 0 }}>{category.name} — card</h2><p className="hint">Shown on the app's Featured category cards.</p></div><button className="link" onClick={onClose}>Close</button></div>

        <label style={label}>Description</label>
        <input value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Short line shown under the title" style={{ width: '100%' }} />

        <label style={label}>Banner images (5:1) — list view. Add more than one and the app fades between them every second.</label>
        <ImageSlotGrid images={banners} aspect="5/1" tileWidth={140} onRemove={(i) => setBanners((b) => b.filter((_, j) => j !== i))} />
        <button className="primary" disabled={busy} onClick={() => pick('banner', (url) => setBanners((b) => [...b, url]))}>
          {busy ? 'Uploading…' : banners.length ? '+ Add another banner' : '+ Upload banner'}
        </button>

        <label style={label}>Square images (1:1) — grid view. Add more than one and the app fades between them every second.</label>
        <ImageSlotGrid images={squares} aspect="1/1" tileWidth={100} onRemove={(i) => setSquares((s) => s.filter((_, j) => j !== i))} />
        <button className="primary" disabled={busy} onClick={() => pick('square', (url) => setSquares((s) => [...s, url]))}>
          {busy ? 'Uploading…' : squares.length ? '+ Add another square' : '+ Upload square'}
        </button>

        <label style={label}>Category page background — behind the screen you land on when this category is opened (not the card above).</label>
        <ColorSwatchPicker value={bgColor} onChange={setBgColor} />
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
          {bgImage && <img src={bgImage} alt="" style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 6 }} />}
          <button className="primary" disabled={busy} onClick={() => pick('categoryBg', setBgImage)}>
            {busy ? 'Uploading…' : bgImage ? 'Replace image' : '+ Add background image'}
          </button>
          {bgImage && <button className="link" disabled={busy} onClick={() => setBgImage('')}>Remove image</button>}
        </div>

        {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 8 }}>{msg}</p>}
        <div style={{ marginTop: 18, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <button className="link" onClick={onClose}>Cancel</button>
          <button className="primary" disabled={busy} onClick={save}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </div>
  );
}

/**
 * Edit card images, description, and page background for a Group (Type) — same fields as
 * CategoryMetaModal but stored on the 'groups' collection.
 */
function GroupMetaModal({ group, onClose, onSaved }) {
  const [desc, setDesc] = useState(group.description || '');
  const [banners, setBanners] = useState(
    group.bannerImages?.length ? group.bannerImages : [group.bannerImage].filter(Boolean),
  );
  const [squares, setSquares] = useState(
    group.squareImages?.length ? group.squareImages : [group.squareImage].filter(Boolean),
  );
  const [bgColor, setBgColor] = useState(group.bgColor || '');
  const [bgImage, setBgImage] = useState(group.bgImage || '');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  function pick(kind, onAdd) {
    const inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0]; if (!f) return;
      setBusy(true); setMsg('');
      try { const url = await uploadToR2(f, { collectionId: '', kind }); onAdd(url); setMsg('✓ Uploaded — click Save to keep it.'); }
      catch (e) { setMsg(`✗ ${e.message}`); }
      setBusy(false);
    };
    inp.click();
  }

  async function save() {
    setBusy(true); setMsg('');
    try {
      await api.updateGroupMeta(group.id, {
        description: desc.trim(),
        bannerImages: banners, bannerImage: banners[0] || '',
        squareImages: squares, squareImage: squares[0] || '',
        bgColor, bgImage,
      });
      (onSaved || onClose)();
    } catch (e) { setMsg(`✗ ${e.message}`); setBusy(false); }
  }

  const overlay = { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, padding: 16 };
  const box = { background: '#17171b', borderRadius: 14, padding: 20, width: 'min(540px, 96vw)', maxHeight: '90vh', overflowY: 'auto' };
  const label = { display: 'block', color: '#f0c040', fontSize: 12, fontWeight: 700, letterSpacing: 0.5, margin: '14px 0 6px' };

  return (
    <div style={overlay} onClick={onClose}>
      <div style={box} onClick={(e) => e.stopPropagation()}>
        <div className="card-head"><div><h2 style={{ margin: 0 }}>{group.name} — card</h2><p className="hint">Card images and background for this group.</p></div><button className="link" onClick={onClose}>Close</button></div>

        <label style={label}>Description</label>
        <input value={desc} onChange={(e) => setDesc(e.target.value)} placeholder="Short line shown under the title" style={{ width: '100%' }} />

        <label style={label}>Banner images (5:1) — list view.</label>
        <ImageSlotGrid images={banners} aspect="5/1" tileWidth={140} onRemove={(i) => setBanners((b) => b.filter((_, j) => j !== i))} />
        <button className="primary" disabled={busy} onClick={() => pick('banner', (url) => setBanners((b) => [...b, url]))}>
          {busy ? 'Uploading…' : banners.length ? '+ Add another banner' : '+ Upload banner'}
        </button>

        <label style={label}>Square images (1:1) — grid view.</label>
        <ImageSlotGrid images={squares} aspect="1/1" tileWidth={100} onRemove={(i) => setSquares((s) => s.filter((_, j) => j !== i))} />
        <button className="primary" disabled={busy} onClick={() => pick('square', (url) => setSquares((s) => [...s, url]))}>
          {busy ? 'Uploading…' : squares.length ? '+ Add another square' : '+ Upload square'}
        </button>

        <label style={label}>Group page background — behind the screen this group opens into.</label>
        <ColorSwatchPicker value={bgColor} onChange={setBgColor} />
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
          {bgImage && <img src={bgImage} alt="" style={{ width: 40, height: 40, objectFit: 'cover', borderRadius: 6 }} />}
          <button className="primary" disabled={busy} onClick={() => pick('categoryBg', setBgImage)}>
            {busy ? 'Uploading…' : bgImage ? 'Replace image' : '+ Add background image'}
          </button>
          {bgImage && <button className="link" disabled={busy} onClick={() => setBgImage('')}>Remove image</button>}
        </div>

        {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 8 }}>{msg}</p>}
        <div style={{ marginTop: 18, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <button className="link" onClick={onClose}>Cancel</button>
          <button className="primary" disabled={busy} onClick={save}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </div>
    </div>
  );
}

/**
 * Per-trending-card page backdrop: the colour and/or image behind the screen that card opens
 * into. Distinct from `bgColor`, which tints the card's own title panel on Featured.
 */
function TrendingPageBg({ card, onSaved }) {
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  async function save(next) {
    setBusy(true); setMsg('');
    try { await api.updateTrendingCard(card.id, next); await onSaved?.(); }
    catch (e) { setMsg(e.message); }
    setBusy(false);
  }

  function pickBg() {
    const inp = document.createElement('input');
    inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = async () => {
      const f = inp.files[0]; if (!f) return;
      setBusy(true); setMsg('');
      try { await save({ pageBgImage: await uploadToR2(f, { collectionId: '', kind: 'trendingBg' }) }); }
      catch (e) { setMsg(e.message); setBusy(false); }
    };
    inp.click();
  }

  return (
    <span style={{ display: 'inline-flex', gap: 6, alignItems: 'center' }} title="Background of the page this card opens">
      {/* Labelled: the row already has a colour picker (the card's own panel tint), so
          without this the two swatch rows are indistinguishable. */}
      <span className="muted small">page:</span>
      {card.pageBgImage ? (
        <img src={card.pageBgImage} alt="" style={{ width: 26, height: 26, objectFit: 'cover', borderRadius: 5 }} />
      ) : null}
      <ColorSwatchPicker value={card.pageBgColor || ''} onChange={(c) => save({ pageBgColor: c })} />
      <button className="link" disabled={busy} onClick={pickBg}>
        {busy ? '…' : card.pageBgImage ? 'Page bg ✓' : '+ Page bg'}
      </button>
      {card.pageBgImage && (
        <button className="link" disabled={busy} onClick={() => save({ pageBgImage: '' })}>✕bg</button>
      )}
      {msg && <span className="error small">{msg}</span>}
    </span>
  );
}

/** A row of image thumbnails with a small ✕ badge on each to remove it. */
function ImageSlotGrid({ images, aspect, tileWidth, onRemove }) {
  if (!images.length) return null;
  return (
    <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 8 }}>
      {images.map((url, i) => (
        <div key={url + i} style={{ position: 'relative', width: tileWidth }}>
          <img src={url} alt="" style={{ width: '100%', aspectRatio: aspect, objectFit: 'cover', borderRadius: 8, display: 'block' }} />
          <button
            className="del"
            title="Remove"
            style={{ position: 'absolute', top: -6, right: -6, width: 22, height: 22, borderRadius: '50%', padding: 0, lineHeight: '22px' }}
            onClick={() => onRemove(i)}
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
}

/** Always-visible, inline-editable tag chips for one sticker — no popup, view + edit together. */
function StickerTagChips({ sticker, onChanged }) {
  const [tags, setTags] = useState(sticker.tags || []);
  const [input, setInput] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => { setTags(sticker.tags || []); }, [sticker.id, sticker.tags]);

  async function commit(next) {
    setTags(next);
    setBusy(true);
    try { await api.updateStickerTags(sticker.id, next); onChanged?.(next); }
    finally { setBusy(false); }
  }

  function addFromInput() {
    const toAdd = splitTags(input).filter((t) => !tags.includes(t));
    setInput('');
    if (!toAdd.length) return;
    commit([...tags, ...toAdd]);
  }

  return (
    <div className="celltags-edit">
      {tags.map((t) => (
        <span key={t} className="tagchip">
          {t}
          <button type="button" onClick={() => commit(tags.filter((x) => x !== t))} disabled={busy}>×</button>
        </span>
      ))}
      <input
        className="tagchip-input"
        value={input}
        placeholder={tags.length ? '+ tag' : 'add tags…'}
        onChange={(e) => setInput(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ',' || e.key === ' ') { e.preventDefault(); addFromInput(); }
        }}
        onBlur={addFromInput}
        disabled={busy}
      />
    </div>
  );
}

function AllTagsManager({ onClose }) {
  const [loading, setLoading] = useState(true);
  const [tags, setTags] = useState([]); // [{ tag, count }]
  const [popular, setPopular] = useState([]); // [{ id, tag }] — already in Popular Tags
  const [query, setQuery] = useState('');
  const [busyTag, setBusyTag] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [stickers, pop] = await Promise.all([api.listAllStickers(), api.listPopularTags()]);
      const counts = new Map();
      for (const s of stickers) {
        for (const t of s.tags || []) {
          const key = t.trim().toLowerCase();
          if (!key) continue;
          counts.set(key, (counts.get(key) || 0) + 1);
        }
      }
      const list = [...counts.entries()].map(([tag, count]) => ({ tag, count })).sort((a, b) => b.count - a.count);
      setTags(list);
      setPopular(pop);
    } catch (e) { setError(e.message); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  const filtered = query.trim() ? tags.filter((t) => t.tag.includes(query.trim().toLowerCase())) : tags;

  // Toggle: promote a tag to Popular, or undo/deselect it if it's already there.
  async function toggle(tag) {
    setBusyTag(tag);
    try {
      const existing = popular.find((p) => p.tag === tag);
      if (existing) { await api.deletePopularTag(existing.id); setPopular((p) => p.filter((x) => x.tag !== tag)); }
      else { await api.addPopularTag(tag); const fresh = await api.listPopularTags(); setPopular(fresh); }
    } catch (e) { setError(e.message); }
    setBusyTag('');
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>All tags</h2>
          <p className="hint">Every tag currently used across your stickers, with how many stickers carry it. Click "+ Popular" to feature a tag as a pill on Featured — click again ("✓ Popular") to undo.</p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Filter tags…" style={{ width: '100%', marginBottom: 12 }} />
      {error && <div className="error">{error}</div>}
      {loading ? (
        <p className="muted">Loading tags…</p>
      ) : filtered.length === 0 ? (
        <p className="muted">{tags.length === 0 ? 'No tags yet — tag some stickers first.' : 'No tags match that filter.'}</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {filtered.map(({ tag, count }) => {
            const isPopular = popular.some((p) => p.tag === tag);
            return (
              <div key={tag} style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#1c1c20', borderRadius: 10, padding: '9px 14px' }}>
                <strong>#{tag}</strong>
                <span className="muted small">{count} sticker{count === 1 ? '' : 's'}</span>
                <button
                  className={isPopular ? 'del' : 'primary'}
                  style={{ marginLeft: 'auto', padding: '5px 12px', fontSize: 12 }}
                  disabled={busyTag === tag}
                  title={isPopular ? 'Undo — remove from Popular Tags' : 'Add to Popular Tags'}
                  onClick={() => toggle(tag)}
                >
                  {busyTag === tag ? '…' : isPopular ? '✓ Popular (undo)' : '+ Popular'}
                </button>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

/**
 * Browse & Move — look at what's actually in each part of the catalog and fix placements.
 *
 * The import can only ever be as good as the data it came from, so this is the cleanup tool:
 * pick any category / group / collection, see every sticker in it, select the ones that are
 * in the wrong place and move them somewhere else in bulk.
 */
function BrowseMoveManager({ onClose }) {
  const [loading, setLoading] = useState(true);
  const [stickers, setStickers] = useState([]);
  const [cats, setCats] = useState([]);
  const [groups, setGroups] = useState([]);
  const [colls, setColls] = useState([]);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');

  // What we're looking at. '' = "everything at this level and below".
  const [vCat, setVCat] = useState('');
  const [vGroup, setVGroup] = useState('');
  const [vColl, setVColl] = useState('');

  // Where selected stickers should go.
  const [dCat, setDCat] = useState('');
  const [dGroup, setDGroup] = useState('');
  const [dColl, setDColl] = useState('');

  const [picked, setPicked] = useState(() => new Set());
  const lastClicked = useRef(null);

  // Inline "make a new category/group/collection" prompt. `dest` says whether the new thing
  // should be selected as the move destination or as the thing we're browsing.
  const [creating, setCreating] = useState(null); // { level: 'cat'|'group'|'coll', dest: bool }
  const [newName, setNewName] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [all, categories] = await Promise.all([api.listAllStickers(), api.listCategories()]);
      const groupLists = await Promise.all(categories.map((c) => api.listGroups(c.id)));
      const allGroups = groupLists.flat();
      const collLists = await Promise.all(allGroups.map((g) => api.listCollections(g.id)));
      setStickers(all);
      setCats(categories);
      setGroups(allGroups);
      setColls(collLists.flat());
      if (!vCat && categories.length) setVCat(categories[0].id);
    } catch (e) { setError(e.message); }
    setLoading(false);
  }, [vCat]);
  useEffect(() => { load(); }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const catName = useMemo(() => Object.fromEntries(cats.map((c) => [c.id, c.name])), [cats]);
  const groupName = useMemo(() => Object.fromEntries(groups.map((g) => [g.id, g.name])), [groups]);
  const collName = useMemo(() => Object.fromEntries(colls.map((c) => [c.id, c.name])), [colls]);

  const viewGroups = useMemo(() => groups.filter((g) => g.categoryId === vCat), [groups, vCat]);
  const viewColls = useMemo(() => colls.filter((c) => c.groupId === vGroup), [colls, vGroup]);
  const destGroups = useMemo(() => groups.filter((g) => g.categoryId === dCat), [groups, dCat]);
  const destColls = useMemo(() => colls.filter((c) => c.groupId === dGroup), [colls, dGroup]);

  // Stickers in the current scope. 'DIRECT' means "sitting on this level, not nested deeper".
  const shown = useMemo(() => {
    let list = stickers.filter((s) => s.categoryId === vCat);
    if (vGroup === 'DIRECT') list = list.filter((s) => !s.groupId);
    else if (vGroup) {
      list = list.filter((s) => s.groupId === vGroup);
      if (vColl === 'DIRECT') list = list.filter((s) => !s.collectionId);
      else if (vColl) list = list.filter((s) => s.collectionId === vColl);
    }
    return list;
  }, [stickers, vCat, vGroup, vColl]);

  // Clicking with shift extends from the last click, so a run of mis-filed stickers is one gesture.
  function toggle(id, index, shiftKey) {
    setPicked((prev) => {
      const next = new Set(prev);
      if (shiftKey && lastClicked.current !== null) {
        const [a, b] = [lastClicked.current, index].sort((x, y) => x - y);
        for (let i = a; i <= b; i++) next.add(shown[i].id);
      } else if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
    lastClicked.current = index;
  }

  /**
   * Create a category / group / collection without leaving this screen, and immediately
   * select it — so "these 20 belong in a new collection called X" is one flow, not a trip
   * back to the catalog and a re-hunt for the same stickers.
   */
  async function createNode() {
    const name = newName.trim();
    if (!name || !creating) return;
    setBusy(true);
    setError('');
    try {
      const { level, dest } = creating;
      if (level === 'cat') {
        const id = await api.addCategory(name);
        setCats((p) => [...p, { id, name }]);
        if (dest) { setDCat(id); setDGroup(''); setDColl(''); }
        else { setVCat(id); setVGroup(''); setVColl(''); setPicked(new Set()); }
      } else if (level === 'group') {
        const parent = dest ? dCat : vCat;
        const id = await api.addGroup(parent, name);
        setGroups((p) => [...p, { id, categoryId: parent, name }]);
        if (dest) { setDGroup(id); setDColl(''); }
        else { setVGroup(id); setVColl(''); setPicked(new Set()); }
      } else {
        const pCat = dest ? dCat : vCat;
        const pGroup = dest ? dGroup : vGroup;
        const id = await api.addCollection(pCat, pGroup, name, false);
        setColls((p) => [...p, { id, categoryId: pCat, groupId: pGroup, name }]);
        if (dest) setDColl(id);
        else { setVColl(id); setPicked(new Set()); }
      }
      setCreating(null);
      setNewName('');
      setStatus(`Created "${name}"`);
    } catch (e) { setError(e.message); }
    setBusy(false);
  }

  // '＋ New…' is offered as an option inside each dropdown, so creating is where you already are.
  const NEW = '__NEW__';
  function pickOrCreate(value, level, dest, apply) {
    if (value === NEW) { setCreating({ level, dest }); setNewName(''); return; }
    apply(value);
  }

  const creatorRow = (dest) => {
    if (!creating || creating.dest !== dest) return null;
    const where = creating.level === 'cat'
      ? 'as a new top-level category'
      : creating.level === 'group'
        ? `inside ${catName[dest ? dCat : vCat] || '?'}`
        : `inside ${catName[dest ? dCat : vCat] || '?'} / ${groupName[dest ? dGroup : vGroup] || '?'}`;
    const label = creating.level === 'cat' ? 'category' : creating.level === 'group' ? 'group' : 'collection';
    return (
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', margin: '8px 0', flexWrap: 'wrap' }}>
        <input
          autoFocus
          value={newName}
          onChange={(e) => setNewName(e.target.value)}
          onKeyDown={(e) => { if (e.key === 'Enter') createNode(); if (e.key === 'Escape') { setCreating(null); setNewName(''); } }}
          placeholder={`New ${label} name…`}
          style={{ minWidth: 220 }}
          disabled={busy}
        />
        <button className="primary" disabled={busy || !newName.trim()} onClick={createNode}>Create</button>
        <button className="link" onClick={() => { setCreating(null); setNewName(''); }}>Cancel</button>
        <span className="muted small">{where}</span>
      </div>
    );
  };

  async function move() {
    if (!dCat || picked.size === 0) return;
    setBusy(true);
    setError('');
    setStatus('');
    try {
      const ids = [...picked];
      const dest = {
        categoryId: dCat,
        groupId: dGroup && dGroup !== 'DIRECT' ? dGroup : '',
        collectionId: dColl && dColl !== 'DIRECT' ? dColl : '',
      };
      await api.moveStickers(ids, dest);
      const moved = new Set(ids);
      setStickers((prev) => prev.map((s) => (moved.has(s.id) ? { ...s, ...dest } : s)));
      setPicked(new Set());
      lastClicked.current = null;
      const where = [catName[dest.categoryId], groupName[dest.groupId], collName[dest.collectionId]]
        .filter(Boolean).join(' / ');
      setStatus(`Moved ${ids.length} sticker${ids.length === 1 ? '' : 's'} → ${where}`);
    } catch (e) { setError(e.message); }
    setBusy(false);
  }

  const destLabel = [catName[dCat], dGroup === 'DIRECT' ? '(directly on category)' : groupName[dGroup],
    dColl === 'DIRECT' ? '(directly on group)' : collName[dColl]].filter(Boolean).join(' / ');

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Browse &amp; move stickers</h2>
          <p className="hint">
            Pick any part of the catalog to see exactly what's in it. Click stickers to select
            (shift-click to grab a run), choose a destination, and move them in bulk. Use this
            to fix anything the import filed in the wrong place.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      {error && <div className="error">{error}</div>}

      {/* ---- what we're looking at ---- */}
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center', marginBottom: 12 }}>
        <span className="muted small">Showing</span>
        <select
          value={vCat}
          onChange={(e) => pickOrCreate(e.target.value, 'cat', false, (v) => { setVCat(v); setVGroup(''); setVColl(''); setPicked(new Set()); })}
        >
          {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          <option value={NEW}>＋ New category…</option>
        </select>
        <select
          value={vGroup}
          onChange={(e) => pickOrCreate(e.target.value, 'group', false, (v) => { setVGroup(v); setVColl(''); setPicked(new Set()); })}
        >
          <option value="">— all groups —</option>
          <option value="DIRECT">(directly on category)</option>
          {viewGroups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
          {vCat && <option value={NEW}>＋ New group…</option>}
        </select>
        {vGroup && vGroup !== 'DIRECT' && (
          <select
            value={vColl}
            onChange={(e) => pickOrCreate(e.target.value, 'coll', false, (v) => { setVColl(v); setPicked(new Set()); })}
          >
            <option value="">— all collections —</option>
            <option value="DIRECT">(directly on group)</option>
            {viewColls.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            <option value={NEW}>＋ New collection…</option>
          </select>
        )}
        <button className="link" onClick={load}>↻ Reload</button>
      </div>
      {creatorRow(false)}

      <div style={{ display: 'flex', gap: 10, alignItems: 'center', marginBottom: 10, flexWrap: 'wrap' }}>
        <strong>{shown.length}</strong><span className="muted small">sticker{shown.length === 1 ? '' : 's'} here</span>
        {picked.size > 0 && <span className="muted small">· {picked.size} selected</span>}
        <button className="link" onClick={() => setPicked(new Set(shown.map((s) => s.id)))}>Select all</button>
        <button className="link" onClick={() => { setPicked(new Set()); lastClicked.current = null; }}>Clear</button>
        {status && <span className="muted small" style={{ marginLeft: 'auto' }}>{status}</span>}
      </div>

      {/* ---- move bar, only once something is selected ---- */}
      {picked.size > 0 && (
        <div style={{ background: '#1c1c20', border: '1px solid #333', borderRadius: 10, padding: 12, marginBottom: 12, position: 'sticky', top: 8, zIndex: 5 }}>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            <strong>Move {picked.size}</strong><span className="muted small">to</span>
            <select
              value={dCat}
              onChange={(e) => pickOrCreate(e.target.value, 'cat', true, (v) => { setDCat(v); setDGroup(''); setDColl(''); })}
            >
              <option value="">— pick a category —</option>
              {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              <option value={NEW}>＋ New category…</option>
            </select>
            {dCat && (
              <select
                value={dGroup}
                onChange={(e) => pickOrCreate(e.target.value, 'group', true, (v) => { setDGroup(v); setDColl(''); })}
              >
                <option value="DIRECT">(directly on category)</option>
                {destGroups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
                <option value={NEW}>＋ New group…</option>
              </select>
            )}
            {dCat && dGroup && dGroup !== 'DIRECT' && (
              <select
                value={dColl}
                onChange={(e) => pickOrCreate(e.target.value, 'coll', true, setDColl)}
              >
                <option value="DIRECT">(directly on group)</option>
                {destColls.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
                <option value={NEW}>＋ New collection…</option>
              </select>
            )}
            <button className="primary" disabled={!dCat || busy} onClick={move} style={{ marginLeft: 'auto' }}>
              {busy ? 'Moving…' : 'Move →'}
            </button>
          </div>
          {creatorRow(true)}
          {dCat && <div className="muted small" style={{ marginTop: 8 }}>Destination: {destLabel}</div>}
        </div>
      )}

      {loading ? (
        <p className="muted">Loading stickers…</p>
      ) : shown.length === 0 ? (
        <p className="muted">Nothing here.</p>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(92px, 1fr))', gap: 8 }}>
          {shown.map((s, i) => {
            const on = picked.has(s.id);
            return (
              <div
                key={s.id}
                onClick={(e) => toggle(s.id, i, e.shiftKey)}
                title={[groupName[s.groupId], collName[s.collectionId]].filter(Boolean).join(' / ') || 'directly on category'}
                style={{
                  position: 'relative', cursor: 'pointer', background: '#fff', borderRadius: 8,
                  outline: on ? '3px solid #f5c518' : '1px solid #333', aspectRatio: '1',
                  display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
                }}
              >
                <img src={s.imageUrl} alt="" loading="lazy" style={{ maxWidth: '100%', maxHeight: '100%', objectFit: 'contain' }} />
                {on && (
                  <span style={{ position: 'absolute', top: 3, right: 5, background: '#f5c518', color: '#000', borderRadius: 10, fontSize: 11, fontWeight: 700, padding: '0 6px' }}>✓</span>
                )}
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

/**
 * Organize — file legacy-import stickers that landed directly on a group.
 *
 * The old site never recorded what's in these (no sub-sub-category), and they're mostly
 * photos of real people, so naming them needs a human who recognises the faces. To keep
 * that from being ~1,200 decisions, stickers are shown grouped by `importBatch` — the
 * upload burst they arrived in on the old site, which almost always means one person or
 * one movie. Name a batch once and the whole batch is filed.
 */
function OrganizeManager({ onClose }) {
  const [loading, setLoading] = useState(true);
  const [stickers, setStickers] = useState([]);
  const [cats, setCats] = useState([]);
  const [groups, setGroups] = useState([]);
  const [colls, setColls] = useState([]);
  const [groupFilter, setGroupFilter] = useState('');
  const [names, setNames] = useState({});   // batchKey -> typed collection name
  const [busy, setBusy] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [unfiled, categories] = await Promise.all([api.listUnfiledStickers(), api.listCategories()]);
      // groups + collections for every category, so we can show a breadcrumb and offer
      // the existing collection names as autocomplete when filing.
      const groupLists = await Promise.all(categories.map((c) => api.listGroups(c.id)));
      const allGroups = groupLists.flat();
      const collLists = await Promise.all(allGroups.map((g) => api.listCollections(g.id)));
      setStickers(unfiled);
      setCats(categories);
      setGroups(allGroups);
      setColls(collLists.flat());
    } catch (e) { setError(e.message); }
    setLoading(false);
  }, []);
  useEffect(() => { load(); }, [load]);

  const catName = useMemo(() => Object.fromEntries(cats.map((c) => [c.id, c.name])), [cats]);
  const groupById = useMemo(() => Object.fromEntries(groups.map((g) => [g.id, g])), [groups]);

  // Bundle the unfiled stickers into their original upload batches.
  const batches = useMemo(() => {
    const map = new Map();
    for (const s of stickers) {
      if (groupFilter && s.groupId !== groupFilter) continue;
      const key = s.importBatch || `${s.groupId}_nobatch`;
      if (!map.has(key)) map.set(key, { key, groupId: s.groupId, items: [] });
      map.get(key).items.push(s);
    }
    return [...map.values()].sort((a, b) => {
      const ga = groupById[a.groupId]?.name || '';
      const gb = groupById[b.groupId]?.name || '';
      return ga.localeCompare(gb) || a.key.localeCompare(b.key);
    });
  }, [stickers, groupFilter, groupById]);

  // Groups that still have unfiled stickers, for the filter dropdown.
  const groupsWithWork = useMemo(() => {
    const counts = new Map();
    for (const s of stickers) counts.set(s.groupId, (counts.get(s.groupId) || 0) + 1);
    return [...counts.entries()]
      .map(([id, n]) => ({ id, n, g: groupById[id] }))
      .filter((x) => x.g)
      .sort((a, b) => b.n - a.n);
  }, [stickers, groupById]);

  async function fileBatch(batch) {
    const name = (names[batch.key] || '').trim();
    if (!name) return;
    setBusy(batch.key);
    setError('');
    try {
      const g = groupById[batch.groupId];
      // Reuse a collection of that name under this group if it already exists, so
      // filing several batches of the same person merges instead of duplicating.
      let target = colls.find(
        (c) => c.groupId === batch.groupId && c.name.trim().toLowerCase() === name.toLowerCase()
      );
      if (!target) {
        const id = await api.addCollection(g.categoryId, batch.groupId, name, false);
        target = { id, categoryId: g.categoryId, groupId: batch.groupId, name };
        setColls((prev) => [...prev, target]);
      }
      const ids = batch.items.map((s) => s.id);
      await api.assignStickersToCollection(ids, target.id);
      const done = new Set(ids);
      setStickers((prev) => prev.filter((s) => !done.has(s.id)));
      setNames((prev) => { const next = { ...prev }; delete next[batch.key]; return next; });
    } catch (e) { setError(e.message); }
    setBusy('');
  }

  const remaining = stickers.length;

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Organize imported stickers</h2>
          <p className="hint">
            These came from the old site, which never recorded what's in them — so they're
            sitting directly on a group instead of inside a collection. They're shown in the
            batches they were originally uploaded in, which is usually one person or one
            movie per batch. Type a name and hit File to move the whole batch at once.
            Typing a collection name that already exists merges into it.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      {error && <div className="error">{error}</div>}

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14, flexWrap: 'wrap' }}>
        <strong>{remaining}</strong>
        <span className="muted small">sticker{remaining === 1 ? '' : 's'} left to file</span>
        <select value={groupFilter} onChange={(e) => setGroupFilter(e.target.value)} style={{ marginLeft: 'auto' }}>
          <option value="">All groups ({groupsWithWork.length})</option>
          {groupsWithWork.map(({ id, n, g }) => (
            <option key={id} value={id}>{catName[g.categoryId]} / {g.name} — {n}</option>
          ))}
        </select>
        <button className="link" onClick={load}>↻ Reload</button>
      </div>

      {loading ? (
        <p className="muted">Loading imported stickers…</p>
      ) : batches.length === 0 ? (
        <p className="muted">
          {remaining === 0
            ? '🎉 Everything is filed — no stickers are sitting loose on a group.'
            : 'No batches match that filter.'}
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {batches.map((batch) => {
            const g = groupById[batch.groupId];
            const options = colls.filter((c) => c.groupId === batch.groupId);
            const listId = `colls-${batch.groupId}`;
            return (
              <div key={batch.key} style={{ background: '#1c1c20', borderRadius: 10, padding: 12 }}>
                <div className="muted small" style={{ marginBottom: 8 }}>
                  {catName[g?.categoryId] || '?'} / {g?.name || '?'} · {batch.items.length} sticker
                  {batch.items.length === 1 ? '' : 's'}
                  {batch.items[0]?.legacyCreatedAt ? ` · uploaded ${batch.items[0].legacyCreatedAt}` : ''}
                </div>
                <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 8 }}>
                  {batch.items.map((s) => (
                    <img
                      key={s.id}
                      src={s.imageUrl}
                      alt=""
                      loading="lazy"
                      style={{ width: 84, height: 84, objectFit: 'contain', background: '#fff', borderRadius: 8, flex: '0 0 auto' }}
                    />
                  ))}
                </div>
                <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
                  <input
                    list={listId}
                    value={names[batch.key] || ''}
                    onChange={(e) => setNames((p) => ({ ...p, [batch.key]: e.target.value }))}
                    onKeyDown={(e) => { if (e.key === 'Enter') fileBatch(batch); }}
                    placeholder="Who / what is this? e.g. Samantha"
                    style={{ flex: 1 }}
                    disabled={busy === batch.key}
                  />
                  <datalist id={listId}>
                    {options.map((c) => <option key={c.id} value={c.name} />)}
                  </datalist>
                  <button
                    className="primary"
                    disabled={busy === batch.key || !(names[batch.key] || '').trim()}
                    onClick={() => fileBatch(batch)}
                  >
                    {busy === batch.key ? '…' : 'File →'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}

function EmojiManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [val, setVal] = useState('');
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');

  const load = useCallback(async () => setItems(await api.listEmojis()), []);
  useEffect(() => { load(); }, [load]);

  async function add() {
    const toAdd = val.split(/\s+/).map((s) => s.trim()).filter(Boolean);
    if (!toAdd.length) return;
    setBusy(true);
    setMsg('');
    try {
      for (const e of toAdd) await api.addEmoji(e);
      setVal('');
      await load();
      setMsg(`✓ Saved ${toAdd.length} emoji${toAdd.length > 1 ? 's' : ''}. Reopen the app editor to see them.`);
    } catch (e) {
      // Most likely a Firestore rules problem on the `emojis` collection — surface it clearly.
      setMsg(`✗ Couldn't save: ${e.message}. If this says "permission", allow read+write on the "emojis" collection in your Firestore rules.`);
    }
    setBusy(false);
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Emojis</h2>
          <p className="hint">
            These show in the app editor's Emoji tab. Add one or more at once (separate them with spaces).
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <div className="field" style={{ flexDirection: 'row', gap: 8, alignItems: 'center' }}>
        <input
          value={val}
          onChange={(e) => setVal(e.target.value)}
          placeholder="😀 😂 🥰 🔥 …"
          style={{ flex: 1 }}
          onKeyDown={(e) => { if (e.key === 'Enter') add(); }}
        />
        <button className="primary" disabled={!val.trim() || busy} onClick={add}>+ Add</button>
      </div>

      {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 6 }}>{msg}</p>}

      {items.length === 0 ? (
        <p className="muted">No emojis yet — the app falls back to a built-in set until you add some.</p>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 12 }}>
          {items.map((it) => (
            <div
              key={it.id}
              style={{
                position: 'relative', width: 46, height: 46, display: 'flex',
                alignItems: 'center', justifyContent: 'center', fontSize: 26,
                background: '#232327', borderRadius: 10,
              }}
            >
              {it.value}
              <button
                className="del"
                title="Delete"
                onClick={async () => { await api.deleteEmoji(it.id); load(); }}
              >
                ✕
              </button>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

// The fonts already bundled in the app editor — shown read-only for reference so the admin
// doesn't upload a confusing duplicate. Keep this in sync with EDITOR_FONTS in the Android app.
// Mirrors EDITOR_FONTS in the Android app (minus "Default", which isn't a real font to hide).
const BUILTIN_FONTS = [
  { label: 'Quicksand', path: 'fonts/Quicksand-Medium.ttf' },
  ...Array.from({ length: 10 }, (_, i) => ({ label: `Font ${i + 1}`, path: `fonts/font_${i + 1}.ttf` })),
];

function FontManager({ onClose }) {
  const [items, setItems] = useState([]);
  const [hidden, setHidden] = useState([]);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [renamingId, setRenamingId] = useState(null);
  const [renameVal, setRenameVal] = useState('');

  const [order, setOrder] = useState([]);

  const load = useCallback(async () => setItems(await api.listFonts()), []);
  const loadHidden = useCallback(async () => setHidden(await api.listHiddenFonts()), []);
  const loadOrder = useCallback(async () => setOrder(await api.getFontOrder()), []);
  useEffect(() => { load(); loadHidden(); loadOrder(); }, [load, loadHidden, loadOrder]);

  const hiddenPaths = new Set(hidden.map((h) => h.path));
  const visibleBuiltins = BUILTIN_FONTS.filter((f) => !hiddenPaths.has(f.path));

  // Every font the app's picker shows, in the saved order — same keys and same rule the app
  // uses: anything not in the saved list yet keeps its natural place after the ordered ones.
  const rank = new Map(order.map((k, i) => [k, i]));
  const pickerFonts = [
    { key: 'default', label: 'Default', kind: 'built-in' },
    ...visibleBuiltins.map((f) => ({ key: f.path, label: f.label, kind: 'built-in' })),
    ...items.map((f) => ({ key: `custom:${f.id}`, label: f.name, kind: 'uploaded' })),
  ]
    .map((f, i) => ({ f, r: rank.has(f.key) ? rank.get(f.key) : 1e9 + i }))
    .sort((a, b) => a.r - b.r)
    .map((x) => x.f);

  async function moveFont(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= pickerFonts.length) return;
    const arr = [...pickerFonts];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    const keys = arr.map((f) => f.key);
    setOrder(keys); // optimistic
    try { await api.setFontOrder(keys); }
    catch (e) { setMsg(`✗ ${e.message}`); loadOrder(); }
  }

  async function hideBuiltin(path) {
    setBusy(true); setMsg('');
    try { await api.hideBuiltinFont(path); await loadHidden(); }
    catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  async function restoreBuiltin(id) {
    setBusy(true); setMsg('');
    try { await api.unhideBuiltinFont(id); await loadHidden(); }
    catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  function pick() {
    const inp = document.createElement('input');
    inp.type = 'file';
    inp.accept = '.ttf,.otf,font/ttf,font/otf,application/x-font-ttf,application/x-font-otf,application/font-sfnt';
    inp.onchange = async () => {
      const f = inp.files[0];
      if (!f) return;
      setBusy(true); setMsg('');
      try {
        const url = await uploadToR2(f, { collectionId: '', kind: 'font' });
        const name = f.name.replace(/\.(ttf|otf)$/i, '');
        await api.addFont(name, url);
        await load();
        setMsg(`✓ "${name}" added — it shows in the app editor's font picker.`);
      } catch (e) { setMsg(`✗ ${e.message}`); }
      setBusy(false);
    };
    inp.click();
  }

  function startRename(f) {
    setRenamingId(f.id);
    setRenameVal(f.name);
  }

  async function saveRename(id) {
    if (!renameVal.trim()) return;
    try { await api.renameFont(id, renameVal); setRenamingId(null); await load(); }
    catch (e) { setMsg(`✗ ${e.message}`); }
  }

  async function remove(id) {
    setBusy(true);
    try { await api.deleteFont(id); await load(); }
    catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Fonts</h2>
          <p className="hint">
            Upload .ttf/.otf files — each shows up as its own option in the app editor's font picker,
            named however you like. Rename or delete any of them any time.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <button className="primary" disabled={busy} onClick={pick}>{busy ? 'Uploading…' : '+ Upload font'}</button>
      {msg && <p className={msg.startsWith('✗') ? 'error' : 'hint'} style={{ marginTop: 6 }}>{msg}</p>}

      <h3 className="section">Order in the app's font picker</h3>
      <p className="hint">
        First to last, exactly as the font picker shows them in both app editors. Use ▲▼ to move a
        font; new uploads start at the end.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {pickerFonts.map((f, i) => (
          <div key={f.key} style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#1c1c20', borderRadius: 10, padding: '7px 12px' }}>
            <div className="reorder">
              <button className="mv" title="Move up" disabled={i === 0} onClick={() => moveFont(i, -1)}>▲</button>
              <button className="mv" title="Move down" disabled={i === pickerFonts.length - 1} onClick={() => moveFont(i, 1)}>▼</button>
            </div>
            <span className="muted small" style={{ minWidth: 22 }}>{i + 1}.</span>
            <strong style={{ flex: 1 }}>{f.label}</strong>
            <span className="muted small">{f.kind}</span>
          </div>
        ))}
      </div>

      <h3 className="section">Custom fonts ({items.length})</h3>
      {items.length === 0 ? (
        <p className="muted">None uploaded yet.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {items.map((f) => (
            <div key={f.id} style={{ display: 'flex', alignItems: 'center', gap: 10, background: '#1c1c20', borderRadius: 10, padding: '9px 12px' }}>
              {renamingId === f.id ? (
                <>
                  <input
                    value={renameVal}
                    onChange={(e) => setRenameVal(e.target.value)}
                    onKeyDown={(e) => { if (e.key === 'Enter') saveRename(f.id); if (e.key === 'Escape') setRenamingId(null); }}
                    autoFocus
                    style={{ flex: 1 }}
                  />
                  <button className="link" onClick={() => saveRename(f.id)}>Save</button>
                  <button className="link" onClick={() => setRenamingId(null)}>Cancel</button>
                </>
              ) : (
                <>
                  <strong style={{ flex: 1 }}>{f.name}</strong>
                  <button className="link" onClick={() => startRename(f)}>✎ Rename</button>
                  <button className="del" disabled={busy} onClick={() => remove(f.id)}>✕</button>
                </>
              )}
            </div>
          ))}
        </div>
      )}

      <h3 className="section">Already in the app (built-in)</h3>
      <p className="hint">
        These ship inside the app, so "delete" just hides them from the editor's font picker — restorable any time.
      </p>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
        {visibleBuiltins.length === 0 && <p className="muted small">All built-in fonts are hidden.</p>}
        {visibleBuiltins.map((f) => (
          <span
            key={f.path}
            className="muted small"
            style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#1c1c20', borderRadius: 8, padding: '5px 6px 5px 10px' }}
          >
            {f.label}
            <button
              className="del"
              disabled={busy}
              title="Hide from the app's font picker"
              style={{ width: 18, height: 18, padding: 0, lineHeight: '18px' }}
              onClick={() => hideBuiltin(f.path)}
            >
              ✕
            </button>
          </span>
        ))}
      </div>

      {hidden.length > 0 && (
        <>
          <h3 className="section">Hidden built-in fonts</h3>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {hidden.map((h) => {
              const f = BUILTIN_FONTS.find((x) => x.path === h.path);
              return (
                <span
                  key={h.id}
                  className="muted small"
                  style={{ display: 'flex', alignItems: 'center', gap: 6, background: '#1c1c20', borderRadius: 8, padding: '5px 10px', opacity: 0.6 }}
                >
                  {f?.label || h.path}
                  <button className="link" disabled={busy} onClick={() => restoreBuiltin(h.id)}>↺ Restore</button>
                </span>
              );
            })}
          </div>
        </>
      )}
    </section>
  );
}

function Breadcrumb({ category, group, coll, go }) {
  return (
    <nav className="crumbs">
      <button onClick={() => go('root')}>Categories</button>
      {category && (<><i>›</i><button onClick={() => go('category')}>{category.name}</button></>)}
      {group && (<><i>›</i><button onClick={() => go('group')}>{group.name}</button></>)}
      {coll && (<><i>›</i><span className="cur">{coll.name}{coll.animated ? ' 🎞' : ''}</span></>)}
    </nav>
  );
}

/** Generic list of named things (categories / types / names) with add, rename, reorder, delete. */
function NameList({ title, hint, collName, load, onAdd, onDelete, onOpen, allowAnimated, onEdit }) {
  const [items, setItems] = useState([]);
  const [busy, setBusy] = useState(true);
  const [name, setName] = useState('');
  const [animated, setAnimated] = useState(false);
  const [error, setError] = useState('');

  const refresh = useCallback(async () => {
    setBusy(true);
    setError('');
    try {
      setItems(await load());
    } catch (e) {
      setError(e.message);
    }
    setBusy(false);
  }, [load]);

  useEffect(() => { refresh(); }, [refresh]);

  async function add(e) {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      await onAdd(name.trim(), animated);
      setName('');
      setAnimated(false);
      await refresh();
    } catch (e) {
      setError(e.message);
      setBusy(false);
    }
  }

  async function rename(it) {
    const next = window.prompt('Rename to:', it.name);
    if (!next || !next.trim() || next.trim() === it.name) return;
    try {
      await api.updateName(collName, it.id, next.trim());
      refresh();
    } catch (e) {
      setError(e.message);
    }
  }

  async function move(i, dir) {
    const j = i + dir;
    if (j < 0 || j >= items.length) return;
    const arr = [...items];
    [arr[i], arr[j]] = [arr[j], arr[i]];
    setItems(arr); // optimistic
    try {
      await api.saveOrder(collName, arr);
    } catch (e) {
      setError(e.message);
      refresh();
    }
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>{title}</h2>
          <p className="hint">{hint}</p>
        </div>
      </div>

      <form className="addrow" onSubmit={add}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Add a name…"
        />
        {allowAnimated && (
          <label className="toggle" title="Animated collections hold moving stickers (animated WebP). Upload an mp4 video or a GIF / animated WebP / APNG.">
            <input type="checkbox" checked={animated} onChange={(e) => setAnimated(e.target.checked)} />
            <span>Animated sticker</span>
          </label>
        )}
        <button className="primary" disabled={!name.trim()}>+ Add</button>
      </form>

      {error && <div className="error">{error}</div>}
      {busy && <div className="muted">Loading…</div>}

      {!busy && items.length === 0 && <div className="muted">Nothing here yet — add one above.</div>}

      <ul className="list">
        {items.map((it, i) => (
          <li key={it.id}>
            <div className="reorder">
              <button className="mv" title="Move up" disabled={i === 0} onClick={() => move(i, -1)}>▲</button>
              <button className="mv" title="Move down" disabled={i === items.length - 1} onClick={() => move(i, 1)}>▼</button>
            </div>
            <button className="open" onClick={() => onOpen(it)}>
              {it.cover ? (
                <img className="thumb" src={it.cover} alt="" />
              ) : (
                allowAnimated && <span className="thumb empty">?</span>
              )}
              {it.name}
              {it.animated && <em className="gif">ANIM</em>}
            </button>
            {onEdit && <button className="icon" title="Card image & description" onClick={() => onEdit(it)}>⚙</button>}
            <button className="icon" title="Rename" onClick={() => rename(it)}>✎</button>
            <button
              className="del"
              title="Delete"
              onClick={async () => {
                if (!confirm(`Delete "${it.name}"? This does not delete its uploaded stickers.`)) return;
                await onDelete(it.id);
                refresh();
              }}
            >
              ✕
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

/**
 * Lets the admin place stickers (or animations) directly on a Category or a Group, skipping
 * Type and/or Name entirely — the exact same upload capability a Collection has, "without
 * needing one". Pass groupId to scope this to a group; omit it to scope to the category itself.
 */
/** All-time per-sticker views & downloads (from the app's analytics) for the tile badges. */
function useStickerInsights() {
  const [insights, setInsights] = useState({ views: {}, downloads: {} });
  useEffect(() => { api.getStickerInsights().then(setInsights); }, []);
  return insights;
}

/** A sticker's all-time views and downloads (sent to WhatsApp, Telegram…) from the app. */
function InsightBadge({ insights, id }) {
  const v = insights.views[id] || 0;
  const d = insights.downloads[id] || 0;
  if (!v && !d) return null;
  return (
    <span className="sent-badge" title={`Viewed ${v} time${v === 1 ? '' : 's'} · downloaded ${d} time${d === 1 ? '' : 's'} (all time, from the app)`}>
      👁 {v} · ⬇ {d}
    </span>
  );
}

function DirectStickers({ title, hint, categoryId, groupId = '', autoTags }) {
  const insights = useStickerInsights();
  const [stickers, setStickers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [files, setFiles] = useState([]);
  const [tags, setTags] = useState('');
  const [isAnimated, setIsAnimated] = useState(false);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(0);
  const [error, setError] = useState('');
  const [arrange, setArrange] = useState(false);

  const refresh = useCallback(async () => {
    setLoading(true);
    try { setStickers(await api.listDirectStickers({ categoryId, groupId })); }
    catch (e) { setError(e.message); }
    setLoading(false);
  }, [categoryId, groupId]);
  useEffect(() => { refresh(); }, [refresh]);

  const [bulkTags, setBulkTags] = useState('');
  const [tagBusy, setTagBusy] = useState(false);
  const [removeTagsInput, setRemoveTagsInput] = useState('');
  const [removeBusy, setRemoveBusy] = useState(false);

  async function applyBulkTags() {
    const add = splitTags(bulkTags);
    if (!add.length || !stickers.length) return;
    setTagBusy(true);
    setError('');
    try {
      for (const s of stickers) {
        const merged = [...new Set([...(s.tags || []), ...add])];
        await api.updateStickerTags(s.id, merged);
      }
      setBulkTags('');
      await refresh();
    } catch (e) {
      setError(e.message);
    }
    setTagBusy(false);
  }

  /** Removes the given tag(s) from every sticker below that actually has one — untouched
   * stickers aren't rewritten, so this stays cheap even on a large collection. */
  async function applyRemoveTags() {
    const remove = splitTags(removeTagsInput);
    if (!remove.length || !stickers.length) return;
    setRemoveBusy(true);
    setError('');
    try {
      for (const s of stickers) {
        const current = s.tags || [];
        if (!current.some((t) => remove.includes(t))) continue;
        await api.updateStickerTags(s.id, current.filter((t) => !remove.includes(t)));
      }
      setRemoveTagsInput('');
      await refresh();
    } catch (e) {
      setError(e.message);
    }
    setRemoveBusy(false);
  }

  const previews = useMemo(
    () => files.map((f) => ({ url: URL.createObjectURL(f), isVideo: f.type.startsWith('video/') })),
    [files]
  );
  useEffect(() => () => previews.forEach((p) => URL.revokeObjectURL(p.url)), [previews]);

  function removePreview(i) { setFiles((fs) => fs.filter((_, idx) => idx !== i)); }

  async function upload() {
    if (!files.length) return;
    setBusy(true); setDone(0); setError('');
    const extra = splitTags(tags);
    const merged = [...new Set([...extra, ...autoTags.map((t) => t.toLowerCase())])];
    try {
      for (const f of files) {
        const url = await uploadToR2(f, { collectionId: groupId || categoryId, kind: isAnimated ? 'animated' : 'sticker' });
        await api.addSticker({ categoryId, groupId: groupId || '', collectionId: '', imageUrl: url, tags: merged, animated: isAnimated });
        setDone((d) => d + 1);
      }
      setFiles([]);
      await refresh();
    } catch (e) { setError(e.message); }
    setBusy(false);
  }

  return (
    <section className="card" style={{ marginTop: 16 }}>
      <div className="card-head">
        <div><h2>{title}</h2><p className="hint">{hint}</p></div>
      </div>

      <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginBottom: 10 }}>
        <div className="mode-toggle" style={{ display: 'inline-flex', background: '#1c1c20', borderRadius: 999, padding: 3 }}>
          <button
            className={!isAnimated ? 'primary' : 'link'}
            style={{ padding: '6px 14px', fontSize: 13, borderRadius: 999 }}
            onClick={() => setIsAnimated(false)}
          >
            🖼 Sticker
          </button>
          <button
            className={isAnimated ? 'primary' : 'link'}
            style={{ padding: '6px 14px', fontSize: 13, borderRadius: 999 }}
            onClick={() => setIsAnimated(true)}
          >
            🎬 Animation
          </button>
        </div>
      </div>

      <input
        key={isAnimated ? 'anim' : 'static'}
        type="file"
        multiple
        accept={isAnimated ? 'image/*,video/*' : 'image/*'}
        onChange={(e) => setFiles([...e.target.files])}
      />

      {previews.length > 0 && (
        <div className="grid" style={{ marginTop: 10 }}>
          {previews.map((p, i) => (
            <div className="cell-wrap" key={i}>
              <div className="cell">
                {p.isVideo ? <video src={p.url} muted loop playsInline /> : <img src={p.url} alt="" />}
                <button className="del" onClick={() => removePreview(i)}>✕</button>
              </div>
            </div>
          ))}
        </div>
      )}

      <input
        value={tags}
        onChange={(e) => setTags(e.target.value)}
        placeholder="Extra tags (comma separated)"
        style={{ width: '100%', marginTop: 10 }}
      />
      <p className="muted small">Auto-added tags: <code>{autoTags.join(', ')}</code></p>

      {busy ? (
        <div className="progress">
          <div className="bar" style={{ width: `${(done / files.length) * 100 || 0}%` }} />
          <span>Uploading {done} / {files.length}…</span>
        </div>
      ) : (
        <button className="primary" disabled={!files.length} onClick={upload}>
          Upload {files.length || ''} {isAnimated ? 'animation' : 'sticker'}{files.length === 1 ? '' : 's'}
        </button>
      )}
      {error && <div className="error">{error}</div>}

      <h3 className="section">
        {stickers.length} placed directly here
        {stickers.length > 1 && !arrange && (
          <button className="link" onClick={() => setArrange(true)}>↕ Arrange order</button>
        )}
      </h3>
      {arrange ? (
        <StickerArranger stickers={stickers} onClose={() => { setArrange(false); refresh(); }} />
      ) : loading ? (
        <p className="muted">Loading…</p>
      ) : stickers.length === 0 ? (
        <p className="muted">None yet — stickers uploaded above show here.</p>
      ) : (
        <>
          <div className="bulk-tags">
            <input
              value={bulkTags}
              onChange={(e) => setBulkTags(e.target.value)}
              placeholder="Add tags to ALL stickers below (comma or space separated)"
            />
            <button className="primary" disabled={!bulkTags.trim() || tagBusy} onClick={applyBulkTags}>
              {tagBusy ? 'Adding…' : 'Add to all'}
            </button>
          </div>
          <div className="bulk-tags" style={{ marginTop: 8 }}>
            <input
              value={removeTagsInput}
              onChange={(e) => setRemoveTagsInput(e.target.value)}
              placeholder="Remove tag(s) from every sticker below that has them"
            />
            <button className="primary danger" disabled={!removeTagsInput.trim() || removeBusy} onClick={applyRemoveTags}>
              {removeBusy ? 'Removing…' : 'Remove from all'}
            </button>
          </div>
          <p className="muted small">Tags show under every sticker below — type to add one, click × to remove.</p>
          <div className="grid">
            {stickers.map((s) => (
              <div className="cell-wrap" key={s.id}>
                <div className="cell">
                  {s.imageUrl?.endsWith('.mp4') ? (
                    <video src={s.imageUrl} muted loop playsInline
                      onMouseOver={(e) => e.currentTarget.play()}
                      onMouseOut={(e) => e.currentTarget.pause()} />
                  ) : (
                    <img src={s.imageUrl} alt="" />
                  )}
                  <InsightBadge insights={insights} id={s.id} />
                  <button
                    className="del"
                    onClick={async () => {
                      if (!confirm('Delete this sticker?')) return;
                      await api.deleteSticker(s.id);
                      refresh();
                    }}
                  >
                    ✕
                  </button>
                </div>
                <StickerTagChips
                  sticker={s}
                  onChanged={(tags) => setStickers((prev) => prev.map((x) => (x.id === s.id ? { ...x, tags } : x)))}
                />
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}

/**
 * Arrange the order stickers appear in inside the app (first → last). Drag a tile onto another,
 * type a position number, or use ⇤ / ⇥ to send one to the very start / end. Every change is
 * saved to Firestore straight away (`position` on each sticker) and the app picks it up on its
 * next catalog refresh. Stickers uploaded later land after the arranged ones until re-arranged.
 */
function StickerArranger({ stickers, onClose }) {
  const [list, setList] = useState(stickers);
  const [dragFrom, setDragFrom] = useState(null);
  const [overIdx, setOverIdx] = useState(null);
  const [pending, setPending] = useState(0);
  const [error, setError] = useState('');
  const persisted = useRef(new Map(stickers.map((s) => [s.id, s.position])));
  const chain = useRef(Promise.resolve());

  function persist(next) {
    setPending((n) => n + 1);
    chain.current = chain.current
      .then(async () => {
        await api.saveStickerOrder(next.map((s) => ({ id: s.id, position: persisted.current.get(s.id) })));
        next.forEach((s, i) => persisted.current.set(s.id, i));
        setError('');
      })
      .catch((e) => setError(`Couldn't save the order: ${e.message}`))
      .finally(() => setPending((n) => n - 1));
  }

  function move(from, to) {
    to = Math.max(0, Math.min(list.length - 1, to));
    if (from === to || from == null) return;
    const next = list.slice();
    const [item] = next.splice(from, 1);
    next.splice(to, 0, item);
    setList(next);
    persist(next);
  }

  return (
    <div className="arranger">
      <div className="arranger-bar">
        <span className="muted small">
          Drag a sticker onto another spot, type a position number, or use ⇤ ⇥ for first / last. #1 is the
          first sticker shown in the app.
        </span>
        <span className="arranger-status">{pending > 0 ? 'Saving…' : error ? '' : '✓ Saved'}</span>
        <button className="primary" onClick={() => onClose(list)}>Done</button>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="grid">
        {list.map((s, i) => (
          <div
            key={s.id}
            className={`arr-tile${dragFrom === i ? ' dragging' : ''}${overIdx === i && dragFrom !== i ? ' over' : ''}`}
            draggable
            onDragStart={(e) => { setDragFrom(i); e.dataTransfer.effectAllowed = 'move'; }}
            onDragOver={(e) => { e.preventDefault(); setOverIdx(i); }}
            onDragLeave={() => setOverIdx((o) => (o === i ? null : o))}
            onDrop={(e) => { e.preventDefault(); move(dragFrom, i); setDragFrom(null); setOverIdx(null); }}
            onDragEnd={() => { setDragFrom(null); setOverIdx(null); }}
          >
            <div className="cell">
              {s.imageUrl?.endsWith('.mp4') ? (
                <video src={s.imageUrl} muted loop playsInline draggable={false} />
              ) : (
                <img src={s.imageUrl} alt="" draggable={false} />
              )}
              <span className="arr-num">{i + 1}</span>
            </div>
            <div className="arr-ctrl">
              <button className="mv" title="Move to first" disabled={i === 0} onClick={() => move(i, 0)}>⇤</button>
              <input
                key={`${s.id}-${i}`}
                className="arr-pos"
                type="number"
                min="1"
                max={list.length}
                defaultValue={i + 1}
                title="Type a position and press Enter"
                onKeyDown={(e) => { if (e.key === 'Enter') e.currentTarget.blur(); }}
                onBlur={(e) => {
                  const n = parseInt(e.target.value, 10);
                  if (Number.isFinite(n) && n - 1 !== i) move(i, n - 1);
                  else e.target.value = i + 1;
                }}
              />
              <button className="mv" title="Move to last" disabled={i === list.length - 1} onClick={() => move(i, list.length - 1)}>⇥</button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/** The main event: mass upload + mass tags for one collection. */
function Upload({ category, group, coll }) {
  const isAnimated = !!coll.animated;
  const insights = useStickerInsights();

  const [files, setFiles] = useState([]);
  const [tags, setTags] = useState('');
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(0);
  const [stickers, setStickers] = useState([]);
  const [error, setError] = useState('');
  const [arrange, setArrange] = useState(false);
  const [drag, setDrag] = useState(false);
  const [cover, setCover] = useState(coll.cover || '');
  const [coverBusy, setCoverBusy] = useState(false);

  async function changeCover(file) {
    if (!file) return;
    setCoverBusy(true);
    setError('');
    try {
      const url = await uploadToR2(file, { collectionId: coll.id, kind: 'cover' });
      await api.setCollectionCover(coll.id, url);
      setCover(url);
    } catch (e) {
      setError(e.message);
    }
    setCoverBusy(false);
  }

  async function removeCover() {
    setCoverBusy(true);
    try {
      await api.setCollectionCover(coll.id, '');
      setCover('');
    } catch (e) {
      setError(e.message);
    }
    setCoverBusy(false);
  }

  const autoTags = [category.name, group.name, coll.name]
    .filter(Boolean)
    .map((t) => t.toLowerCase());

  const refresh = useCallback(async () => {
    try {
      setStickers(await api.listStickers(coll.id));
    } catch (e) {
      setError(e.message);
    }
  }, [coll.id]);

  useEffect(() => { refresh(); }, [refresh]);

  const [bulkTags, setBulkTags] = useState('');
  const [tagBusy, setTagBusy] = useState(false);
  const [removeTagsInput, setRemoveTagsInput] = useState('');
  const [removeBusy, setRemoveBusy] = useState(false);

  async function applyBulkTags() {
    const add = splitTags(bulkTags);
    if (!add.length || !stickers.length) return;
    setTagBusy(true);
    setError('');
    try {
      for (const s of stickers) {
        const merged = [...new Set([...(s.tags || []), ...add])];
        await api.updateStickerTags(s.id, merged);
      }
      setBulkTags('');
      await refresh();
    } catch (e) {
      setError(e.message);
    }
    setTagBusy(false);
  }

  /** Removes the given tag(s) from every sticker in this collection that actually has one —
   * untouched stickers aren't rewritten, so this stays cheap even on a large collection. */
  async function applyRemoveTags() {
    const remove = splitTags(removeTagsInput);
    if (!remove.length || !stickers.length) return;
    setRemoveBusy(true);
    setError('');
    try {
      for (const s of stickers) {
        const current = s.tags || [];
        if (!current.some((t) => remove.includes(t))) continue;
        await api.updateStickerTags(s.id, current.filter((t) => !remove.includes(t)));
      }
      setRemoveTagsInput('');
      await refresh();
    } catch (e) {
      setError(e.message);
    }
    setRemoveBusy(false);
  }

  // Preview thumbnails for the files chosen but not yet uploaded.
  const previews = useMemo(
    () => files.map((f) => ({ url: URL.createObjectURL(f), isVideo: f.type.startsWith('video/') })),
    [files]
  );
  useEffect(() => () => previews.forEach((p) => URL.revokeObjectURL(p.url)), [previews]);

  function removeAt(i) {
    setFiles(files.filter((_, j) => j !== i));
  }

  function pick(list) {
    // Animated collections take a video (mp4/mov/webm) OR an animated image (GIF/WebP/APNG),
    // so accept both here — the old video-only filter silently dropped webp/gif files, which
    // left no preview and a disabled Upload button. Empty-type files (some .apng/.webp) pass
    // too; the server validates the real content.
    const accepted = Array.from(list).filter((f) =>
      isAnimated
        ? f.type === '' || f.type.startsWith('video/') || f.type.startsWith('image/')
        : f.type.startsWith('image/')
    );
    setFiles((prev) => [...prev, ...accepted]);
  }

  async function upload() {
    if (!files.length) return;
    setBusy(true);
    setDone(0);
    setError('');

    const extra = splitTags(tags);
    const allTags = [...new Set([...extra, ...autoTags])];

    let ok = 0;
    for (const file of files) {
      try {
        const url = await uploadToR2(file, {
          collectionId: coll.id,
          kind: isAnimated ? 'animated' : 'sticker',
        });
        await api.addSticker({
          categoryId: category.id,
          groupId: group.id,
          collectionId: coll.id,
          imageUrl: url,
          tags: allTags,
          animated: isAnimated,
        });
        ok++;
      } catch (e) {
        setError(`${file.name}: ${e.message}`);
      }
      setDone((d) => d + 1);
    }

    setBusy(false);
    setFiles([]);
    setTags('');
    await refresh();
    alert(`Uploaded ${ok} / ${files.length}`);
  }

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>
            {coll.name} {isAnimated && <em className="gif">ANIMATED · WebP</em>}
          </h2>
          <p className="hint">
            {isAnimated
              ? 'Drop an mp4 video or animated GIF / WebP / APNG. Converted to a 512×512 animated WebP (<500KB) that plays inline in WhatsApp.'
              : 'Drop images. Auto-converted to 512×512 WebP stickers (<100KB).'}
          </p>
        </div>
      </div>

      {/* Cover photo — shown on the group page (e.g. Hindi → Actress → Kareena's photo) */}
      <div className="cover-row">
        <div className="cover-preview">
          {cover ? <img src={cover} alt="cover" /> : <span className="muted small">No cover</span>}
        </div>
        <div className="cover-info">
          <strong>Cover photo</strong>
          <p className="hint">
            Shown on the <b>{group.name}</b> page — e.g. a photo of {coll.name}.
            If you don't set one, the first sticker is used.
          </p>
          <div className="cover-actions">
            <label className="primary file">
              {coverBusy ? 'Uploading…' : cover ? 'Change photo' : 'Add photo'}
              <input
                type="file"
                accept="image/*"
                hidden
                disabled={coverBusy}
                onChange={(e) => changeCover(e.target.files[0])}
              />
            </label>
            {cover && !coverBusy && (
              <button className="link" onClick={removeCover}>remove</button>
            )}
          </div>
        </div>
      </div>

      <div
        className={`drop ${drag ? 'over' : ''}`}
        onDragOver={(e) => { e.preventDefault(); setDrag(true); }}
        onDragLeave={() => setDrag(false)}
        onDrop={(e) => { e.preventDefault(); setDrag(false); pick(e.dataTransfer.files); }}
      >
        <p>
          <strong>Drag &amp; drop {isAnimated ? 'mp4 videos or GIF / WebP files' : 'images'} here</strong>
        </p>
        <p className="muted">or</p>
        <label className="primary file">
          Choose files
          <input
            type="file"
            multiple
            accept={isAnimated ? 'video/mp4,video/quicktime,video/webm,image/gif,image/webp,image/apng,image/png' : 'image/*'}
            onChange={(e) => pick(e.target.files)}
            hidden
          />
        </label>
        {files.length > 0 && (
          <p className="picked">
            {files.length} file{files.length > 1 ? 's' : ''} ready
            <button className="link" onClick={() => setFiles([])}>clear all</button>
          </p>
        )}
      </div>

      {previews.length > 0 && (
        <>
          <h3 className="section">Preview — {previews.length} to upload</h3>
          <div className="grid">
            {previews.map((p, i) => (
              <div className="cell" key={i}>
                {p.isVideo ? (
                  <video src={p.url} muted loop playsInline
                    onMouseOver={(e) => e.currentTarget.play()}
                    onMouseOut={(e) => e.currentTarget.pause()} />
                ) : (
                  <img src={p.url} alt="" />
                )}
                <button className="del" title="Remove" onClick={() => removeAt(i)}>✕</button>
              </div>
            ))}
          </div>
        </>
      )}

      <label className="field">
        <span>Tags for ALL of these (comma separated)</span>
        <input
          value={tags}
          onChange={(e) => setTags(e.target.value)}
          placeholder="e.g. bollywood, funny, love"
        />
      </label>
      <p className="muted small">
        Auto-added tags: <code>{autoTags.join(', ')}</code>
      </p>

      {busy ? (
        <div className="progress">
          <div className="bar" style={{ width: `${(done / files.length) * 100 || 0}%` }} />
          <span>Uploading {done} / {files.length}…</span>
        </div>
      ) : (
        <button className="primary big" disabled={!files.length} onClick={upload}>
          Upload {files.length || ''} {isAnimated ? 'animation' : 'sticker'}
          {files.length === 1 ? '' : 's'}
        </button>
      )}

      {error && <div className="error">{error}</div>}

      <h3 className="section">
        {stickers.length} in this collection
        {stickers.length > 1 && !arrange && (
          <button className="link" onClick={() => setArrange(true)}>↕ Arrange order</button>
        )}
      </h3>

      {arrange && (
        <StickerArranger stickers={stickers} onClose={() => { setArrange(false); refresh(); }} />
      )}

      {!arrange && stickers.length > 0 && (
        <>
          <div className="bulk-tags">
            <input
              value={bulkTags}
              onChange={(e) => setBulkTags(e.target.value)}
              placeholder="Add tags to ALL stickers below (comma or space separated)"
            />
            <button className="primary" disabled={!bulkTags.trim() || tagBusy} onClick={applyBulkTags}>
              {tagBusy ? 'Adding…' : 'Add to all'}
            </button>
          </div>
          <div className="bulk-tags" style={{ marginTop: 8 }}>
            <input
              value={removeTagsInput}
              onChange={(e) => setRemoveTagsInput(e.target.value)}
              placeholder="Remove tag(s) from every sticker below that has them"
            />
            <button className="primary danger" disabled={!removeTagsInput.trim() || removeBusy} onClick={applyRemoveTags}>
              {removeBusy ? 'Removing…' : 'Remove from all'}
            </button>
          </div>
        </>
      )}
      {!arrange && <p className="muted small">Tags show under every sticker below — type to add one, click × to remove.</p>}

      {!arrange && <div className="grid">
        {stickers.map((s) => (
          <div className="cell-wrap" key={s.id}>
            <div className="cell">
              {s.imageUrl?.endsWith('.mp4') ? (
                <video src={s.imageUrl} muted loop playsInline
                  onMouseOver={(e) => e.currentTarget.play()}
                  onMouseOut={(e) => e.currentTarget.pause()} />
              ) : (
                // Animated WebP autoplays right inside an <img>.
                <img src={s.imageUrl} alt="" />
              )}
              <InsightBadge insights={insights} id={s.id} />
              <button
                className="del"
                onClick={async () => {
                  if (!confirm('Delete this sticker?')) return;
                  await api.deleteSticker(s.id);
                  refresh();
                }}
              >
                ✕
              </button>
            </div>
            <StickerTagChips
              sticker={s}
              onChanged={(tags) => setStickers((prev) => prev.map((x) => (x.id === s.id ? { ...x, tags } : x)))}
            />
          </div>
        ))}
      </div>}
    </section>
  );
}
