/* =========================================================
   CROSS ROAD BET — juego completo
   ========================================================= */

/* ---------- Configuración ---------- */
const COLS = 9;
const ROWS = 12;              // 0 = zona segura, 1-10 = carreteras, 11 = meta
const TILE = 48;
const W = COLS * TILE;
const H = ROWS * TILE;

// Multiplicador según la fila alcanzada (índice = fila)
const MULT = [0.9, 1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 2, 2.1];

// Configuración de cada carril (fila 1 a 10)
const LANES = [
  { dir:  1, speed: 1.8, interval: 2.6, len: 1, color: '#ff6b6b' },
  { dir: -1, speed: 2.1, interval: 2.4, len: 2, color: '#feca57' },
  { dir:  1, speed: 2.5, interval: 2.1, len: 1, color: '#48dbfb' },
  { dir: -1, speed: 2.4, interval: 2.2, len: 3, color: '#ff9ff3' },
  { dir:  1, speed: 2.9, interval: 1.8, len: 1, color: '#1dd1a1' },
  { dir: -1, speed: 3.1, interval: 1.7, len: 2, color: '#f368e0' },
  { dir:  1, speed: 3.5, interval: 1.5, len: 1, color: '#ff9f43' },
  { dir: -1, speed: 3.3, interval: 1.6, len: 2, color: '#54a0ff' },
  { dir:  1, speed: 3.9, interval: 1.3, len: 1, color: '#ee5253' },
  { dir: -1, speed: 4.1, interval: 1.2, len: 2, color: '#c8d6e5' },
];

/* ---------- Canvas ---------- */
const canvas = document.getElementById('game');
const ctx = canvas.getContext('2d');
const dpr = Math.min(window.devicePixelRatio || 1, 2);

canvas.width = W * dpr;
canvas.height = H * dpr;
canvas.style.aspectRatio = W + ' / ' + H;
ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

function rowY(row) { return (ROWS - 1 - row) * TILE; }

/* ---------- Formato de dinero (siempre 2 decimales) ---------- */
function money(n) {
  return '$' + n.toFixed(2);
}

/* ---------- Estado del juego ---------- */
const G = {
  phase: 'betting',       // betting | playing | crashed | cashed
  balance: 0,              // se sobreescribe con el saldo real al cargar (ver GameActivity)
  bet: 0,
  lastBet: 0,
  chicken: { col: 4, row: 0, x: 4 * TILE, y: rowY(0), tx: 4 * TILE, ty: rowY(0), hop: 0 },
  cars: [],
  laneTimers: [],
  time: 0,
  flash: 0,
  shake: 0,
};

/* ---------- Referencias DOM ---------- */
const el = {
  bet:        document.getElementById('betDisplay'),
  balance:    document.getElementById('balanceDisplay'),
  mult:       document.getElementById('multDisplay'),
  win:        document.getElementById('winDisplay'),
  msg:        document.getElementById('bigMessage'),
  betCtrls:   document.getElementById('betControls'),
  betCtrls2:  document.getElementById('betControls2'),
  playCtrls:  document.getElementById('playControls'),
  dpad:       document.getElementById('dpad'),
  endCtrls:   document.getElementById('endControls'),
  hint:       document.getElementById('hint'),
  btnPlay:    document.getElementById('btnPlay'),
  btnClear:   document.getElementById('btnClear'),
  btnRepeat:  document.getElementById('btnRepeat'),
  btnCash:    document.getElementById('btnCash'),
  btnNext:    document.getElementById('btnNext'),
};

/* =========================================================
   LÓGICA DEL JUEGO
   ========================================================= */

function currentMultiplier() {
  return MULT[G.chicken.row] || 1;
}

function spawnCar(laneIndex) {
  const lane = LANES[laneIndex];
  const row = laneIndex + 1;
  const y = rowY(row);

  // Evitar solapamiento con el coche más cercano en el mismo carril
  const spawnX = lane.dir > 0 ? -lane.len * TILE : W;
  for (const c of G.cars) {
    if (c.row !== row) continue;
    if (Math.abs(c.x - spawnX) < TILE * 1.5) return;
  }

  G.cars.push({
    x: spawnX,
    y,
    row,
    len: lane.len,
    dir: lane.dir,
    speed: lane.speed,
    color: lane.color,
  });
}

