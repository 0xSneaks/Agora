// Main chat screen: top bar, drawer, messages, composer. Licensed under GPL-3.0.
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'preact/hooks';
import { chatStore, currentModel, deleteChat, newChat, openChat, renameChat, send, setConversationModel, stop } from '../core/chat';
import { greetingFor } from '../core/greeting';
import { isIos, isStandalone } from '../core/platform';
import { useStore } from '../core/store';
import { activePath } from '../core/tree';
import { MessageView } from './MessageView';
import { ModelSheet } from './ModelSheet';
import { Settings } from './Settings';

const finePointer = typeof window !== 'undefined' && (window.matchMedia?.('(pointer: fine)').matches ?? false);

export function ChatScreen({ name, onName }: { name: string; onName: (n: string) => void }) {
  const state = useStore(chatStore);
  const [drawer, setDrawer] = useState(false);
  const [models, setModels] = useState(false);
  const [settings, setSettings] = useState(false);
  const [draft, setDraft] = useState('');
  const scroller = useRef<HTMLElement>(null);
  const stick = useRef(true);
  const taRef = useRef<HTMLTextAreaElement>(null);
  const greetHour = useMemo(() => new Date().getHours(), [state.currentId]);

  const conv = state.conversations.find((c) => c.id === state.currentId);
  const path = conv ? activePath(conv, state.messages.values()) : [];
  const busy = !!conv && state.running.has(conv.id);
  const model = currentModel();

  useLayoutEffect(() => {
    const el = scroller.current;
    if (el && stick.current) el.scrollTop = el.scrollHeight;
  }, [state.version, state.currentId]);

  useEffect(() => {
    const ta = taRef.current;
    if (!ta) return;
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, window.innerHeight * 0.4) + 'px';
  }, [draft]);

  const submit = () => {
    if (busy || !draft.trim()) return;
    const text = draft;
    setDraft('');
    stick.current = true;
    send(text);
  };

  return (
    <div class="shell">
      <header class="topbar">
        <button class="icon-btn" aria-label="Chats" data-testid="open-drawer" onClick={() => setDrawer(true)}>☰</button>
        <div class="title">
          <button class="model-chip" data-testid="model-chip" onClick={() => setModels(true)}>{model.model ?? 'Choose a model'}</button>
        </div>
        <button class="icon-btn" aria-label="New chat" data-testid="new-chat" onClick={() => { newChat(); setDraft(''); }}>✎</button>
      </header>
      <main class="main" ref={scroller} onScroll={(e) => {
        const el = e.currentTarget as HTMLElement;
        stick.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80;
      }}>
        {!isStandalone() && isIos() && <div class="banner" data-testid="install-hint">Install: tap <b>Share</b> → <b>Add to Home Screen</b>.</div>}
        {path.length === 0 ? (
          <div class="greeting" data-testid="greeting">{greetingFor(greetHour, name)}</div>
        ) : (
          <div class="msgs" data-testid="messages">
            {path.map((m) => <MessageView key={m.id} msg={m} all={state.messages} busy={busy} />)}
          </div>
        )}
      </main>
      <footer class="bottom">
        <form class="composer" onSubmit={(e) => { e.preventDefault(); submit(); }}>
          <textarea ref={taRef} rows={1} placeholder="Message" data-testid="composer" value={draft}
            onInput={(e) => setDraft((e.target as HTMLTextAreaElement).value)}
            onKeyDown={(e) => { if (finePointer && e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); submit(); } }} />
          {busy ? (
            <button type="button" class="send stop" aria-label="Stop" data-testid="stop" onClick={() => stop()}>■</button>
          ) : (
            <button type="submit" class="send" aria-label="Send" data-testid="send" disabled={!draft.trim()}>↑</button>
          )}
        </form>
      </footer>

      {drawer && (
        <>
          <div class="scrim" onClick={() => setDrawer(false)} />
          <nav class="drawer" data-testid="drawer">
            <button class="btn primary" onClick={() => { newChat(); setDrawer(false); }}>+ New chat</button>
            <div class="list">
              {state.conversations.length === 0 && <p class="subtle center">No chats yet</p>}
              {state.conversations.map((c) => (
                <div class={'conv' + (c.id === state.currentId ? ' active' : '')} key={c.id}>
                  <button data-testid="conv-item" onClick={() => { openChat(c.id); setDrawer(false); }}>{state.running.has(c.id) ? '● ' : ''}{c.title}</button>
                  <button class="icon-btn" aria-label="Rename" onClick={() => { const t = prompt('Rename chat', c.title); if (t) renameChat(c.id, t); }}>✎</button>
                  <button class="icon-btn" aria-label="Delete" onClick={() => { if (confirm(`Delete “${c.title}”?`)) deleteChat(c.id); }}>🗑</button>
                </div>
              ))}
            </div>
            <button class="btn" data-testid="open-settings" onClick={() => { setDrawer(false); setSettings(true); }}>⚙ Settings</button>
          </nav>
        </>
      )}
      {models && (
        <ModelSheet current={model} onClose={() => setModels(false)} onPick={(pid, m) => { if (conv) setConversationModel(pid, m); }} />
      )}
      {settings && <Settings onClose={() => setSettings(false)} name={name} onName={onName} />}
    </div>
  );
}
