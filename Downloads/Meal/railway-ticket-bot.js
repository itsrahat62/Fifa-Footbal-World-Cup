/**
 * Bangladesh Railway Ticket Bot
 * ==============================
 * Automatically books train tickets on eticket.railway.gov.bd
 *
 * Usage: node railway-ticket-bot.js
 */

const { chromium } = require('playwright');
const readline = require('readline');

// ============================================================
//  ⚙️  CONFIG - এখানে আপনার তথ্য দিন
// ============================================================
const CONFIG = {
  // লগিন তথ্য
  mobile: '01767552562',
  password: 'r01718910687',

  // যাত্রার তথ্য
  fromCity:   'Nilphamari',    // যেখান থেকে (হুবহু website এর মতো)
  toCity:     'Dhaka',         // যেখানে যাবেন
  date:       '20-May-2026',   // DD-Mon-YYYY format, যেমন: 29-May-2026
  trainName:  'CHILAHATI',     // Train এর নামের অংশ (CHILAHATI, NILSAGAR ইত্যাদি) অথবা '' (খালি) = যেকোনো train
  seatClass:  'S_CHAIR',       // S_CHAIR | SNIGDHA | AC_S | AC_B | F_BERTH | F_SEAT | F_CHAIR | SHOVAN
  seatCount:  1,               // কতটি seat (সর্বোচ্চ 4)

  // Payment
  paymentMethod: 'bkash',      // bkash | nagad | rocket | upay | tap
};
// ============================================================

const SITE = 'https://eticket.railway.gov.bd';

function ask(question) {
  const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
  return new Promise(resolve => rl.question(question, ans => { rl.close(); resolve(ans.trim()); }));
}

const sleep = ms => new Promise(r => setTimeout(r, ms));

async function log(msg) { console.log(msg); }

