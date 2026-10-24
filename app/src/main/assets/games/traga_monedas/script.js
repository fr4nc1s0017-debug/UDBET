/* =========================================================
   SLOT MACHINE — lógica completa
   ========================================================= */

/* ---------- Símbolos y pesos ----------
   RTP ≈ 95% (calculado sobre 5 líneas) */
const SYMBOLS = [
  { id:'cherry',  emoji:'🍒', weight:8, pay3:14,  pay2:1,  name:'Cereza'   },
  { id:'lemon',   emoji:'🍋', weight:7, pay3:18,  pay2:1,  name:'Limón'    },
  { id:'orange',  emoji:'🍊', weight:6, pay3:25,  pay2:1,  name:'Naranja'  },
  { id:'grape',   emoji:'🍇', weight:5, pay3:36,  pay2:2,  name:'Uva'      },
  { id:'bell',    emoji:'🔔', weight:4, pay3:55,  pay2:3,  name:'Campana'  },
  { id:'star',    emoji:'⭐', weight:2, pay3:130, pay2:5,  name:'Estrella' },
  { id:'diamond', emoji:'💎', weight:1, pay3:350, pay2:8,  name:'Diamante' },
  { id:'seven',   emoji:'7️⃣', weight:1, pay3:900, pay2:15, name:'Siete'    },
];

const TOTAL_WEIGHT = SYMBOLS.reduce((s, x) => s + x.weight, 0);

const PAYLINES = [
  { id:0, cells:[[0,0],[1,0],[2,0]], color:'#ff4d6d' }, // fila superior
  { id:1, cells:[[0,1],[1,1],[2,1]], color:'#4dabf7' }, // fila central
  { id:2, cells:[[0,2],[1,2],[2,2]], color:'#3fd67a' }, // fila inferior
  { id:3, cells:[[0,0],[1,1],[2,2]], color:'#cc5de8' }, // diagonal ↘
  { id:4, cells:[[0,2],[1,1],[2,0]], color:'#ff922b' }, // diagonal ↗
];

const BET_OPTIONS = [1, 2, 5, 10, 25, 50];
const NUM_LINES = PAYLINES.length;

/* ---------- Estado ---------- */
const state = {
  balance: 0,            // se sobreescribe con el saldo real al cargar (ver GameActivity)
  betIndex: 2,          // $5 por línea → $25 total
  spinning: false,
  auto: false,
  autoCount: 0,
  muted: false,
  grid: [[], [], []],   // grid[reel][row]
};

/* ---------- DOM ---------- */
const el = {
  reels:      document.getElementById('reels'),
  balance:    document.getElementById('balance'),
  totalBet:   document.getElementById('totalBet'),
  betPerLine: document.getElementById('betPerLine'),
  winText:    document.getElementById('winText'),
  winSub:     document.getElementById('winSub'),
  spinBtn:    document.getElementById('spinBtn'),
  betUp:      document.getElementById('betUp'),
  betDown:    document.getElementById('betDown'),
  maxBetBtn:  document.getElementById('maxBetBtn'),
  autoBtn:    document.getElementById('autoBtn'),
  paytableBtn:document.getElementById('paytableBtn'),
  payModal:   document.getElementById('payModal'),
  modalClose: document.getElementById('modalClose'),
  pay3Table:  document.getElementById('pay3Table'),
  pay2Table:  document.getElementById('pay2Table'),
  muteBtn:    document.getElementById('muteBtn'),
  toast:      document.getElementById('toast'),
  hint:       document.getElementById('hint'),
};

const strips = [0, 1, 2].map(i =>
  document.querySelector(`.reel[data-reel="${i}"] .strip`)
);

/* ---------- Formato de dinero (siempre 2 decimales) ---------- */
function money(n) {
  return '$' + n.toFixed(2);
}

/* =========================================================
   AUDIO (síntesis con Web Audio API)
   ========================================================= */
let audioCtx = null;

function initAudio() {
  if (audioCtx) {
    if (audioCtx.state === 'suspended') audioCtx.resume();
    return;
  }
  try {
    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
  } catch (e) { audioCtx = null; }
}

function tone(freq, dur, type = 'sine', vol = 0.08, delay = 0) {
  if (!audioCtx || state.muted) return;
  const t = audioCtx.currentTime + delay;
  const osc = audioCtx.createOscillator();
  const gain = audioCtx.createGain();
  osc.type = type;
  osc.frequency.setValueAtTime(freq, t);
  gain.gain.setValueAtTime(0.0001, t);
  gain.gain.exponentialRampToValueAtTime(vol, t + 0.012);
  gain.gain.exponentialRampToValueAtTime(0.0001, t + dur);
  osc.connect(gain);
  gain.connect(audioCtx.destination);
  osc.start(t);
  osc.stop(t + dur + 0.05);
}

