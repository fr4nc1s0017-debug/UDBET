/* =========================================================
   BLACKJACK — lógica del juego (Adaptado para Android)
   ========================================================= */

const NUM_DECKS = 6;
const RESHUFFLE_THRESHOLD = 0.25;
const DEALER_STANDS_ON = 17;

const SUITS = [
  { sym: '♠', red: false },
  { sym: '♥', red: true  },
  { sym: '♦', red: true  },
  { sym: '♣', red: false }
];
const RANKS = ['A', '2', '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K'];

function shuffle(arr) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

function newShoe(decks = NUM_DECKS) {
  const cards = [];
  for (let d = 0; d < decks; d++) {
    for (const suit of SUITS) {
      for (const rank of RANKS) {
        cards.push({ rank, suit: suit.sym, red: suit.red });
      }
    }
  }
  return shuffle(cards);
}

function cardValue(card) {
  if (card.rank === 'A') return 11;
  if (card.rank === 'J' || card.rank === 'Q' || card.rank === 'K') return 10;
  return Number(card.rank);
}

function handValue(cards) {
  let total = 0;
  let aces = 0;
  for (const c of cards) {
    total += cardValue(c);
    if (c.rank === 'A') aces++;
  }
  while (total > 21 && aces > 0) {
    total -= 10;
    aces--;
  }
  return total;
}

function isSoft(cards) {
  let total = 0, aces = 0;
  for (const c of cards) {
    total += cardValue(c);
    if (c.rank === 'A') aces++;
  }
  while (total > 21 && aces > 0) { total -= 10; aces--; }
  return aces > 0 && total <= 21;
}

function money(n) {
  return '$' + n.toFixed(2);
}

/* ---------- ESTADO GLOBAL ---------- */
window.state = window.state || {
  shoe: newShoe(),
  balance: 0,
  currentBet: 0,
  lastBet: 0,
  playerHands: [],
  activeHand: 0,
  dealer: { cards: [] },
  hideDealer: true,
  phase: 'betting',
  message: 'Haz tu apuesta',
  messageType: ''
};

const state = window.state;

const el = {
  balance:     document.getElementById('balanceDisplay'),
  bet:         document.getElementById('betDisplay'),
  dealerCards: document.getElementById('dealerCards'),
  dealerScore: document.getElementById('dealerScore'),
  playerHands: document.getElementById('playerHands'),
  message:     document.getElementById('message'),
  chips:       document.getElementById('chips'),
  btnDeal:     document.getElementById('btnDeal'),
  btnClear:    document.getElementById('btnClear'),
  btnRepeat:   document.getElementById('btnRepeat'),
  btnHit:      document.getElementById('btnHit'),
  btnStand:    document.getElementById('btnStand'),
  btnDouble:   document.getElementById('btnDouble'),
  btnSplit:    document.getElementById('btnSplit'),
  btnNext:     document.getElementById('btnNext')
};

function currentHand() { return state.playerHands[state.activeHand] || null; }
function drawCard() {
  if (state.shoe.length === 0) state.shoe = newShoe();
  return state.shoe.pop();
}

/* =========================================================
   RENDERIZADO
   ========================================================= */

function makeCardEl(card, hidden) {
  const div = document.createElement('div');
  div.className = 'card';
  if (hidden) { div.classList.add('back'); return div; }
  if (card.red) div.classList.add('red');
  div.innerHTML = `
    <div class="corner tl"><span>${card.rank}</span><span class="s">${card.suit}</span></div>
    <div class="center">${card.suit}</div>
    <div class="corner br"><span>${card.rank}</span><span class="s">${card.suit}</span></div>
  `;
  return div;
}

function renderDealer() {
  el.dealerCards.innerHTML = '';
  state.dealer.cards.forEach((card, i) => {
    const hidden = state.hideDealer && i === 1;
    el.dealerCards.appendChild(makeCardEl(card, hidden));
  });

  if (state.dealer.cards.length === 0) { el.dealerScore.textContent = ''; }
  else if (state.hideDealer && state.dealer.cards.length > 1) {
    el.dealerScore.textContent = handValue([state.dealer.cards[0]]) + ' + ?';
  } else { el.dealerScore.textContent = handValue(state.dealer.cards); }
}

