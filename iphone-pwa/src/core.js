// SPDX-License-Identifier: GPL-3.0-only
// New web implementation by Sneaks AI, 2026. Android behavior reference: Agora.
export const DATA_KEY = 'sneaks-ai.data.v1';
export const KEYS_KEY = 'sneaks-ai.keys.v1';
export const normalizeName = value => String(value).trim().slice(0, 40);
export function greeting(name, hour = new Date().getHours()) {
  if (!normalizeName(name)) return 'Welcome to Sneaks AI';
  const period = hour >= 5 && hour < 12 ? 'Good morning' : hour >= 12 && hour < 17 ? 'Good afternoon' : hour >= 17 && hour < 22 ? 'Good evening' : 'Hey';
  return `${period}, ${normalizeName(name)}`;
}
export function endpoint(value, ownOrigin) {
  let url;
  try { url = new URL(value); } catch { throw new Error('Enter a complete HTTPS API base URL.'); }
  if (url.protocol !== 'https:' || url.username || url.password || url.search || url.hash || (ownOrigin && url.origin === ownOrigin)) {
    throw new Error('Use an external HTTPS provider URL without credentials, query parameters or a fragment.');
  }
  return url.href.replace(/\/+$/, '');
}
export function modelIds(value) {
  return [...new Set(String(value).split('\n').map(s => s.trim()).filter(Boolean))];
}
export function validateProvider(input, ownOrigin) {
  const name = String(input.name || '').trim().slice(0, 60);
  const models = modelIds(input.models.join('\n'));
  if (!name) throw new Error('Give this provider a name.');
  if (!['openai', 'anthropic', 'google'].includes(input.protocol)) throw new Error('Select a supported protocol.');
  if (models.some(m => m.length > 200 || /[\s?#]/.test(m))) throw new Error('Model IDs must be exact IDs with no spaces, ? or #.');
  return { id: input.id, name, protocol: input.protocol, baseUrl: endpoint(input.baseUrl, ownOrigin), models };
}
export const emptyData = () => ({ version: 1, onboarded: false, name: '', providers: [], conversations: [], selected: '' });
export function validateData(data, ownOrigin) {
  if (!data || data.version !== 1 || typeof data.name !== 'string' || typeof data.onboarded !== 'boolean' || !Array.isArray(data.providers) || !Array.isArray(data.conversations)) throw new Error('Unrecognized local data. Export or reset it from the recovery screen.');
  const ids = new Set();
  for (const p of data.providers) {
    if (typeof p.id !== 'string' || !p.id || ids.has(p.id) || !Array.isArray(p.models) || p.models.some(m => typeof m !== 'string')) throw new Error('Invalid provider data.');
    validateProvider(p, ownOrigin); ids.add(p.id);
  }
  const chats = new Set();
  for (const c of data.conversations) {
    if (!c || typeof c.id !== 'string' || chats.has(c.id) || typeof c.title !== 'string' || !Array.isArray(c.messages) || typeof c.providerId !== 'string' || typeof c.model !== 'string' || (c.system !== undefined && typeof c.system !== 'string')) throw new Error('Invalid conversation data.');
    chats.add(c.id);
    for (const m of c.messages) if (!m || !['user', 'assistant'].includes(m.role) || typeof m.content !== 'string') throw new Error('Invalid message data.');
  }
  return { ...data, name: normalizeName(data.name), selected: typeof data.selected === 'string' ? data.selected : '' };
}
export function readData(storage, ownOrigin) {
  const raw = storage.getItem(DATA_KEY);
  return raw === null ? emptyData() : validateData(JSON.parse(raw), ownOrigin);
}
export function readKeys(storage) {
  const keys = JSON.parse(storage.getItem(KEYS_KEY) || '{}');
  if (!keys || Array.isArray(keys) || typeof keys !== 'object' || Object.values(keys).some(v => typeof v !== 'string')) throw new Error('Invalid local key storage.');
  return keys;
}
export function writeData(storage, data) {
  try { storage.setItem(DATA_KEY, JSON.stringify(data)); }
  catch { throw new Error('Could not save on this device. Storage may be full or blocked. Export your chats before clearing space.'); }
}
export function writeKeys(storage, keys) {
  try { storage.setItem(KEYS_KEY, JSON.stringify(keys)); }
  catch { throw new Error('Could not save API keys on this device. Storage may be full or blocked.'); }
}
export function publicBackup(data) {
  // Whitelist fields: credentials are never included, even if an imported object has extra fields.
  return { version: 1, onboarded: data.onboarded, name: data.name, selected: data.selected,
    providers: data.providers.map(({id,name,protocol,baseUrl,models}) => ({id,name,protocol,baseUrl,models})),
    conversations: data.conversations.map(({id,title,providerId,model,system,createdAt,messages}) => ({id,title,providerId,model,system:system || '',createdAt,messages:messages.map(({role,content}) => ({role,content}))})) };
}
