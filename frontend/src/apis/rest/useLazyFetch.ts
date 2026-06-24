import { useCallback, useRef, useState } from 'react';
import { api } from './api';
import { ErrorDisplayMode } from './error';
import { useErrorToast } from './useErrorToast';

type UseLazyFetchOptions<T> = {
  endpoint: string;
  onSuccess?: (data: T) => void;
  onError?: (error: Error) => void;
  errorDisplayMode?: ErrorDisplayMode;
};

type UseLazyFetchReturn<T> = {
  data: T | undefined;
  loading: boolean;
  error: Error | null;
  execute: () => Promise<T | undefined>;
};

const useLazyFetch = <T>(options: UseLazyFetchOptions<T>): UseLazyFetchReturn<T> => {
  const { endpoint, onSuccess, onError, errorDisplayMode } = options;

  const [data, setData] = useState<T | undefined>(undefined);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const { showErrorToast } = useErrorToast();

  const onSuccessRef = useRef(onSuccess);
  const onErrorRef = useRef(onError);

  const execute = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await api.get<T>(endpoint, { errorDisplayMode });
      setData(result);
      onSuccessRef.current?.(result);
      return result;
    } catch (error) {
      setError(error as Error);
      onErrorRef.current?.(error as Error);

      if (errorDisplayMode === 'toast') {
        showErrorToast(error as Error);
        setError(null);
        return;
      }

      if (errorDisplayMode === 'text') {
        setError(null);
        return;
      }
    } finally {
      setLoading(false);
    }
  }, [endpoint, errorDisplayMode, showErrorToast]);

  if (error) throw error;

  return { data, loading, error, execute };
};

export default useLazyFetch;
