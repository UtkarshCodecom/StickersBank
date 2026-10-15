import { useEffect, useRef, useState, useCallback } from 'react';
import * as api from './db';
import { uploadToR2 } from './api';

/**
 * Admin sticker editor — a faithful web port of the app's editor. Same tools: a left rail
 * (Photo/Text/Sticker/Overlay/Emoji/Shape/More), a 512² checkerboard canvas, and the six
 * bottom tabs (Adjust / Filter / Draw / Erase / Layer / Text) with every control. Composes on
 * an HTML canvas, then pushes the result straight through the existing R2 + Firestore pipeline.
 */

const SIZE = 512;

const FONTS = [
  { label: 'Default', css: '"Poppins", sans-serif' },
  { label: 'Bangers', css: '"Bangers", system-ui' },
  { label: 'Anton', css: '"Anton", sans-serif' },
  { label: 'Bebas', css: '"Bebas Neue", sans-serif' },
  { label: 'Oswald', css: '"Oswald", sans-serif' },
  { label: 'Righteous', css: '"Righteous", cursive' },
  { label: 'Pacifico', css: '"Pacifico", cursive' },
  { label: 'Lobster', css: '"Lobster", cursive' },
  { label: 'Impact', css: 'Impact, "Arial Black", sans-serif' },
  { label: 'Serif', css: 'Georgia, serif' },
];
const TEXT_COLORS = ['#ffffff', '#000000', '#FFC107', '#E5484D', '#46A758', '#3B82F6', '#EC4899', '#F59E0B', '#8B5CF6', '#14B8A6'];
const BG_COLORS = [null, '#000000', '#ffffff', '#FFC107', '#E5484D', '#46A758', '#3B82F6', '#EC4899', '#F59E0B', '#8B5CF6'];
const HIGHLIGHTS = [null, '#000000', '#ffffff', '#FFC107', '#E5484D', '#3B82F6', '#EC4899'];
const FILTERS = ['Original', 'Mono', 'Sepia', 'Vivid', 'Cool', 'Warm', 'Noir', 'Fade'];
const EMOJIS = '😀 😂 🥰 😍 😎 🤩 😭 😡 👍 🙏 🔥 💯 🎉 ❤️ ✨ ⭐ 💥 👑 💀 🤡 🥳 😴 🤔 😱'.split(' ');
const SHAPES = ['circle', 'square', 'rounded', 'triangle', 'heart'];

