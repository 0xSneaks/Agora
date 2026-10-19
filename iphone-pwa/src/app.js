// SPDX-License-Identifier: GPL-3.0-only
import {DATA_KEY,KEYS_KEY,normalizeName,greeting,modelIds,validateProvider,readData,readKeys,writeData,writeKeys,publicBackup,validateData} from './core.js';
import {PRESETS,chatRequest,directFetch,consumeResponse,fetchModels} from './providers.js';

const app = document.querySelector('#app');
const origin = location.origin;
let data, keys, active = '', page = 'chat', busy = false, controller, saveTimer, noticeTimer, uiError = '', chatHour = new Date().getHours();
let draft = '', systemDraft = '', selectedProvider = '', selectedModel = '', stale = false;
const $ = s => document.querySelector(s);
const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const uid = () => crypto.randomUUID();
function notice(message) {
  const el = $('#notice'); el.textContent = message; el.hidden = false;
  clearTimeout(noticeTimer); noticeTimer = setTimeout(() => { el.hidden = true; }, 7000);
}
function persist() { if(stale) throw new Error('Data changed in another window. Close and reopen this window before continuing.'); writeData(localStorage,data); }
function mutate(change) {
  const previous = structuredClone(data);
  try { change(); persist(); return true; }
  catch (err) { data = previous; notice(err.message); return false; }
}
function backup() {
  const blob = new Blob([JSON.stringify(publicBackup(data),null,2)],{type:'application/json'});
  const url = URL.createObjectURL(blob), a = document.createElement('a');
  a.href = url; a.download = `sneaks-ai-${new Date().toISOString().slice(0,10)}.json`; a.click();
  setTimeout(() => URL.revokeObjectURL(url), 10000);
}
const currentChat = () => data.conversations.find(c => c.id === active);
const currentProvider = () => data.providers.find(p => p.id === selectedProvider);
function selectDefaults() {
  const c = currentChat();
  const preferred = c?.providerId || data.selected;
  const p = data.providers.find(p => p.id === preferred) || (!c ? data.providers[0] : null);
  selectedProvider = p?.id || ''; selectedModel = c?.model || p?.models[0] || '';
}
function topbar(title = 'Sneaks AI') {
  return `<div class="topbar"><div class="row"><button id="menu" aria-label="Open conversations">☰</button><div class="brand">${esc(title).replace(' AI',' <span>AI</span>')}</div></div><div class="row"><span class="pill" id="connection">${navigator.onLine ? 'DEVICE ONLY' : 'OFFLINE'}</span><button id="settings">Settings</button></div></div>`;
}
function goChat() { page = 'chat'; uiError = ''; selectDefaults(); render(); }
function newChat() { if (busy) return; active = ''; draft = ''; systemDraft = ''; chatHour = new Date().getHours(); goChat(); }
function renderIntro() {
  app.innerHTML = `<main class="onboard"><section class="welcome"><div class="eyebrow">SNEAKS AI / PERSONAL TERMINAL</div><div class="sign-frame"><h1 class="neon">0x Sneaky Alpha</h1><div class="eyebrow pink">YOUR MODELS. YOUR RULES.</div></div><h2>A little neon.<br>A lot of possibility.</h2><p>Bring your own API key. Keep your conversations on this device.</p><button class="primary" id="begin">Enter Sneaks AI →</button><footer>NO ACCOUNT · NO RELAY · NO ANALYTICS</footer></section></main>`;
  $('#begin').onclick = renderName;
}
function renderName() {
  app.innerHTML = `<main class="onboard"><form class="welcome" id="name-form"><div class="eyebrow">MAKE YOURSELF AT HOME</div><div class="sigil">S_</div><h1>What should I call you?</h1><p>Optional. Your name stays on this device and is only used to greet you.</p><label for="display-name">Your name</label><input id="display-name" maxlength="40" autocomplete="off" value="${esc(data.name)}" placeholder="Sneaks"><div class="help">40 characters maximum. You can change this in Settings.</div><button class="primary" type="submit">Continue →</button><button class="quiet" id="skip" type="button">Skip for now</button></form></main>`;
  function complete(name) {
    if (mutate(() => { data.name = normalizeName(name); data.onboarded = true; })) { goChat(); navigator.storage?.persist?.().catch(() => {}); }
  }
  $('#name-form').onsubmit = e => { e.preventDefault(); complete($('#display-name').value); };
  $('#skip').onclick = () => complete('');
}
function render() {
  if (!data.onboarded) return renderIntro();
  if (page === 'settings') return renderSettings();
  if (page === 'provider') return;
  renderChat();
}
function renderChat() {
  const p = currentProvider(), c = currentChat();
  app.innerHTML = `<div class="shell">${topbar()}<div class="workspace"><aside class="sidebar" id="sidebar"><button class="primary" id="new-chat" ${busy?'disabled':''}>＋ New chat</button><div class="eyebrow" style="margin:12px 8px 0">CONVERSATIONS</div><nav class="history" aria-label="Conversations">${data.conversations.length ? data.conversations.map(c => `<div class="history-item ${active===c.id?'active':''}"><button data-chat="${esc(c.id)}" ${busy?'disabled':''}>${esc(c.title)}</button><button class="delete" data-delete="${esc(c.id)}" aria-label="Delete ${esc(c.title)}" ${busy?'disabled':''}>×</button></div>`).join('') : '<p class="help" style="padding:8px">Your next idea starts here.</p>'}</nav><button id="export">Export chats</button><div class="local-note">Stored on this device.<br>No account. No conversation sync.</div></aside><main class="main"><div class="messages" id="messages" aria-label="Chat messages">${c?.messages.length ? c.messages.map(messageHTML).join('') : emptyHTML()}<div class="error" id="chat-error" role="alert" ${uiError?'':'hidden'}>${esc(uiError)}</div></div><form class="composer" id="composer"><div class="model-row"><label class="grow"><span class="sr-only" hidden>Provider</span><select id="provider-select" aria-label="Provider" ${busy?'disabled':''}><option value="">Select provider</option>${data.providers.map(p => `<option value="${esc(p.id)}" ${p.id===selectedProvider?'selected':''}>${esc(p.name)}</option>`).join('')}</select></label><label class="grow"><span hidden>Model</span><select id="model-select" aria-label="Model" ${busy?'disabled':''}><option value="">Select model</option>${p?.models.map(m => `<option ${selectedModel===m?'selected':''} value="${esc(m)}">${esc(m)}</option>`).join('') || ''}</select></label><button type="button" id="configure" aria-label="Configure providers">＋</button></div><div class="composer-box"><textarea id="prompt" aria-label="Message" rows="1" placeholder="${p?'Message your AI…':'Add a provider to start…'}" ${busy?'disabled':''}>${esc(draft)}</textarea><button type="${busy?'button':'submit'}" class="primary send" id="send">${busy?'Stop':'Send ↑'}</button></div><div class="composer-foot">${p?`Sends this conversation directly to ${esc(new URL(p.baseUrl).host)}`:'Your keys. Your device. Configure a provider to begin.'}</div></form></main></div></div>`;
  $('#menu').onclick = () => $('#sidebar').classList.toggle('open');
  $('#settings').onclick = () => { if(busy) return notice('Stop the reply before editing settings.'); page='settings'; render(); };
  $('#configure').onclick = () => { if(busy) return; page='settings'; render(); };
  $('#new-chat').onclick = newChat;
  $('#export').onclick = backup;
  document.querySelectorAll('[data-chat]').forEach(el => el.onclick = () => { active=el.dataset.chat; draft=''; systemDraft=''; chatHour=new Date().getHours(); goChat(); });
  document.querySelectorAll('[data-delete]').forEach(el => el.onclick = () => {
    if (!confirm('Delete this conversation from this device?')) return;
    if (mutate(() => { data.conversations = data.conversations.filter(c => c.id !== el.dataset.delete); })) { if(active===el.dataset.delete) active=''; goChat(); }
  });
  $('#provider-select').onchange = e => {
    const next = data.providers.find(p => p.id === e.target.value);
    if (c?.messages.length && next && next.id!==c.providerId && !confirm(`Send this conversation's history to ${next.name} at ${new URL(next.baseUrl).origin} on your next message?`)) {e.target.value=selectedProvider;return;}
    selectedProvider=next?.id || ''; selectedModel=next?.models[0] || ''; renderChat();
  };
  $('#model-select').onchange = e => { selectedModel=e.target.value; };
  $('#prompt').oninput = e => { draft=e.target.value; e.target.style.height='auto'; e.target.style.height=`${Math.min(e.target.scrollHeight,160)}px`; };
  $('#composer').onsubmit = sendMessage;
  if(busy) $('#send').onclick=() => controller?.abort();
  $('#open-providers')?.addEventListener('click',() => {page='settings';render();});
  $('#system')?.addEventListener('input',e=>{systemDraft=e.target.value;});
  bindCopy(); scrollBottom();
}
function emptyHTML() {
  return `<section class="empty"><div class="empty-inner"><div class="eyebrow">A SPACE FOR YOUR NEXT IDEA</div><div class="sigil">S_</div><h1>${esc(greeting(data.name,chatHour))}</h1><p>Pick a model. Ask a question.<br>Make something worth staying up for.</p>${data.providers.length ? '<div class="mini-grid"><span>BYOK</span><span>DIRECT TO PROVIDER</span><span>LOCAL HISTORY</span></div>' : '<button class="primary" id="open-providers">Add your first provider →</button>'}<details style="margin-top:24px;text-align:left"><summary class="help">System instruction (optional)</summary><label for="system">Used for this new conversation</label><textarea id="system" placeholder="How should the AI respond?">${esc(systemDraft)}</textarea></details></div></section>`;
}
function messageHTML(m,i) {
  return `<article class="bubble ${m.role}" data-message="${i}"><div class="role">${m.role==='user'?'YOU':'SNEAKS AI'}</div><div class="content">${esc(m.content)}</div>${m.content?`<button class="copy" type="button" data-copy="${i}">Copy</button>`:''}</article>`;
}
function bindCopy() {document.querySelectorAll('[data-copy]').forEach(el=>el.onclick=async()=>{try{await navigator.clipboard.writeText(currentChat().messages[Number(el.dataset.copy)].content);notice('Copied.');}catch{notice('Copy unavailable. Touch and hold the message to select its text.');}});}
function scrollBottom() {const el=$('#messages'); if(el) el.scrollTop=el.scrollHeight;}
async function sendMessage(e) {
  e.preventDefault(); if(busy) return;
  const text=$('#prompt').value.trim(), p=currentProvider();
  if(!text) return;
  if(!p || !selectedModel || !p.models.includes(selectedModel)) return notice('Add a provider and choose a model first.');
  if(!navigator.onLine) return notice('You are offline. History is available; AI replies need an internet connection.');
  if(!keys[p.id] && !confirm(`No API key saved for ${p.name}. Try a keyless request?`)) return;
  let c=currentChat();
  const before=structuredClone(data), wasActive=active;
  if(!c) {c={id:uid(),title:text.slice(0,65),providerId:p.id,model:selectedModel,system:systemDraft,createdAt:new Date().toISOString(),messages:[]};data.conversations.unshift(c);active=c.id;}
  c.providerId=p.id; c.model=selectedModel; data.selected=p.id;
  c.messages.push({role:'user',content:text});
  try {persist();}catch(err){data=before;active=wasActive;return notice(err.message);}
  const requestMessages=c.messages.map(m=>({...m}));
  const reply={role:'assistant',content:''};c.messages.push(reply);
  busy=true;draft='';uiError='';controller=new AbortController();renderChat();
  const timeout=setTimeout(()=>controller?.abort(),180000);
  function savePartial() {
    clearTimeout(saveTimer); saveTimer=setTimeout(()=>{try{persist();}catch{controller?.abort();uiError='Device storage is full. Export your chats now; the reply is still visible in this session.';}},500);
  }
  try {
    const req=chatRequest(p,keys[p.id] || '',selectedModel,requestMessages,c.system,origin);
    const response=await directFetch(req.url,{...req.options,signal:controller.signal});
    await consumeResponse(response,p.protocol,delta=>{
      reply.content+=delta;
      const container=$('#messages'), atBottom=container.scrollHeight-container.scrollTop-container.clientHeight<110;
      const content=document.querySelector(`[data-message="${c.messages.length-1}"] .content`);
      if(content) content.textContent=reply.content;
      if(atBottom) scrollBottom(); savePartial();
    });
  } catch(err) {
    uiError=err.name==='AbortError'?'Reply stopped or timed out. Any partial text is kept.':err.message;
  } finally {
    clearTimeout(timeout);clearTimeout(saveTimer);
    if(!reply.content)c.messages.pop();
    try{persist();}catch(err){uiError=err.message;}
    busy=false;controller=null;renderChat();
  }
}
function renderSettings() {
  page='settings';
  app.innerHTML=`<div class="shell">${topbar()}<main class="page" style="overflow:auto;width:100%"><header class="row between"><div><div class="eyebrow">YOUR PERSONAL TERMINAL</div><h1 style="margin:8px 0 0">Settings</h1></div><button id="back">← Chat</button></header><section class="card settings-group"><h3>Make it yours</h3><form id="profile-form"><label for="profile-name">What should I call you?</label><div class="row"><input id="profile-name" maxlength="40" autocomplete="off" value="${esc(data.name)}" placeholder="Optional name"><button type="submit">Save</button></div></form></section><section class="settings-group"><div class="row between" style="margin:26px 0 14px"><h2 style="margin:0">Providers & models</h2><button id="add-provider" class="primary">＋ Add</button></div><p class="help">Your own keys. Exact model IDs. No subscription to Sneaks AI.</p><div class="stack">${data.providers.length?data.providers.map(p=>`<article class="card provider-card"><div class="row between"><h3>${esc(p.name)}</h3><span class="pill">${keys[p.id]?'KEY SAVED':'NO KEY'}</span></div><p class="origin" style="margin-top:10px">${esc(p.baseUrl)}</p><div class="models">${p.models.length?esc(p.models.join(' · ')):'No models yet. Edit to add IDs or fetch the provider’s list.'}</div><div class="button-grid"><button data-edit="${esc(p.id)}">Edit provider & models</button><button class="danger" data-remove="${esc(p.id)}">Remove</button></div></article>`).join(''):'<div class="card muted">No providers configured. Add one to start chatting.</div>'}</div></section><section class="card settings-group"><h3>Your data stays here</h3><p class="help">Chats, name, settings and API keys are stored in this browser’s localStorage. Keys are not encrypted by this app and are readable by code running on this origin, browser tools, or someone with access to your unlocked device. Use limited keys and revoke them if exposed.</p><p class="help">Only a message or model-list request you initiate sends your key directly to the provider URL you saved. A chat sends that conversation’s history and system instruction. No analytics, crash uploads, provider auto-sync, or third-party update checks. Vercel serves static files and may keep normal hosting access logs; it receives no app chat/key payloads. The browser can check this origin for app/service-worker updates.</p><p class="help">Clearing website data, removing the web app, private browsing, or storage eviction may erase local data. Home Screen and Safari storage may differ: install first, then add your keys. Export regularly. Exports exclude keys; enter them again after restoring.</p><div class="button-grid"><button id="backup">Export backup (no keys)</button><button id="restore">Import backup</button><button id="persist">Request storage protection</button><button class="danger" id="erase">Erase all local data</button></div><input type="file" id="import-file" accept="application/json,.json" hidden></section><section class="card settings-group"><h3>Install on iPhone</h3><p class="help">OPEN this address in Safari → TAP Share → SCROLL to Add to Home Screen → leave Open as Web App on → TAP Add. Then OPEN Sneaks AI from its icon.</p><p class="help" id="offline-status">Offline cache: checking…</p><p class="help">Offline mode opens the app and saved conversations. AI replies require internet. Keep the app open during replies; iOS may suspend it in the background.</p></section><section class="card"><h3>About Sneaks AI</h3><p class="help">Independent web implementation referencing <a href="https://github.com/0xSneaks/Agora/tree/feat/welcome-name-greeting" target="_blank" rel="noopener noreferrer">Sneaks’ Android branch</a> and upstream <a href="https://github.com/newo-ether/Agora" target="_blank" rel="noopener noreferrer">Agora by newo-ether and contributors</a>. Sneaks AI is not affiliated with or endorsed by upstream Agora. No Agora logo or artwork is used.</p><div class="button-grid"><a href="/LICENSE.txt" target="_blank" rel="noopener">GPL-3.0 license</a><a href="/SOURCE.txt" target="_blank" rel="noopener">Source & limitations</a></div><p class="help">v1.0.0 · iPhone PWA · No Android device-control features</p></section></main></div>`;
  $('#menu').hidden=true;$('#settings').hidden=true;
  $('#back').onclick=goChat;$('#add-provider').onclick=()=>renderProvider();
  $('#profile-form').onsubmit=e=>{e.preventDefault();if(mutate(()=>{data.name=normalizeName($('#profile-name').value);}))notice('Name saved on this device.');};
  document.querySelectorAll('[data-edit]').forEach(el=>el.onclick=()=>renderProvider(data.providers.find(p=>p.id===el.dataset.edit)));
  document.querySelectorAll('[data-remove]').forEach(el=>el.onclick=()=>{
    if(!confirm('Remove this provider and its saved key? Conversation history will stay.'))return;
    const previous=keys;keys={...keys};delete keys[el.dataset.remove];
    try{writeKeys(localStorage,keys);}catch(err){keys=previous;return notice(err.message);}
    if(mutate(()=>{data.providers=data.providers.filter(p=>p.id!==el.dataset.remove);}))renderSettings();
  });
  $('#backup').onclick=backup;$('#restore').onclick=()=>$('#import-file').click();
  $('#import-file').onchange=async e=>{
    const file=e.target.files[0];if(!file)return;
    try{
      if(file.size>4000000)throw new Error('Backup is too large (4 MB maximum).');
      const imported=publicBackup(validateData(JSON.parse(await file.text()),origin));
      if(!confirm('Replace local conversations and provider settings with this backup? Saved API keys will be removed.'))return;
      writeKeys(localStorage,{});keys={};
      writeData(localStorage,imported);data=imported;active='';notice('Backup restored. Add your provider keys again.');goChat();
    }catch{notice('Could not import. Check that this is a valid Sneaks AI backup and device storage is available.');}
  };
  $('#persist').onclick=async()=>{try{notice(await navigator.storage?.persist?.()?'Persistent storage granted. Keep exporting backups.':'Safari did not grant persistent storage. Keep exporting backups.');}catch{notice('Storage protection unavailable. Keep exporting backups.');}};
  $('#erase').onclick=()=>{if(confirm('Erase ALL Sneaks AI chats, provider keys and your name on this device? This cannot be undone.')){try{localStorage.removeItem(DATA_KEY);localStorage.removeItem(KEYS_KEY);location.reload();}catch{notice('Storage could not be cleared. Use Safari website data settings.');}}};
  offlineState();
}
async function offlineState() {
  const el=$('#offline-status');if(!el)return;
  try{const reg=await navigator.serviceWorker?.getRegistration();el.textContent=reg?.active?'Offline cache: installed. Saved chats can open without internet.':'Offline cache: not ready. Reopen after the first online visit.';}catch{el.textContent='Offline cache: unavailable in this browser.';}
}
function renderProvider(existing) {
  page='provider';const original=existing?structuredClone(existing):null;
  app.innerHTML=`<div class="shell">${topbar()}<main class="page" style="overflow:auto;width:100%"><header class="row between"><div><div class="eyebrow">DIRECT CONNECTION</div><h1 style="margin:8px 0 0">${existing?'Edit':'Add'} provider</h1></div><button id="cancel">Cancel</button></header><form id="provider-form"><div class="card"><label for="preset" style="margin-top:0">Start with a provider</label><select id="preset"><option value="">Custom / keep current settings</option>${PRESETS.map((p,i)=>`<option value="${i}">${esc(p.name)}</option>`).join('')}</select><label for="provider-name">Display name</label><input id="provider-name" maxlength="60" required value="${esc(existing?.name)}" placeholder="My provider"><label for="protocol">API protocol</label><select id="protocol"><option value="openai">OpenAI compatible / Chat Completions</option><option value="anthropic" ${existing?.protocol==='anthropic'?'selected':''}>Anthropic Messages</option><option value="google" ${existing?.protocol==='google'?'selected':''}>Google Gemini</option></select><label for="base-url">HTTPS API base URL</label><input id="base-url" type="url" inputmode="url" required autocomplete="off" autocapitalize="none" spellcheck="false" value="${esc(existing?.baseUrl)}" placeholder="https://provider.example/v1"><p class="help">Include the version path (such as /v1 or /v1beta), not /chat/completions or /messages. Your key is sent only to this endpoint. The provider must allow browser requests (CORS).</p><label for="api-key">API key</label><input id="api-key" type="password" autocomplete="off" autocapitalize="none" spellcheck="false" data-1p-ignore data-lpignore="true" value=""><div class="row" style="margin-top:6px"><button type="button" id="show-key" class="quiet">Show key</button><small>Saved only on this device.</small></div><div class="warning key-warning">localStorage is not a secure vault. This app stores your key as readable text on this origin. Use a restricted key and protect access to your phone.</div><label for="models">Model IDs — one per line</label><textarea id="models" rows="5" autocapitalize="none" spellcheck="false" placeholder="Paste the exact model ID from your provider">${esc(existing?.models.join('\n'))}</textarea><p class="help">Add, change or delete lines to edit your models. Fetching only fills an empty list; otherwise new IDs are added. The chat picker uses exactly these IDs.</p><button type="button" id="fetch-models">Fetch model list from provider</button><div class="error" id="provider-error" role="alert" hidden></div><div class="line"></div><button class="primary" id="save-provider" type="submit">Save on this device →</button></div></form></main></div>`;
  $('#menu').hidden=true;$('#settings').hidden=true;
  $('#api-key').value=existing?keys[existing.id] || '':'';
  $('#cancel').onclick=renderSettings;
  function clearChangedOrigin() {
    if(!original)return;
    try{if(new URL($('#base-url').value).origin!==new URL(original.baseUrl).origin){$('#api-key').value='';notice('Endpoint origin changed. Re-enter the key intended for this provider.');}}catch{$('#api-key').value='';}
  }
  $('#base-url').oninput=clearChangedOrigin;
  $('#preset').onchange=e=>{if(e.target.value==='')return;const p=PRESETS[Number(e.target.value)];$('#provider-name').value=p.name;$('#protocol').value=p.protocol;$('#base-url').value=p.baseUrl;$('#api-key').value='';$('#models').value='';};
  $('#show-key').onclick=()=>{const el=$('#api-key');el.type=el.type==='password'?'text':'password';$('#show-key').textContent=el.type==='password'?'Show key':'Hide key';};
  function formProvider(){return validateProvider({id:existing?.id || uid(),name:$('#provider-name').value,protocol:$('#protocol').value,baseUrl:$('#base-url').value,models:modelIds($('#models').value)},origin);}
  function error(message){$('#provider-error').textContent=message;$('#provider-error').hidden=false;}
  $('#fetch-models').onclick=async()=>{
    const button=$('#fetch-models');let p;
    try{p=formProvider();}catch(err){return error(err.message);}
    if(!confirm(`Send the entered key directly to ${new URL(p.baseUrl).origin} to fetch models?`))return;
    button.disabled=true;button.textContent='Fetching…';$('#cancel').disabled=true;$('#save-provider').disabled=true;
    const abort=new AbortController(), timeout=setTimeout(()=>abort.abort(),20000);
    try{const list=await fetchModels(p,$('#api-key').value.trim(),origin,fetch,abort.signal);$('#models').value=[...new Set([...modelIds($('#models').value),...list])].join('\n');notice(`Found ${list.length} model IDs. Review the list before saving.`);}
    catch(err){error(err.name==='AbortError'?'Model fetch timed out. Enter model IDs manually.':err.message);}
    finally{clearTimeout(timeout);button.disabled=false;button.textContent='Fetch model list from provider';$('#cancel').disabled=false;$('#save-provider').disabled=false;}
  };
  $('#provider-form').onsubmit=e=>{
    e.preventDefault();let p;
    try{p=formProvider();}catch(err){return error(err.message);}
    if(!confirm(`Trust ${p.name} at ${new URL(p.baseUrl).origin}? Your key and messages will go directly to this endpoint when used.`))return;
    const previous=keys;keys={...keys,[p.id]:$('#api-key').value.trim()};
    try{writeKeys(localStorage,keys);}catch(err){keys=previous;return error(err.message);}
    if(mutate(()=>{const i=data.providers.findIndex(x=>x.id===p.id);if(i<0)data.providers.push(p);else data.providers[i]=p;data.selected=p.id;})){notice('Provider saved on this device.');renderSettings();}
    else{keys=previous;try{writeKeys(localStorage,keys);}catch{notice('Settings save failed; check the saved key before trying again.');}}
  };
}
function boot() {
  try {data=readData(localStorage,origin);keys=readKeys(localStorage);selectDefaults();render();}
  catch {
    app.innerHTML='<main class="onboard"><section class="welcome"><h1>Local storage needs attention</h1><p>Saved data could not be read, or storage is blocked. Nothing has been overwritten. If you have important chats, preserve this site’s localStorage before resetting.</p><button id="retry">Try again</button><button id="reset" class="danger">Reset Sneaks AI data</button></section></main>';
    $('#retry').onclick=()=>location.reload();$('#reset').onclick=()=>{if(confirm('Permanently delete unreadable Sneaks AI chats and keys on this device?')){try{localStorage.removeItem(DATA_KEY);localStorage.removeItem(KEYS_KEY);location.reload();}catch{notice('Allow browser storage or leave private browsing, then try again.');}}};
  }
}
window.addEventListener('online',()=>{if($('#connection'))$('#connection').textContent='DEVICE ONLY';});
window.addEventListener('offline',()=>{if($('#connection'))$('#connection').textContent='OFFLINE';});
window.addEventListener('pagehide',()=>{if(data && !stale){try{persist();}catch{/* No logs or uploads. */}}});
window.addEventListener('storage',event=>{if(event.key===DATA_KEY || event.key===KEYS_KEY || event.key===null){stale=true;controller?.abort();notice('Sneaks AI changed in another window. Reopen this window before continuing.');app.inert=true;}});
boot();
// iOS keyboard changes the visual viewport without always resizing the layout viewport.
if(window.visualViewport) {
  const fitViewport=()=>document.documentElement.style.setProperty('--viewport-height',`${window.visualViewport.height}px`);
  window.visualViewport.addEventListener('resize',fitViewport);fitViewport();
}
if('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js').then(()=>navigator.serviceWorker.ready).then(offlineState).catch(()=>notice('Offline installation failed. Reopen online to try again.'));
