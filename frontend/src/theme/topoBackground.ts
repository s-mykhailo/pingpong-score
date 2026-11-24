// Animated topographic-contour background rendered on a fixed canvas behind #app.
// A 3D simplex noise field is sampled on a grid each frame; contour lines are
// extracted per threshold via marching squares, giving a slowly drifting
// topo-map effect tinted with the current Material You accent color.

const CELL = 28; // px per grid cell (CSS pixels)
const LEVELS = [-0.55, -0.4, -0.25, -0.1, 0.05, 0.2, 0.35, 0.5];
const SPEED = 0.00005; // noise z-units per ms — how fast contours drift
const MAX_DPR = 1.5;
const LINE_ALPHA = 0.16;

let canvas: HTMLCanvasElement | null = null;
let ctx: CanvasRenderingContext2D | null = null;
let raf = 0;
let dpr = 1;
let w = 0;
let h = 0;
let cols = 0;
let rows = 0;
let field: Float32Array | null = null;
let z = 0;
let lastTs = 0;
let running = false;
let reduceMotion = false;

export function initTopoBackground(): void {
  if (canvas || typeof document === 'undefined') return;

  canvas = document.createElement('canvas');
  canvas.className = 'topo-bg';
  canvas.setAttribute('aria-hidden', 'true');
  document.body.insertBefore(canvas, document.body.firstChild);
  ctx = canvas.getContext('2d');
  if (!ctx) return;

  const motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');
  reduceMotion = motionQuery.matches;
  motionQuery.addEventListener('change', (e) => {
    reduceMotion = e.matches;
    if (reduceMotion) stop();
    else if (!document.hidden) start();
  });

  resize();
  window.addEventListener('resize', resize);

  document.addEventListener('visibilitychange', () => {
    if (document.hidden) stop();
    else if (!reduceMotion) start();
  });

  // Theme changes (accent color, light/dark) patch CSS vars onto the inline
  // style of <html> — repaint so the contour color stays in sync.
  new MutationObserver(() => drawFrame()).observe(document.documentElement, {
    attributes: true,
    attributeFilter: ['style'],
  });

  drawFrame();
  if (!reduceMotion) start();
}

function resize(): void {
  if (!canvas || !ctx) return;
  dpr = Math.min(window.devicePixelRatio || 1, MAX_DPR);
  w = window.innerWidth;
  h = window.innerHeight;
  canvas.style.width = `${w}px`;
  canvas.style.height = `${h}px`;
  canvas.width = Math.round(w * dpr);
  canvas.height = Math.round(h * dpr);
  cols = Math.ceil(w / CELL) + 1;
  rows = Math.ceil(h / CELL) + 1;
  field = new Float32Array(cols * rows);
  drawFrame();
}

function start(): void {
  if (running || reduceMotion) return;
  running = true;
  lastTs = performance.now();
  raf = requestAnimationFrame(tick);
}

function stop(): void {
  running = false;
  if (raf) cancelAnimationFrame(raf);
  raf = 0;
}

function tick(ts: number): void {
  const dt = ts - lastTs;
  lastTs = ts;
  z += dt * SPEED;
  drawFrame();
  raf = requestAnimationFrame(tick);
}

function sampleField(): void {
  if (!field) return;
  for (let j = 0; j < rows; j++) {
    for (let i = 0; i < cols; i++) {
      field[j * cols + i] = simplex3(i * 0.09, j * 0.09, z);
    }
  }
}

function drawFrame(): void {
  if (!ctx || !canvas || !field) return;
  sampleField();

  ctx.save();
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  ctx.clearRect(0, 0, w, h);
  ctx.lineWidth = 1;
  ctx.strokeStyle = contourColor();

  for (const level of LEVELS) marchSquares(level);
  ctx.restore();
}

function contourColor(): string {
  const raw = getComputedStyle(document.documentElement).getPropertyValue('--md-sys-color-primary').trim();
  return toRgba(raw || '#6750a4', LINE_ALPHA);
}

function toRgba(hex: string, alpha: number): string {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex);
  if (!m) return `rgba(103, 80, 164, ${alpha})`;
  const int = parseInt(m[1], 16);
  const r = (int >> 16) & 255;
  const g = (int >> 8) & 255;
  const b = int & 255;
  return `rgba(${r}, ${g}, ${b}, ${alpha})`;
}

function lerp(a: number, b: number, va: number, vb: number, level: number): number {
  const t = vb === va ? 0.5 : (level - va) / (vb - va);
  return a + (b - a) * t;
}

function marchSquares(level: number): void {
  if (!ctx || !field) return;
  ctx.beginPath();
  for (let j = 0; j < rows - 1; j++) {
    for (let i = 0; i < cols - 1; i++) {
      const tl = field[j * cols + i];
      const tr = field[j * cols + i + 1];
      const br = field[(j + 1) * cols + i + 1];
      const bl = field[(j + 1) * cols + i];

      let idx = 0;
      if (tl > level) idx |= 8;
      if (tr > level) idx |= 4;
      if (br > level) idx |= 2;
      if (bl > level) idx |= 1;
      if (idx === 0 || idx === 15) continue;

      const x0 = i * CELL;
      const y0 = j * CELL;
      const x1 = x0 + CELL;
      const y1 = y0 + CELL;

      const top: [number, number] = [lerp(x0, x1, tl, tr, level), y0];
      const right: [number, number] = [x1, lerp(y0, y1, tr, br, level)];
      const bottom: [number, number] = [lerp(x0, x1, bl, br, level), y1];
      const left: [number, number] = [x0, lerp(y0, y1, tl, bl, level)];

      drawCase(idx, top, right, bottom, left);
    }
  }
  ctx.stroke();
}