function renderPlayerHands() {
  el.playerHands.innerHTML = '';
  state.playerHands.forEach((hand, i) => {
    const wrap = document.createElement('div');
    wrap.className = 'hand';
    if (state.phase === 'playing' && i === state.activeHand && state.playerHands.length > 1) {
      wrap.classList.add('active');
    }
    const cardsDiv = document.createElement('div');
    cardsDiv.className = 'cards';
    hand.cards.forEach(c => cardsDiv.appendChild(makeCardEl(c, false)));
    wrap.appendChild(cardsDiv);

    const value = handValue(hand.cards);
    const info = document.createElement('div');
    info.className = 'hand-info';
    let txt = String(value);
    if (value > 21) txt += ' · PASADO';
    else if (hand.doubled) txt += ' · DOBLADA';
    else if (isSoft(hand.cards) && value < 21) txt += ' (suave)';
    if (hand.bet) txt += ` · ${money(hand.bet)}`;
    info.textContent = txt;
    wrap.appendChild(info);

    if (hand.result) {
      const badge = document.createElement('div');
      badge.className = 'badge ' + hand.result;
      badge.textContent = { win: 'GANA', lose: 'PIERDE', push: 'EMPATE', blackjack: 'BLACKJACK' }[hand.result];
      wrap.appendChild(badge);
    }
    el.playerHands.appendChild(wrap);
  });
}

function renderControls() {
  const betting = state.phase === 'betting';
  const playing = state.phase === 'playing';
  const over    = state.phase === 'roundOver';

  el.chips.classList.toggle('hidden', !betting);
  el.btnDeal.classList.toggle('hidden', !betting);
  el.btnClear.classList.toggle('hidden', !betting);
  el.btnRepeat.classList.toggle('hidden', !betting);

  el.btnHit.classList.toggle('hidden', !playing);
  el.btnStand.classList.toggle('hidden', !playing);
  el.btnDouble.classList.toggle('hidden', !playing);
  el.btnSplit.classList.toggle('hidden', !playing);

  el.btnNext.classList.toggle('hidden', !over);

  el.btnDeal.disabled = state.currentBet <= 0 || state.currentBet > state.balance;
  el.btnClear.disabled = state.currentBet <= 0;
  el.btnRepeat.disabled = state.lastBet <= 0 || state.lastBet > state.balance;

  [...el.chips.children].forEach(btn => {
    const value = Number(btn.dataset.value);
    btn.disabled = state.balance < state.currentBet + value;
  });

  if (playing) {
    const hand = currentHand();
    const canAct = hand && !hand.done;
    el.btnHit.disabled = !canAct;
    el.btnStand.disabled = !canAct;
    el.btnDouble.disabled = !canAct || hand.cards.length !== 2 || hand.splitAces || state.balance < hand.bet;
    el.btnSplit.disabled = !canSplit(hand);
  }
}

function render() {
  el.balance.textContent = money(state.balance);
  el.bet.textContent = money(state.currentBet);

  renderDealer();
  renderPlayerHands();

  el.message.textContent = state.message;
  el.message.className = state.messageType || '';

  renderControls();
}

/* =========================================================
   APUESTAS
   ========================================================= */

function addChip(value) {
  if (state.phase !== 'betting') return;
  if (state.balance < state.currentBet + value) return;
  state.currentBet += value;
  render();
}

function clearBet() {
  if (state.phase !== 'betting') return;
  state.currentBet = 0;
  render();
}

function repeatBet() {
  if (state.phase !== 'betting') return;
  if (state.lastBet <= 0 || state.lastBet > state.balance) return;
  state.currentBet = state.lastBet;
  render();
}

/* =========================================================
   REPARTO
   ========================================================= */