function startGame() {
  if (G.phase !== 'betting') return;
  if (G.bet <= 0 || G.bet > G.balance) return;

  G.balance -= G.bet;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.placeBet(G.bet);
  G.lastBet = G.bet;
  G.phase = 'playing';
  G.cars = [];
  G.time = 0;
  G.flash = 0;
  G.shake = 0;

  G.chicken = {
    col: 4, row: 0,
    x: 4 * TILE, y: rowY(0),
    tx: 4 * TILE, ty: rowY(0),
    hop: 0,
  };

  // Timers iniciales aleatorios para que no aparezcan todos a la vez
  G.laneTimers = LANES.map(l => 0.8 + Math.random() * l.interval);

  showMessage('', '');
  updateUI();
}

function moveChicken(dx, dy) {
  if (G.phase !== 'playing') return;

  const nc = G.chicken.col + dx;
  const nr = G.chicken.row + dy;

  if (nc < 0 || nc >= COLS) return;
  if (nr < 0 || nr >= ROWS) return;
  if (nc === G.chicken.col && nr === G.chicken.row) return;

  G.chicken.col = nc;
  G.chicken.row = nr;
  G.chicken.tx = nc * TILE;
  G.chicken.ty = rowY(nr);
  G.chicken.hop = 1;

  updateUI();

  // ¿Llegó a la meta?
  if (nr === ROWS - 1) {
    setTimeout(() => {
      if (G.phase === 'playing') cashOut(true);
    }, 180);
  }
}

function cashOut(reachedGoal = false) {
  if (G.phase !== 'playing') return;

  const mult = currentMultiplier();
  const win = G.bet * mult;

  G.balance += win;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.reportResult(true, win);
  G.phase = 'cashed';

  if (reachedGoal) {
    showMessage(`¡META! Ganaste ${money(win)} (${mult.toFixed(2)}x)`, 'win');
  } else {
    showMessage(`Cobraste ${money(win)} (${mult.toFixed(2)}x)`, 'win');
  }

  updateUI();
}

function crash() {
  if (G.phase !== 'playing') return;
  G.phase = 'crashed';
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.reportResult(false, 0);
  G.flash = 1;
  G.shake = 1;
  showMessage(`¡Atropellado! Perdiste ${money(G.bet)}`, 'lose');
  updateUI();
}

/* =========================================================
   BUCLE PRINCIPAL
   ========================================================= */

let lastTime = performance.now();

function loop(now) {
  const dt = Math.min((now - lastTime) / 1000, 0.05);
  lastTime = now;

  update(dt);
  render();

  requestAnimationFrame(loop);
}

function update(dt) {
  // Decaimiento de efectos
  if (G.flash > 0) G.flash = Math.max(0, G.flash - dt * 1.5);
  if (G.shake > 0) G.shake = Math.max(0, G.shake - dt * 1.5);

  // Interpolación del pollo
  const c = G.chicken;
  const t = 1 - Math.pow(0.0001, dt);
  c.x += (c.tx - c.x) * t;
  c.y += (c.ty - c.y) * t;
  if (c.hop > 0) c.hop = Math.max(0, c.hop - dt * 6);

  if (G.phase !== 'playing') return;

  G.time += dt;

  // ---- Spawn de coches ----
  for (let i = 0; i < LANES.length; i++) {
    G.laneTimers[i] -= dt;
    if (G.laneTimers[i] <= 0) {
      spawnCar(i);
      G.laneTimers[i] = LANES[i].interval;
    }
  }

  // ---- Mover coches ----
  for (const car of G.cars) {
    car.x += car.dir * car.speed * TILE * dt;
  }

  // ---- Eliminar coches fuera de pantalla ----
  G.cars = G.cars.filter(car => {
    if (car.dir > 0) return car.x < W + TILE;
    return car.x + car.len * TILE > -TILE;
  });

  // ---- Colisión ----
  checkCollision();
}

function checkCollision() {
  const c = G.chicken;

  // Hitbox del pollo (con margen de perdón)
  const chx = c.x + 11;
  const chy = c.y + 11;
  const chw = TILE - 22;
  const chh = TILE - 22;

  for (const car of G.cars) {
    const cx = car.x + 5;
    const cy = car.y + 10;
    const cw = car.len * TILE - 10;
    const ch = TILE - 20;

    if (chx < cx + cw && chx + chw > cx && chy < cy + ch && chy + chh > cy) {
      crash();
      return;
    }
  }
}

/* =========================================================
   RENDERIZADO
   ========================================================= */

function render() {
  ctx.save();

  // Shake
  if (G.shake > 0) {
    const s = G.shake * 6;
    ctx.translate((Math.random() - 0.5) * s, (Math.random() - 0.5) * s);
  }

  drawBackground();
  drawLaneMarkings();
  drawGoalAndStart();
  drawCars();
  drawChicken();

  ctx.restore();

  // Flash rojo
  if (G.flash > 0) {
    ctx.fillStyle = `rgba(248, 81, 73, ${G.flash * 0.35})`;
    ctx.fillRect(0, 0, W, H);
  }
}

