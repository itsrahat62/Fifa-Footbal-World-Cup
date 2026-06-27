/**
 * FIFA WC 2026 — Live-TV stream proxy (Cloudflare Worker)
 * ------------------------------------------------------------------
 * Makes the HTTPS site able to play streams that a browser can't reach directly:
 *   • plain-HTTP m3u8 (mixed-content blocked in the browser)
 *   • streams that require a specific Referer / User-Agent header
 * It fetches the stream server-side (with the right headers), rewrites every
 * m3u8 segment/sub-playlist URL to keep flowing through this Worker, and adds CORS.
 *
 * DEPLOY (free, ~3 min):
 *   1. https://dash.cloudflare.com → Workers & Pages → Create → Worker
 *   2. Name it (e.g. "tv-proxy"), Deploy, then "Edit code"
 *   3. Delete the sample, paste THIS whole file, Deploy
 *   4. Copy the Worker URL (e.g. https://tv-proxy.YOURNAME.workers.dev)
 *   5. Put it in index.html:  const TV_PROXY = "https://tv-proxy.YOURNAME.workers.dev";
 *
 * Usage:  https://tv-proxy.xxx.workers.dev/?url=<enc stream>&ref=<enc referer>&ua=<enc user-agent>
 */
export default {
  async fetch(request) {
    const reqUrl = new URL(request.url);
    const target = reqUrl.searchParams.get('url');
    const ref = reqUrl.searchParams.get('ref') || '';
    const ua  = reqUrl.searchParams.get('ua')  || 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)';

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
    const refDec = ref ? (() => { try { return decodeURIComponent(ref); } catch (e) { return ref; } })() : '';

    const upHeaders = { 'User-Agent': ua };
    if (refDec) { upHeaders['Referer'] = refDec; upHeaders['Origin'] = (() => { try { return new URL(refDec).origin; } catch (e) { return refDec; } })(); }

    let upstream;
    try {
      upstream = await fetch(decoded, { headers: upHeaders, cf: { cacheTtl: 0 } });
    } catch (e) {
      return new Response('upstream error: ' + e.message, { status: 502, headers: cors });
    }

    const ct = (upstream.headers.get('content-type') || '').toLowerCase();
    const isM3U8 = decoded.toLowerCase().includes('.m3u8') || ct.includes('mpegurl');

    const self = reqUrl.origin + reqUrl.pathname;
    const extra = (ref ? '&ref=' + encodeURIComponent(refDec) : '') + (ua ? '&ua=' + encodeURIComponent(ua) : '');
    const proxify = (abs) => `${self}?url=${encodeURIComponent(abs)}${extra}`;
    const toAbs = (u, base) => { if (/^https?:\/\//i.test(u)) return u; try { return new URL(u, base).href; } catch (e) { return u; } };

    if (isM3U8) {
      let text = await upstream.text();
      const base = decoded;
      text = text.split('\n').map(line => {
        const t = line.trim();
        if (!t) return line;
        if (t.startsWith('#')) return t.replace(/URI="([^"]+)"/g, (m, u) => `URI="${proxify(toAbs(u, base))}"`);
        return proxify(toAbs(t, base));
      }).join('\n');
      return new Response(text, { headers: { ...cors, 'Content-Type': 'application/vnd.apple.mpegurl', 'Cache-Control': 'no-store' } });
    }

    const headers = new Headers();
    const ctype = upstream.headers.get('content-type');
    if (ctype) headers.set('Content-Type', ctype);
    Object.entries(cors).forEach(([k, v]) => headers.set(k, v));
    headers.set('Cache-Control', 'no-store');
    return new Response(upstream.body, { status: upstream.status, headers });
  }
};
