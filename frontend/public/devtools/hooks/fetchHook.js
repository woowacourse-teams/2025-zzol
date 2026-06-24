/* eslint-env browser */

import { extractRequestInfo } from '../utils/fetch/extractRequestInfo.js';
import { addRequest } from '../utils/common/addRequest.js';
import { getSafeWindow } from '../utils/common/getSafeWindow.js';
import { checkAlreadyHooked } from '../utils/common/checkAlreadyHooked.js';
import { defineHookedProperty } from '../utils/common/defineHookedProperty.js';

export const setupFetchHook = (win, collector, context = {}) => {
  win = getSafeWindow(win);
  if (!win) return null;

  if (checkAlreadyHooked(win, 'fetch')) {
    return null;
  }

  const originalFetch = win.fetch;

  const hookedFetch = function (input, init = {}) {
    const startedAt = Date.now();
    const { method, url } = extractRequestInfo(input, init) || {
      method: 'GET',
      url: String(input),
    };

    const originalPromise = originalFetch(input, init);

    originalPromise
      .then(async (res) => {
        try {
          let responseBody = null;
          const ct = res?.headers?.get?.('content-type') || '';

          if (res?.clone && (ct.includes('application/json') || ct.startsWith('text/'))) {
            try {
              responseBody = (await res.clone().text()).slice(0, 2048);
            } catch {
              /* noop */
            }
          }

          addRequest(collector, {
            type: 'fetch',
            context,
            method,
            url,
            status: res.status,
            startedAt,
            responseBody,
          });
        } catch {
          /* noop */
        }
        return res;
      })
      .catch((err) => {
        try {
          addRequest(collector, {
            type: 'fetch',
            context,
            method,
            url,
            status: 'NETWORK_ERROR',
            startedAt,
            errorMessage: String(err).slice(0, 512),
          });
        } catch {
          /* noop */
        }
      });

    return originalPromise;
  };

  defineHookedProperty(win, 'fetch', hookedFetch);
};
