/**
 * Bangladesh Railway Pure API Ticket Bot
 * ========================================
 * CapSolver দিয়ে Turnstile solve → direct API calls
 * Browser লাগবে না — অনেক দ্রুত
 *
 * Setup:
 *   npm install axios
 *   CapSolver key: https://dashboard.capsolver.com
 *
 * Usage: node railway-api-bot.js
 */

const axios    = require('axios');
const readline = require('readline');
const crypto   = require('crypto');
const { exec } = require('child_process');

// ═══════════════════════════════════════════════════
//  ⚙️  CONFIG
// ═══════════════════════════════════════════════════
const CONFIG = {
  capsolverKey: 'CAP-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX',

  mobile:   '01767552562',
  password: 'r01718910687',

  fromCity:  'Nilphamari',
  toCity:    'Dhaka',
  date:      '20-May-2026',   // DD-Mon-YYYY
  trainName: 'CHILAHATI',     // অংশ হলেও হবে, '' = যেকোনো

  seatClass: 'S_CHAIR',       // S_CHAIR | SNIGDHA | AC_S | AC_B
  seatCount: 1,               // 1–4

  paymentMethod: 'bkash',     // bkash | nagad | rocket | upay

  debug: false,
};
// ═══════════════════════════════════════════════════

const API_BASE = 'https://railspaapi.shohoz.com/v1.0/web';
const SITE     = 'https://eticket.railway.gov.bd';
const CAP_URL  = 'https://api.capsolver.com';

// Cloudflare Turnstile sitekeys (eticket.railway.gov.bd Angular bundle থেকে)
const TURNSTILE_LOGIN     = '0x4AAAAAAB5VTjZ90pUxRuXR';  // login page (visible)
const TURNSTILE_INVISIBLE = '0x4AAAAAACNkZ_TxQr_zpcZW';  // seat layout + reserve (invisible)

// ── session ────────────────────────────────────────
let authToken = '';
// device key = client-side fingerprint (Angular SDK generates, stores in localStorage as 'ssdk')
// server কে validate করে না strictly, random 64-byte hex কাজ করে
const DEVICE_KEY = crypto.randomBytes(64).toString('hex');
const DEVICE_ID  = crypto.randomBytes(16).toString('hex');

// ── helpers ────────────────────────────────────────
const sleep = ms => new Promise(r => setTimeout(r, ms));

function ask(q) {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise(r => rl.question(q, a => { rl.close(); r(a.trim()); }));
}

function dbg(label, obj) {
  if (CONFIG.debug) console.log(`\n[DBG] ${label}:`, JSON.stringify(obj, null, 2));
}

// ── headers factory ───────────────────────────────
function makeHeaders(extra = {}) {
  return {
    'accept':            'application/json, text/plain, */*',
    'accept-language':   'en-US,en;q=0.9',
    'content-type':      'application/json',
    'origin':            SITE,
    'referer':           SITE + '/',
    'x-requested-with':  'XMLHttpRequest',
    'user-agent':        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36',
    'x-device-key':      DEVICE_KEY,   // client-generated fingerprint (Angular SDK)
    'x-device-id':       DEVICE_ID,
    ...(authToken ? { 'authorization': `Bearer ${authToken}` } : {}),
    ...extra,
  };
}

// ── HTTP ──────────────────────────────────────────
async function api(method, path, { body = null, params = {} } = {}) {
  try {
    const { data } = await axios({
      method,
      url:     `${API_BASE}${path}`,
      headers: makeHeaders(),
      ...(body   ? { data: body }   : {}),
      ...(Object.keys(params).length ? { params } : {}),
    });
    dbg(`${method} ${path}`, data);
    return data;
  } catch (e) {
    const r = e.response?.data;
    throw new Error(r?.message || r?.error || JSON.stringify(r) || e.message);
  }
}

// ═══════════════════════════════════════════════════
//  CapSolver — Turnstile solve
// ═══════════════════════════════════════════════════
async function solveTurnstile(siteKey, pageUrl, action = '') {
  process.stdout.write(`  🔐 Solving Turnstile (${siteKey.slice(0, 16)}...)`);

  const { data: cr } = await axios.post(`${CAP_URL}/createTask`, {
    clientKey: CONFIG.capsolverKey,
    task: {
      type:       'AntiTurnstileTaskProxyLess',
      websiteURL: pageUrl,
      websiteKey: siteKey,
      ...(action ? { metadata: { action } } : {}),
    },
  });
  if (cr.errorId) throw new Error(`CapSolver create: ${cr.errorDescription}`);

  const { taskId } = cr;
  for (let i = 0; i < 60; i++) {
    await sleep(2000);
    process.stdout.write('.');
    const { data: res } = await axios.post(`${CAP_URL}/getTaskResult`, {
      clientKey: CONFIG.capsolverKey,
      taskId,
    });
    if (res.status === 'ready')  { process.stdout.write(' ✅\n'); return res.solution.token; }
    if (res.status === 'failed') throw new Error(`CapSolver failed: ${res.errorDescription}`);
  }
  throw new Error('CapSolver timeout (120s)');
}