function sfxSpin() {
  tone(180, 0.08, 'square', 0.05);
  tone(140, 0.10, 'square', 0.04, 0.06);
}

function sfxReelStop(index) {
  tone(90 + index * 18, 0.14, 'triangle', 0.09);
}

function sfxTick() {
  tone(1200, 0.02, 'square', 0.02);
}

function sfxWin(kind, mult) {
  const notes = kind === 3
    ? [523, 659, 784, 1047]
    : [440, 554];
  notes.forEach((f, i) => tone(f, 0.16, 'sine', 0.10, i * 0.09));
}

function sfxBigWin() {
  const notes = [523, 659, 784, 1047, 1319, 1568];
  notes.forEach((f, i) => {
    tone(f, 0.20, 'sine', 0.11, i * 0.10);
    tone(f * 2, 0.14, 'triangle', 0.05, i * 0.10);
  });
}

function sfxLose() {
  tone(180, 0.22, 'sawtooth', 0.05);
}

/* =========================================================
   UTILIDADES
   ========================================================= */

function weightedRandom() {
  let r = Math.random() * TOTAL_WEIGHT;
  for (const s of SYMBOLS) {
    r -= s.weight;
    if (r <= 0) return s;
  }
  return SYMBOLS[SYMBOLS.length - 1];
}

function betPerLine() { return BET_OPTIONS[state.betIndex]; }
function totalBet()    { return betPerLine() * NUM_LINES; }

const sleep = ms => new Promise(r => setTimeout(r, ms));

function showToast(text) {
  el.toast.textContent = text;
  el.toast.classList.add('show');
  clearTimeout(el.toast._t);
  el.toast._t = setTimeout(() => el.toast.classList.remove('show'), 1500);
}

/* =========================================================
   RENDERIZADO DE RODILLOS
   ========================================================= */

function renderStrip(stripEl, symbols) {
  stripEl.innerHTML = symbols
    .map(s => `<div class="cell">${s.emoji}</div>`)
    .join('');
}

function initReels() {
  for (let r = 0; r < 3; r++) {
    const syms = Array.from({ length: 6 }, () => weightedRandom());
    renderStrip(strips[r], syms);
    strips[r].style.transition = 'none';
    strips[r].style.transform = 'translateY(0)';
    state.grid[r] = syms.slice(0, 3);
  }
}

/** Anima un rodillo hasta mostrar `targets` (array de 3 símbolos). */
function animateReel(reelIndex, targets, duration) {
  return new Promise(resolve => {
    const stripEl = strips[reelIndex];

    const before = 22 + Math.floor(Math.random() * 10);
    const after  = 5;

    const cells = [
      ...Array.from({ length: before }, () => weightedRandom()),
      ...targets,
      ...Array.from({ length: after },  () => weightedRandom()),
    ];

    renderStrip(stripEl, cells);

    const L = cells.length;
    const targetPercent = (before / L) * 100;

    // Reset sin transición
    stripEl.style.transition = 'none';
    stripEl.style.transform  = 'translateY(0)';
    void stripEl.offsetHeight; // forzar reflow

    // Animación con ligero rebote
    stripEl.style.transition =
      `transform ${duration}ms cubic-bezier(0.18, 1.06, 0.32, 1)`;
    stripEl.style.transform = `translateY(-${targetPercent}%)`;

    setTimeout(() => {
      // Normalizar: dejar los targets arriba sin salto visual
      stripEl.style.transition = 'none';
      renderStrip(stripEl, [...targets, ...Array.from({ length: 3 }, () => weightedRandom())]);
      stripEl.style.transform = 'translateY(0)';
      sfxReelStop(reelIndex);
      resolve();
    }, duration + 30);
  });
}

/* =========================================================
   EVALUACIÓN DE GANANCIAS
   ========================================================= */

function evaluate(grid, bpl) {
  const results = [];
  let totalWin = 0;

  for (const line of PAYLINES) {
    const [a, b, c] = line.cells.map(([r, row]) => grid[r][row]);
    let mult = 0, kind = 0, sym = a;

    if (a.id === b.id && b.id === c.id) {
      mult = a.pay3;
      kind = 3;
    } else if (a.id === b.id) {
      mult = a.pay2;
      kind = 2;
    }

    if (mult > 0) {
      const amount = mult * bpl;
      totalWin += amount;
      results.push({ line: line.id, symbol: sym, kind, mult, amount });
    }
  }

  return { results, totalWin };
}

/* =========================================================
   PRESENTACIÓN DE GANANCIAS
   ========================================================= */

function clearWinHighlights() {
  document.querySelectorAll('.payline').forEach(p => {
    p.classList.remove('active');
    p.style.stroke = '';
  });
  document.querySelectorAll('.cell.win').forEach(c => c.classList.remove('win'));
}

