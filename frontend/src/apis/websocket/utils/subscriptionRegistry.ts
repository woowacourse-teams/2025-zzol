type SubscriptionHandler = (data: unknown) => void;

const handlers = new Map<string, Set<SubscriptionHandler>>();

export const subscriptionRegistry = {
  register: (destination: string, handler: SubscriptionHandler) => {
    if (!handlers.has(destination)) {
      handlers.set(destination, new Set());
    }
    handlers.get(destination)!.add(handler);
  },

  unregister: (destination: string, handler: SubscriptionHandler) => {
    const set = handlers.get(destination);
    if (set) {
      set.delete(handler);
      if (set.size === 0) {
        handlers.delete(destination);
      }
    }
  },

  dispatch: (destination: string, data: unknown) => {
    const set = handlers.get(destination);
    if (set) {
      set.forEach((handler) => {
        try {
          handler(data);
        } catch (error) {
          console.error('Recovery 메시지 처리 실패:', error);
        }
      });
      return true;
    }
    return false;
  },

  hasHandler: (destination: string) => {
    return handlers.has(destination) && handlers.get(destination)!.size > 0;
  },
};