function deal() {
  if (state.phase !== 'betting') return;
  const bet = state.currentBet;
  if (bet <= 0 || bet > state.balance) return;

  if (state.shoe.length < NUM_DECKS * 52 * RESHUFFLE_THRESHOLD) {
    state.shoe = newShoe();
    state.message = 'Barajando de nuevo...';
  }

  state.balance -= bet;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.placeBet(bet);
  state.lastBet = bet;
  state.currentBet = 0;

  state.playerHands = [{
    cards: [], bet, done: false, doubled: false, isSplit: false, splitAces: false, blackjack: false, result: null
  }];
  state.dealer = { cards: [] };
  state.activeHand = 0;
  state.hideDealer = true;
  state.phase = 'playing';
  state.message = 'Tu turno';
  state.messageType = '';

  const hand = state.playerHands[0];
  hand.cards.push(drawCard());
  state.dealer.cards.push(drawCard());
  hand.cards.push(drawCard());
  state.dealer.cards.push(drawCard());

  hand.blackjack = handValue(hand.cards) === 21;
  const dealerBJ = handValue(state.dealer.cards) === 21;

  if (hand.blackjack || dealerBJ) {
    state.hideDealer = false;
    state.phase = 'roundOver';
    state.message = hand.blackjack
      ? (dealerBJ ? '¡Blackjack en ambos lados!' : '¡BLACKJACK!')
      : 'El crupier tiene Blackjack';
    render();
    setTimeout(settleRound, 800);
    return;
  }
  render();
}

/* =========================================================
   ACCIONES DEL JUGADOR
   ========================================================= */

function hit() {
  const hand = currentHand();
  if (state.phase !== 'playing' || !hand || hand.done) return;
  hand.cards.push(drawCard());
  const value = handValue(hand.cards);
  if (value >= 21) {
    hand.done = true;
    state.message = value > 21 ? 'Te pasaste' : 'Veintiuno';
  }
  afterAction();
}

function stand() {
  const hand = currentHand();
  if (state.phase !== 'playing' || !hand || hand.done) return;
  hand.done = true;
  afterAction();
}

function doubleDown() {
  const hand = currentHand();
  if (state.phase !== 'playing' || !hand || hand.done) return;
  if (hand.cards.length !== 2 || hand.splitAces) return;
  if (state.balance < hand.bet) return;

  state.balance -= hand.bet;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.placeBet(hand.bet);
  hand.bet *= 2;
  hand.doubled = true;
  hand.cards.push(drawCard());
  hand.done = true;

  state.message = handValue(hand.cards) > 21 ? 'Te pasaste al doblar' : 'Doblaste';
  afterAction();
}

function canSplit(hand) {
  if (!hand || hand.done) return false;
  if (hand.cards.length !== 2) return false;
  if (state.playerHands.length >= 4) return false;
  if (state.balance < hand.bet) return false;
  return cardValue(hand.cards[0]) === cardValue(hand.cards[1]);
}

function split() {
  const hand = currentHand();
  if (state.phase !== 'playing' || !canSplit(hand)) return;

  const idx = state.activeHand;
  const isAces = hand.cards[0].rank === 'A';

  state.balance -= hand.bet;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.placeBet(hand.bet);

  const movedCard = hand.cards.pop();
  const newHand = {
    cards: [movedCard], bet: hand.bet, done: false, doubled: false, isSplit: true, splitAces: isAces, blackjack: false, result: null
  };

  hand.isSplit = true;
  hand.splitAces = isAces;
  hand.blackjack = false;

  state.playerHands.splice(idx + 1, 0, newHand);

  hand.cards.push(drawCard());
  newHand.cards.push(drawCard());

  if (isAces) { hand.done = true; newHand.done = true; }

  state.message = 'Mano dividida';
  afterAction();
}

/* =========================================================
   FLUJO DE TURNOS
   ========================================================= */

function afterAction() {
  const next = state.playerHands.findIndex(h => !h.done);
  if (next === -1) { dealerTurn(); }
  else { state.activeHand = next; render(); }
}

function dealerTurn() {
  state.phase = 'dealer';
  state.hideDealer = false;
  state.message = 'Turno del crupier';
  render();

  const anyAlive = state.playerHands.some(h => handValue(h.cards) <= 21);
  if (!anyAlive) { setTimeout(settleRound, 600); return; }

  const step = () => {
    const value = handValue(state.dealer.cards);
    if (value < DEALER_STANDS_ON) {
      state.dealer.cards.push(drawCard());
      render();
      setTimeout(step, 650);
    } else { setTimeout(settleRound, 450); }
  };
  setTimeout(step, 650);
}

/* =========================================================
   RESOLUCIÓN DE LA RONDA
   ========================================================= */