function highlightCells(cells, color) {
  for (const [reel, row] of cells) {
    const cell = strips[reel].children[row];
    if (cell) {
      cell.classList.add('win');
      cell.style.color = color;
    }
  }
}

async function presentWin(results, totalWin) {
  const isBig = totalWin >= betPerLine() * 40;

  // Iluminar líneas ganadoras
  for (let i = 0; i < results.length; i++) {
    const res = results[i];
    const line = PAYLINES[res.line];
    const path = document.querySelector(`.payline[data-line="${res.line}"]`);

    path.style.stroke = line.color;
    path.style.color  = line.color;
    path.classList.add('active');

    highlightCells(line.cells, line.color);
    sfxWin(res.kind, res.mult);

    await sleep(280);
  }

  // Contador animado
  el.winText.className = isBig ? 'big' : 'win';
  await countUp(el.winText, 0, totalWin, isBig ? 900 : 600, isBig);

  if (isBig) {
    sfxBigWin();
    showToast(`¡GRAN PREMIO! ${money(totalWin)}`);
  }

  const detail = results.length === 1
    ? `${results[0].kind === 3 ? '3' : '2'} × ${results[0].symbol.emoji} en línea ${results[0].line + 1}`
    : `${results.length} líneas ganadoras`;
  el.winSub.textContent = detail;

  await sleep(isBig ? 1800 : 1300);
}

function countUp(node, from, to, duration, big) {
  return new Promise(resolve => {
    const start = performance.now();
    function tick(now) {
      const t = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      const val = from + (to - from) * eased;
      node.textContent = big ? `¡${money(val)}!` : `¡GANASTE ${money(val)}!`;
      if (t < 1) requestAnimationFrame(tick);
      else resolve();
    }
    requestAnimationFrame(tick);
  });
}

/* =========================================================
   GIRO
   ========================================================= */

async function spin() {
  if (state.spinning) return;
  const cost = totalBet();

  if (state.balance < cost) {
    showToast('Fondos insuficientes');
    stopAuto();
    return;
  }

  initAudio();
  state.spinning = true;
  state.balance -= cost;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.placeBet(cost);

  clearWinHighlights();
  el.winText.className = '';
  el.winText.textContent = 'Girando...';
  el.winSub.textContent = `Apuesta: ${money(cost)}`;
  updateUI();

  sfxSpin();

  // 1) Generar resultado ANTES de animar
  const grid = [[], [], []];
  for (let r = 0; r < 3; r++) {
    for (let row = 0; row < 3; row++) {
      grid[r][row] = weightedRandom();
    }
  }
  state.grid = grid;

  // 2) Animar los 3 rodillos con paradas escalonadas
  const durations = [950, 1180, 1420];
  await Promise.all(
    [0, 1, 2].map(r => animateReel(r, [grid[r][0], grid[r][1], grid[r][2]], durations[r]))
  );

  await sleep(180);

  // 3) Evaluar
  const { results, totalWin } = evaluate(grid, betPerLine());
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.reportResult(totalWin > 0, totalWin);

  if (totalWin > 0) {
    state.balance += totalWin;
    updateUI();
    await presentWin(results, totalWin);
  } else {
    el.winText.className = 'lose';
    el.winText.textContent = 'Sin suerte...';
    el.winSub.textContent = `Perdiste ${money(cost)}`;
    sfxLose();
    await sleep(900);
  }

  // 4) Limpiar
  clearWinHighlights();
  el.winText.className = '';
  el.winText.textContent = '¡Buena suerte!';
  el.winSub.textContent = `${NUM_LINES} líneas activas`;

  state.spinning = false;
  updateUI();

  // Continuar autoplay
  if (state.auto) {
    if (state.balance < totalBet()) {
      stopAuto();
      showToast('Auto detenido: fondos insuficientes');
    } else {
      setTimeout(() => { if (state.auto) spin(); }, 500);
    }
  }
}

/* =========================================================
   AUTOPLAY
   ========================================================= */

function startAuto() {
  if (state.auto) { stopAuto(); return; }
  if (state.balance < totalBet()) {
    showToast('Fondos insuficientes');
    return;
  }
  state.auto = true;
  el.autoBtn.classList.add('active');
  el.autoBtn.textContent = 'STOP';
  if (!state.spinning) spin();
}

function stopAuto() {
  state.auto = false;
  el.autoBtn.classList.remove('active');
  el.autoBtn.textContent = 'AUTO';
}

/* =========================================================
   UI
   ========================================================= */