// ═══════════════════════════════════════════════════
//  STEP 1 — LOGIN
// ═══════════════════════════════════════════════════
async function login() {
  console.log('\n📱 [1/6] Login...');
  const cfToken = await solveTurnstile(TURNSTILE_LOGIN, `${SITE}/login`);

  const resp = await api('POST', '/auth/sign-in', {
    body: {
      mobile_number: CONFIG.mobile,
      password:      CONFIG.password,
      token:         cfToken,       // Turnstile token
    },
  });

  authToken = resp?.data?.token;
  if (!authToken) throw new Error(`Login failed. Response: ${JSON.stringify(resp)}`);
  console.log('✅ Login সফল!\n');
}

// ═══════════════════════════════════════════════════
//  STEP 2 — TRAIN SEARCH
// ═══════════════════════════════════════════════════
async function searchTrain() {
  console.log(`🔍 [2/6] Train খুঁজছি (${CONFIG.fromCity} → ${CONFIG.toCity})...`);

  const resp = await api('GET', '/bookings/search-trips-v2', {
    params: {
      from_city:       CONFIG.fromCity,
      to_city:         CONFIG.toCity,
      date_of_journey: CONFIG.date,
      seat_class:      CONFIG.seatClass,
    },
  });

  const trains = resp?.data?.trains || [];
  if (!trains.length) throw new Error('কোনো train পাওয়া যায়নি');

  // Target train খুঁজি
  let train = CONFIG.trainName
    ? trains.find(t => t.trip_number.toUpperCase().includes(CONFIG.trainName.toUpperCase()))
    : trains[0];
  if (!train) {
    console.log(`  ⚠️  "${CONFIG.trainName}" পাওয়া যায়নি — প্রথম train নিচ্ছি`);
    train = trains[0];
  }

  // Target seat class
  const seatType = train.seat_types.find(s => s.type === CONFIG.seatClass);
  if (!seatType) throw new Error(`${CONFIG.seatClass} class নেই "${train.trip_number}" তে`);

  const avail = (seatType.seat_counts?.online ?? 0) + (seatType.seat_counts?.offline ?? 0);
  if (avail < CONFIG.seatCount) {
    throw new Error(`মাত্র ${avail} seat available — ${CONFIG.seatCount} চাই`);
  }

  console.log(`✅ Train: ${train.trip_number}`);
  console.log(`   Seats: ${avail} available | Fare: ৳${seatType.fare}\n`);

  return {
    tripNumber:   train.trip_number,
    tripId:       seatType.trip_id,
    tripRouteId:  seatType.trip_route_id,
    originName:   train.boarding_points?.[0]?.location_name || CONFIG.fromCity,
  };
}

// ═══════════════════════════════════════════════════
//  STEP 3 — SEAT LAYOUT (best coach)
// ═══════════════════════════════════════════════════
async function getSeatLayout(tripInfo) {
  console.log('🚃 [3/6] Seat layout লোড করছি...');

  // Invisible Turnstile — cft_response query param হিসেবে যায়
  const cfToken = await solveTurnstile(TURNSTILE_INVISIBLE, `${SITE}/booking/seat-plan`);

  const resp = await api('GET', '/bookings/seat-layout', {
    params: {
      trip_id:       tripInfo.tripId,
      trip_route_id: tripInfo.tripRouteId,
      cft_response:  cfToken,
    },
  });

  const layout = resp?.data?.seatLayout || [];
  if (!layout.length) throw new Error('Seat layout empty');

  // Best coach = সবচেয়ে বেশি available seat
  let bestCoach = null, bestAvail = [];
  for (const coach of layout) {
    const avail = (coach.layout || [])
      .flat()
      .filter(s => s.seat_availability === 1 && s.seat_number && !s.isHidden);
    if (avail.length > bestAvail.length) {
      bestCoach = coach;
      bestAvail = avail;
    }
  }

  if (!bestAvail.length) throw new Error('কোনো available seat নেই');

  const chosen = bestAvail.slice(0, CONFIG.seatCount);
  console.log(`✅ Coach: ${bestCoach.floor_name} (${bestAvail.length} available)`);
  console.log(`   Choosing: ${chosen.map(s => s.seat_number).join(', ')}\n`);

  return chosen; // [{seat_number, ticket_id}, ...]
}

// ═══════════════════════════════════════════════════
//  STEP 4 — RESERVE SEATS (per seat, 1 call each)
// ═══════════════════════════════════════════════════
async function reserveSeats(tripInfo, seats) {
  console.log(`🪑 [4/6] ${seats.length}টি seat reserve করছি...`);
  const ticketIds = [];

  for (const seat of seats) {
    // প্রতিটা seat এর জন্য আলাদা Turnstile token দরকার
    const actionToken = await solveTurnstile(
      TURNSTILE_INVISIBLE,
      `${SITE}/booking/seat-plan`,
      'reserve'
    );

    await api('PATCH', '/bookings/reserve-seat', {
      body: {
        ticket_id:    seat.ticket_id,
        route_id:     tripInfo.tripRouteId,   // = trip_route_id
        extras: {
          seat_number:      seat.seat_number,
          trip_number:      tripInfo.tripNumber,
          origin_name:      tripInfo.originName,
          destination_name: CONFIG.toCity,
        },
        action_token: actionToken,            // Invisible Turnstile token
      },
    });

    ticketIds.push(seat.ticket_id);
    console.log(`  ✅ ${seat.seat_number} reserved (ticket_id: ${seat.ticket_id})`);
  }

  console.log('');
  return ticketIds;
}