const ADJUSTS = [
  { key: 'brightness', label: 'Brightness', ic: '☀', min: -100, max: 100, step: 5, def: 0, fmt: (v) => `${Math.round(v)}` },
  { key: 'contrast', label: 'Contrast', ic: '◐', min: 0.5, max: 2, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` },
  { key: 'saturation', label: 'Saturation', ic: '💧', min: 0, max: 2, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` },
  { key: 'warmth', label: 'Warmth', ic: '🌡', min: -100, max: 100, step: 5, def: 0, fmt: (v) => `${Math.round(v)}` },
  { key: 'shadows', label: 'Shadows', ic: '◑', min: -100, max: 100, step: 5, def: 0, fmt: (v) => `${Math.round(v)}` },
  { key: 'highlights', label: 'Highlights', ic: '☼', min: -100, max: 100, step: 5, def: 0, fmt: (v) => `${Math.round(v)}` },
];
const LAYER_TOOLS = [
  { key: 'alpha', label: 'Opacity', ic: '💧', min: 0.1, max: 1, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` },
  { key: 'scale', label: 'Size', ic: '⤢', min: 0.2, max: 4, step: 0.1, def: 1, fmt: (v) => `${Math.round(v * 100)}%` },
  { key: 'rotation', label: 'Rotate', ic: '↻', min: -180, max: 180, step: 5, def: 0, fmt: (v) => `${Math.round(v)}°` },
];

let uid = 1;

/**
 * Loads an already-uploaded template image back into the editor. `_url` marks it as "already in
 * R2" so saving reuses that URL rather than re-uploading (see `up` in saveTemplate). Resolves
 * null instead of rejecting: one dead image shouldn't stop a template from opening.
 */
function loadRemoteImage(url) {
  return new Promise((resolve) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => { img._url = url; resolve(img); };
    img.onerror = () => resolve(null);
    img.src = url;
  });
}

const clamp = (v, a, b) => Math.max(a, Math.min(b, v));

function presetFilter(p) {
  switch (p) {
    case 'Mono': return 'grayscale(1)';
    case 'Sepia': return 'sepia(0.75)';
    case 'Vivid': return 'saturate(1.6)';
    case 'Cool': return 'saturate(1.1) hue-rotate(-12deg) brightness(1.02)';
    case 'Warm': return 'sepia(0.35) saturate(1.2) brightness(1.02)';
    case 'Noir': return 'grayscale(1) contrast(1.35) brightness(0.92)';
    case 'Fade': return 'contrast(0.82) brightness(1.12) saturate(0.82)';
    default: return '';
  }
}

/**
 * `template` turns this into template-authoring mode: instead of flattening the canvas to a
 * PNG and uploading it as a sticker, it saves the canvas *configuration* (background, base
 * photo, and every layer with its relative position) so the Android editor can rebuild it.
 * Shape: { title, image, textColor, bgColor } — the thumbnail the admin already filled in.
 */
export default function Editor({ onClose, template = null, onTemplateSaved }) {
  const canvasRef = useRef(null);
  const dragRef = useRef(null);
  const strokeRef = useRef(null);
  const cropRef = useRef(null);

  const [mode, setMode] = useState('sticker'); // 'sticker' | 'anim'

  // The canvas is SIZE (512) wide always — the reference scale every position/size field is
  // stored relative to — but its HEIGHT follows the template's own ratio (width/height) instead
  // of always being forced square. Plain sticker mode has no ratio concept, so it stays square.
  // A template whose own ratio is unset (0, "Default") falls back to the same 3.2:5 its card
  // already shows in the app's browse grid (DEFAULT_TEMPLATE_RATIO there) — not to square.
  const DEFAULT_TEMPLATE_RATIO = 3.2 / 5;
  const H = template ? SIZE / (template.ratio > 0 ? template.ratio : DEFAULT_TEMPLATE_RATIO) : SIZE;

  // Base image + transform + adjustments
  const [baseImg, setBaseImg] = useState(null);
  // stretchY = height ÷ width scale, ≠ 1 only for a photo stretched by the app editor's corner
  // handles (this editor always scales uniformly); kept so a re-save here doesn't flatten it.
  const [base, setBase] = useState({ x: 0, y: 0, scale: 1, rotation: 0, flipX: false, stretchY: 1, alpha: 1 });
  const [adj, setAdj] = useState({ brightness: 0, contrast: 1, saturation: 1, warmth: 0, shadows: 0, highlights: 0 });
  const [preset, setPreset] = useState('Original');
  const [bgColor, setBgColor] = useState(null);

  // Crop marquee, in 512-canvas coords, while the Crop tab is open. null = nothing drawn yet.
  const [crop, setCrop] = useState(null);
  // 'free' (any rectangle) · 'square' (locked 1:1) · 'circle' (locked 1:1, masked round).
  const [cropShape, setCropShape] = useState('free');

  const [drawStrokes, setDrawStrokes] = useState([]);
  const [eraseStrokes, setEraseStrokes] = useState([]);
  const [drawColor, setDrawColor] = useState('#FFC107');
  const [drawWidth, setDrawWidth] = useState(16);
  const [brush, setBrush] = useState(60);

  const [items, setItems] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  // Built up via each item's own multi-select handle (a small fixed-corner circle, template
  // mode only) — separate from `selectedId`, which stays the plain "click the item" selection.
  const [multiSelectedIds, setMultiSelectedIds] = useState(new Set());

  const [tab, setTab] = useState('Adjust');
  const [adjTool, setAdjTool] = useState('brightness');
  const [layerTool, setLayerTool] = useState('alpha');
  const [rail, setRail] = useState(0);
  const [moreOpen, setMoreOpen] = useState(false);

  const [textDialog, setTextDialog] = useState(null); // { value, editId } | null
  const [emojiPicker, setEmojiPicker] = useState(false);
  const [shapePicker, setShapePicker] = useState(false);

  // ---- Canva-style template chrome (template mode only) ----
  // Which docked panel is open (null = the plain tool row), and which modal sheet is up.
  const [tplPanel, setTplPanel] = useState(null);   // 'adjust' | 'filter' | 'background' | 'text' | 'layer'
  const [tplSheet, setTplSheet] = useState(null);   // 'photo' | 'overlay' | 'shape' | 'text'
  // Destination
  const [cats, setCats] = useState([]); const [groups, setGroups] = useState([]); const [colls, setColls] = useState([]);
  const [catId, setCatId] = useState(''); const [groupId, setGroupId] = useState(''); const [collId, setCollId] = useState('');
  const [tags, setTags] = useState('');
  const [busy, setBusy] = useState(false); const [msg, setMsg] = useState('');

  const selected = items.find((l) => l.id === selectedId) || null;
  const selText = selected && selected.type === 'text' ? selected : null;
  const isEmpty = !baseImg && items.length === 0 && drawStrokes.length === 0 && !bgColor;

  useEffect(() => { api.listCategories().then(setCats).catch((e) => setMsg(e.message)); }, []);
  useEffect(() => { setGroupId(''); setColls([]); setCollId(''); if (catId) api.listGroups(catId).then(setGroups); else setGroups([]); }, [catId]);
  useEffect(() => { setCollId(''); if (groupId) api.listCollections(groupId).then(setColls); else setColls([]); }, [groupId]);

  /* ---------------- render ---------------- */
  const renderTo = useCallback((ctx, { forExport = false } = {}) => {
    ctx.clearRect(0, 0, SIZE, H);
    if (bgColor) { ctx.fillStyle = bgColor; ctx.fillRect(0, 0, SIZE, H); }

    // Base image (with adjustments + erase) drawn to an offscreen so erase only hits the photo.
    if (baseImg) {
      const off = document.createElement('canvas'); off.width = SIZE; off.height = H;
      const o = off.getContext('2d');
      o.save();
      const b = 1 + adj.brightness / 100 + adj.shadows / 500 + adj.highlights / 700;
      o.filter = `brightness(${b}) contrast(${adj.contrast}) saturate(${adj.saturation}) ${presetFilter(preset)}`.trim();
      o.translate(SIZE / 2 + base.x, H / 2 + base.y);
      o.rotate((base.rotation * Math.PI) / 180);
      o.scale(base.scale * (base.flipX ? -1 : 1), base.scale * (base.stretchY ?? 1));
      o.globalAlpha = base.alpha ?? 1;
      const s = Math.min(SIZE / baseImg.width, H / baseImg.height);
      o.drawImage(baseImg, (-baseImg.width * s) / 2, (-baseImg.height * s) / 2, baseImg.width * s, baseImg.height * s);
      o.restore();
      if (adj.warmth !== 0) {
        o.save(); o.globalCompositeOperation = 'source-atop';
        o.globalAlpha = Math.min(Math.abs(adj.warmth) / 100 * 0.35, 0.35);
        o.fillStyle = adj.warmth > 0 ? '#ff9028' : '#2f7bff';
        o.fillRect(0, 0, SIZE, H); o.restore();
      }
      o.save(); o.globalCompositeOperation = 'destination-out'; o.lineCap = 'round'; o.lineJoin = 'round';
      for (const st of eraseStrokes) strokePath(o, st.points, st.width);
      o.restore();
      ctx.drawImage(off, 0, 0);
    }

    for (const l of items) drawItem(ctx, l, forExport);
    for (const st of drawStrokes) { ctx.strokeStyle = st.color; strokePath(ctx, st.points, st.width, true); }
    if (strokeRef.current && strokeRef.current.kind === 'draw') { ctx.strokeStyle = strokeRef.current.color; strokePath(ctx, strokeRef.current.points, strokeRef.current.width, true); }

    // The base/background photo's own resize+rotate border — shown whenever it's the thing
    // being worked on (nothing else selected), same handle set as every item, but every handle
    // scales it UNIFORMLY (see baseHandleHitTest/onMove's resizeBase branch) rather than
    // stretching one axis, so a real photo never comes out distorted.
    if (!forExport && baseImg && !selectedId) {
      const nat = baseImgBox();
      const w = nat.w * base.scale, h = nat.h * base.scale * (base.stretchY ?? 1);
      const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
      const rad = (base.rotation * Math.PI) / 180;
      const cos = Math.cos(rad), sin = Math.sin(rad);
      const toCanvasPt = (lx, ly) => ({ x: cx + lx * cos - ly * sin, y: cy + lx * sin + ly * cos });

      ctx.save();
      ctx.strokeStyle = '#FFC107'; ctx.lineWidth = 2; ctx.setLineDash([7, 5]);
      ctx.translate(cx, cy); ctx.rotate(rad);
      ctx.strokeRect(-w / 2 - 8, -h / 2 - 6, w + 16, h + 12);
      ctx.setLineDash([]); ctx.restore();

      for (const [sx, sy] of RESIZE_HANDLES) {
        const { x: hx, y: hy } = toCanvasPt(sx * (w / 2 + 8), sy * (h / 2 + 6));
        ctx.save();
        ctx.beginPath(); ctx.arc(hx, hy, HANDLE_R, 0, Math.PI * 2);
        ctx.fillStyle = '#ffffff'; ctx.fill();
        ctx.lineWidth = 1.5; ctx.strokeStyle = '#FFC107'; ctx.stroke();
        ctx.restore();
      }
      const topEdge = toCanvasPt(0, -(h / 2 + 6));
      const rotateHandle = toCanvasPt(0, -(h / 2 + 6) - ROTATE_HANDLE_OFFSET);
      ctx.save();
      ctx.strokeStyle = '#FFC107'; ctx.lineWidth = 1.5;
      ctx.beginPath(); ctx.moveTo(topEdge.x, topEdge.y); ctx.lineTo(rotateHandle.x, rotateHandle.y); ctx.stroke();
      ctx.beginPath(); ctx.arc(rotateHandle.x, rotateHandle.y, HANDLE_R, 0, Math.PI * 2);
      ctx.fillStyle = '#ffffff'; ctx.fill();
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(rotateHandle.x, rotateHandle.y, 4.5, 0.6, Math.PI * 1.7);
      ctx.stroke();
      ctx.restore();
    }

    // Crop marquee — screen only (forExport skips it), so it never bakes into the sticker.
    if (!forExport && crop && crop.w > 0 && crop.h > 0) {
      ctx.save();
      ctx.fillStyle = 'rgba(0,0,0,0.45)';
      ctx.beginPath();
      ctx.rect(0, 0, SIZE, H);
      ctx.rect(crop.x, crop.y, crop.w, crop.h);
      // even-odd → the inner shape cuts a hole, leaving only the surround dimmed. The hole is
      // the crop's actual SHAPE, so an ellipse crop previews as an ellipse before you apply it.
      if (cropShape === 'circle') {
        ctx.ellipse(crop.x + crop.w / 2, crop.y + crop.h / 2, crop.w / 2, crop.h / 2, 0, 0, Math.PI * 2);
      } else {
        ctx.rect(crop.x, crop.y, crop.w, crop.h);
      }
      ctx.fill('evenodd');
      ctx.strokeStyle = '#FFC107';
      ctx.lineWidth = 2;
      if (cropShape === 'circle') {
        ctx.beginPath();
        ctx.ellipse(crop.x + crop.w / 2, crop.y + crop.h / 2, crop.w / 2, crop.h / 2, 0, 0, Math.PI * 2);
        ctx.stroke();
        // Faint bounding box too, so the corner handles still read as the thing you drag.
        ctx.save();
        ctx.globalAlpha = 0.4;
        ctx.setLineDash([5, 4]);
        ctx.strokeRect(crop.x, crop.y, crop.w, crop.h);
        ctx.restore();
      } else {
        ctx.strokeRect(crop.x, crop.y, crop.w, crop.h);
      }
      ctx.fillStyle = '#FFC107';
      for (const [hx, hy] of [[crop.x, crop.y], [crop.x + crop.w, crop.y], [crop.x, crop.y + crop.h], [crop.x + crop.w, crop.y + crop.h]]) {
        ctx.fillRect(hx - 4, hy - 4, 8, 8);
      }
      ctx.restore();
    }
  }, [baseImg, base, adj, preset, bgColor, items, drawStrokes, eraseStrokes, selectedId, crop, cropShape, template]);

  useEffect(() => { const c = canvasRef.current; if (c) renderTo(c.getContext('2d')); });

  // ---- Reopening a saved template -------------------------------------------------------
  // Rebuild the canvas from the config that was stored when it was created, so an existing
  // template can be adjusted instead of only rebuilt from scratch. Layers round-trip almost
  // 1:1 (the save above writes the same field names), so this is mostly a rename of `url` back
  // into a loaded <img>. Runs once per template.
  const loadedTemplateId = useRef(null);
  useEffect(() => {
    const cfg = template?.config;
    if (!template?.id || !cfg || loadedTemplateId.current === template.id) return;
    loadedTemplateId.current = template.id;

    if (cfg.bgColor) setBgColor(cfg.bgColor);
    if (cfg.base) setBase({
      x: cfg.base.x || 0, y: cfg.base.y || 0, scale: cfg.base.scale || 1, rotation: cfg.base.rotation || 0, flipX: !!cfg.base.flipX,
      stretchY: cfg.base.scaleY && cfg.base.scale ? cfg.base.scaleY / cfg.base.scale : 1,
      alpha: cfg.base.alpha ?? 1,
    });
    if (cfg.baseImage) loadRemoteImage(cfg.baseImage).then((img) => { if (img) setBaseImg(img); });

    const rebuilt = [];
    for (const l of cfg.layers || []) {
      const common = { id: uid++, xr: l.xr, yr: l.yr, scale: l.scale ?? 1, rotation: l.rotation || 0, alpha: l.alpha ?? 1 };
      if (l.type === 'text') {
        rebuilt.push({
          ...common, type: 'text', text: l.text, color: l.color, size: l.size,
          bold: !!l.bold, italic: !!l.italic, align: l.align || 'center',
          outline: !!l.outline, outlineColor: l.outlineColor || '#000000', outlineWidth: l.outlineWidth || 0,
          letterSpacing: l.letterSpacing || 0, lineHeight: l.lineHeight || 1.2, highlight: l.highlight || '',
          gradient: !!l.gradient, color2: l.gradientColor2 || '',
          shadow: !!l.shadow, shadowColor: l.shadowColor || '#000000', shadowBlur: l.shadowBlur || 0,
          glow: !!l.glow, glowColor: l.glowColor || '',
          skewX: l.skewX || 0, bend: l.bend || 0,
          stretchX: l.stretchX ?? 1, stretchY: l.stretchY ?? 1,
        });
      } else if (l.type === 'emoji') {
        rebuilt.push({ ...common, type: 'emoji', text: l.text, size: l.size, baseSize: l.size });
      } else if (l.type === 'shape') {
        rebuilt.push({ ...common, type: 'shape', shapeKind: l.shapeKind, shapeColor: l.shapeColor, baseSize: l.baseSize || 200 });
      } else if (l.type === 'image' && l.url) {
        const item = {
          ...common, type: 'image', img: null, baseSize: l.baseSize || 220,
          baseW: l.baseW || null, baseH: l.baseH || null,
          strokeW: l.strokeW || 0, strokeColor: l.strokeColor || '#ffffff',
        };
        rebuilt.push(item);
        // Streams in on its own so one slow image can't hold up the rest of the rebuild.
        loadRemoteImage(l.url).then((img) => {
          if (img) setItems((ls) => ls.map((x) => (x.id === item.id ? { ...x, img } : x)));
        });
      }
    }
    if (rebuilt.length) setItems(rebuilt);
  }, [template]);

  function strokePath(ctx, pts, width, round) {
    if (!pts.length) return;
    if (round) { ctx.lineCap = 'round'; ctx.lineJoin = 'round'; }
    ctx.lineWidth = width; ctx.beginPath();
    pts.forEach((p, i) => (i ? ctx.lineTo(p.x, p.y) : ctx.moveTo(p.x, p.y)));
    if (pts.length === 1) { ctx.lineTo(pts[0].x + 0.1, pts[0].y + 0.1); }
    ctx.stroke();
  }

  const HANDLE_R = 11;
  // PowerPoint/Office-style selection border: 4 corner handles (resize both axes at once,
  // anchored at the opposite corner) + 4 edge-midpoint handles (resize just that one axis,
  // anchored at the opposite edge — same math, sx or sy is just 0 for these). 'tl' is skipped
  // in template mode since the multi-select toggle circle already lives exactly there.
  const RESIZE_HANDLES = [
    [-1, -1, 'tl'], [0, -1, 't'], [1, -1, 'tr'],
    [-1, 0, 'l'], [1, 0, 'r'],
    [-1, 1, 'bl'], [0, 1, 'b'], [1, 1, 'br'],
  ];
  // How far beyond the top-edge handle's own position the rotate handle sits, canvas px.
  const ROTATE_HANDLE_OFFSET = 34;
  /** Canvas-space center of an item's multi-select handle — a fixed spot (its box's top-left
   * corner, same place the selection outline starts) so it's always in the same place on every
   * item. Ignores rotation, same simplification `hitTest` already makes for the item body. */
  function handleCenter(ctx, l) {
    const box = itemBox(ctx, l);
    return { x: l.xr * SIZE - box.w / 2 - 8, y: l.yr * H - box.h / 2 - 6 };
  }

  function itemFontStr(l) { return `${l.italic ? 'italic ' : ''}${l.bold ? '700' : '400'} ${l.size}px ${l.font}`; }
  /** Text's natural (unstretched) box, purely from font metrics — the size it would be at
   * `scale=1` with no corner/edge resize ever applied. */
  function measureTextBox(ctx, l) {
    ctx.save(); ctx.font = itemFontStr(l); ctx.letterSpacing = `${l.letterSpacing}px`;
    const lines = l.text.split('\n');
    const w = Math.max(1, ...lines.map((t) => ctx.measureText(t).width));
    const h = l.size * l.lineHeight * lines.length;
    ctx.restore();
    return { w, h };
  }

  /** The base/background photo's box at `base.scale = 1` — the "fit" size that lands it fully
   * inside the canvas (letterboxed on whichever axis doesn't exactly match), same math as the
   * actual drawImage call in renderTo. */
  function baseImgBox() {
    const s0 = Math.min(SIZE / baseImg.width, H / baseImg.height);
    return { w: baseImg.width * s0, h: baseImg.height * s0 };
  }

  // Every layer type now supports the same corner/edge resize border (see RESIZE_HANDLES) —
  // baseW/baseH (once a resize has actually happened) win over the type's own "natural" box.
  /** A text layer's own X/Y stretch (the app editors' "X stretch"/"Y stretch"); 1×1 otherwise. */
  function stretchOf(l) {
    return l.type === 'text' ? [l.stretchX ?? 1, l.stretchY ?? 1] : [1, 1];
  }

  function itemBox(ctx, l) {
    const [sx, sy] = stretchOf(l);
    if (l.baseW && l.baseH) return { w: l.baseW * l.scale * sx, h: l.baseH * l.scale * sy };
    if (l.type === 'text') {
      const nat = measureTextBox(ctx, l);
      return { w: nat.w * l.scale * sx, h: nat.h * l.scale * sy };
    }
    const d = (l.baseSize || 200) * l.scale;
    return { w: d, h: d };
  }

  function drawItem(ctx, l, forExport) {
    ctx.save();
    ctx.translate(l.xr * SIZE, l.yr * H);
    ctx.rotate((l.rotation * Math.PI) / 180);
    ctx.globalAlpha = l.alpha;
    if (l.type === 'text') {
      if (l.skewX) ctx.transform(1, 0, l.skewX, 1, 0, 0);
      ctx.scale(l.scale, l.scale);
      const [stX, stY] = stretchOf(l);
      ctx.scale(stX, stY);
      // Once a corner/edge handle has resized this box, stretch the natural (font-metrics) box
      // to fill it — same "just stretch the rendered content" approach as emoji/shape below.
      if (l.baseW && l.baseH) {
        const nat = measureTextBox(ctx, l);
        ctx.scale(l.baseW / Math.max(1, nat.w), l.baseH / Math.max(1, nat.h));
      }
      drawTextLayer(ctx, l);
    } else if (l.type === 'emoji') {
      // baseW drives the font size (its natural "width" reference); baseH/baseW stretches the
      // glyph vertically to fill a non-square box once resized — a plain scale transform works
      // for any canvas drawing, fillText included.
      const dw = (l.baseW && l.baseH ? l.baseW : l.baseSize) * l.scale;
      const dh = (l.baseW && l.baseH ? l.baseH : l.baseSize) * l.scale;
      ctx.save();
      ctx.scale(1, dh / dw);
      ctx.font = `${dw}px "Poppins", sans-serif`;
      ctx.textAlign = 'center'; ctx.textBaseline = 'middle';
      ctx.fillText(l.text, 0, 0);
      ctx.restore();
    } else if (l.type === 'image') {
      // Aspect-correct box (from the image's own w/h ratio) when known, else the legacy plain
      // square — see itemBox for why (older/no-ratio layers only ever had one `baseSize`).
      const dw = (l.baseW && l.baseH ? l.baseW : l.baseSize) * l.scale;
      const dh = (l.baseW && l.baseH ? l.baseH : l.baseSize) * l.scale;
      if (l.img) {
        // Sticker-style outline: stamp the image's own silhouette in the stroke colour
        // around the edge, then draw the real image on top. `source-in` recolours the
        // silhouette on a scratch canvas so the stroke follows the cut-out, not the box.
        if (l.strokeW > 0) {
          const pad = Math.ceil(l.strokeW) + 2;
          const sc = document.createElement('canvas');
          sc.width = Math.ceil(dw) + pad * 2; sc.height = Math.ceil(dh) + pad * 2;
          const sx = sc.getContext('2d');
          sx.drawImage(l.img, pad, pad, dw, dh);
          sx.globalCompositeOperation = 'source-in';
          sx.fillStyle = l.strokeColor || '#ffffff';
          sx.fillRect(0, 0, sc.width, sc.height);
          const steps = 16;
          for (let i = 0; i < steps; i++) {
            const a = (i / steps) * Math.PI * 2;
            ctx.drawImage(sc, -dw / 2 - pad + Math.cos(a) * l.strokeW, -dh / 2 - pad + Math.sin(a) * l.strokeW);
          }
        }
        ctx.drawImage(l.img, -dw / 2, -dh / 2, dw, dh);
      }
    } else if (l.type === 'shape') {
      // Same stretch trick as emoji: drawShape only knows how to draw a symmetric d×d shape, so
      // draw it at width dw and scale the Y axis by dh/dw to reach a non-square box.
      const dw = (l.baseW && l.baseH ? l.baseW : l.baseSize) * l.scale;
      const dh = (l.baseW && l.baseH ? l.baseH : l.baseSize) * l.scale;
      ctx.fillStyle = l.shapeColor;
      ctx.save();
      ctx.scale(1, dh / dw);
      drawShape(ctx, l.shapeKind, dw);
      ctx.restore();
    }
    ctx.restore();

    if (!forExport && (l.id === selectedId || multiSelectedIds.has(l.id))) {
      const box = itemBox(ctx, l);
      ctx.save();
      ctx.translate(l.xr * SIZE, l.yr * H);
      ctx.rotate((l.rotation * Math.PI) / 180);
      ctx.strokeStyle = multiSelectedIds.has(l.id) ? '#3B82F6' : '#FFC107'; ctx.lineWidth = 2; ctx.setLineDash([7, 5]);
      ctx.strokeRect(-box.w / 2 - 8, -box.h / 2 - 6, box.w + 16, box.h + 12);
      ctx.setLineDash([]); ctx.restore();
    }

    // Multi-select handle — a small circle fixed at the same corner on every item (template
    // mode only), so it can be tapped to add/remove this item from the group selection without
    // disturbing the normal click-to-select-and-drag behavior of clicking the item's own body.
    if (!forExport && template) {
      const on = multiSelectedIds.has(l.id);
      const c = handleCenter(ctx, l);
      ctx.save();
      ctx.beginPath(); ctx.arc(c.x, c.y, HANDLE_R, 0, Math.PI * 2);
      ctx.fillStyle = on ? '#3B82F6' : '#ffffff';
      ctx.fill();
      ctx.lineWidth = 1.5; ctx.strokeStyle = on ? '#ffffff' : '#3B82F6'; ctx.stroke();
      if (on) {
        ctx.strokeStyle = '#ffffff'; ctx.lineWidth = 2; ctx.lineCap = 'round'; ctx.lineJoin = 'round';
        ctx.beginPath();
        ctx.moveTo(c.x - 4, c.y); ctx.lineTo(c.x - 1.5, c.y + 3.5); ctx.lineTo(c.x + 4.5, c.y - 3.5);
        ctx.stroke();
      }
      ctx.restore();
    }

    // PowerPoint-style selection border — corner + edge resize handles, and a rotate handle on
    // a stem above the top edge. Any layer type, and only the one actually selected (not part
    // of a 2+ multi-select group, which drags as a whole instead). See
    // resizeHandleHitTest/rotateHandleHitTest for the matching hit-tests and onMove for the drag math.
    if (!forExport && l.id === selectedId && !(multiSelectedIds.has(l.id) && multiSelectedIds.size > 1)) {
      const box = itemBox(ctx, l);
      const cx = l.xr * SIZE, cy = l.yr * H;
      const rad = (l.rotation * Math.PI) / 180;
      const cos = Math.cos(rad), sin = Math.sin(rad);
      const toCanvasPt = (lx, ly) => ({ x: cx + lx * cos - ly * sin, y: cy + lx * sin + ly * cos });

      for (const [sx, sy, corner] of RESIZE_HANDLES) {
        if (corner === 'tl' && template) continue;
        const { x: hx, y: hy } = toCanvasPt(sx * (box.w / 2 + 8), sy * (box.h / 2 + 6));
        ctx.save();
        ctx.beginPath(); ctx.arc(hx, hy, HANDLE_R, 0, Math.PI * 2);
        ctx.fillStyle = '#ffffff'; ctx.fill();
        ctx.lineWidth = 1.5; ctx.strokeStyle = '#FFC107'; ctx.stroke();
        ctx.restore();
      }

      // Rotate handle: a small stem running up from the top edge to a circular-arrow handle.
      const topEdge = toCanvasPt(0, -(box.h / 2 + 6));
      const rotateHandle = toCanvasPt(0, -(box.h / 2 + 6) - ROTATE_HANDLE_OFFSET);
      ctx.save();
      ctx.strokeStyle = '#FFC107'; ctx.lineWidth = 1.5;
      ctx.beginPath(); ctx.moveTo(topEdge.x, topEdge.y); ctx.lineTo(rotateHandle.x, rotateHandle.y); ctx.stroke();
      ctx.beginPath(); ctx.arc(rotateHandle.x, rotateHandle.y, HANDLE_R, 0, Math.PI * 2);
      ctx.fillStyle = '#ffffff'; ctx.fill();
      ctx.stroke();
      // A little circular-arrow glyph inside the handle, so it doesn't look like a 9th resize dot.
      ctx.beginPath();
      ctx.arc(rotateHandle.x, rotateHandle.y, 4.5, 0.6, Math.PI * 1.7);
      ctx.stroke();
      ctx.restore();
    }
  }

  function drawTextLayer(ctx, l) {
    ctx.font = itemFontStr(l);
    ctx.textBaseline = 'middle';
    ctx.textAlign = l.align === 0 ? 'left' : l.align === 2 ? 'right' : 'center';
    ctx.letterSpacing = `${l.letterSpacing}px`;
    const lines = l.text.split('\n');
    const lh = l.size * l.lineHeight;
    const total = lh * lines.length;
    const maxW = Math.max(1, ...lines.map((t) => ctx.measureText(t).width));
    const ax = l.align === 0 ? -maxW / 2 : l.align === 2 ? maxW / 2 : 0;

    if (l.highlight) {
      ctx.save(); ctx.fillStyle = l.highlight;
      roundRect(ctx, -maxW / 2 - 14, -total / 2 - 6, maxW + 28, total + 12, 10); ctx.fill(); ctx.restore();
    }
    if (l.bend && lines.length === 1) { drawCurved(ctx, l, lines[0]); return; }
    lines.forEach((line, i) => paintLine(ctx, l, line, ax, -total / 2 + lh / 2 + i * lh));
  }

  function paintLine(ctx, l, text, x, y) {
    if (l.shadow) {
      ctx.save(); ctx.shadowColor = l.shadowColor; ctx.shadowBlur = l.shadowBlur;
      ctx.shadowOffsetX = l.shadowBlur * 0.5; ctx.shadowOffsetY = l.shadowBlur * 0.5;
      ctx.fillStyle = l.color; ctx.fillText(text, x, y); ctx.restore();
    }
    if (l.glow) {
      ctx.save(); ctx.shadowColor = l.glowColor; ctx.shadowBlur = l.size * 0.45; ctx.fillStyle = l.color;
      for (let k = 0; k < 3; k++) ctx.fillText(text, x, y); ctx.restore();
    }
    if (l.outline) {
      ctx.save(); ctx.lineWidth = l.outlineWidth; ctx.strokeStyle = l.outlineColor; ctx.lineJoin = 'round';
      ctx.strokeText(text, x, y); ctx.restore();
    }
    if (l.gradient) {
      const g = ctx.createLinearGradient(0, y - l.size / 2, 0, y + l.size / 2);
      g.addColorStop(0, l.color); g.addColorStop(1, l.color2); ctx.fillStyle = g;
    } else ctx.fillStyle = l.color;
    ctx.fillText(text, x, y);
  }

  function drawCurved(ctx, l, text) {
    const bend = clamp(l.bend / 100, -1, 1);
    ctx.font = itemFontStr(l);
    const w = Math.max(1, ctx.measureText(text).width);
    const sweep = Math.abs(bend) * 150 * Math.PI / 180;
    const radius = sweep < 0.01 ? 100000 : w / sweep;
    ctx.textAlign = 'center';
    let angle = -sweep / 2 * Math.sign(bend || 1);
    const dir = bend >= 0 ? 1 : -1;
    for (const ch of [...text]) {
      const cw = ctx.measureText(ch).width;
      const a = angle + (cw / 2 / radius) * dir;
      ctx.save();
      ctx.translate(Math.sin(a) * radius, dir * (radius - Math.cos(a) * radius));
      ctx.rotate(a * dir);
      paintLine(ctx, l, ch, 0, 0);
      ctx.restore();
      angle += (cw / radius) * dir;
    }
  }

  function drawShape(ctx, kind, d) {
    const r = d / 2;
    ctx.beginPath();
    if (kind === 'circle') ctx.arc(0, 0, r, 0, Math.PI * 2);
    else if (kind === 'square') ctx.rect(-r, -r, d, d);
    else if (kind === 'rounded') roundRect(ctx, -r, -r, d, d, d * 0.22);
    else if (kind === 'triangle') { ctx.moveTo(0, -r); ctx.lineTo(r, r); ctx.lineTo(-r, r); ctx.closePath(); }
    else if (kind === 'heart') {
      const x = 0, y = -r * 0.5;
      ctx.moveTo(x, y + r * 0.3);
      ctx.bezierCurveTo(x, y, x - r, y - r * 0.2, x - r, y + r * 0.35);
      ctx.bezierCurveTo(x - r, y + r, x, y + r * 1.1, x, y + r * 1.5);
      ctx.bezierCurveTo(x, y + r * 1.1, x + r, y + r, x + r, y + r * 0.35);
      ctx.bezierCurveTo(x + r, y - r * 0.2, x, y, x, y + r * 0.3);
    }
    ctx.fill();
  }

  function roundRect(ctx, x, y, w, h, r) {
    ctx.beginPath(); ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r); ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r); ctx.arcTo(x, y, x + w, y, r); ctx.closePath();
  }

  /* ---------------- pointer ---------------- */
  function toCanvas(e) {
    const r = canvasRef.current.getBoundingClientRect();
    return { x: ((e.clientX - r.left) / r.width) * SIZE, y: ((e.clientY - r.top) / r.height) * H };
  }
  function hitTest(p) {
    const ctx = canvasRef.current.getContext('2d');
    for (let i = items.length - 1; i >= 0; i--) {
      const l = items[i]; const box = itemBox(ctx, l);
      if (Math.abs(p.x - l.xr * SIZE) <= box.w / 2 + 12 && Math.abs(p.y - l.yr * H) <= box.h / 2 + 10) return l;
    }
    return null;
  }
  /** Multi-select handles float above everything else, so check them before the item bodies. */
  function handleHitTest(p) {
    const ctx = canvasRef.current.getContext('2d');
    for (let i = items.length - 1; i >= 0; i--) {
      const l = items[i]; const c = handleCenter(ctx, l);
      if (Math.hypot(p.x - c.x, p.y - c.y) <= HANDLE_R + 6) return l;
    }
    return null;
  }
  /** The currently-selected item, if eligible for the resize/rotate border — any layer type
   * (image, shape, emoji, text), just not part of an active 2+ multi-select group, which drags
   * as a whole instead. */
  function resizableSelected() {
    if (!selectedId) return null;
    if (multiSelectedIds.has(selectedId) && multiSelectedIds.size > 1) return null;
    return items.find((it) => it.id === selectedId) || null;
  }
  /**
   * PowerPoint-style corner + edge resize handles for the SELECTED image layer (only —
   * text/emoji/shape keep the slider-only "Size" control for now). Rotation-aware: a handle's
   * canvas-space position is its unrotated local offset from the item's own center, rotated by
   * `l.rotation` — same trick as the Android editor's `ResizeHandle`. The top-left corner is
   * skipped in template mode since the multi-select toggle circle already lives exactly there
   * (see `handleCenter`).
   */
  function resizeHandleHitTest(p) {
    const l = resizableSelected();
    if (!l) return null;
    const ctx = canvasRef.current.getContext('2d');
    const box = itemBox(ctx, l);
    const cx = l.xr * SIZE, cy = l.yr * H;
    const rad = (l.rotation * Math.PI) / 180;
    const cos = Math.cos(rad), sin = Math.sin(rad);
    for (const [sx, sy, corner] of RESIZE_HANDLES) {
      if (corner === 'tl' && template) continue;
      const lx = sx * (box.w / 2 + 8), ly = sy * (box.h / 2 + 6);
      const hx = cx + lx * cos - ly * sin, hy = cy + lx * sin + ly * cos;
      if (Math.hypot(p.x - hx, p.y - hy) <= HANDLE_R + 6) return { id: l.id, sx, sy };
    }
    return null;
  }
  /** The rotate handle on its stem above the top edge — drag it to spin the item around its
   * own center (see onMove's `rotate` branch for the angle math). */
  function rotateHandleHitTest(p) {
    const l = resizableSelected();
    if (!l) return null;
    const ctx = canvasRef.current.getContext('2d');
    const box = itemBox(ctx, l);
    const cx = l.xr * SIZE, cy = l.yr * H;
    const rad = (l.rotation * Math.PI) / 180;
    const cos = Math.cos(rad), sin = Math.sin(rad);
    const ly = -(box.h / 2 + 6) - ROTATE_HANDLE_OFFSET;
    const hx = cx - ly * sin, hy = cy + ly * cos;
    return Math.hypot(p.x - hx, p.y - hy) <= HANDLE_R + 6 ? { id: l.id } : null;
  }
  /** The base/background photo's own resize handles — only live when nothing else is selected
   * (matches the drawing condition in renderTo). Any handle scales it UNIFORMLY (see onMove's
   * resizeBase branch), so only the handle's presence, not which one, is returned. */
  function baseHandleHitTest(p) {
    if (selectedId || !baseImg) return null;
    const nat = baseImgBox();
    const w = nat.w * base.scale, h = nat.h * base.scale * (base.stretchY ?? 1);
    const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
    const rad = (base.rotation * Math.PI) / 180;
    const cos = Math.cos(rad), sin = Math.sin(rad);
    for (const [sx, sy] of RESIZE_HANDLES) {
      const lx = sx * (w / 2 + 8), ly = sy * (h / 2 + 6);
      const hx = cx + lx * cos - ly * sin, hy = cy + lx * sin + ly * cos;
      if (Math.hypot(p.x - hx, p.y - hy) <= HANDLE_R + 6) return true;
    }
    return false;
  }
  function baseRotateHandleHitTest(p) {
    if (selectedId || !baseImg) return null;
    const nat = baseImgBox();
    const h = nat.h * base.scale * (base.stretchY ?? 1);
    const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
    const rad = (base.rotation * Math.PI) / 180;
    const cos = Math.cos(rad), sin = Math.sin(rad);
    const ly = -(h / 2 + 6) - ROTATE_HANDLE_OFFSET;
    const hx = cx - ly * sin, hy = cy + ly * cos;
    return Math.hypot(p.x - hx, p.y - hy) <= HANDLE_R + 6;
  }
  function toggleMultiSelect(id) {
    setMultiSelectedIds((s) => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
    setSelectedId(id);
  }
  function onDown(e) {
    e.currentTarget.setPointerCapture?.(e.pointerId);
    const p = toCanvas(e);
    if (tab === 'Crop') {
      if (!baseImg) return;
      cropRef.current = { x0: p.x, y0: p.y };
      setCrop({ x: p.x, y: p.y, w: 0, h: 0 });
      return;
    }
    if (tab === 'Draw') { strokeRef.current = { kind: 'draw', color: drawColor, width: drawWidth, points: [p] }; forceRender(); return; }
    if (tab === 'Erase') {
      if (!baseImg) return;
      const stroke = { kind: 'erase', width: brush, points: [p] };
      strokeRef.current = stroke;
      setEraseStrokes((s) => [...s, stroke]); // captured, not read off the ref — see onMove
      return;
    }
    if (template) {
      const handleHit = handleHitTest(p);
      if (handleHit) { toggleMultiSelect(handleHit.id); return; }
    }
    const rotateHit = rotateHandleHitTest(p);
    if (rotateHit) {
      dragRef.current = { rotate: true, id: rotateHit.id };
      return;
    }
    const resizeHit = resizeHandleHitTest(p);
    if (resizeHit) {
      dragRef.current = { resize: true, id: resizeHit.id, sx: resizeHit.sx, sy: resizeHit.sy, lastX: p.x, lastY: p.y };
      return;
    }
    if (baseRotateHandleHitTest(p)) {
      dragRef.current = { rotateBase: true };
      return;
    }
    if (baseHandleHitTest(p)) {
      const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
      dragRef.current = { resizeBase: true, startDist: Math.max(1, Math.hypot(p.x - cx, p.y - cy)), startScale: base.scale };
      return;
    }
    const hit = hitTest(p);
    if (hit) {
      setSelectedId(hit.id); setTab(hit.type === 'text' ? 'Text' : 'Layer');
      const group = multiSelectedIds.has(hit.id) && multiSelectedIds.size > 1;
      if (group) {
        // A plain click-drag on any grouped item moves every grouped item together.
        dragRef.current = { group: true, ids: [...multiSelectedIds], dx: p.x - hit.xr * SIZE, dy: p.y - hit.yr * H, anchorXr: hit.xr, anchorYr: hit.yr };
      } else {
        // Clicking an item outside the current multi-selection drops the group back to just
        // that one item — the handle is the only way to build the group back up.
        if (!multiSelectedIds.has(hit.id)) setMultiSelectedIds(new Set());
        dragRef.current = { id: hit.id, dx: p.x - hit.xr * SIZE, dy: p.y - hit.yr * H };
      }
    } else if (baseImg) {
      setSelectedId(null); setMultiSelectedIds(new Set());
      dragRef.current = { base: true, dx: p.x - base.x, dy: p.y - base.y };
    } else { setSelectedId(null); setMultiSelectedIds(new Set()); }
  }
  function onMove(e) {
    const p = toCanvas(e);
    if (cropRef.current) {
      const { x0, y0 } = cropRef.current;
      let x = Math.min(x0, p.x), y = Math.min(y0, p.y);
      let w = Math.abs(p.x - x0), h = Math.abs(p.y - y0);
      // Circle locks 1:1 too — an ellipse crop out of a rectangle isn't what "circle" means here.
      if (cropShape !== 'free') { const d = Math.min(w, h); w = d; h = d; if (p.x < x0) x = x0 - d; if (p.y < y0) y = y0 - d; }
      // Keep the box inside the canvas so the crop can't sample empty space.
      x = clamp(x, 0, SIZE); y = clamp(y, 0, H);
      w = Math.min(w, SIZE - x); h = Math.min(h, H - y);
      setCrop({ x, y, w, h });
      return;
    }
    if (strokeRef.current) {
      strokeRef.current.points.push(p);
      if (strokeRef.current.kind === 'erase') setEraseStrokes((s) => [...s]);
      else forceRender();
      return;
    }
    if (!dragRef.current) return;
    if (dragRef.current.rotateBase) {
      const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
      const dx = p.x - cx, dy = p.y - cy;
      const rotation = (Math.atan2(dx, -dy) * 180) / Math.PI;
      setBase((b) => ({ ...b, rotation }));
      return;
    }
    if (dragRef.current.resizeBase) {
      // Uniform only — a real photo shouldn't come out stretched/distorted the way a shape or
      // emoji reasonably can. Scales relative to how far the CURRENT pointer sits from center
      // vs. where the drag started, so the photo grows/shrinks around its own fixed center
      // (unlike items, base.x/y are deliberately left untouched here).
      const { startDist, startScale } = dragRef.current;
      const cx = SIZE / 2 + base.x, cy = H / 2 + base.y;
      const curDist = Math.hypot(p.x - cx, p.y - cy);
      const scale = clamp(startScale * (curDist / startDist), 0.1, 6);
      setBase((b) => ({ ...b, scale }));
      return;
    }
    if (dragRef.current.rotate) {
      const { id } = dragRef.current;
      setItems((ls) => ls.map((l) => {
        if (l.id !== id) return l;
        // Absolute angle from the item's own center to the pointer, not an incremental delta
        // (rotation follows the handle directly, same feel as PowerPoint's rotate handle).
        // atan2(dx, -dy) rather than the usual atan2(dy, dx) so 0° means "handle straight up".
        const cx = l.xr * SIZE, cy = l.yr * H;
        const dx = p.x - cx, dy = p.y - cy;
        const rotation = (Math.atan2(dx, -dy) * 180) / Math.PI;
        return { ...l, rotation };
      }));
      return;
    }
    if (dragRef.current.resize) {
      const { id, sx, sy, lastX, lastY } = dragRef.current;
      const dxCanvas = p.x - lastX, dyCanvas = p.y - lastY;
      const ctx = canvasRef.current.getContext('2d');
      setItems((ls) => ls.map((l) => {
        if (l.id !== id) return l;
        // Rotate the canvas-space drag delta into the item's own unrotated local frame —
        // dragging "right" on a 90°-rotated box should grow its (local) height, not width.
        const rad = (-l.rotation * Math.PI) / 180;
        const cos = Math.cos(rad), sin = Math.sin(rad);
        const ldx = dxCanvas * cos - dyCanvas * sin;
        const ldy = dxCanvas * sin + dyCanvas * cos;
        // itemBox — not a generic baseSize fallback — so a text item being resized for the very
        // first time (no baseW/baseH yet) starts from its REAL font-metrics box instead of
        // snapping to an arbitrary 200×200 the moment you touch a handle.
        const curBox = itemBox(ctx, l);
        const curW = curBox.w, curH = curBox.h;
        const newW = Math.max(24, curW + sx * ldx);
        const newH = Math.max(24, curH + sy * ldy);
        const dW = newW - curW, dH = newH - curH;
        // Anchored at the OPPOSITE corner: growing one corner while the other stays put means
        // the center has to shift by half the applied delta, in that corner's local direction,
        // rotated back into canvas space (translation is applied in canvas space, unlike the
        // rotation/scale which pivot around the item's own center — see drawItem).
        const lsx = (sx * dW) / 2, lsy = (sy * dH) / 2;
        const frad = (l.rotation * Math.PI) / 180;
        const fcos = Math.cos(frad), fsin = Math.sin(frad);
        const shiftX = lsx * fcos - lsy * fsin, shiftY = lsx * fsin + lsy * fcos;
        return {
          ...l,
          // Stretch is applied on top of baseW/baseH (see itemBox), so divide it back out.
          baseW: newW / (l.scale * stretchOf(l)[0]), baseH: newH / (l.scale * stretchOf(l)[1]),
          xr: clamp(l.xr + shiftX / SIZE, 0, 1),
          yr: clamp(l.yr + shiftY / H, 0, 1),
        };
      }));
      dragRef.current = { ...dragRef.current, lastX: p.x, lastY: p.y };
      return;
    }
    if (dragRef.current.base) {
      // Read the offsets out of the ref FIRST. React can re-run a functional updater on a
      // later render, and by then onUp has set dragRef.current = null — dereferencing the
      // ref inside the closure would then throw.
      const { dx, dy } = dragRef.current;
      setBase((b) => ({ ...b, x: p.x - dx, y: p.y - dy }));
      return;
    }
    if (dragRef.current.group) {
      const { ids, dx, dy, anchorXr, anchorYr } = dragRef.current;
      const nx = clamp((p.x - dx) / SIZE, 0, 1);
      const ny = clamp((p.y - dy) / H, 0, 1);
      const ddx = nx - anchorXr, ddy = ny - anchorYr;
      setItems((ls) => ls.map((l) => (ids.includes(l.id) ? { ...l, xr: clamp(l.xr + ddx, 0, 1), yr: clamp(l.yr + ddy, 0, 1) } : l)));
      dragRef.current = { ...dragRef.current, anchorXr: nx, anchorYr: ny };
      return;
    }
    const { id, dx, dy } = dragRef.current;
    setItems((ls) => ls.map((l) => (l.id === id ? { ...l, xr: clamp((p.x - dx) / SIZE, 0, 1), yr: clamp((p.y - dy) / H, 0, 1) } : l)));
  }
  function onUp() {
    if (cropRef.current) { cropRef.current = null; return; }
    const stroke = strokeRef.current;
    if (stroke && stroke.kind === 'draw') setDrawStrokes((s) => [...s, stroke]);
    strokeRef.current = null; dragRef.current = null; forceRender();
  }
  const [, setTick] = useState(0);
  const forceRender = () => setTick((t) => t + 1);

  /* ---------------- add / patch ---------------- */
  function addText(text) {
    const id = uid++;
    setItems((ls) => [...ls, {
      id, type: 'text', text, xr: 0.5, yr: 0.5, scale: 1, rotation: 0, alpha: 1,
      color: '#ffffff', color2: '#FFC107', gradient: false, font: FONTS[0].css, size: 56,
      letterSpacing: 0, lineHeight: 1.1, align: 1, bold: true, italic: false,
      outline: false, outlineWidth: 8, outlineColor: '#000000', shadow: false, shadowBlur: 6, shadowColor: '#000000',
      glow: false, glowColor: '#FFC107', highlight: null, skewX: 0, bend: 0, stretchX: 1, stretchY: 1,
    }]);
    setSelectedId(id); setTab('Text');
  }
  function addEmoji(ch) { const id = uid++; setItems((ls) => [...ls, { id, type: 'emoji', text: ch, xr: 0.5, yr: 0.5, scale: 1, rotation: 0, alpha: 1, size: 120, baseSize: 120 }]); setSelectedId(id); setTab('Layer'); }
  function addShape(kind, color) { const id = uid++; setItems((ls) => [...ls, { id, type: 'shape', shapeKind: kind, shapeColor: color, xr: 0.5, yr: 0.5, scale: 1, rotation: 0, alpha: 1, baseSize: 200 }]); setSelectedId(id); setTab('Layer'); }
  /** [baseW]/[baseH] are the box's real w/h at scale=1 — the image's own aspect ratio, long
   * side pinned to the same 220 reference `baseSize` used to be alone. Without this every image
   * layer drew squashed into a plain square regardless of the photo's actual shape, and resizing
   * it (the Size slider, or the on-canvas corner handles) only ever grew that square uniformly —
   * never independently wide or tall, which is what "enlarge" looked broken for a non-square
   * photo. `baseSize` is kept too, as the fallback for shapes/emoji and any layer saved before
   * this existed. */
  function addImageLayer(img) {
    const id = uid++;
    const iw = img.naturalWidth || img.width || 1;
    const ih = img.naturalHeight || img.height || 1;
    const long = 220;
    const baseW = iw >= ih ? long : (long * iw) / ih;
    const baseH = ih > iw ? long : (long * ih) / iw;
    setItems((ls) => [...ls, { id, type: 'image', img, xr: 0.5, yr: 0.5, scale: 1, rotation: 0, alpha: 1, baseSize: 220, baseW, baseH }]);
    setSelectedId(id); setTab('Layer');
  }
  const patch = (props) => setItems((ls) => ls.map((l) => (l.id === selectedId ? { ...l, ...props } : l)));
  const patchAdj = (k, v) => setAdj((a) => ({ ...a, [k]: v }));
  const patchBase = (k, v) => setBase((b) => ({ ...b, [k]: v }));

  /**
   * Bake the marquee: re-draw the base photo through its current transform, cut out the
   * selected rectangle, and make that the new base image. Only the GEOMETRY is baked —
   * adjustments and filters stay live sliders afterwards. Erase strokes are dropped because
   * their coordinates belonged to the old framing and would land in the wrong place.
   */
  function applyCrop() {
    if (!baseImg || !crop || crop.w < 8 || crop.h < 8) return;
    const off = document.createElement('canvas'); off.width = SIZE; off.height = H;
    const o = off.getContext('2d');
    o.save();
    o.translate(SIZE / 2 + base.x, H / 2 + base.y);
    o.rotate((base.rotation * Math.PI) / 180);
    o.scale(base.scale * (base.flipX ? -1 : 1), base.scale * (base.stretchY ?? 1));
    const s0 = Math.min(SIZE / baseImg.width, H / baseImg.height);
    o.drawImage(baseImg, (-baseImg.width * s0) / 2, (-baseImg.height * s0) / 2, baseImg.width * s0, baseImg.height * s0);
    o.restore();

    const cut = document.createElement('canvas');
    cut.width = Math.max(1, Math.round(crop.w));
    cut.height = Math.max(1, Math.round(crop.h));
    const cc = cut.getContext('2d');
    if (cropShape === 'circle') {
      // Clip BEFORE drawing so the corners come out transparent, not white — the sticker keeps
      // its own transparency all the way through to export.
      cc.beginPath();
      cc.ellipse(cut.width / 2, cut.height / 2, cut.width / 2, cut.height / 2, 0, 0, Math.PI * 2);
      cc.clip();
    }
    cc.drawImage(off, crop.x, crop.y, crop.w, crop.h, 0, 0, cut.width, cut.height);

    const img = new Image();
    img.onload = () => {
      setBaseImg(img);
      setBase({ x: 0, y: 0, scale: 1, rotation: 0, flipX: false, stretchY: 1, alpha: 1 });
      setEraseStrokes([]);
      setCrop(null);
      setTab('Adjust');
    };
    img.src = cut.toDataURL('image/png');
  }
  const removeSelected = () => { setItems((ls) => ls.filter((l) => l.id !== selectedId)); setSelectedId(null); };
  function duplicateSelected() {
    if (!selected) return; const id = uid++;
    setItems((ls) => [...ls, { ...selected, id, xr: clamp(selected.xr + 0.06, 0, 1), yr: clamp(selected.yr + 0.06, 0, 1) }]);
    setSelectedId(id);
  }
  function deleteGroup() {
    setItems((ls) => ls.filter((l) => !multiSelectedIds.has(l.id)));
    setSelectedId(null); setMultiSelectedIds(new Set());
  }
  function duplicateGroup() {
    const copies = items
      .filter((l) => multiSelectedIds.has(l.id))
      .map((l) => ({ ...l, id: uid++, xr: clamp(l.xr + 0.06, 0, 1), yr: clamp(l.yr + 0.06, 0, 1) }));
    setItems((ls) => [...ls, ...copies]);
    setMultiSelectedIds(new Set(copies.map((c) => c.id)));
    setSelectedId(copies.at(-1)?.id ?? null);
  }

  function pickFile(kind) {
    const inp = document.createElement('input'); inp.type = 'file'; inp.accept = 'image/*';
    inp.onchange = () => {
      const f = inp.files[0]; if (!f) return;
      const img = new Image();
      img.onload = () => {
        // Keep the original File on the image: template saving has to re-upload these to R2
        // so the phone can fetch each layer, and a blob: object URL is useless off this page.
        img._file = f;
        if (kind === 'base') {
          setBaseImg(img);
          // Crop is the first step on a new photo, same as the phone editor's flow: land on the
          // Crop tab with a box already drawn (inset from the edges so it's obviously draggable)
          // rather than making the user go find the tab and drag from scratch.
          setBase({ x: 0, y: 0, scale: 1, rotation: 0, flipX: false, stretchY: 1, alpha: 1 });
          setCrop({ x: SIZE * 0.1, y: H * 0.1, w: SIZE * 0.8, h: H * 0.8 });
          setTab('Crop');
        } else addImageLayer(img);
      };
      img.src = URL.createObjectURL(f);
    };
    inp.click();
  }

  /**
   * Save the canvas as a reusable template. Uploads the base photo and any photo layers to
   * R2, then writes the layer list (relative coords, so it lands the same on any phone).
   */
  async function saveAsTemplate() {
    if (!template) return;
    setBusy(true); setMsg('');
    try {
      const up = async (img) => {
        // A layer reloaded from a saved template already lives in R2 — re-uploading it would
        // just duplicate the object, so its existing URL is reused unless a new file replaced it.
        if (img?._file) return await uploadToR2(img._file, { collectionId: '', kind: 'canvasTemplate' });
        return img?._url || '';
      };
      const baseImage = baseImg ? await up(baseImg) : '';

      const layers = [];
      for (const l of items) {
        const common = { xr: l.xr, yr: l.yr, scale: l.scale, rotation: l.rotation, alpha: l.alpha };
        if (l.type === 'text') {
          layers.push({
            ...common, type: 'text', text: l.text, color: l.color, size: l.size,
            bold: !!l.bold, italic: !!l.italic, align: l.align,
            outline: !!l.outline, outlineColor: l.outlineColor, outlineWidth: l.outlineWidth,
            letterSpacing: l.letterSpacing, lineHeight: l.lineHeight, highlight: l.highlight || '',
            // Previously dropped on save — the app had no fields for them, so a template
            // built with any of these silently lost the effect when reopened.
            gradient: !!l.gradient, gradientColor2: l.color2 || '',
            shadow: !!l.shadow, shadowColor: l.shadowColor, shadowBlur: l.shadowBlur,
            glow: !!l.glow, glowColor: l.glowColor,
            skewX: l.skewX || 0, bend: l.bend || 0,
            stretchX: l.stretchX ?? 1, stretchY: l.stretchY ?? 1,
          });
        } else if (l.type === 'emoji') {
          layers.push({ ...common, type: 'emoji', text: l.text, size: l.size });
        } else if (l.type === 'shape') {
          layers.push({ ...common, type: 'shape', shapeKind: l.shapeKind, shapeColor: l.shapeColor, baseSize: l.baseSize });
        } else if (l.type === 'image') {
          const url = await up(l.img);
          if (url) {
            layers.push({
              ...common, type: 'image', url, baseSize: l.baseSize,
              baseW: l.baseW || null, baseH: l.baseH || null,
              strokeW: l.strokeW || 0, strokeColor: l.strokeColor || '#ffffff',
            });
          }
        }
      }

      const payload = {
        title: template.title,
        image: template.image,
        textColor: template.textColor,
        bgColor: template.bgColor,
        category: template.category,
        tags: template.tags,
        ratio: template.ratio,
        config: {
          bgColor: bgColor || '',
          baseImage,
          // How the base photo was left arranged — without this the app always reloads it
          // centred and unscaled, which almost never matches what was actually set here.
          base: baseImg ? {
            x: base.x, y: base.y, scale: base.scale, scaleY: base.scale * (base.stretchY ?? 1),
            rotation: base.rotation, flipX: !!base.flipX, alpha: base.alpha ?? 1,
          } : null,
          layers,
        },
      };
      // Editing an existing template rewrites it in place; a new one gets created.
      if (template.id) await api.updateCanvasTemplate(template.id, payload);
      else await api.addCanvasTemplate(payload);
      setMsg(template.id ? '✓ Template updated.' : '✓ Template saved.');
      onTemplateSaved?.(!!template.id);
    } catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  async function upload() {
    if (!collId) { setMsg('Pick a destination collection first.'); return; }
    if (isEmpty) { setMsg('Add an image, text, drawing or shape first.'); return; }
    setBusy(true); setMsg('');
    try {
      const off = document.createElement('canvas'); off.width = SIZE; off.height = SIZE;
      renderTo(off.getContext('2d'), { forExport: true });
      const blob = await new Promise((r) => off.toBlob(r, 'image/png'));
      const file = new File([blob], `editor_${Date.now()}.png`, { type: 'image/png' });
      const url = await uploadToR2(file, { collectionId: collId, kind: 'sticker' });
      const cat = cats.find((c) => c.id === catId), grp = groups.find((g) => g.id === groupId), col = colls.find((c) => c.id === collId);
      const autoTags = [cat?.name, grp?.name, col?.name].filter(Boolean).map((t) => t.toLowerCase());
      const extra = tags.split(',').map((t) => t.trim().toLowerCase()).filter(Boolean);
      await api.addSticker({ categoryId: catId, groupId, collectionId: collId, imageUrl: url, tags: [...new Set([...extra, ...autoTags])], animated: false });
      setMsg(`✓ Uploaded to ${col?.name || 'collection'}.`);
    } catch (e) { setMsg(`✗ ${e.message}`); }
    setBusy(false);
  }

  /* ---------------- UI ---------------- */
  const railItems = [
    { i: 0, label: mode === 'anim' ? 'Video' : 'Photo', ic: mode === 'anim' ? '🎬' : '🖼', on: () => pickFile('base') },
    { i: 1, label: 'Text', ic: 'T', on: () => setTextDialog({ value: '', editId: null }) },
    { i: 2, label: 'Sticker', ic: '😊', on: () => setEmojiPicker(true) },
    { i: 3, label: 'Overlay', ic: '▤', on: () => pickFile('overlay') },
    { i: 5, label: 'Emoji', ic: '🙂', on: () => setEmojiPicker(true) },
    { i: 4, label: 'Shape', ic: '◆', on: () => setShapePicker(true) },
  ];

  /* ================= Canva-style chrome (canvas templates only) =================
     Same rendering engine and the same layer helpers as the sticker editor above — only the
     chrome differs: canvas takes the whole stage, tools live in one slim strip along the
     bottom, and anything with options opens either a docked panel (adjusting something already
     on the canvas, so it stays visible while you drag) or a modal sheet (adding new content,
     where there is nothing to preview against yet). */
  if (template) {
    const closePanel = () => setTplPanel(null);
    const sel = selected;

    return (
      <div className="tpl">
        <div className="tpl-top">
          <button className="tpl-round" title="Back" onClick={onClose}>←</button>
          <div className="name">{template.title || 'Template'}</div>
          <button className="tpl-done" disabled={busy} onClick={saveAsTemplate}>
            {busy ? 'Saving…' : template.id ? 'Update template' : 'Save template'}
          </button>
        </div>

        <div className="tpl-stage">
          {/* width/height deliberately left unset (both stay `auto`) — with both maxes given,
              the browser picks the largest box that satisfies the aspect-ratio within BOTH
              bounds at once, i.e. real letterboxed "contain" sizing. An explicit width (the
              previous approach) breaks this: aspect-ratio computes height from that width
              first, THEN max-height clips it independently, so anything but a square ratio
              ends up neither square nor the requested shape. */}
          <div className="tpl-canvas-wrap" style={{ maxWidth: '100%', maxHeight: 'calc(100vh - 340px)', aspectRatio: template.ratio > 0 ? template.ratio : DEFAULT_TEMPLATE_RATIO }}>
            <canvas ref={canvasRef} width={SIZE} height={H} onPointerDown={onDown} onPointerMove={onMove} onPointerUp={onUp} onPointerLeave={onUp} />
            {isEmpty && (
              <div className="tpl-placeholder" onClick={() => setTplSheet('photo')}>
                <span className="ic">🖼️</span>
                <div className="big">Add a photo</div>
                <div>or add text, a shape or a sticker below</div>
              </div>
            )}
          </div>
        </div>

        <div className="tpl-bottom">
          {/* Group toolbar (2+ items via their multi-select handles) takes over from everything
              else — one docked panel at a time, else the single-item toolbar, else the main tools. */}
          {multiSelectedIds.size > 1 ? (
            <div className="tpl-tools">
              <span className="tpl-label" style={{ margin: 0 }}>{multiSelectedIds.size} selected</span>
              <span className="spacer" />
              <button className="tpl-tool" onClick={duplicateGroup}><span className="ic">⧉</span>Duplicate</button>
              <button className="tpl-tool danger" onClick={deleteGroup}><span className="ic">🗑</span>Delete</button>
              <button className="tpl-check" onClick={() => { setSelectedId(null); setMultiSelectedIds(new Set()); }}>✓</button>
            </div>
          ) : tplPanel ? (
            <div className="tpl-panel">
              <div className="tpl-panel-head">
                <span className="t">{tplPanel === 'text' ? 'Text style' : tplPanel === 'layer' ? 'Layer' : tplPanel}</span>
                <button className="tpl-check" onClick={closePanel}>✓</button>
              </div>

              {tplPanel === 'adjust' && (
                <>
                  <div className="tpl-chips">
                    {ADJUSTS.map((a) => (
                      <button key={a.key} className={`tpl-chip ${adjTool === a.key ? 'sel' : ''}`} onClick={() => setAdjTool(a.key)}>{a.label}</button>
                    ))}
                  </div>
                  {ADJUSTS.filter((a) => a.key === adjTool).map((a) => (
                    <Adj key={a.key} a={a} value={adj[a.key]} onChange={(v) => patchAdj(a.key, v)} />
                  ))}
                </>
              )}

              {tplPanel === 'filter' && (
                <div className="tpl-chips">
                  {FILTERS.map((f) => (
                    <button key={f} className={`tpl-chip ${preset === f ? 'sel' : ''}`} onClick={() => setPreset(f)}>{f}</button>
                  ))}
                </div>
              )}

              {tplPanel === 'background' && (
                <div className="tpl-swatches">
                  {BG_COLORS.map((c, i) => (
                    <button key={i} className={`tpl-sw ${bgColor === c ? 'sel' : ''}`}
                      style={c ? { background: c } : { background: 'repeating-conic-gradient(#ddd 0% 25%, #fff 0% 50%) 50%/12px 12px' }}
                      title={c || 'None'} onClick={() => setBgColor(c)} />
                  ))}
                </div>
              )}

              {tplPanel === 'text' && selText && (
                <>
                  <div className="tpl-label">Color</div>
                  <div className="tpl-swatches">
                    {TEXT_COLORS.map((c) => (
                      <button key={c} className={`tpl-sw ${selText.color === c ? 'sel' : ''}`} style={{ background: c }} onClick={() => patch({ color: c })} />
                    ))}
                  </div>
                  <div className="tpl-label">Font</div>
                  <div className="tpl-chips">
                    {FONTS.map((f) => (
                      <button key={f.label} className={`tpl-chip ${selText.font === f.css ? 'sel' : ''}`} style={{ fontFamily: f.css }} onClick={() => patch({ font: f.css })}>Aa</button>
                    ))}
                  </div>
                  <Adj a={{ label: 'Size', min: 16, max: 200, step: 2, def: 56, fmt: (v) => `${Math.round(v)}` }} value={selText.size} onChange={(v) => patch({ size: v })} />
                  <Adj a={{ label: 'Opacity', min: 0.1, max: 1, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.alpha} onChange={(v) => patch({ alpha: v })} />
                  <div className="tpl-label">Style</div>
                  <div className="tpl-chips">
                    <Toggle on={selText.bold} label="Bold" onClick={() => patch({ bold: !selText.bold })} />
                    <Toggle on={selText.italic} label="Italic" onClick={() => patch({ italic: !selText.italic })} />
                    <Toggle on={selText.outline} label="Outline" onClick={() => patch({ outline: !selText.outline })} />
                    <Toggle on={selText.shadow} label="Shadow" onClick={() => patch({ shadow: !selText.shadow })} />
                    <Toggle on={selText.glow} label="Glow" onClick={() => patch({ glow: !selText.glow })} />
                    <Toggle on={selText.gradient} label="Gradient" onClick={() => patch({ gradient: !selText.gradient })} />
                  </div>
                </>
              )}

              {tplPanel === 'layer' && sel && (
                <>
                  {LAYER_TOOLS.map((a) => (
                    <Adj key={a.key} a={a} value={sel[a.key]} onChange={(v) => patch({ [a.key]: v })} />
                  ))}
                  {sel.type === 'image' && (
                    <>
                      <div className="tpl-label">Stroke</div>
                      <Adj a={{ label: 'Stroke', min: 0, max: 24, step: 1, def: 0, fmt: (v) => (v <= 0 ? 'off' : `${Math.round(v)}px`) }}
                        value={sel.strokeW || 0} onChange={(v) => patch({ strokeW: v })} />
                      <div className="tpl-swatches">
                        {TEXT_COLORS.map((c) => (
                          <button key={c} className={`tpl-sw ${sel.strokeColor === c ? 'sel' : ''}`} style={{ background: c }} onClick={() => patch({ strokeColor: c })} />
                        ))}
                      </div>
                    </>
                  )}
                </>
              )}
            </div>
          ) : sel ? (
            <div className="tpl-tools">
              <button className="tpl-tool" onClick={() => (sel.type === 'text' ? setTextDialog({ value: sel.text, editId: sel.id }) : pickFile('overlay'))}>
                <span className="ic">{sel.type === 'text' ? '✎' : '🖼'}</span>{sel.type === 'text' ? 'Edit' : 'Replace'}
              </button>
              <button className="tpl-tool" onClick={() => setTplPanel(sel.type === 'text' ? 'text' : 'layer')}><span className="ic">🎛</span>Style</button>
              <button className="tpl-tool" onClick={duplicateSelected}><span className="ic">⧉</span>Duplicate</button>
              <button className="tpl-tool danger" onClick={removeSelected}><span className="ic">🗑</span>Delete</button>
              <span className="spacer" />
              <button className="tpl-check" onClick={() => setSelectedId(null)}>✓</button>
            </div>
          ) : (
            <div className="tpl-tools">
              <button className="tpl-tool" onClick={() => setTplSheet('photo')}><span className="ic">🖼</span>Photo</button>
              <button className="tpl-tool" onClick={() => setTextDialog({ value: '', editId: null })}><span className="ic">T</span>Text</button>
              <button className="tpl-tool" onClick={() => setEmojiPicker(true)}><span className="ic">😊</span>Sticker</button>
              <button className="tpl-tool" onClick={() => setTplSheet('overlay')}><span className="ic">▤</span>Overlay</button>
              <button className="tpl-tool" onClick={() => setTplSheet('shape')}><span className="ic">◆</span>Shape</button>
              <button className="tpl-tool" onClick={() => setTplPanel('background')}><span className="ic">🎨</span>Background</button>
              <button className="tpl-tool" onClick={() => setTplPanel('adjust')}><span className="ic">🎛</span>Adjust</button>
              <button className="tpl-tool" onClick={() => setTplPanel('filter')}><span className="ic">✨</span>Filter</button>
              <button className="tpl-tool" onClick={() => { if (!baseImg) { setMsg('Add a photo first.'); return; } setCrop({ x: 64, y: 64, w: SIZE - 128, h: H - 128 }); setTab('Crop'); }}>
                <span className="ic">⛶</span>Crop
              </button>
              <button className="tpl-tool" disabled={!baseImg} onClick={() => patchBase('flipX', !base.flipX)}><span className="ic">⇄</span>Flip</button>
            </div>
          )}

          {/* The crop marquee needs its Apply/Clear affordances even in this chrome. */}
          {crop && (
            <div className="tpl-panel">
              <div className="tpl-panel-head"><span className="t">Crop</span></div>
              <div className="tpl-hint">Drag a box on the canvas, then apply. Filters and adjustments stay editable afterwards.</div>
              <div className="tpl-row">
                <button className="tpl-btn primary" disabled={crop.w < 8 || crop.h < 8} onClick={applyCrop}>Apply crop</button>
                <button className="tpl-btn" onClick={() => setCrop(null)}>Cancel</button>
              </div>
            </div>
          )}

          {msg && <div className="tpl-hint" style={{ padding: '0 14px 10px' }}>{msg}</div>}
        </div>

        {/* ---- Modal sheets: adding new content ---- */}
        {tplSheet && (
          <div className="tpl-scrim" onClick={() => setTplSheet(null)}>
            <div className="tpl-sheet" onClick={(e) => e.stopPropagation()}>
              <div className="grab" />
              {tplSheet === 'photo' && (
                <>
                  <h4>Add a photo</h4>
                  <button className="tpl-btn primary" onClick={() => { pickFile('base'); setTplSheet(null); }}>🖼 Choose from computer</button>
                  <div className="tpl-hint">This becomes the template's background photo — users swap it for their own.</div>
                </>
              )}
              {tplSheet === 'overlay' && (
                <>
                  <h4>Add an image overlay</h4>
                  <div className="tpl-hint">Adds a photo as its own movable, resizable layer on top of everything else.</div>
                  <button className="tpl-btn primary" onClick={() => { pickFile('overlay'); setTplSheet(null); }}>🖼 Choose from computer</button>
                </>
              )}
              {tplSheet === 'shape' && (
                <>
                  <h4>Add a shape</h4>
                  <div className="tpl-chips" style={{ marginBottom: 14 }}>
                    {TEXT_COLORS.map((c) => (
                      <button key={c} className="tpl-sw" style={{ background: c }} onClick={() => { addShape('circle', c); setTplSheet(null); }} title={`Circle ${c}`} />
                    ))}
                  </div>
                  <div className="tpl-row">
                    {SHAPES.map((k) => (
                      <button key={k} className="tpl-btn" onClick={() => { addShape(k, '#FFC107'); setTplSheet(null); }}>{shapeGlyph(k)} {k}</button>
                    ))}
                  </div>
                </>
              )}
            </div>
          </div>
        )}

        {/* Reuse the existing text + emoji dialogs so behaviour stays identical. */}
        {textDialog && (
          <div className="med-modal-bg" onClick={() => setTextDialog(null)}>
            <div className="med-modal" onClick={(e) => e.stopPropagation()}>
              <h3>{textDialog.editId ? 'Edit text' : 'Text'}</h3>
              <textarea autoFocus value={textDialog.value} onChange={(e) => setTextDialog({ ...textDialog, value: e.target.value })} placeholder="Type here…" />
              <div className="med-modal-actions">
                <button className="cancel" onClick={() => setTextDialog(null)}>Cancel</button>
                <button className="ok" onClick={() => {
                  const v = textDialog.value;
                  if (textDialog.editId) patch({ text: v || 'Text' });
                  else if (v.trim()) addText(v);
                  setTextDialog(null);
                }}>{textDialog.editId ? 'Save' : 'Add'}</button>
              </div>
            </div>
          </div>
        )}
        {emojiPicker && (
          <div className="med-modal-bg" onClick={() => setEmojiPicker(false)}>
            <div className="med-modal" onClick={(e) => e.stopPropagation()}>
              <h3>Emoji &amp; stickers</h3>
              <div className="med-emoji-grid">
                {EMOJIS.map((e) => <button key={e} onClick={() => { addEmoji(e); setEmojiPicker(false); }}>{e}</button>)}
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="med">
      {/* Top bar */}
      <div className="med-top">
        <button className="med-iconbtn" title="Back" onClick={onClose}>←</button>
        <div style={{ flex: '0 1 auto' }}>
          <div className="med-title">EDITOR 👑</div>
          <div className="med-sub">Unleash your creativity</div>
        </div>
        <div style={{ flex: 1 }} />
        <div className="med-modes">
          <button className={`med-mode ${mode === 'sticker' ? 'sel' : ''}`} onClick={() => setMode('sticker')}>🖼 Sticker</button>
          <button className={`med-mode ${mode === 'anim' ? 'sel' : ''}`} onClick={() => setMode('anim')}>🎬 Animation</button>
        </div>
        <button className="med-save" title="Scroll down to upload" disabled={isEmpty} onClick={() => document.getElementById('med-dest')?.scrollIntoView({ behavior: 'smooth' })}>✓</button>
      </div>

      {mode === 'anim' && (
        <div className="med-hint" style={{ marginBottom: 10 }}>
          🎬 Animated stickers come from a video — add those in the app or via an <b>Animated</b> collection uploader.
          This editor makes static stickers.
        </div>
      )}

      {/* Body: rail + canvas on the left, tabs/controls/upload on the right.
          Side-by-side on desktop; stacks on narrow screens. */}
      <div className="med-body">
      <div className="med-stage">
        <div className="med-rail">
          {railItems.map((r) => (
            <button key={r.label} className={rail === r.i ? 'sel' : ''} onClick={() => { setRail(r.i); r.on(); }}>
              <span className="ic">{r.ic}</span>{r.label}
            </button>
          ))}
          <div className="med-rail-more">
            <button onClick={() => setMoreOpen((v) => !v)}><span className="ic">⋯</span>More</button>
            {moreOpen && (
              <div className="med-menu">
                <button disabled={!baseImg} onClick={() => { patchBase('flipX', !base.flipX); setMoreOpen(false); }}>Flip photo</button>
                <button disabled={!selected} onClick={() => { duplicateSelected(); setMoreOpen(false); }}>Duplicate layer</button>
                <button onClick={() => { setAdj({ brightness: 0, contrast: 1, saturation: 1, warmth: 0, shadows: 0, highlights: 0 }); setPreset('Original'); setMoreOpen(false); }}>Reset adjustments</button>
              </div>
            )}
          </div>
        </div>

        <div className="med-canvas-wrap">
          <canvas ref={canvasRef} width={SIZE} height={SIZE} onPointerDown={onDown} onPointerMove={onMove} onPointerUp={onUp} onPointerLeave={onUp} />
          {isEmpty && (
            <div className="med-placeholder clickable" onClick={() => pickFile('base')}>
              <span className="ic">🖼️</span>
              <div className="big">ADD A PHOTO</div>
              <div className="m">Tap to add from gallery</div>
              <div className="m">or add text, emoji &amp; stickers below</div>
            </div>
          )}
        </div>
      </div>

      <div className="med-side">
      {/* Tabs — the 3 reached for most (Adjust/Layer/Text) stay in a horizontal row up top;
          the rest (Filter/Draw/Erase) move to a vertical column down the right edge, so the
          two rows trace an L around the panel instead of one cramped, scrolling row of six. */}
      <div className="med-tabs">
        {['Adjust', 'Layer', 'Text'].map((t) => (
          <button key={t} className={`med-tab ${tab === t ? 'sel' : ''}`} onClick={() => setTab(t)}>{t.toUpperCase()}</button>
        ))}
      </div>

      <div className="med-panel-row">
      {/* Panel */}
      <div className="med-panel">
        {tab === 'Adjust' && (
          <>
            {!baseImg ? <div className="med-hint">Add a photo to adjust it.</div> : (
              <>
                <div className="med-iconrow">
                  {ADJUSTS.map((a) => (
                    <button key={a.key} className={`med-icontool ${adjTool === a.key ? 'sel' : ''}`} onClick={() => setAdjTool(a.key)}>
                      <span className="ic">{a.ic}</span>{a.label}
                    </button>
                  ))}
                </div>
                {ADJUSTS.filter((a) => a.key === adjTool).map((a) => (
                  <Adj key={a.key} a={a} value={adj[a.key]} onChange={(v) => patchAdj(a.key, v)} />
                ))}
              </>
            )}
            <div className="med-label">Background</div>
            <Swatches list={BG_COLORS} value={bgColor} onPick={setBgColor} allowNone />
          </>
        )}

        {tab === 'Crop' && (
          !baseImg ? <div className="med-hint">Add a photo first, then drag on it to choose the crop.</div> : (
            <>
              <div className="med-hint">Drag a box on the canvas, then Apply. Adjustments and filters stay editable after cropping.</div>
              <div className="med-iconrow">
                {[
                  ['free', '▭', 'Rectangle'],
                  ['square', '◻', 'Square'],
                  ['circle', '⬤', 'Circle'],
                ].map(([shape, glyph, label]) => (
                  <button
                    key={shape}
                    className={`med-icontool ${cropShape === shape ? 'sel' : ''}`}
                    onClick={() => {
                      setCropShape(shape);
                      // Re-square an existing box immediately, so the shape you picked is what
                      // the marquee shows without having to redraw it first.
                      if (shape !== 'free') {
                        setCrop((c) => {
                          if (!c) return c;
                          const d = Math.min(c.w, c.h);
                          return { x: c.x + (c.w - d) / 2, y: c.y + (c.h - d) / 2, w: d, h: d };
                        });
                      }
                    }}
                  >
                    <span className="ic">{glyph}</span>{label}
                  </button>
                ))}
                <button className="med-icontool" onClick={() => setCrop({ x: 0, y: 0, w: SIZE, h: H })}>
                  <span className="ic">⛶</span>Select all
                </button>
              </div>
              <CropPreview baseImg={baseImg} base={base} crop={crop} shape={cropShape} SIZE={SIZE} H={H} />
              <div style={{ display: 'flex', gap: 8, marginTop: 10, flexWrap: 'wrap' }}>
                <button className="primary" disabled={!crop || crop.w < 8 || crop.h < 8} onClick={applyCrop}>Apply crop</button>
                <button className="link" disabled={!crop} onClick={() => setCrop(null)}>Clear</button>
                {crop && crop.w >= 8 ? (
                  <span className="muted small" style={{ alignSelf: 'center' }}>
                    {Math.round(crop.w)} x {Math.round(crop.h)}
                  </span>
                ) : null}
              </div>
            </>
          )
        )}

        {tab === 'Filter' && (
          !baseImg ? <div className="med-hint">Add a photo to apply filters.</div> : (
            <>
              <div className="med-label">Filters</div>
              <div className="med-chips scroll">
                {FILTERS.map((f) => (
                  <button key={f} className={`med-chip ${preset === f ? 'sel' : ''}`} onClick={() => setPreset(f)}>{f}</button>
                ))}
              </div>
            </>
          )
        )}

        {tab === 'Draw' && (
          <>
            <div className="med-hint">Drag on the canvas to draw.</div>
            <div className="med-label">Brush color</div>
            <Swatches list={TEXT_COLORS} value={drawColor} onPick={setDrawColor} />
            <Adj a={{ label: 'Brush size', min: 4, max: 60, step: 2, def: 16, fmt: (v) => `${Math.round(v)}` }} value={drawWidth} onChange={setDrawWidth} />
            <button className="med-pill" onClick={() => setDrawStrokes((s) => s.slice(0, -1))}>↺ Undo last stroke</button>
          </>
        )}

        {tab === 'Erase' && (
          !baseImg ? <div className="med-hint">Add a photo, then drag on it to erase parts.</div> : (
            <>
              <div className="med-hint">Drag on the image to erase. Great for cutting out a subject.</div>
              <Adj a={{ label: 'Eraser size', min: 15, max: 160, step: 5, def: 60, fmt: (v) => `${Math.round(v)}` }} value={brush} onChange={setBrush} />
              <button className="med-pill" onClick={() => setEraseStrokes((s) => s.slice(0, -1))}>↺ Undo erase</button>
            </>
          )
        )}

        {tab === 'Layer' && (
          !selected ? <div className="med-hint">Tap a sticker or text on the canvas to edit it here.</div> : (
            <>
              <div className="med-iconrow">
                {LAYER_TOOLS.map((a) => (
                  <button key={a.key} className={`med-icontool ${layerTool === a.key ? 'sel' : ''}`} onClick={() => setLayerTool(a.key)}>
                    <span className="ic">{a.ic}</span>{a.label}
                  </button>
                ))}
              </div>
              {LAYER_TOOLS.filter((a) => a.key === layerTool).map((a) => (
                <Adj key={a.key} a={a} value={selected[a.key]} onChange={(v) => patch({ [a.key]: v })} />
              ))}
              {selected.type === 'image' && (
                <>
                  <Adj
                    a={{ key: 'strokeW', label: 'Stroke', ic: '◎', min: 0, max: 24, step: 1, def: 0, fmt: (v) => (v ? `${Math.round(v)}px` : 'off') }}
                    value={selected.strokeW || 0}
                    onChange={(v) => patch({ strokeW: v })}
                  />
                  <div className="med-swatches">
                    {['#ffffff', '#000000', '#FFC107', '#E5484D', '#46A758', '#3B82F6', '#EC4899', '#8B5CF6'].map((c) => (
                      <button
                        key={c}
                        className={`med-swatch ${(selected.strokeColor || '#ffffff') === c ? 'sel' : ''}`}
                        style={{ background: c }}
                        title={`Stroke ${c}`}
                        onClick={() => patch({ strokeColor: c, strokeW: selected.strokeW || 6 })}
                      />
                    ))}
                  </div>
                </>
              )}
              <div className="med-row2">
                <button className="med-pill" onClick={duplicateSelected}>⧉ Duplicate</button>
                <button className="med-pill danger" onClick={removeSelected}>🗑 Delete</button>
              </div>
            </>
          )
        )}

        {tab === 'Text' && (
          <>
            <button className="med-pill filled" onClick={() => setTextDialog({ value: '', editId: null })}>T  ＋ Add text</button>
            {!selText ? <div className="med-hint">Add text, or tap existing text on the canvas to style it.</div> : (
              <>
                <div className="med-label">Color</div>
                <Swatches list={TEXT_COLORS} value={selText.color} onPick={(c) => patch({ color: c })} />
                <div className="med-label">Font</div>
                <div className="med-chips scroll">
                  {FONTS.map((f) => (
                    <button key={f.label} className={`med-chip ${selText.font === f.css ? 'sel' : ''}`} style={{ fontFamily: f.css }} onClick={() => patch({ font: f.css })}>{f.label}</button>
                  ))}
                </div>
                <Adj a={{ label: 'Text size', min: 16, max: 140, step: 2, def: 56, fmt: (v) => `${Math.round(v)}` }} value={selText.size} onChange={(v) => patch({ size: v })} />
                <Adj a={{ label: 'Letter spacing', min: 0, max: 24, step: 1, def: 0, fmt: (v) => `${Math.round(v)}` }} value={selText.letterSpacing} onChange={(v) => patch({ letterSpacing: v })} />
                <Adj a={{ label: 'Line height', min: 0.8, max: 2.5, step: 0.05, def: 1.1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.lineHeight} onChange={(v) => patch({ lineHeight: v })} />
                <Adj a={{ label: 'Opacity', min: 0.1, max: 1, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.alpha} onChange={(v) => patch({ alpha: v })} />
                <div className="med-label">Transform</div>
                <Adj a={{ label: 'Rotation', min: -180, max: 180, step: 5, def: 0, fmt: (v) => `${Math.round(v)}°` }} value={selText.rotation} onChange={(v) => patch({ rotation: v })} />
                <Adj a={{ label: 'Scale', min: 0.2, max: 4, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.scale} onChange={(v) => patch({ scale: v })} />
                <Adj a={{ label: 'X stretch', min: 0.3, max: 3, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.stretchX ?? 1} onChange={(v) => patch({ stretchX: v })} />
                <Adj a={{ label: 'Y stretch', min: 0.3, max: 3, step: 0.05, def: 1, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.stretchY ?? 1} onChange={(v) => patch({ stretchY: v })} />
                <Adj a={{ label: 'Skew', min: -1, max: 1, step: 0.05, def: 0, fmt: (v) => `${Math.round(v * 100)}%` }} value={selText.skewX} onChange={(v) => patch({ skewX: v })} />
                <Adj a={{ label: 'Bend', min: -100, max: 100, step: 5, def: 0, fmt: (v) => `${Math.round(v)}` }} value={selText.bend} onChange={(v) => patch({ bend: v })} />
                <div className="med-label">Align</div>
                <div className="med-chips">
                  {['⟸', '≡', '⟹'].map((s, i) => (
                    <button key={i} className={`med-chip pill med-align ${selText.align === i ? 'sel' : ''}`} onClick={() => patch({ align: i })}>{s}</button>
                  ))}
                </div>
                <div className="med-label">Style</div>
                <div className="med-chips scroll">
                  <Toggle on={selText.bold} label="Bold" onClick={() => patch({ bold: !selText.bold })} />
                  <Toggle on={selText.italic} label="Italic" onClick={() => patch({ italic: !selText.italic })} />
                  <Toggle on={selText.outline} label="Outline" onClick={() => patch({ outline: !selText.outline })} />
                  <Toggle on={selText.shadow} label="Shadow" onClick={() => patch({ shadow: !selText.shadow })} />
                  <Toggle on={selText.glow} label="Glow" onClick={() => patch({ glow: !selText.glow })} />
                  <Toggle on={selText.gradient} label="Gradient" onClick={() => patch({ gradient: !selText.gradient })} />
                </div>
                {selText.gradient && (<><div className="med-label">Gradient 2nd color</div><Swatches list={TEXT_COLORS} value={selText.color2} onPick={(c) => patch({ color2: c })} /></>)}
                {selText.glow && (<><div className="med-label">Glow color</div><Swatches list={TEXT_COLORS} value={selText.glowColor} onPick={(c) => patch({ glowColor: c })} /></>)}
                {selText.outline && (<>
                  <Adj a={{ label: 'Outline width', min: 2, max: 30, step: 1, def: 8, fmt: (v) => `${Math.round(v)}` }} value={selText.outlineWidth} onChange={(v) => patch({ outlineWidth: v })} />
                  <div className="med-label">Outline color</div><Swatches list={TEXT_COLORS} value={selText.outlineColor} onPick={(c) => patch({ outlineColor: c })} />
                </>)}
                {selText.shadow && (<>
                  <Adj a={{ label: 'Shadow blur', min: 0, max: 30, step: 1, def: 6, fmt: (v) => `${Math.round(v)}` }} value={selText.shadowBlur} onChange={(v) => patch({ shadowBlur: v })} />
                  <div className="med-label">Shadow color</div><Swatches list={TEXT_COLORS} value={selText.shadowColor} onPick={(c) => patch({ shadowColor: c })} />
                </>)}
                <div className="med-label">Highlight</div>
                <Swatches list={HIGHLIGHTS} value={selText.highlight} onPick={(c) => patch({ highlight: c })} allowNone />
                <div className="med-row2">
                  <button className="med-pill" onClick={() => setTextDialog({ value: selText.text, editId: selText.id })}>✎ Edit text</button>
                  <button className="med-pill danger" onClick={removeSelected}>🗑 Delete</button>
                </div>
              </>
            )}
          </>
        )}
      </div>

      {/* Vertical tab column — right edge of the panel, completing the L. */}
      <div className="med-tabs-right">
        {['Crop', 'Filter', 'Draw', 'Erase'].map((t) => (
          <button key={t} className={`med-tab-v ${tab === t ? 'sel' : ''}`} onClick={() => setTab(t)}>{t.toUpperCase()}</button>
        ))}
      </div>
      </div>{/* /med-panel-row */}

      {/* Destination + upload — or, in template mode, save the canvas configuration */}
      {template ? (
        <div className="med-dest" id="med-dest" style={{ marginTop: 14 }}>
          <div className="med-label">Save as template</div>
          <div className="med-hint">
            Building <strong>{template.title}</strong>. The layout, text and photos below are saved as
            the template — users open this exact canvas and swap the picture and the words.
          </div>
          <button className="med-upload" disabled={busy || isEmpty} onClick={saveAsTemplate}>
            {busy ? 'Saving…' : 'Save canvas as template'}
          </button>
          {msg && <div className={msg.startsWith('✗') ? 'error' : 'med-hint'}>{msg}</div>}
        </div>
      ) : (
        <div className="med-dest" id="med-dest" style={{ marginTop: 14 }}>
          <div className="med-label">Upload to</div>
          <select value={catId} onChange={(e) => setCatId(e.target.value)}>
            <option value="">Category…</option>
            {cats.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
          <select value={groupId} onChange={(e) => setGroupId(e.target.value)} disabled={!catId}>
            <option value="">Type…</option>
            {groups.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
          </select>
          <select value={collId} onChange={(e) => setCollId(e.target.value)} disabled={!groupId}>
            <option value="">Collection…</option>
            {colls.map((c) => <option key={c.id} value={c.id}>{c.name}{c.animated ? ' 🎞' : ''}</option>)}
          </select>
          <input value={tags} onChange={(e) => setTags(e.target.value)} placeholder="Extra tags (comma separated)" />
          <button className="med-upload" disabled={busy || !collId} onClick={upload}>{busy ? 'Uploading…' : 'Create & upload sticker'}</button>
          {msg && <div className={msg.startsWith('✗') ? 'error' : 'med-hint'}>{msg}</div>}
        </div>
      )}
      </div>{/* /med-side */}
      </div>{/* /med-body */}

      {/* Dialogs */}
      {textDialog && (
        <div className="med-modal-bg" onClick={() => setTextDialog(null)}>
          <div className="med-modal" onClick={(e) => e.stopPropagation()}>
            <h3>{textDialog.editId ? 'Edit text' : 'Text'}</h3>
            <textarea autoFocus value={textDialog.value} onChange={(e) => setTextDialog({ ...textDialog, value: e.target.value })} placeholder="Type here…" />
            <div className="med-modal-actions">
              <button className="cancel" onClick={() => setTextDialog(null)}>Cancel</button>
              <button className="ok" onClick={() => {
                const v = textDialog.value;
                if (textDialog.editId) patch({ text: v || 'Text' });
                else if (v.trim()) addText(v);
                setTextDialog(null);
              }}>{textDialog.editId ? 'Save' : 'Add'}</button>
            </div>
          </div>
        </div>
      )}

      {emojiPicker && (
        <div className="med-modal-bg" onClick={() => setEmojiPicker(false)}>
          <div className="med-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Emoji &amp; stickers</h3>
            <div className="med-emoji-grid">
              {EMOJIS.map((e) => <button key={e} onClick={() => { addEmoji(e); setEmojiPicker(false); }}>{e}</button>)}
            </div>
          </div>
        </div>
      )}

      {shapePicker && (
        <div className="med-modal-bg" onClick={() => setShapePicker(false)}>
          <div className="med-modal" onClick={(e) => e.stopPropagation()}>
            <h3>Add a shape</h3>
            <div className="med-emoji-grid">
              {SHAPES.map((k) => <button key={k} title={k} onClick={() => { addShape(k, '#FFC107'); setShapePicker(false); }}>{shapeGlyph(k)}</button>)}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function shapeGlyph(k) { return { circle: '●', square: '■', rounded: '▢', triangle: '▲', heart: '♥' }[k] || '◆'; }

function Adj({ a, value, onChange }) {
  const step = a.step || 1;
  return (
    <div className="med-adj">
      <div className="med-adj-head">
        <span className="lab">{a.label}</span>
        <span className="val">{a.fmt ? a.fmt(value) : Math.round(value)}</span>
        <span className="steps">
          <button onClick={() => onChange(clamp(+(value - step).toFixed(3), a.min, a.max))}>−</button>
          <button onClick={() => onChange(clamp(+(value + step).toFixed(3), a.min, a.max))}>+</button>
          {a.def !== undefined && <button title="Reset" onClick={() => onChange(a.def)}>↺</button>}
        </span>
      </div>
      <input type="range" min={a.min} max={a.max} step={step} value={value} onChange={(e) => onChange(+e.target.value)} />
    </div>
  );
}

/**
 * Live "here's what Apply will give you" thumbnail of the crop — the same pixels applyCrop cuts,
 * masked to the same shape, so a circle crop is visibly round before it's committed rather than
 * a promise the shape buttons make and only the result keeps.
 */
function CropPreview({ baseImg, base, crop, shape, SIZE, H }) {
  const ref = useRef(null);
  const MAX = 128;
  useEffect(() => {
    const cv = ref.current;
    if (!cv || !baseImg || !crop || crop.w < 8 || crop.h < 8) return;
    // Re-render the photo exactly as the canvas shows it, then cut the crop rect out of it.
    const off = document.createElement('canvas'); off.width = SIZE; off.height = H;
    const o = off.getContext('2d');
    o.save();
    o.translate(SIZE / 2 + base.x, H / 2 + base.y);
    o.rotate((base.rotation * Math.PI) / 180);
    o.scale(base.scale * (base.flipX ? -1 : 1), base.scale * (base.stretchY ?? 1));
    const s0 = Math.min(SIZE / baseImg.width, H / baseImg.height);
    o.drawImage(baseImg, (-baseImg.width * s0) / 2, (-baseImg.height * s0) / 2, baseImg.width * s0, baseImg.height * s0);
    o.restore();

    const fit = Math.min(MAX / crop.w, MAX / crop.h);
    cv.width = Math.max(1, Math.round(crop.w * fit));
    cv.height = Math.max(1, Math.round(crop.h * fit));
    const c = cv.getContext('2d');
    c.clearRect(0, 0, cv.width, cv.height);
    if (shape === 'circle') {
      c.beginPath();
      c.ellipse(cv.width / 2, cv.height / 2, cv.width / 2, cv.height / 2, 0, 0, Math.PI * 2);
      c.clip();
    }
    c.drawImage(off, crop.x, crop.y, crop.w, crop.h, 0, 0, cv.width, cv.height);
  }, [baseImg, base, crop, shape, SIZE, H]);

  if (!crop || crop.w < 8 || crop.h < 8) {
    return <div className="med-hint" style={{ marginTop: 10 }}>Drag a box on the canvas to see a preview here.</div>;
  }
  return (
    <div style={{ marginTop: 10 }}>
      <div className="med-label" style={{ marginBottom: 6 }}>Preview</div>
      <div
        style={{
          display: 'inline-block', padding: 6, borderRadius: 8,
          // Checkerboard so a circle crop's transparent corners read as transparent.
          backgroundImage: 'repeating-conic-gradient(#3a3a40 0% 25%, #2a2a30 0% 50%)',
          backgroundSize: '12px 12px',
        }}
      >
        <canvas ref={ref} style={{ display: 'block', maxWidth: MAX, maxHeight: MAX }} />
      </div>
    </div>
  );
}

function Swatches({ list, value, onPick, allowNone }) {
  return (
    <div className="med-swatches">
      {list.map((c, i) => (
        <button
          key={i}
          className={`med-sw ${value === c ? 'sel' : ''} ${c === null ? 'none' : ''} ${c === '#ffffff' ? '' : ''}`}
          style={c ? { background: c } : undefined}
          title={c === null ? (allowNone ? 'None' : '') : c}
          onClick={() => onPick(c)}
        />
      ))}
    </div>
  );
}

function Toggle({ on, label, onClick }) {
  return <button className={`med-chip pill ${on ? 'sel' : ''}`} onClick={onClick}>{label}</button>;
}