function drawCase(
  idx: number,
  top: [number, number],
  right: [number, number],
  bottom: [number, number],
  left: [number, number],
): void {
  if (!ctx) return;
  const seg = (a: [number, number], b: [number, number]) => {
    ctx!.moveTo(a[0], a[1]);
    ctx!.lineTo(b[0], b[1]);
  };
  switch (idx) {
    case 1:
    case 14:
      seg(left, bottom);
      break;
    case 2:
    case 13:
      seg(bottom, right);
      break;
    case 3:
    case 12:
      seg(left, right);
      break;
    case 4:
    case 11:
      seg(top, right);
      break;
    case 5:
      seg(left, top);
      seg(bottom, right);
      break;
    case 6:
    case 9:
      seg(top, bottom);
      break;
    case 7:
    case 8:
      seg(left, top);
      break;
    case 10:
      seg(left, bottom);
      seg(top, right);
      break;
  }
}

// --- 3D simplex noise (classic permutation-table implementation) ---

const GRAD3 = [
  [1, 1, 0], [-1, 1, 0], [1, -1, 0], [-1, -1, 0],
  [1, 0, 1], [-1, 0, 1], [1, 0, -1], [-1, 0, -1],
  [0, 1, 1], [0, -1, 1], [0, 1, -1], [0, -1, -1],
];

const PERM = new Uint8Array(512);
(function buildPermutationTable() {
  const base = new Uint8Array(256);
  for (let i = 0; i < 256; i++) base[i] = i;
  // Fixed seed shuffle (deterministic, so repeated page loads look the same).
  let seed = 1337;
  const rand = () => {
    seed = (seed * 1103515245 + 12345) & 0x7fffffff;
    return seed / 0x7fffffff;
  };
  for (let i = 255; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    [base[i], base[j]] = [base[j], base[i]];
  }
  for (let i = 0; i < 512; i++) PERM[i] = base[i & 255];
})();

const F3 = 1 / 3;
const G3 = 1 / 6;

function dot3(g: number[], x: number, y: number, z: number): number {
  return g[0] * x + g[1] * y + g[2] * z;
}

function simplex3(xin: number, yin: number, zin: number): number {
  const s = (xin + yin + zin) * F3;
  const i = Math.floor(xin + s);
  const j = Math.floor(yin + s);
  const k = Math.floor(zin + s);
  const t = (i + j + k) * G3;
  const x0 = xin - (i - t);
  const y0 = yin - (j - t);
  const z0 = zin - (k - t);

  let i1, j1, k1, i2, j2, k2;
  if (x0 >= y0) {
    if (y0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
    else if (x0 >= z0) { i1 = 1; j1 = 0; k1 = 0; i2 = 1; j2 = 0; k2 = 1; }
    else { i1 = 0; j1 = 0; k1 = 1; i2 = 1; j2 = 0; k2 = 1; }
  } else {
    if (y0 < z0) { i1 = 0; j1 = 0; k1 = 1; i2 = 0; j2 = 1; k2 = 1; }
    else if (x0 < z0) { i1 = 0; j1 = 1; k1 = 0; i2 = 0; j2 = 1; k2 = 1; }
    else { i1 = 0; j1 = 1; k1 = 0; i2 = 1; j2 = 1; k2 = 0; }
  }

  const x1 = x0 - i1 + G3;
  const y1 = y0 - j1 + G3;
  const z1 = z0 - k1 + G3;
  const x2 = x0 - i2 + 2 * G3;
  const y2 = y0 - j2 + 2 * G3;
  const z2 = z0 - k2 + 2 * G3;
  const x3 = x0 - 1 + 3 * G3;
  const y3 = y0 - 1 + 3 * G3;
  const z3 = z0 - 1 + 3 * G3;

  const ii = i & 255;
  const jj = j & 255;
  const kk = k & 255;

  const gi0 = PERM[ii + PERM[jj + PERM[kk]]] % 12;
  const gi1 = PERM[ii + i1 + PERM[jj + j1 + PERM[kk + k1]]] % 12;
  const gi2 = PERM[ii + i2 + PERM[jj + j2 + PERM[kk + k2]]] % 12;
  const gi3 = PERM[ii + 1 + PERM[jj + 1 + PERM[kk + 1]]] % 12;

  let n0 = 0, n1 = 0, n2 = 0, n3 = 0;

  let t0 = 0.6 - x0 * x0 - y0 * y0 - z0 * z0;
  if (t0 >= 0) { t0 *= t0; n0 = t0 * t0 * dot3(GRAD3[gi0], x0, y0, z0); }

  let t1 = 0.6 - x1 * x1 - y1 * y1 - z1 * z1;
  if (t1 >= 0) { t1 *= t1; n1 = t1 * t1 * dot3(GRAD3[gi1], x1, y1, z1); }

  let t2 = 0.6 - x2 * x2 - y2 * y2 - z2 * z2;
  if (t2 >= 0) { t2 *= t2; n2 = t2 * t2 * dot3(GRAD3[gi2], x2, y2, z2); }

  let t3 = 0.6 - x3 * x3 - y3 * y3 - z3 * z3;
  if (t3 >= 0) { t3 *= t3; n3 = t3 * t3 * dot3(GRAD3[gi3], x3, y3, z3); }

  return 32 * (n0 + n1 + n2 + n3);
}
