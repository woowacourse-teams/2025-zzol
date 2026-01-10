/* eslint-env browser */

import { setupFetchHook } from './hooks/fetchHook.js';
import { setupWebSocketHook } from './hooks/websocketHook.js';
import { getSafeWindow } from './utils/common/getSafeWindow.js';
import { validateWindow } from './utils/snippet/validateWindow.js';
import { initializeSnippet } from './utils/snippet/initializeSnippet.js';
import { initializeCollector } from './utils/snippet/initializeCollector.js';

const w = getSafeWindow();

validateWindow(w);

const collector = initializeCollector(w);
initializeSnippet(w);

const context = w.self === w.top ? 'MAIN' : w.name || 'IFRAME';

setupFetchHook(w, collector, context);
setupWebSocketHook(w, collector, context);
