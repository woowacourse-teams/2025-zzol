import { useCallback, useRef, useState } from 'react';
import { api } from './api';
import { ErrorDisplayMode } from './error';
import { Method } from './apiRequest';
import { useErrorToast } from './useErrorToast';

type UseMutationOptions<TData, TVariables> = {
  endpoint: string;
  method: Method;
  onSuccess?: (data: TData, variables: TVariables) => void;
  onError?: (error: Error, variables: TVariables) => void;
  errorDisplayMode: ErrorDisplayMode;
};

type UseMutationReturn<TData, TVariables> = {
  data: TData | undefined;
  loading: boolean;
  error: Error | null;
  mutate: (variables: TVariables) => Promise<TData | undefined>;
  reset: () => void;
};

const useMutation = <TData = unknown, TVariables = void>(
  options: UseMutationOptions<TData, TVariables>
): UseMutationReturn<TData, TVariables> => {
  const { endpoint, method, onSuccess, onError, errorDisplayMode } = options;

  const [data, setData] = useState<TData | undefined>(undefined);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<Error | null>(null);
  const { showErrorToast } = useErrorToast();

  const onSuccessRef = useRef(onSuccess);
  const onErrorRef = useRef(onError);

  onSuccessRef.current = onSuccess;
  onErrorRef.current = onError;

  const mutate = useCallback(
    async (variables: TVariables): Promise<TData | undefined> => {
      try {
        setLoading(true);
        setError(null);

        let result: TData;

        switch (method) {
          case 'DELETE':
            result = await api.delete<TData>(endpoint, { errorDisplayMode });
            break;
          case 'POST':
            result = await api.post<TData, TVariables>(endpoint, variables, { errorDisplayMode });
            break;
          case 'PUT':
            result = await api.put<TData, TVariables>(endpoint, variables, { errorDisplayMode });
            break;
          case 'PATCH':
            result = await api.patch<TData, TVariables>(endpoint, variables, { errorDisplayMode });
            break;
          default:
            throw new Error(`지원되지 않는 타입입니다. (${method})`);
        }

        setData(result);
        onSuccessRef.current?.(result, variables);

        return result;
      } catch (err) {
        const error = err as Error;
        setError(error);
        onErrorRef.current?.(error, variables);

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
    },
    [endpoint, method, errorDisplayMode, showErrorToast]
  );

  const reset = useCallback(() => {
    setData(undefined);
    setError(null);
    setLoading(false);
  }, []);

  if (error) throw error;

  return { data, loading, error, mutate, reset };
};

export default useMutation;
