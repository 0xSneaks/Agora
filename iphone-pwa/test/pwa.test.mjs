import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile,access} from 'node:fs/promises';
import vm from 'node:vm';
const root=new URL('../',import.meta.url);
const read=path=>readFile(new URL(path,root),'utf8');
test('installable standalone manifest includes valid PNG sizes and Apple icon',async()=>{
  const manifest=JSON.parse(await read('public/manifest.webmanifest'));
  assert.equal(manifest.name,'Sneaks AI');assert.equal(manifest.display,'standalone');assert.equal(manifest.start_url,'/');assert.equal(manifest.scope,'/');
  for(const icon of manifest.icons){const bytes=await readFile(new URL('public'+icon.src,root));const size=Number(icon.sizes.split('x')[0]);assert.equal(bytes.readUInt32BE(16),size);assert.equal(bytes.readUInt32BE(20),size);}
  const html=await read('public/index.html');assert.match(html,/viewport-fit=cover/);assert.match(html,/apple-touch-icon/);assert.match(html,/manifest.webmanifest/);assert.ok(!/https?:\/\//.test(html));
  const apple=await readFile(new URL('public/icons/apple-touch-icon.png',root));assert.equal(apple.readUInt32BE(16),180);
});
test('CSP prevents remote scripts, embeds and forms; service worker cannot be long cached',async()=>{
  const config=JSON.parse(await read('vercel.json'));const headers=config.headers[0].headers;const csp=headers.find(h=>h.key==='Content-Security-Policy').value;
  for(const rule of ["script-src 'self'","object-src 'none'","frame-ancestors 'none'","form-action 'none'"])assert.ok(csp.includes(rule));
  assert.ok(!csp.includes("script-src 'self' 'unsafe-inline'"));assert.equal(config.headers.find(h=>h.source==='/sw.js').headers.find(h=>h.key==='Cache-Control').value,'no-cache');
});
async function workerHarness(){
  const handlers={},entries=new Map(),deleted=[],network=[];
  const cache={async addAll(paths){for(const path of paths){await access(new URL('dist/'+(path==='/'?'index.html':path.slice(1)),root));entries.set(path,{cached:path});}},async match(path){return entries.get(path);}};
  const context={URL,Promise,caches:{async open(){return cache;},async keys(){return ['unrelated-cache','sneaks-ai-old','sneaks-ai-__REVISION__'];},async delete(name){deleted.push(name);}},fetch:async request=>{network.push(request);throw new Error('offline');},self:{location:{origin:'https://app.example'},clients:{claim:async()=>{}},addEventListener:(event,fn)=>{handlers[event]=fn;}}};
  vm.runInNewContext(await read('public/sw.js'),context);
  async function lifecycle(event){let promise;handlers[event]({waitUntil(p){promise=p;}});await promise;}
  function request({url='https://app.example/',method='GET',mode='cors'}={}){let response;handlers.fetch({request:{url,method,mode},respondWith(p){response=p;}});return response;}
  return {entries,deleted,network,lifecycle,request};
}
test('offline shell precaches every declared asset and serves navigation without network',async()=>{
  const w=await workerHarness();await w.lifecycle('install');assert.ok(w.entries.size>=13);
  const page=await w.request({url:'https://app.example/',mode:'navigate'});assert.equal(page.cached,'/index.html');
  assert.equal((await w.request({url:'https://app.example/src/app.js'})).cached,'/src/app.js');assert.equal(w.network.length,0);
});
test('service worker never handles provider traffic, POST, query URLs or unknown assets',async()=>{
  const w=await workerHarness();
  for(const req of [{url:'https://api.example/v1/chat/completions',method:'POST'},{url:'https://api.example/v1/models'},{url:'https://app.example/anything',method:'POST'},{url:'https://app.example/index.html?key=secret'},{url:'https://app.example/unlisted.json'}])assert.equal(w.request(req),undefined);
  assert.equal(w.network.length,0);
});
test('activation only removes previous Sneaks AI cache, preserves unrelated caches',async()=>{
  const w=await workerHarness();await w.lifecycle('activate');assert.deepEqual(w.deleted,['sneaks-ai-old']);
});
test('build ships GPL and public source reference with static client modules',async()=>{
  assert.match(await read('dist/LICENSE.txt'),/GNU GENERAL PUBLIC LICENSE/);
  assert.match(await read('dist/SOURCE.txt'),/not affiliated/);
  assert.ok(!(await read('dist/sw.js')).includes('__REVISION__'));
  for(const path of ['dist/src/app.js','dist/src/core.js','dist/src/providers.js'])await access(new URL(path,root));
});
test('client never logs, calls analytics, or renders provider content as HTML',async()=>{
  const app=await read('src/app.js'),providers=await read('src/providers.js');
  assert.ok(!/console\.|sendBeacon|sentry|google-analytics|vercel\/analytics/.test(app+providers));
  assert.match(app,/content\.textContent=reply\.content/);assert.match(app,/esc\(m\.content\)/);
  assert.ok(!/autofocus/.test(app));assert.match(app,/stale=true/);
});
