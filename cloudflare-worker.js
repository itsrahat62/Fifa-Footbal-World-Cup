/**
 * FIFA WC 2026 — HLS HTTP→HTTPS Proxy (Cloudflare Worker)
 * ------------------------------------------------------------------
 * Lets the HTTPS site play plain-HTTP m3u8 streams (e.g. 198.195.239.50:8095)
 * by fetching them server-side, rewriting every segment / sub-playlist URL to
 * route back through this Worker, and adding open CORS headers.
 *
 * DEPLOY (free, ~3 min):
 *   1. Go to https://dash.cloudflare.com  → Workers & Pages → Create → Worker
 *   2. Name it (e.g. "wc-proxy"), click Deploy, then "Edit code"
 *   3. Delete the sample, paste THIS whole file, click Deploy
 *   4. Copy your Worker URL (e.g. https://wc-proxy.YOURNAME.workers.dev)
 *   5. Send me that URL — I'll wire the channels in.
 *
 * Usage:  https://wc-proxy.xxx.workers.dev/?url=<encoded http m3u8 url>
 */
export default {
  async fetch(request) {
    const reqUrl = new URL(request.url);
    const target = reqUrl.searchParams.get('url');

    const cors = {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET,HEAD,OPTIONS',
      'Access-Control-Allow-Headers': '*',
      'Access-Control-Expose-Headers': '*'
    };
    if (request.method === 'OPTIONS') return new Response(null, { headers: cors });
    if (!target) return new Response('missing ?url=', { status: 400, headers: cors });

    let decoded;
    try { decoded = decodeURIComponent(target); } catch (e) { decoded = target; }

    let upstream;
    try {
      upstream = await fetch(decoded, {
        headers: {
          'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
          'Referer': new URL(decoded).origin + '/',
          'Origin': new URL(decoded).origin
        },
        cf: { cacheTtl: 0 }
      });
    } catch (e) {
      return new Response('upstream error: ' + e.message, { status: 502, headers: cors });
    }

    const ct = (upstream.headers.get('content-type') || '').toLowerCase();
    const isM3U8 = decoded.toLowerCase().includes('.m3u8') || ct.includes('mpegurl');

    const self = reqUrl.origin + reqUrl.pathname;           // this worker's base
    const proxify = (abs) => `${self}?url=${encodeURIComponent(abs)}`;
    const toAbs = (u, base) => {
      if (/^https?:\/\//i.test(u)) return u;
      try { return new URL(u, base).href; } catch (e) { return u; }
    };

    if (isM3U8) {
      let text = await upstream.text();
      const base = decoded;
      text = text.split('\n').map(line => {
        const t = line.trim();
        if (!t) return line;
        if (t.startsWith('#')) {
          // rewrite URI="..." inside tags (#EXT-X-KEY, #EXT-X-MEDIA, #EXT-X-MAP)
          return t.replace(/URI="([^"]+)"/g, (m, u) => `URI="${proxify(toAbs(u, base))}"`);
        }
        // a media segment or a sub/variant playlist line
        return proxify(toAbs(t, base));
      }).join('\n');
      return new Response(text, {
        headers: { ...cors, 'Content-Type': 'application/vnd.apple.mpegurl', 'Cache-Control': 'no-store' }
      });
    }

    // binary passthrough for .ts segments / keys / anything else
    const headers = new Headers();
    const ctype = upstream.headers.get('content-type');
    if (ctype) headers.set('Content-Type', ctype);
    Object.entries(cors).forEach(([k, v]) => headers.set(k, v));
    headers.set('Cache-Control', 'no-store');
    return new Response(upstream.body, { status: upstream.status, headers });
  }
};
