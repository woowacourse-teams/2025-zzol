import { useCallback } from 'react';
import useToast from '@/components/@common/Toast/useToast';
import { getErrorInfo } from '@/utils/errorMessages';
import { ApiError } from './error';

export const useErrorToast = () => {
  const { showToast } = useToast();

  const showErrorToast = useCallback(
    (error: Error): void => {
      const errorMessage = error instanceof ApiError ? error.message : getErrorInfo(error).message;

      showToast({
        type: 'error',
        message: errorMessage,
      });
    },
    [showToast]
  );

  return { showErrorToast };
};
