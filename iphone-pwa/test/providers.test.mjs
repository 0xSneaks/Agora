import {test} from 'node:test';
import assert from 'node:assert/strict';
import {PRESETS,chatRequest,directFetch,consumeResponse,fetchModels} from '../src/providers.js';
const p={id:'p1',name:'Test',protocol:'openai',baseUrl:'https://api.example/v1'};
const key='KEY_SENTINEL_123';
const messages=[{role:'user',content:'Hello',privateField:key},{role:'assistant',content:'Hi'}];
const stream=(text,step=3)=>new Response(new ReadableStream({start(c){const bytes=new TextEncoder().encode(text);for(let i=0;i<bytes.length;i+=step)c.enqueue(bytes.slice(i,i+step));c.close();}}),{headers:{'content-type':'text/event-stream'}});
test('all eight reference presets are HTTPS with no hardcoded models',()=>{assert.equal(PRESETS.length,8);assert.ok(PRESETS.every(p=>p.baseUrl.startsWith('https://')&&!p.models));});
for(const protocol of ['openai','anthropic','google'])test(`${protocol}: key only in chosen provider auth header, never URL/body`,()=>{
  const r=chatRequest({...p,protocol},key,'model-1',messages,'instruction','https://app.example');
  assert.equal(new URL(r.url).origin,'https://api.example');assert.ok(!r.url.includes(key));assert.ok(!r.options.body.includes(key));
  assert.equal(r.options.headers[protocol==='openai'?'Authorization':protocol==='anthropic'?'x-api-key':'x-goog-api-key'],protocol==='openai'?`Bearer ${key}`:key);
  assert.equal(r.options.redirect,'error');assert.equal(r.options.credentials,'omit');assert.equal(r.options.referrerPolicy,'no-referrer');assert.equal(r.options.cache,'no-store');
  const body=JSON.parse(r.options.body);
  if(protocol==='google'){assert.equal(body.contents[1].role,'model');assert.equal(body.systemInstruction.parts[0].text,'instruction');}
  if(protocol==='anthropic'){assert.equal(body.max_tokens,4096);assert.equal(body.system,'instruction');assert.equal(r.options.headers['anthropic-dangerous-direct-browser-access'],'true');}
  if(protocol==='openai')assert.equal(body.messages[0].role,'system');
});
test('unsupported protocol and same-origin provider are rejected',()=>{
  assert.throws(()=>chatRequest({...p,protocol:'unknown'},key,'m',messages));
  assert.throws(()=>chatRequest({...p,baseUrl:'https://app.example/api'},key,'m',messages,'','https://app.example'));
});
test('OpenAI SSE handles split UTF-8, CRLF and final event without delimiter',async()=>{
  let result='';await consumeResponse(stream('data: {"choices":[{"delta":{"content":"Hi 🦊"}}]}\r\n\r\ndata: {"choices":[{"delta":{"content":" there"}}]}\n\ndata: [DONE]',1),'openai',t=>result+=t);assert.equal(result,'Hi 🦊 there');
});
test('Anthropic stream ignores thinking and extracts text deltas',async()=>{
  let result='';await consumeResponse(stream('event: content_block_delta\ndata: {"type":"content_block_delta","delta":{"type":"thinking_delta","thinking":"hidden"}}\n\ndata: {"type":"content_block_delta","delta":{"type":"text_delta","text":"Hello"}}\n\n'),'anthropic',t=>result+=t);assert.equal(result,'Hello');
});
test('Gemini stream extracts visible parts and does not show thought parts',async()=>{
  let result='';await consumeResponse(stream('data: {"candidates":[{"content":{"parts":[{"thought":true,"text":"secret thought"},{"text":"Answer"}]}}]}\n\n'),'google',t=>result+=t);assert.equal(result,'Answer');
});
test('stream errors never expose raw provider payload',async()=>{
  for(const payload of [`data: {"error":{"message":"${key}"}}\n\n`,`data: invalid ${key}\n\n`])await assert.rejects(consumeResponse(stream(payload),'openai',()=>{}),e=>!e.message.includes(key));
});
test('empty response and provider safety block show actionable errors',async()=>{
  await assert.rejects(consumeResponse(stream('data: [DONE]\n\n'),'openai',()=>{}),/No text/);
  await assert.rejects(consumeResponse(stream('data: {"promptFeedback":{"blockReason":"SAFETY"}}\n\n'),'google',()=>{}),/blocked/);
});
test('non-stream fallback parses all three protocol responses',async()=>{
  for(const [protocol,value]of [['openai',{choices:[{message:{content:'ok'}}]}],['anthropic',{content:[{type:'text',text:'ok'}]}],['google',{candidates:[{content:{parts:[{text:'ok'}]}}]}]]){let result='';await consumeResponse(Response.json(value),protocol,t=>result+=t);assert.equal(result,'ok');}
});
test('invalid JSON is sanitized instead of echoing secrets',async()=>{
  await assert.rejects(consumeResponse(new Response(`not-json ${key}`),'openai',()=>{}),e=>!e.message.includes(key)&&/invalid JSON/.test(e.message));
});
test('HTTP failures, CORS failures and redirects cannot echo key or provider body',async()=>{
  for(const status of [400,401,403,404,429,500])await assert.rejects(directFetch(p.baseUrl,{},async()=>new Response(key,{status})),e=>!e.message.includes(key)&&e.message.includes(String(status)));
  await assert.rejects(directFetch(p.baseUrl,{},async()=>{throw new Error(key);}),e=>!e.message.includes(key)&&/CORS/.test(e.message));
});
test('abort propagates so Stop can retain a partial response',async()=>{
  await assert.rejects(directFetch(p.baseUrl,{},async()=>{throw new DOMException('stop','AbortError');}),e=>e.name==='AbortError');
});
test('model sync only contacts entered endpoint; handles Gemini names',async()=>{
  for(const protocol of ['openai','anthropic','google']){
    let calls=0;
    const models=await fetchModels({...p,protocol},key,'https://app.example',async(url,opts)=>{calls++;assert.equal(url,p.baseUrl+'/models');assert.ok(!url.includes(key));assert.equal(opts.redirect,'error');return Response.json(protocol==='google'?{models:[{name:'models/m1',supportedGenerationMethods:['generateContent']},{name:'models/embedding',supportedGenerationMethods:['embedContent']}]}:{data:[{id:'m1'},{id:'m1'}]});});
    assert.equal(calls,1);assert.deepEqual(models,['m1']);
  }
});
