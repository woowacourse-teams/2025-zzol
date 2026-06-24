import { ApiError, NetworkError } from '@/apis/rest/error';
import {
  HTTP_ERROR_MESSAGE,
  NETWORK_ERROR_MESSAGE,
  UNEXPECTED_ERROR_MESSAGE,
} from '@/constants/error';

export type HTTP_ERROR_STATUS = keyof typeof HTTP_ERROR_MESSAGE;

export const getErrorInfo = (error: Error): { message: string; description: string } => {
  if (error instanceof ApiError) {
    if (!HTTP_ERROR_MESSAGE[error.status as HTTP_ERROR_STATUS]) {
      return UNEXPECTED_ERROR_MESSAGE;
    }

    return {
      message: HTTP_ERROR_MESSAGE[error.status as HTTP_ERROR_STATUS].message,
      description: HTTP_ERROR_MESSAGE[error.status as HTTP_ERROR_STATUS].description,
    };
  }

  if (error instanceof NetworkError) {
    return NETWORK_ERROR_MESSAGE;
  }

  return UNEXPECTED_ERROR_MESSAGE;
};
