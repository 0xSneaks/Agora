// SPDX-License-Identifier: GPL-3.0-only
import { endpoint } from './core.js';
// Endpoint defaults verified against Android reference ce19a838; models are user-entered or fetched.
export const PRESETS = [
  { name: 'OpenRouter', protocol: 'openai', baseUrl: 'https://openrouter.ai/api/v1' },
  { name: 'OpenAI', protocol: 'openai', baseUrl: 'https://api.openai.com/v1' },
  { name: 'Anthropic', protocol: 'anthropic', baseUrl: 'https://api.anthropic.com/v1' },
  { name: 'Google Gemini', protocol: 'google', baseUrl: 'https://generativelanguage.googleapis.com/v1beta' },
  { name: 'DeepSeek', protocol: 'openai', baseUrl: 'https://api.deepseek.com' },
  { name: 'Qwen / DashScope', protocol: 'openai', baseUrl: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1' },
  { name: 'Groq', protocol: 'openai', baseUrl: 'https://api.groq.com/openai/v1' },
  { name: 'OpenCode Go', protocol: 'openai', baseUrl: 'https://opencode.ai/zen/go/v1' }
];
export function headersFor(protocol, key) {
  const headers = { 'Content-Type': 'application/json' };
  if (protocol === 'anthropic') Object.assign(headers, { 'anthropic-version': '2023-06-01', 'anthropic-dangerous-direct-browser-access': 'true', ...(key ? {'x-api-key': key} : {}) });
  else if (protocol === 'google') { if (key) headers['x-goog-api-key'] = key; }
  else if (key) headers.Authorization = `Bearer ${key}`;
  return headers;
}
export function chatRequest(provider, key, model, messages, system = '', ownOrigin) {
  const base = endpoint(provider.baseUrl, ownOrigin);
  if (!model || /[\s?#]/.test(model)) throw new Error('Choose an exact model ID first.');
  const clean = messages.filter(m => ['user','assistant'].includes(m.role)).map(({role,content}) => ({role,content}));
  let url, body;
  if (provider.protocol === 'google') {
    const id = model.replace(/^models\//, '');
    url = `${base}/models/${encodeURIComponent(id)}:streamGenerateContent?alt=sse`;
    body = { contents: clean.map(m => ({role:m.role === 'assistant' ? 'model' : 'user',parts:[{text:m.content}]})), ...(system ? {systemInstruction:{parts:[{text:system}]}} : {}) };
  } else if (provider.protocol === 'anthropic') {
    url = `${base}/messages`;
    body = { model, max_tokens: 4096, stream: true, messages: clean, ...(system ? {system} : {}) };
  } else if (provider.protocol === 'openai') {
    url = `${base}/chat/completions`;
    body = { model, stream:true, messages: [...(system ? [{role:'system',content:system}] : []), ...clean] };
  } else throw new Error('Unsupported provider protocol.');
  return {url, options:{method:'POST',headers:headersFor(provider.protocol,key),body:JSON.stringify(body),credentials:'omit',redirect:'error',referrerPolicy:'no-referrer',cache:'no-store'}};
}
export async function directFetch(url, options, fetcher = fetch) {
  let response;
  try { response = await fetcher(url, options); }
  catch (err) {
    if (err.name === 'AbortError') throw err;
    // Do not surface raw errors: providers may echo secrets in errors or URLs.
    throw new Error('Connection blocked or unavailable. Check your connection and API URL. This provider may not allow Safari/browser requests (CORS). No relay is used.');
  }
  if (!response.ok) {
    const reasons = {400:'Request rejected. Check the model and protocol.',401:'API key rejected.',403:'Access denied by the provider.',404:'Model or API path not found.',429:'Provider rate limit or credit limit reached.'};
    throw new Error(`Provider HTTP ${response.status}. ${reasons[response.status] || 'Provider request failed.'}`);
  }
  return response;
}
async function safeJson(response) {
  try { return await response.json(); }
  catch { throw new Error('The provider returned invalid JSON. Check the API URL and protocol.'); }
}
export function deltaText(protocol, event) {
  if (event.error || event.type === 'error') throw new Error('The provider returned a streaming error. Check your provider account and model.');
  if (protocol === 'anthropic') return event.type === 'content_block_delta' && event.delta?.type === 'text_delta' ? event.delta.text || '' : '';
  if (protocol === 'google') {
    if (event.promptFeedback?.blockReason || event.candidates?.some(c => ['SAFETY','RECITATION','PROHIBITED_CONTENT'].includes(c.finishReason))) throw new Error('The provider blocked this response.');
    return (event.candidates?.[0]?.content?.parts || []).filter(p => !p.thought).map(p => p.text || '').join('');
  }
  const c = event.choices?.[0];
  return c?.delta?.content || c?.message?.content || '';
}
export async function consumeResponse(response, protocol, onText) {
  if (!(response.headers.get('content-type') || '').includes('text/event-stream')) {
    const event = await safeJson(response);
    const text = protocol === 'anthropic' ? (event.content || []).filter(p => p.type === 'text').map(p => p.text).join('') : deltaText(protocol,event);
    if (!text) throw new Error('No text returned. This model may require tools, another API or a different request format.');
    onText(text); return;
  }
  if (!response.body) throw new Error('Streaming is unavailable in this browser.');
  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '', total = 0;
  function process(block) {
    const data = block.split('\n').filter(l => l.startsWith('data:')).map(l => l.slice(5).trimStart()).join('\n');
    if (!data || data === '[DONE]') return;
    let event;
    try { event = JSON.parse(data); } catch { throw new Error('The provider returned an invalid stream.'); }
    const delta = deltaText(protocol,event);
    if (delta) { total += delta.length; if (total > 2000000) throw new Error('Response too large. Partial reply kept on device.'); onText(delta); }
  }
  let complete = false;
  try {
    while (true) {
      const {value,done} = await reader.read();
      buffer += decoder.decode(value, {stream:!done});
      buffer = buffer.replace(/\r\n/g,'\n');
      let boundary;
      while ((boundary = buffer.indexOf('\n\n')) >= 0) { process(buffer.slice(0,boundary)); buffer = buffer.slice(boundary+2); }
      if (buffer.length > 2000000) throw new Error('Provider stream event too large.');
      if (done) { if (buffer.trim()) process(buffer); complete = true; break; }
    }
    if (!total) throw new Error('No text returned. This model may require tools or another API.');
  } finally { if (!complete) await reader.cancel().catch(() => {}); reader.releaseLock(); }
}
export async function fetchModels(provider, key, ownOrigin, fetcher = fetch, signal) {
  const base = endpoint(provider.baseUrl,ownOrigin);
  const response = await directFetch(`${base}/models`, {method:'GET',headers:headersFor(provider.protocol,key),credentials:'omit',redirect:'error',referrerPolicy:'no-referrer',cache:'no-store',signal},fetcher);
  const data = await safeJson(response);
  const items = provider.protocol === 'google' ? data.models : data.data;
  if (!Array.isArray(items)) throw new Error('Model listing is unavailable. Enter model IDs manually.');
  return [...new Set(items.filter(m => provider.protocol !== 'google' || !m.supportedGenerationMethods || m.supportedGenerationMethods.includes('generateContent')).map(m => provider.protocol === 'google' ? m.name?.replace(/^models\//,'') : m.id).filter(m => typeof m === 'string'))].sort();
}
