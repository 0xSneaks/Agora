// Local mock of an OpenAI-compatible provider for E2E tests. Streams "Echo: <last user text>"
// word by word. Text containing "slow" streams for ~10s so Stop can be tested.
import http from 'node:http';

const port = Number(process.env.MOCK_PORT ?? 4174);
const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, content-type, accept',
  'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
};

http.createServer((req, res) => {
  if (req.method === 'OPTIONS') { res.writeHead(204, cors); return res.end(); }
  if (req.url === '/v1/models') {
    res.writeHead(200, { ...cors, 'Content-Type': 'application/json' });
    return res.end(JSON.stringify({ data: [{ id: 'mock-1' }, { id: 'mock-2' }] }));
  }
  if (req.url === '/v1/chat/completions' && req.method === 'POST') {
    let body = '';
    req.on('data', (c) => (body += c));
    req.on('end', () => {
      const j = JSON.parse(body);
      const last = [...j.messages].reverse().find((m) => m.role === 'user')?.content ?? '';
      const slow = /slow/.test(last);
      const words = slow ? Array.from({ length: 70 }, (_, i) => `w${i} `) : `Echo: ${last}`.split(/(?<= )/);
      res.writeHead(200, { ...cors, 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' });
      let i = 0;
      const tick = () => {
        if (res.destroyed) return;
        if (i < words.length) {
          res.write(`data: ${JSON.stringify({ choices: [{ delta: { content: words[i++] } }] })}\n\n`);
          setTimeout(tick, slow ? 150 : 60);
        } else {
          res.write(`data: ${JSON.stringify({ choices: [{ delta: {}, finish_reason: 'stop' }] })}\n\n`);
          res.end('data: [DONE]\n\n');
        }
      };
      tick();
    });
    return;
  }
  res.writeHead(404, cors);
  res.end();
}).listen(port, '127.0.0.1');