function drawBackground() {
  // Base
  ctx.fillStyle = '#151a21';
  ctx.fillRect(-20, -20, W + 40, H + 40);

  // Zona segura inferior
  ctx.fillStyle = '#1a3a24';
  ctx.fillRect(0, rowY(0), W, TILE);

  // Zona segura superior (meta)
  ctx.fillStyle = '#1a3a24';
  ctx.fillRect(0, 0, W, TILE);

  // Carreteras
  for (let i = 0; i < LANES.length; i++) {
    const row = i + 1;
    const y = rowY(row);
    ctx.fillStyle = i % 2 === 0 ? '#1c2128' : '#1a1f26';
    ctx.fillRect(0, y, W, TILE);
  }
}

function drawLaneMarkings() {
  ctx.strokeStyle = 'rgba(255,255,255,.07)';
  ctx.lineWidth = 1;
  ctx.setLineDash([8, 8]);

  for (let row = 1; row <= 10; row++) {
    const y = rowY(row) + TILE / 2;
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(W, y);
    ctx.stroke();
  }

  ctx.setLineDash([]);

  // Líneas verticales tenues
  ctx.strokeStyle = 'rgba(255,255,255,.03)';
  for (let col = 1; col < COLS; col++) {
    ctx.beginPath();
    ctx.moveTo(col * TILE, TILE);
    ctx.lineTo(col * TILE, H - TILE);
    ctx.stroke();
  }
}

function drawGoalAndStart() {
  // Línea de meta
  const gy = rowY(11);
  ctx.fillStyle = 'rgba(240, 196, 25, .12)';
  ctx.fillRect(0, gy, W, TILE);

  // Cuadros de meta
  const size = TILE / 3;
  for (let i = 0; i < COLS; i++) {
    for (let j = 0; j < 3; j++) {
      ctx.fillStyle = (i + j) % 2 === 0 ? 'rgba(240,196,25,.22)' : 'rgba(0,0,0,.15)';
      ctx.fillRect(i * TILE, gy + j * size, TILE / 3, size);
      ctx.fillRect(i * TILE + TILE / 3, gy + j * size, TILE / 3, size);
      ctx.fillRect(i * TILE + 2 * TILE / 3, gy + j * size, TILE / 3, size);
    }
  }

  // Texto META
  ctx.fillStyle = 'rgba(240,196,25,.85)';
  ctx.font = 'bold 13px system-ui, sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText('M E T A', W / 2, gy + TILE / 2);
}

function drawCars() {
  for (const car of G.cars) {
    drawCar(car);
  }
}

function drawCar(car) {
  const x = car.x;
  const y = car.y;
  const w = car.len * TILE;
  const h = TILE;

  const padY = 9;
  const padX = 4;

  // Sombra
  ctx.fillStyle = 'rgba(0,0,0,.4)';
  roundRect(x + padX + 2, y + padY + 3, w - padX * 2, h - padY * 2, 8);
  ctx.fill();

  // Carrocería
  const grad = ctx.createLinearGradient(x, y, x, y + h);
  grad.addColorStop(0, car.color);
  grad.addColorStop(1, shadeColor(car.color, -35));
  ctx.fillStyle = grad;
  roundRect(x + padX, y + padY, w - padX * 2, h - padY * 2, 8);
  ctx.fill();

  // Brillo superior
  ctx.fillStyle = 'rgba(255,255,255,.22)';
  roundRect(x + padX + 3, y + padY + 2, w - padX * 2 - 6, 5, 3);
  ctx.fill();

  // Ventanas
  ctx.fillStyle = 'rgba(15, 25, 40, .75)';
  const winY = y + padY + 8;
  const winH = h - padY * 2 - 16;
  if (car.len === 1) {
    roundRect(x + padX + 8, winY, w - padX * 2 - 16, winH, 4);
    ctx.fill();
  } else {
    const segW = (w - padX * 2 - 20) / car.len - 4;
    for (let i = 0; i < car.len; i++) {
      roundRect(x + padX + 8 + i * (segW + 6), winY, segW, winH, 4);
      ctx.fill();
    }
  }

  // Luces
  const lightW = 5;
  const lightH = 7;
  const lightY = y + h / 2 - lightH / 2;
  ctx.fillStyle = 'rgba(255, 245, 180, .95)';
  if (car.dir > 0) {
    roundRect(x + w - padX - lightW - 1, lightY, lightW, lightH, 2);
    ctx.fill();
  } else {
    roundRect(x + padX + 1, lightY, lightW, lightH, 2);
    ctx.fill();
  }
}

