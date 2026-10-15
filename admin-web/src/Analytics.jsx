import { useEffect, useMemo, useState } from 'react';
import * as api from './db';

/*
 * Usage numbers counted anonymously by the app (Analytics.kt): totals only, nothing per person.
 * One range control scopes the whole page; every chart also shows its numbers as text, so no
 * value is only readable by hovering.
 */

// 0 = all time (every day ever recorded).
const RANGES = [['Today', 1], ['7 days', 7], ['30 days', 30], ['90 days', 90], ['All time', 0]];

const TAB_LABELS = { vibe: 'Vibe', canvas: 'Canvas', freshDrops: 'Fresh Drops', bank: 'Bank', status: 'Status', settings: 'Settings' };
const SCREEN_LABELS = {
  canvasEditor: 'Canvas editor (a template)',
  stickerEditor: 'Sticker editor',
  category: 'Category page',
  collection: 'Collection page',
  templateCategory: 'Template row — View all',
  bankDetail: 'A bank',
  tagPage: 'Popular tag page',
  trendingCard: 'Trending card',
  trendingAll: 'Trending — See all',
  favorites: 'Favorites',
  myStickers: 'My Stickers',
  status: 'Status saver',
};
const SENT_LABELS = {
  whatsapp: 'WhatsApp — packs',
  whatsappSingle: 'WhatsApp — single stickers',
  gif: 'WhatsApp — GIF',
  telegram: 'Telegram',
  share: 'Share sheet',
};

const compact = new Intl.NumberFormat('en', { notation: 'compact', maximumFractionDigits: 1 });
const full = new Intl.NumberFormat('en');

