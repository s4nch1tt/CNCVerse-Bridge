/**
 * CNCVerse Bridge Permanent Gateway - Cloudflare Worker
 * 
 * Proxies Stremio requests (manifest.json, streams, catalogs, web dashboard)
 * to the currently active GitHub Actions runner tunnel.
 */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // Endpoint for GitHub Actions runner to register its latest trycloudflare URL
    if (url.pathname === "/__update_backend" && request.method === "POST") {
      const auth = request.headers.get("Authorization") || "";
      const expectedSecret = env.CF_WORKER_SECRET || "cncverse_secret_key";
      
      if (!auth.includes(expectedSecret)) {
        return new Response(JSON.stringify({ error: "Unauthorized" }), { 
          status: 401, 
          headers: { "content-type": "application/json" } 
        });
      }

      const data = await request.json();
      if (!data.backend_url) {
        return new Response(JSON.stringify({ error: "Missing backend_url" }), { 
          status: 400, 
          headers: { "content-type": "application/json" } 
        });
      }

      await env.STREMIO_KV.put("BACKEND", data.backend_url);
      return new Response(JSON.stringify({ status: "ok", backend: data.backend_url }), {
        headers: { "content-type": "application/json" }
      });
    }

    // Retrieve active backend tunnel URL from Cloudflare KV
    const backend = await env.STREMIO_KV.get("BACKEND");
    if (!backend) {
      return new Response(
        "<!DOCTYPE html><html><body style='background:#0f0f1a;color:#fff;font-family:sans-serif;text-align:center;padding:4rem;'>" +
        "<h2>🚀 CNCVerse Bridge Server is Starting...</h2>" +
        "<p>The GitHub Actions runner is booting up. Please refresh in 30 seconds.</p>" +
        "</body></html>",
        { status: 503, headers: { "content-type": "text/html" } }
      );
    }

    // Proxy request to the active backend
    const target = new URL(url.pathname + url.search, backend);
    const headers = new Headers(request.headers);
    headers.set("X-Forwarded-Host", url.host);
    headers.set("X-Forwarded-Proto", "https");

    return fetch(target.toString(), {
      method: request.method,
      headers: headers,
      body: request.body,
      redirect: "follow"
    });
  }
};