function drawChicken() {
  const c = G.chicken;
  const cx = c.x + TILE / 2;
  const cy = c.y + TILE / 2;

  const hopScale = 1 + c.hop * 0.18;
  const size = TILE * 0.62 * hopScale;

  // Sombra
  ctx.fillStyle = 'rgba(0,0,0,.4)';
  ctx.beginPath();
  ctx.ellipse(cx, c.y + TILE - 8, size * 0.45, size * 0.16, 0, 0, Math.PI * 2);
  ctx.fill();

  ctx.save();
  ctx.translate(cx, cy - c.hop * 4);

  // Patas
  ctx.strokeStyle = '#e67e22';
  ctx.lineWidth = 3;
  ctx.lineCap = 'round';
  ctx.beginPath();
  ctx.moveTo(-size * 0.18, size * 0.35);
  ctx.lineTo(-size * 0.18, size * 0.6);
  ctx.moveTo(size * 0.18, size * 0.35);
  ctx.lineTo(size * 0.18, size * 0.6);
  ctx.stroke();

  // Cuerpo
  ctx.fillStyle = '#fdfaf3';
  ctx.beginPath();
  ctx.ellipse(0, size * 0.12, size * 0.48, size * 0.42, 0, 0, Math.PI * 2);
  ctx.fill();

  // Ala
  ctx.fillStyle = '#e8e2d4';
  ctx.beginPath();
  ctx.ellipse(size * 0.18, size * 0.18, size * 0.2, size * 0.26, 0.3, 0, Math.PI * 2);
  ctx.fill();

  // Cabeza
  ctx.fillStyle = '#fdfaf3';
  ctx.beginPath();
  ctx.arc(0, -size * 0.38, size * 0.3, 0, Math.PI * 2);
  ctx.fill();

  // Cresta
  ctx.fillStyle = '#e74c3c';
  ctx.beginPath();
  ctx.arc(-size * 0.1, -size * 0.66, size * 0.1, 0, Math.PI * 2);
  ctx.arc(size * 0.06, -size * 0.7, size * 0.11, 0, Math.PI * 2);
  ctx.arc(size * 0.2, -size * 0.64, size * 0.09, 0, Math.PI * 2);
  ctx.fill();

  // Pico
  ctx.fillStyle = '#f39c12';
  ctx.beginPath();
  ctx.moveTo(0, -size * 0.36);
  ctx.lineTo(size * 0.28, -size * 0.3);
  ctx.lineTo(0, -size * 0.22);
  ctx.closePath();
  ctx.fill();

  // Ojo
  ctx.fillStyle = '#1a1a1a';
  ctx.beginPath();
  ctx.arc(size * 0.06, -size * 0.44, size * 0.055, 0, Math.PI * 2);
  ctx.fill();

  ctx.restore();
}

/* ---------- Helpers de dibujo ---------- */
function roundRect(x, y, w, h, r) {
  if (w < 2 * r) r = w / 2;
  if (h < 2 * r) r = h / 2;
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
}

function shadeColor(hex, percent) {
  const num = parseInt(hex.replace('#', ''), 16);
  let r = (num >> 16) + percent;
  let g = ((num >> 8) & 0x00FF) + percent;
  let b = (num & 0x0000FF) + percent;
  r = Math.max(0, Math.min(255, r));
  g = Math.max(0, Math.min(255, g));
  b = Math.max(0, Math.min(255, b));
  return '#' + (0x1000000 + r * 0x10000 + g * 0x100 + b).toString(16).slice(1);
}

/* =========================================================
   INTERFAZ
   ========================================================= */

function showMessage(text, type) {
  el.msg.textContent = text;
  el.msg.className = '';
  if (text) {
    el.msg.classList.add('show');
    if (type) el.msg.classList.add(type);
  }
}

