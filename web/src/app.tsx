// Sneaks AI web app shell. Licensed under GPL-3.0.
import { useCallback, useEffect, useState } from 'preact/hooks';
import { Intro } from './ui/Intro';
import { greetingFor } from './core/greeting';
import { kv } from './core/db';
import { isIos, isStandalone, requestPersistentStorage } from './core/platform';

export function App() {
  const [introDone, setIntroDone] = useState(false);
  const [name, setName] = useState('');
  const [standalone] = useState(isStandalone);
  const onIntroDone = useCallback(() => setIntroDone(true), []);

  useEffect(() => {
    kv.get<string>('profile.name').then((n) => setName(n ?? '')).catch(() => undefined);
    requestPersistentStorage();
  }, []);

  return (
    <div class={'shell' + (standalone ? ' standalone' : '')} data-standalone={String(standalone)}>
      {!introDone && <Intro onDone={onIntroDone} />}
      <header class="topbar"><div class="title">Sneaks AI</div></header>
      <main class="main">
        {!standalone && isIos() && (
          <div class="banner" data-testid="install-hint">
            Install: tap <b>Share</b> → <b>Add to Home Screen</b>.
          </div>
        )}
        <div class="greeting" data-testid="greeting">{greetingFor(new Date().getHours(), name)}</div>
      </main>
    </div>
  );
}