function updateUI() {
  el.balance.textContent = money(state.balance);
  el.totalBet.textContent = money(totalBet());
  el.betPerLine.textContent = money(betPerLine());

  const canSpin = !state.spinning && state.balance >= totalBet();
  el.spinBtn.disabled = !canSpin;
  el.spinBtn.textContent = state.spinning ? 'GIRANDO...' : 'GIRAR';

  el.betUp.disabled   = state.spinning || state.betIndex >= BET_OPTIONS.length - 1;
  el.betDown.disabled = state.spinning || state.betIndex <= 0;
  el.maxBetBtn.disabled = state.spinning;

  // Si no puede permitirse la apuesta actual, avisar
  if (!state.spinning && state.balance < totalBet() && state.balance > 0) {
    el.hint.innerHTML = '<b style="color:#ff4d5e">Fondos insuficientes para esta apuesta</b>';
  } else if (!state.spinning && state.balance <= 0) {
    el.hint.innerHTML = '<b style="color:#ff4d5e">Fondos insuficientes. Agrega saldo para seguir jugando</b>';
  } else if (!state.spinning) {
    el.hint.innerHTML = 'Pulsa <b>ESPACIO</b> para girar · 3 iguales pagan · 2 iguales en las dos primeras pagan';
  }
}

/* =========================================================
   TABLA DE PAGOS
   ========================================================= */

function buildPaytable() {
  const sorted = [...SYMBOLS].reverse();
  el.pay3Table.innerHTML = sorted.map(s => `
    <div class="pay-item">
      <div class="pay-syms">${s.emoji}${s.emoji}${s.emoji}</div>
      <div class="pay-mult">${s.pay3}× <small>por línea</small></div>
    </div>
  `).join('');

  el.pay2Table.innerHTML = sorted.map(s => `
    <div class="pay-item">
      <div class="pay-syms">${s.emoji}${s.emoji}·</div>
      <div class="pay-mult">${s.pay2}× <small>por línea</small></div>
    </div>
  `).join('');
}

/* =========================================================
   AJUSTE DE TAMAÑO DEL SÍMBOLO
   ========================================================= */

function updateSymbolSize() {
  const reel = document.querySelector('.reel');
  if (!reel) return;
  const w = reel.getBoundingClientRect().width;
  if (w > 0) {
    document.documentElement.style.setProperty('--sym-size', Math.round(w * 0.56) + 'px');
  }
}

/* =========================================================
   EVENTOS
   ========================================================= */

el.spinBtn.addEventListener('click', () => { initAudio(); spin(); });

el.betUp.addEventListener('click', () => {
  if (state.spinning) return;
  if (state.betIndex < BET_OPTIONS.length - 1) {
    state.betIndex++;
    initAudio(); tone(660, 0.05, 'square', 0.04);
    updateUI();
  }
});

el.betDown.addEventListener('click', () => {
  if (state.spinning) return;
  if (state.betIndex > 0) {
    state.betIndex--;
    initAudio(); tone(440, 0.05, 'square', 0.04);
    updateUI();
  }
});

el.maxBetBtn.addEventListener('click', () => {
  if (state.spinning) return;
  // Máxima apuesta que puede permitirse
  let idx = BET_OPTIONS.length - 1;
  while (idx > 0 && BET_OPTIONS[idx] * NUM_LINES > state.balance) idx--;
  state.betIndex = idx;
  initAudio(); tone(880, 0.08, 'square', 0.05);
  updateUI();
});

el.autoBtn.addEventListener('click', () => { initAudio(); startAuto(); });

el.paytableBtn.addEventListener('click', () => {
  el.payModal.classList.add('show');
});

el.modalClose.addEventListener('click', () => el.payModal.classList.remove('show'));

el.payModal.addEventListener('click', (e) => {
  if (e.target === el.payModal) el.payModal.classList.remove('show');
});

el.muteBtn.addEventListener('click', () => {
  state.muted = !state.muted;
  el.muteBtn.textContent = state.muted ? '🔇' : '🔊';
  if (!state.muted) { initAudio(); tone(700, 0.08, 'sine', 0.06); }
});

/* Teclado */
document.addEventListener('keydown', (e) => {
  const k = e.key.toLowerCase();

  if (k === ' ' || k === 'enter') {
    e.preventDefault();
    if (!el.payModal.classList.contains('show')) {
      initAudio();
      if (!state.spinning) spin();
    }
  }
  if (k === 'escape') el.payModal.classList.remove('show');
  if (k === 'arrowup')   { e.preventDefault(); el.betUp.click(); }
  if (k === 'arrowdown') { e.preventDefault(); el.betDown.click(); }
  if (k === 'm') el.muteBtn.click();
});

/* Resize */
let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(updateSymbolSize, 80);
});

/* =========================================================
   INICIO
   ========================================================= */

buildPaytable();
initReels();
updateSymbolSize();
updateUI();

// Esperar a que las fuentes/emoji carguen para medir bien
setTimeout(updateSymbolSize, 200);
window.addEventListener('load', updateSymbolSize);