function updateUI() {
  el.balance.textContent = money(G.balance);
  el.bet.textContent = money(G.bet);

  const mult = currentMultiplier();
  el.mult.textContent = mult.toFixed(2) + 'x';
  el.win.textContent = money(G.bet * mult);

  // Visibilidad de controles
  const betting = G.phase === 'betting';
  const playing = G.phase === 'playing';
  const ended = G.phase === 'crashed' || G.phase === 'cashed';

  el.betCtrls.classList.toggle('hidden', !betting);
  el.betCtrls2.classList.toggle('hidden', !betting);
  el.playCtrls.classList.toggle('hidden', !playing);
  el.dpad.classList.toggle('hidden', !playing);
  el.endCtrls.classList.toggle('hidden', !ended);

  // Estado botón jugar
  el.btnPlay.disabled = G.bet <= 0 || G.bet > G.balance;
  el.btnClear.disabled = G.bet <= 0;
  el.btnRepeat.disabled = G.lastBet <= 0 || G.lastBet > G.balance;

  // Fichas
  document.querySelectorAll('.chip').forEach(chip => {
    const v = Number(chip.dataset.value);
    chip.disabled = G.balance < G.bet + v;
  });

  // Botón cobrar
  if (playing) {
    const win = G.bet * mult;
    el.btnCash.textContent = `💰 COBRAR ${money(win)}`;
    el.btnCash.disabled = G.chicken.row === 0 && G.cars.length === 0;
  }

  // Texto de ayuda
  if (betting) {
    if (G.balance <= 0) {
      el.hint.innerHTML = '<b style="color:#ff4d5e">Fondos insuficientes. Agrega saldo para seguir jugando</b>';
    } else {
      el.hint.innerHTML = 'Elige tu apuesta y pulsa <b>JUGAR</b>';
    }
  } else if (playing) {
    el.hint.innerHTML = 'Avanza con <b>↑↓←→</b> o <b>WASD</b> · Cobra con <b>Espacio</b>';
  } else {
    el.hint.innerHTML = '';
  }
}

/* =========================================================
   EVENTOS
   ========================================================= */

// Fichas
document.querySelectorAll('.chip').forEach(chip => {
  chip.addEventListener('click', () => {
    if (G.phase !== 'betting') return;
    const v = Number(chip.dataset.value);
    if (G.balance < G.bet + v) return;
    G.bet += v;
    updateUI();
  });
});

el.btnClear.addEventListener('click', () => {
  if (G.phase !== 'betting') return;
  G.bet = 0;
  updateUI();
});

el.btnRepeat.addEventListener('click', () => {
  if (G.phase !== 'betting') return;
  if (G.lastBet <= 0 || G.lastBet > G.balance) return;
  G.bet = G.lastBet;
  updateUI();
});

el.btnPlay.addEventListener('click', startGame);

el.btnCash.addEventListener('click', () => cashOut(false));

el.btnNext.addEventListener('click', () => {
  if (G.balance <= 0) {
    showMessage('Fondos insuficientes. Agrega saldo para seguir jugando.', 'lose');
  } else {
    showMessage('', '');
  }
  G.phase = 'betting';
  G.bet = 0;
  G.cars = [];
  G.chicken = {
    col: 4, row: 0,
    x: 4 * TILE, y: rowY(0),
    tx: 4 * TILE, ty: rowY(0),
    hop: 0,
  };
  updateUI();
});

// D-pad
document.querySelectorAll('.dpad button').forEach(btn => {
  btn.addEventListener('click', (e) => {
    e.preventDefault();
    const dir = btn.dataset.dir;
    if (dir === 'up')    moveChicken(0, 1);
    if (dir === 'down')  moveChicken(0, -1);
    if (dir === 'left')  moveChicken(-1, 0);
    if (dir === 'right') moveChicken(1, 0);
  });
});

// Teclado
document.addEventListener('keydown', (e) => {
  if (e.repeat) return;
  const k = e.key.toLowerCase();

  if (G.phase === 'playing') {
    if (k === 'arrowup'    || k === 'w') { e.preventDefault(); moveChicken(0, 1); }
    if (k === 'arrowdown'  || k === 's') { e.preventDefault(); moveChicken(0, -1); }
    if (k === 'arrowleft'  || k === 'a') { e.preventDefault(); moveChicken(-1, 0); }
    if (k === 'arrowright' || k === 'd') { e.preventDefault(); moveChicken(1, 0); }
    if (k === ' ' || k === 'enter') { e.preventDefault(); cashOut(false); }
  } else if (G.phase === 'betting') {
    if (k === 'enter' || k === ' ') { e.preventDefault(); startGame(); }
  } else if (G.phase === 'crashed' || G.phase === 'cashed') {
    if (k === 'enter' || k === ' ') { e.preventDefault(); el.btnNext.click(); }
  }
});

/* =========================================================
   INICIO Y SINCRONIZACIÓN CON ANDROID
   ========================================================= */
updateUI();
requestAnimationFrame(loop);

// ESTA ES LA FUNCIÓN CLAVE PARA ANDROID
window.actualizarSaldoDesdeAndroid = function(nuevoSaldo) {
  console.log("Android inyectó el saldo: " + nuevoSaldo);
  G.balance = nuevoSaldo;
  updateUI(); // Vuelve a dibujar para desbloquear las fichas y el botón
};