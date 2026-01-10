/* eslint-env browser */

export class NetworkCollector {
  constructor(max = 1000) {
    this.max = max;
    this.requests = [];
    this.listeners = new Set();
  }
  add(request) {
    this.requests.unshift(request);
    if (this.requests.length > this.max) this.requests = this.requests.slice(0, this.max);
    this.listeners.forEach((fn) => {
      try {
        fn(request);
      } catch {
        /* noop */
      }
    });
  }
  getRequests() {
    return this.requests.slice();
  }
  clear() {
    this.requests = [];
  }
  subscribe(listener) {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }
}