// ═══════════════════════════════════════════════════
//  STEP 5 — PASSENGER DETAILS (OTP trigger)
// ═══════════════════════════════════════════════════
async function sendOtp(tripInfo, ticketIds) {
  console.log('📲 [5/6] OTP পাঠাচ্ছি...');

  await api('POST', '/bookings/passenger-details', {
    body: {
      trip_id:       tripInfo.tripId,
      trip_route_id: tripInfo.tripRouteId,
      ticket_ids:    ticketIds,              // সব seat এক সাথে
    },
  });

  console.log(`✅ OTP sent → ${CONFIG.mobile}\n`);
}

// ═══════════════════════════════════════════════════
//  STEP 6 — VERIFY OTP + CONFIRM + PAYMENT
// ═══════════════════════════════════════════════════
async function verifyAndConfirm(tripInfo, ticketIds, otp) {
  console.log('🔢 OTP verify করছি...');

  const vResp = await api('POST', '/bookings/verify-otp', {
    body: {
      trip_id:       tripInfo.tripId,
      trip_route_id: tripInfo.tripRouteId,
      ticket_ids:    ticketIds,
      otp,
    },
  });

  if (!vResp?.data?.success) throw new Error(`OTP failed: ${JSON.stringify(vResp?.data)}`);
  console.log('✅ OTP verified!\n');

  // Confirm booking + payment method
  console.log(`💳 [6/6] ${CONFIG.paymentMethod.toUpperCase()} দিয়ে confirm করছি...`);

  const cResp = await api('PATCH', '/bookings/confirm', {
    body: {
      trip_id:        tripInfo.tripId,
      trip_route_id:  tripInfo.tripRouteId,
      ticket_ids:     ticketIds,
      payment_method: CONFIG.paymentMethod,
    },
  });

  dbg('confirm response', cResp);

  // Payment URL বের করি (বিভিন্ন field নামে থাকতে পারে)
  const d = cResp?.data || cResp;
  const payUrl = d?.payment_url || d?.redirect_url || d?.url || d?.pay_url;
  return payUrl;
}

// ═══════════════════════════════════════════════════
//  MAIN
// ═══════════════════════════════════════════════════
async function main() {
  console.log('');
  console.log('🚂  Bangladesh Railway API Bot  (Pure API + CapSolver)');
  console.log('═══════════════════════════════════════════════════════');
  console.log(`📍  ${CONFIG.fromCity} → ${CONFIG.toCity}  |  📅  ${CONFIG.date}`);
  console.log(`🚄  ${CONFIG.trainName || 'যেকোনো train'}  |  💺  ${CONFIG.seatClass}  |  🎟️  ${CONFIG.seatCount} seat`);
  console.log(`💳  Payment: ${CONFIG.paymentMethod.toUpperCase()}`);
  console.log('');

  if (!CONFIG.capsolverKey || CONFIG.capsolverKey.startsWith('CAP-XXX')) {
    console.error('❌  CONFIG.capsolverKey সেট করুন! (https://dashboard.capsolver.com)');
    process.exit(1);
  }
  if (CONFIG.seatCount < 1 || CONFIG.seatCount > 4) {
    console.error('❌  seatCount 1–4 হতে হবে');
    process.exit(1);
  }

  try {
    await login();

    const tripInfo  = await searchTrain();
    const seats     = await getSeatLayout(tripInfo);
    const ticketIds = await reserveSeats(tripInfo, seats);

    await sendOtp(tripInfo, ticketIds);

    // ── OTP prompt ──────────────────────────────
    console.log('══════════════════════════════════════');
    console.log(`📲  OTP sent → ${CONFIG.mobile}`);
    console.log('══════════════════════════════════════');
    const otp = await ask('🔢 4-digit OTP: ');
    console.log('');

    const payUrl = await verifyAndConfirm(tripInfo, ticketIds, otp);

    // ── Success ─────────────────────────────────
    console.log('');
    console.log('══════════════════════════════════════════════════════');
    console.log('🎉  SUCCESS! Ticket booking সম্পন্ন!');
    console.log(`🎟️   Seats: ${seats.map(s => s.seat_number).join(', ')}`);
    if (payUrl) {
      console.log(`🔗  Payment URL: ${payUrl}`);
      console.log(`💳  ${CONFIG.paymentMethod.toUpperCase()} app এ payment করুন`);
      exec(`start "" "${payUrl}"`, () => {});
    } else {
      console.log('⚠️  Payment URL নেই — debug: true করে full response দেখুন');
    }
    console.log('══════════════════════════════════════════════════════');

  } catch (err) {
    console.error(`\n❌  Error: ${err.message}`);
    if (CONFIG.debug) console.error(err.stack);
    process.exit(1);
  }
}

main();