function settleRound() {
  state.hideDealer = false;
  state.phase = 'roundOver';

  const dealerCards = state.dealer.cards;
  const dealerValue = handValue(dealerCards);
  const dealerBJ = dealerCards.length === 2 && dealerValue === 21;
  const dealerBust = dealerValue > 21;

  let totalReturned = 0;
  let netProfit = 0;

  for (const hand of state.playerHands) {
    const playerValue = handValue(hand.cards);
    const bet = hand.bet;
    let result, payout = 0;

    if (playerValue > 21) { result = 'lose'; }
    else if (hand.blackjack && !hand.isSplit) {
      if (dealerBJ) { result = 'push'; payout = bet; }
      else { result = 'blackjack'; payout = bet * 2.5; }
    } else if (dealerBJ) { result = 'lose'; }
    else if (dealerBust || playerValue > dealerValue) { result = 'win'; payout = bet * 2; }
    else if (playerValue === dealerValue) { result = 'push'; payout = bet; }
    else { result = 'lose'; }

    hand.result = result;
    hand.payout = payout;
    totalReturned += payout;
    netProfit += payout - bet;
  }

  state.balance += totalReturned;
  if (typeof AndroidBridge !== 'undefined') AndroidBridge.reportResult(netProfit > 0, totalReturned);

  const amount = Math.abs(netProfit);

  if (state.playerHands.length === 1) {
    const r = state.playerHands[0].result;
    if (r === 'blackjack') { state.message = `¡BLACKJACK! Ganaste ${money(amount)}`; state.messageType = 'win'; }
    else if (r === 'win') { state.message = dealerBust ? `El crupier se pasó (${dealerValue}). Ganaste ${money(amount)}` : `Ganaste ${money(amount)}`; state.messageType = 'win'; }
    else if (r === 'push') { state.message = `Empate (${playerValue}). Recuperas tu apuesta`; state.messageType = 'push'; }
    else {
      const v = handValue(state.playerHands[0].cards);
      state.message = v > 21 ? `Te pasaste con ${v}. Perdiste ${money(amount)}` : `Pierdes ${money(amount)}. El crupier tiene ${dealerValue}`;
      state.messageType = 'lose';
    }
  } else {
    if (netProfit > 0) { state.message = `Ganaste ${money(amount)} en total`; state.messageType = 'win'; }
    else if (netProfit < 0) { state.message = `Perdiste ${money(amount)} en total`; state.messageType = 'lose'; }
    else { state.message = 'Empate total'; state.messageType = 'push'; }
  }
  render();
}

function newRound() {
  if (state.balance <= 0) {
    state.message = 'Fondos insuficientes. Agrega saldo para seguir jugando.';
    state.messageType = 'lose';
  } else {
    state.message = 'Haz tu apuesta';
    state.messageType = '';
  }

  state.playerHands = [];
  state.dealer = { cards: [] };
  state.currentBet = 0;
  state.activeHand = 0;
  state.hideDealer = true;
  state.phase = 'betting';
  render();
}

/* =========================================================
   SINCRONIZACIÓN CON ANDROID (FUNCIÓN GLOBAL ÚNICA)
   ========================================================= */
window.actualizarSaldoDesdeAndroid = function(nuevoSaldo) {
  console.log("Android inyectó el saldo: " + nuevoSaldo);
  state.balance = nuevoSaldo;
  render(); // Fuerza la actualización visual y desbloquea botones
};

/* =========================================================
   EVENTOS
   ========================================================= */

el.chips.addEventListener('click', (e) => {
  const btn = e.target.closest('.chip');
  if (btn) addChip(Number(btn.dataset.value));
});

el.btnDeal.addEventListener('click', deal);
el.btnClear.addEventListener('click', clearBet);
el.btnRepeat.addEventListener('click', repeatBet);
el.btnHit.addEventListener('click', hit);
el.btnStand.addEventListener('click', stand);
el.btnDouble.addEventListener('click', doubleDown);
el.btnSplit.addEventListener('click', split);
el.btnNext.addEventListener('click', newRound);

document.addEventListener('keydown', (e) => {
  if (e.repeat) return;
  const k = e.key.toLowerCase();
  if (state.phase === 'playing') {
    if (k === 'h' || k === 'arrowright') hit();
    if (k === 's' || k === 'arrowdown') stand();
    if (k === 'd') doubleDown();
    if (k === 'p') split();
  } else if (state.phase === 'betting') {
    if (k === 'enter' || k === ' ') { e.preventDefault(); deal(); }
  } else if (state.phase === 'roundOver') {
    if (k === 'enter' || k === ' ') { e.preventDefault(); newRound(); }
  }
});

// Iniciamos el renderizado
render();