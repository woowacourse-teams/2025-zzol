import { useCallback, useEffect, useRef, useState } from 'react';
import { api } from './api';
import { ErrorDisplayMode } from './error';
import { useErrorToast } from './useErrorToast';

type UseFetchOptions<T> = {
  endpoint: string;
  enabled?: boolean;
  onSuccess?: (data: T) => void;
  onError?: (error: Error) => void;
  errorDisplayMode?: ErrorDisplayMode;
};

type UseFetchReturn<T> = {
  data: T | undefined;
  loading: boolean;
  error: Error | null;
  refetch: () => Promise<void>;
};

const useFetch = <T>(options: UseFetchOptions<T>): UseFetchReturn<T> => {
  const { endpoint, enabled = true, onSuccess, onError, errorDisplayMode } = options;

  const [data, setData] = useState<T | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);
  const { showErrorToast } = useErrorToast();

  const onSuccessRef = useRef(onSuccess);
  const onErrorRef = useRef(onError);

  const fetchData = useCallback(async () => {
    if (!enabled) return;

    try {
      setLoading(true);
      setError(null);
      const result = await api.get<T>(endpoint, { errorDisplayMode });
      setData(result);
      onSuccessRef.current?.(result);
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
  }, [endpoint, enabled, errorDisplayMode, showErrorToast]);

  useEffect(() => {
    if (enabled) {
      fetchData();
    }
  }, [enabled, fetchData]);

  if (error) throw error;

  return { data, loading, error, refetch: fetchData };
};

export default useFetch;