/** Local-date yyyy-MM-dd — the same day boundary the app uses on the phone. */
function dayId(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
function lastNDays(n) {
  const today = new Date();
  return Array.from({ length: n }, (_, i) => {
    const d = new Date(today);
    d.setDate(today.getDate() - (n - 1 - i));
    return dayId(d);
  });
}
function shortDate(id) {
  const [y, m, d] = id.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('en', { month: 'short', day: 'numeric' });
}

/** Adds up one map field (e.g. `tabs`) across the selected days. */
function sumMap(rows, pick) {
  const out = {};
  for (const r of rows) for (const [k, v] of Object.entries(pick(r) || {})) out[k] = (out[k] || 0) + (Number(v) || 0);
  return out;
}

export default function AnalyticsDashboard({ onClose }) {
  const [range, setRange] = useState(7);
  const [rows, setRows] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [names, setNames] = useState({ categories: {}, templates: {}, stickers: {} });

  // Ids → names/thumbnails, loaded once. A lookup failing just leaves raw ids showing.
  useEffect(() => {
    Promise.all([api.listCategories(), api.listCanvasTemplates(), api.listAllStickers()])
      .then(([c, t, s]) => setNames({
        categories: Object.fromEntries(c.map((x) => [x.id, x.name])),
        templates: Object.fromEntries(t.map((x) => [x.id, x.title])),
        stickers: Object.fromEntries(s.map((x) => [x.id, x])),
      }))
      .catch(() => {});
  }, []);

  useEffect(() => {
    let live = true;
    setLoading(true); setError('');
    (range === 0 ? api.getAllAnalytics() : api.getAnalytics(lastNDays(range)))
      .then((r) => { if (live) setRows(r); })
      .catch((e) => { if (live) setError(e.message); })
      .finally(() => { if (live) setLoading(false); });
    return () => { live = false; };
  }, [range]);

  const data = useMemo(() => {
    if (!rows) return null;
    const total = (k) => rows.reduce((a, r) => a + (Number(r.daily[k]) || 0), 0);
    return {
      appOpens: total('appOpens'),
      deviceDays: total('activeDevices'),
      newInstalls: total('newInstalls'),
      stickerViews: total('stickerViews'),
      stickersSent: total('stickersDumped'),
      searches: total('searches'),
      statusSaves: total('statusSaves'),
      days: Math.max(1, rows.length),
      tabs: sumMap(rows, (r) => r.daily.tabs),
      screens: sumMap(rows, (r) => r.daily.screens),
      sent: sumMap(rows, (r) => r.daily.dumps),
      categories: sumMap(rows, (r) => r.daily.categories),
      templates: sumMap(rows, (r) => r.daily.templates),
      searchTerms: sumMap(rows, (r) => r.daily.searchTerms),
      stickerViewsById: sumMap(rows, (r) => r.stickers.views),
      stickerSentById: sumMap(rows, (r) => r.stickers.dumps),
      perDay: rows.map((r) => ({
        day: r.day,
        appOpens: Number(r.daily.appOpens) || 0,
        stickerViews: Number(r.daily.stickerViews) || 0,
        stickersSent: Number(r.daily.stickersDumped) || 0,
      })),
    };
  }, [rows]);

  const stickerLabel = (id) => (names.stickers[id] ? (names.stickers[id].tags || []).slice(0, 2).map((t) => `#${t}`).join(' ') || 'Sticker' : 'Made in the app');
  // Everything a sticker can be found by in the insights search: its tags, its category, its id.
  const stickerSearchText = (id) => {
    const st = names.stickers[id];
    if (!st) return `made in the app ${id}`.toLowerCase();
    return [...(st.tags || []), names.categories[st.categoryId] || '', id].join(' ').toLowerCase();
  };
  const stickerThumb = (id) => {
    const url = names.stickers[id]?.imageUrl;
    if (!url) return <span className="an-thumb" />;
    return url.endsWith('.mp4')
      ? <video className="an-thumb" src={url} muted />
      : <img className="an-thumb" src={url} alt="" />;
  };

  return (
    <section className="card">
      <div className="card-head">
        <div>
          <h2>Analytics</h2>
          <p className="hint">
            Counted anonymously by the app — totals only, nothing per person. A "download" is a
            sticker sent out of the app (WhatsApp, Telegram, share). Numbers start from the app
            version that added this, and arrive within about 30 seconds (or when the app is
            closed). Days follow the phone's own date.
          </p>
        </div>
        <button className="link" onClick={onClose}>← Back to catalog</button>
      </div>

      <div className="an-filters" role="group" aria-label="Date range">
        {RANGES.map(([label, n]) => (
          <button key={n} className={range === n ? 'primary' : ''} aria-pressed={range === n} onClick={() => setRange(n)}>{label}</button>
        ))}
      </div>

      {error && <p className="error">{error}</p>}
      {!data && loading && <p className="muted">Loading…</p>}

      {data && (
        <div style={{ opacity: loading ? 0.5 : 1, transition: 'opacity 150ms' }}>
          <div className="an-tiles">
            <Tile label="App opens" value={data.appOpens} />
            <Tile
              label={range === 1 ? 'Active devices' : 'Active devices per day (avg)'}
              value={Math.round(data.deviceDays / (range || data.days))}
            />
            <Tile label="New installs" value={data.newInstalls} />
            <Tile label="Stickers viewed" value={data.stickerViews} />
            <Tile label="Sticker downloads" value={data.stickersSent} />
            <Tile label="Searches" value={data.searches} />
            <Tile label="Statuses saved" value={data.statusSaves} />
          </div>

          {data.perDay.length > 1 && <DailyColumns days={data.perDay} />}

          <div className="an-grid">
            <Ranked title="Tabs opened" entries={data.tabs} label={(k) => TAB_LABELS[k] || k} limit={10} />
            <Ranked title="Screens opened" entries={data.screens} label={(k) => SCREEN_LABELS[k] || k} limit={12} />
            <Ranked title="Downloads, by where" entries={data.sent} label={(k) => SENT_LABELS[k] || k} note="Counts download actions, not stickers." />
            <Ranked title="Top categories" entries={data.categories} label={(k) => names.categories[k] || `(deleted) ${k}`} />
            <Ranked title="Top templates opened" entries={data.templates} label={(k) => names.templates[k] || `(deleted) ${k}`} />
            <Ranked title="Top searches" entries={data.searchTerms} label={(k) => k} />
            <Ranked title="Most viewed stickers" entries={data.stickerViewsById} label={stickerLabel} thumb={stickerThumb} />
            <Ranked title="Most downloaded stickers" entries={data.stickerSentById} label={stickerLabel} thumb={stickerThumb} />
          </div>

          <StickerInsights views={data.stickerViewsById} downloads={data.stickerSentById} label={stickerLabel} thumb={stickerThumb} searchText={stickerSearchText} />
        </div>
      )}
    </section>
  );
}

function Tile({ label, value }) {
  return (
    <div className="an-tile" title={full.format(value)}>
      <div className="an-tile-label">{label}</div>
      <div className="an-tile-value">{value >= 10000 ? compact.format(value) : full.format(value)}</div>
    </div>
  );
}

/** App opens per day — one series, so no legend; the heading names it. */
function DailyColumns({ days }) {
  const max = Math.max(1, ...days.map((d) => d.appOpens));
  const [showTable, setShowTable] = useState(false);
  const ticks = [0, Math.floor((days.length - 1) / 2), days.length - 1];
  return (
    <section className="an-card">
      <div className="an-card-head">
        <h3>App opens per day</h3>
        <button className="link" onClick={() => setShowTable((v) => !v)}>{showTable ? 'Show chart' : 'Show table'}</button>
      </div>
      {showTable ? (
        <table className="an-table">
          <thead><tr><th>Day</th><th>App opens</th><th>Stickers viewed</th><th>Stickers sent</th></tr></thead>
          <tbody>
            {[...days].reverse().map((d) => (
              <tr key={d.day}><td>{shortDate(d.day)}</td><td>{full.format(d.appOpens)}</td><td>{full.format(d.stickerViews)}</td><td>{full.format(d.stickersSent)}</td></tr>
            ))}
          </tbody>
        </table>
      ) : (
        <>
          <div className="an-cols-scale muted small">{full.format(max)}</div>
          <div className="an-cols" role="img" aria-label="App opens per day — use Show table for the numbers">
            {days.map((d) => (
              <div key={d.day} className="an-col-hit" title={`${shortDate(d.day)}: ${full.format(d.appOpens)} app opens`}>
                <div className="an-col" style={{ height: `${(d.appOpens / max) * 100}%` }} />
              </div>
            ))}
          </div>
          <div className="an-cols-axis muted small">
            {ticks.map((i, n) => <span key={n}>{shortDate(days[i].day)}</span>)}
          </div>
        </>
      )}
    </section>
  );
}

/** A ranked list: name, a bar scaled to the top entry, and the exact number. */
function Ranked({ title, entries, label, thumb, note, limit = 10 }) {
  const sorted = Object.entries(entries || {}).filter(([, v]) => v > 0).sort((a, b) => b[1] - a[1]).slice(0, limit);
  const max = sorted[0]?.[1] || 1;
  return (
    <section className="an-card">
      <h3>{title}</h3>
      {note && <p className="muted small" style={{ margin: '0 0 8px' }}>{note}</p>}
      {sorted.length === 0 ? (
        <p className="muted small">Nothing yet for this range.</p>
      ) : (
        <div className="an-rank">
          {sorted.map(([k, v]) => (
            <div key={k} className="an-row" title={`${label(k)}: ${full.format(v)}`}>
              {thumb && thumb(k)}
              <span className="an-name">{label(k)}</span>
              <span className="an-bar"><span style={{ width: `${Math.max(2, (v / max) * 100)}%` }} /></span>
              <span className="an-val">{full.format(v)}</span>
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

/**
 * Every sticker with any activity in the range: views, downloads, and the share of views that
 * turned into a download. Sortable by any number column; top 50 until expanded.
 */
function StickerInsights({ views, downloads, label, thumb, searchText }) {
  const [sortBy, setSortBy] = useState('downloads');
  const [all, setAll] = useState(false);
  const [query, setQuery] = useState('');
  const allRows = Object.keys({ ...views, ...downloads }).map((id) => {
    const v = views[id] || 0;
    const d = downloads[id] || 0;
    return { id, views: v, downloads: d, rate: v ? d / v : 0 };
  });
  // Every word typed has to match (tag, category or id); a leading # is optional.
  const terms = query.toLowerCase().split(/\s+/).map((t) => t.replace(/^#/, '')).filter(Boolean);
  const rows = terms.length ? allRows.filter((r) => { const hay = searchText(r.id); return terms.every((t) => hay.includes(t)); }) : allRows;
  rows.sort((a, b) => b[sortBy] - a[sortBy] || b.downloads - a.downloads || b.views - a.views);
  const shown = all || terms.length ? rows : rows.slice(0, 50);
  const head = (key, text) => (
    <th>
      <button className="link" aria-pressed={sortBy === key} style={{ fontWeight: sortBy === key ? 800 : 400 }} onClick={() => setSortBy(key)}>
        {text}{sortBy === key ? ' ▾' : ''}
      </button>
    </th>
  );
  return (
    <section className="an-card" style={{ marginTop: 12 }}>
      <h3>Sticker insights</h3>
      {allRows.length > 0 && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10, flexWrap: 'wrap' }}>
          <input
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search stickers by tag, category or id…"
            aria-label="Search sticker insights"
            style={{ flex: '1 1 260px', maxWidth: 420 }}
          />
          {terms.length > 0 && <span className="muted small">{rows.length} of {allRows.length} stickers</span>}
        </div>
      )}
      {allRows.length === 0 ? (
        <p className="muted small">No sticker activity for this range yet.</p>
      ) : rows.length === 0 ? (
        <p className="muted small">No stickers match “{query}”.</p>
      ) : (
        <>
          <table className="an-table">
            <thead>
              <tr><th>Sticker</th>{head('views', 'Views')}{head('downloads', 'Downloads')}{head('rate', 'Download rate')}</tr>
            </thead>
            <tbody>
              {shown.map((r) => (
                <tr key={r.id}>
                  <td><span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>{thumb(r.id)}{label(r.id)}</span></td>
                  <td>{full.format(r.views)}</td>
                  <td>{full.format(r.downloads)}</td>
                  <td>{r.views ? `${Math.round(r.rate * 100)}%` : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {rows.length > 50 && !terms.length && (
            <button className="link" style={{ marginTop: 8 }} onClick={() => setAll((v) => !v)}>
              {all ? 'Show top 50' : `Show all ${rows.length}`}
            </button>
          )}
        </>
      )}
    </section>
  );
}