async function main() {
  console.log('');
  console.log('🚂  Bangladesh Railway Ticket Bot');
  console.log('══════════════════════════════════');
  console.log(`📍  ${CONFIG.fromCity} → ${CONFIG.toCity}`);
  console.log(`📅  ${CONFIG.date}  |  💺 ${CONFIG.seatClass}  |  🎟️  ${CONFIG.seatCount} seat`);
  if (CONFIG.trainName) console.log(`🚄  Train: ${CONFIG.trainName}`);
  console.log('');

  const browser = await chromium.launch({
    headless: false,          // Turnstile এর জন্য false রাখতে হবে (background এ চলবে)
    args: ['--start-minimized'],
  });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 800 } });
  const page = await ctx.newPage();

  // ─────────────────────────────────────────────────
  // STEP 1: LOGIN
  // ─────────────────────────────────────────────────
  log('📱 [1/6] Login করছি...');
  await page.goto(`${SITE}/login`, { waitUntil: 'domcontentloaded' });

  await page.fill('input[placeholder="Enter your mobile number"]', CONFIG.mobile);
  await page.fill('input[placeholder="Enter your password"]', CONFIG.password);

  // Cloudflare Turnstile invisible challenge solve হওয়ার জন্য অপেক্ষা
  log('⏳      Turnstile solve হচ্ছে...');
  await page.waitForFunction(
    () => !document.querySelector('button:has-text("LOGIN")')?.disabled,
    { timeout: 30000 }
  );
  await page.click('button:has-text("LOGIN")');

  // /login থেকে redirect হওয়ার জন্য অপেক্ষা
  await page.waitForURL(url => !url.includes('/login'), { timeout: 20000 });
  log('✅ Login সফল!\n');

  // ─────────────────────────────────────────────────
  // STEP 2: TRAIN SEARCH
  // ─────────────────────────────────────────────────
  log(`🔍 [2/6] Train খুঁজছি...`);
  const searchUrl = `${SITE}/booking/train/search?fromcity=${CONFIG.fromCity}&tocity=${CONFIG.toCity}&doj=${CONFIG.date}&class=${CONFIG.seatClass}`;
  await page.goto(searchUrl, { waitUntil: 'networkidle', timeout: 30000 });
  log('✅ Train list লোড হয়েছে!\n');

  // ─────────────────────────────────────────────────
  // STEP 3: SELECT TRAIN & CLICK BOOK NOW
  // ─────────────────────────────────────────────────
  log('🎯 [3/6] Train ও Seat class select করছি...');

  const clicked = await page.evaluate(({ trainName, seatClass }) => {
    // সব train card heading খুঁজি
    const headings = Array.from(document.querySelectorAll('h2'));

    // Target train heading
    let targetHeading = null;
    if (trainName) {
      targetHeading = headings.find(h => h.textContent.toUpperCase().includes(trainName.toUpperCase()));
    }
    if (!targetHeading) targetHeading = headings[0];
    if (!targetHeading) return { ok: false, msg: 'কোনো train পাওয়া যায়নি' };

    const trainCard = targetHeading.closest('div');
    if (!trainCard) return { ok: false, msg: 'Train card পাওয়া যায়নি' };

    // Seat class name গুলো ঐ card এর ভেতর খুঁজি
    const classNames = Array.from(trainCard.querySelectorAll('[class*="seat-class-name"]'));
    const target = classNames.find(el => el.textContent.trim() === seatClass);
    if (!target) return { ok: false, msg: `${seatClass} class পাওয়া যায়নি` };

    const bookBtn = target.closest('[class*="single-seat"]')?.querySelector('button');
    if (!bookBtn || bookBtn.disabled) return { ok: false, msg: 'BOOK NOW button disabled' };

    bookBtn.scrollIntoView();
    bookBtn.click();
    return { ok: true, train: targetHeading.textContent.trim() };
  }, { trainName: CONFIG.trainName, seatClass: CONFIG.seatClass });

  if (!clicked.ok) {
    // Fallback: click the first visible BOOK NOW
    log(`⚠️  ${clicked.msg} — প্রথম available BOOK NOW ক্লিক করছি`);
    const firstBookBtn = page.locator('button:has-text("BOOK NOW")').first();
    await firstBookBtn.scrollIntoViewIfNeeded();
    await firstBookBtn.click();
  } else {
    log(`✅ Train: ${clicked.train}\n`);
  }

  // Coach selector load হওয়ার জন্য অপেক্ষা
  await page.waitForSelector('#select-bogie', { timeout: 15000 });

  // ─────────────────────────────────────────────────
  // STEP 4: BEST COACH SELECT
  // ─────────────────────────────────────────────────
  log('🚃 [4/6] Best coach select করছি...');

  const bestCoach = await page.evaluate(() => {
    const opts = Array.from(document.querySelectorAll('#select-bogie option'));
    let best = null, bestCount = 0;
    for (const o of opts) {
      const m = o.text.match(/(\d+)\s*Seat/);
      if (m) {
        const n = parseInt(m[1]);
        if (n > bestCount) { bestCount = n; best = o.value; }
      }
    }
    return { value: best, count: bestCount };
  });

  if (bestCoach.value) {
    await page.selectOption('#select-bogie', bestCoach.value);
    log(`✅ Coach selected (${bestCoach.count} available seats)\n`);
    await sleep(600);
  } else {
    log('⚠️  Coach পাওয়া যায়নি, default coach রাখছি\n');
  }

  // ─────────────────────────────────────────────────
  // STEP 5: SEAT SELECTION
  // ─────────────────────────────────────────────────
  log(`🪑 [5/6] ${CONFIG.seatCount}টি seat select করছি...`);

  const selected = await page.evaluate(count => {
    const seats = Array.from(document.querySelectorAll('button.btn-seat'))
      .filter(s =>
        s.className.includes('seat-available') &&
        !s.className.includes('seat-booked') &&
        !s.className.includes('request_pending') &&
        !s.className.includes('in-progress')
      );

    const picked = [];
    for (let i = 0; i < Math.min(count, seats.length, 4); i++) {
      seats[i].click();
      picked.push(seats[i].textContent.trim());
    }
    return picked;
  }, CONFIG.seatCount);

  if (selected.length === 0) {
    log('❌ কোনো available seat পাওয়া যায়নি!');
    await ask('Enter চাপুন বন্ধ করতে...');
    await browser.close();
    return;
  }

  log(`✅ Seat selected: ${selected.join(', ')}`);
  await sleep(1200); // Reserve API complete হতে দিই

  // Seat reserve confirm চেক
  const total = await page.evaluate(() => {
    const el = document.querySelector('[class*="total"]');
    return el ? el.textContent.trim() : '';
  });
  log(`💰 Total: ${total}\n`);

  // ─────────────────────────────────────────────────
  // STEP 6: CONTINUE PURCHASE → OTP → PAYMENT
  // ─────────────────────────────────────────────────
  log('▶️  [6/6] Continue Purchase...');

  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('button'))
      .find(b => b.textContent.includes('CONTINUE PURCHASE'));
    if (btn) { btn.scrollIntoView(); btn.click(); }
  });

  // OTP page load
  await page.waitForURL(`**/trip-info**`, { timeout: 20000 });
  await page.waitForSelector('input', { timeout: 10000 });

  log('');
  log('══════════════════════════════════');
  log(`📲 OTP পাঠানো হয়েছে → ${CONFIG.mobile}`);
  log('══════════════════════════════════');
  const otp = await ask('🔢 4-digit OTP দিন: ');

  // OTP ফিল করি
  const otpBoxes = await page.locator('input[maxlength="1"], input[type="tel"][maxlength="1"]').all();
  if (otpBoxes.length >= 4) {
    for (let i = 0; i < 4; i++) {
      await otpBoxes[i].click();
      await otpBoxes[i].fill(otp[i] || '');
    }
  } else {
    const inp = page.locator('input').first();
    await inp.fill(otp);
  }

  // Verify click
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('button'))
      .find(b => /verify/i.test(b.textContent));
    if (btn) btn.click();
  });

  log('✅ OTP verified!');
  await sleep(1500);

  // Payment method select
  log(`\n💳 Payment: ${CONFIG.paymentMethod.toUpperCase()} select করছি...`);

  // Mobile Banking tab
  await page.evaluate(() => {
    const tabs = Array.from(document.querySelectorAll('*'));
    const mobileTab = tabs.find(el =>
      el.textContent.trim() === 'Mobile Banking' &&
      el.children.length === 0 &&
      el.tagName !== 'BODY'
    );
    if (mobileTab) mobileTab.click();
  });
  await sleep(400);

  // Payment icon click
  await page.evaluate(method => {
    const icon = document.querySelector(
      `.payment-icon-holder.${method}, img[alt="${method}"], [class*="${method}"]`
    );
    if (icon) {
      const holder = icon.closest('[class*="payment-icon-holder"]') ||
                     icon.closest('[class*="payment-icon"]') ||
                     icon.parentElement;
      holder.click();
    }
  }, CONFIG.paymentMethod);
  await sleep(500);

  // PROCEED TO PAYMENT
  await page.evaluate(() => {
    const btn = Array.from(document.querySelectorAll('button'))
      .find(b => b.textContent.includes('PROCEED TO PAYMENT'));
    if (btn && !btn.disabled) { btn.scrollIntoView(); btn.click(); }
  });

  log('⏳ Payment page এ redirect হচ্ছে...');

  try {
    await page.waitForURL('**/train-pay.shohoz.com/**', { timeout: 20000 });
    log('');
    log('══════════════════════════════════════════');
    log('🎉  SUCCESS! Ticket বুকিং সম্পন্ন!');
    log(`🔗  Payment URL: ${page.url()}`);
    log(`💳  ${CONFIG.paymentMethod.toUpperCase()} app থেকে payment করুন`);
    log('══════════════════════════════════════════');
  } catch {
    log(`⚠️  Payment redirect হয়নি। Current URL: ${page.url()}`);
  }

  await ask('\n✅ Payment শেষ হলে Enter চাপুন (browser বন্ধ হবে)...');
  await browser.close();
}

main().catch(err => {
  console.error('❌ Error:', err.message);
  process.exit(1);
});
