import {
  createContext,
  useLayoutEffect,
  useRef,
  ReactNode,
  createElement,
  JSX,
  Context,
  useContext,
  useState,
} from 'react';

type ContextApi<T> = {
  get: () => T;
  subscribe: (subscriber: () => void) => () => void;
};

export const createContextSelector = <T>() => {
  const ContextInstance: Context<ContextApi<T> | null> = createContext<ContextApi<T> | null>(null);

  const Provider = ({ value, children }: { value: T; children: ReactNode }): JSX.Element => {
    const storeRef = useRef(value);
    const subscribers = useRef(new Set<() => void>());

    useLayoutEffect(() => {
      storeRef.current = value;
      subscribers.current.forEach((subscriber: () => void) => subscriber());
    }, [value]);

    const contextApi = useRef<ContextApi<T>>({
      get: () => storeRef.current,
      subscribe: (subscriber: () => void) => {
        subscribers.current.add(subscriber);
        return () => {
          subscribers.current.delete(subscriber);
        };
      },
    });

    return createElement(ContextInstance.Provider, { value: contextApi.current }, children);
  };

  const useContextSelector = <S>(selector: (state: T) => S) => {
    const context = useContext(ContextInstance);
    if (!context) {
      throw new Error('useContextSelector는 Provider 내부에서만 사용해야 합니다.');
    }

    const getSnapshot = context.get;
    const subscribe = context.subscribe;

    const [state, setState] = useState(() => selector(getSnapshot()));

    useLayoutEffect(() => {
      const checkForUpdates = () => {
        const newValue = selector(getSnapshot());
        setState((prev) => (Object.is(prev, newValue) ? prev : newValue));
      };

      return subscribe(checkForUpdates);
    }, [selector, subscribe, getSnapshot]);

    return state;
  };

  return { Provider, useContextSelector };
};
