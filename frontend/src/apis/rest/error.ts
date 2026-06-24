export type ErrorDisplayMode = 'fallback' | 'toast' | 'text';

export type ApiErrorParams = {
  status: number;
  message: string;
  data?: unknown;
  displayMode?: ErrorDisplayMode;
};

export type NetworkErrorParams = {
  message: string;
  displayMode?: ErrorDisplayMode;
};

export class ApiError extends Error {
  public status: number;
  public data: unknown;
  public displayMode: ErrorDisplayMode;

  constructor(params: ApiErrorParams) {
    super(params.message);
    this.name = 'ApiError';
    this.status = params.status;
    this.message = params.message;
    this.data = params.data ?? null;
    this.displayMode = params.displayMode ?? 'toast';
  }
}

export class NetworkError extends Error {
  public displayMode: ErrorDisplayMode;

  constructor(params: NetworkErrorParams) {
    super(params.message);
    this.name = 'NetworkError';
    this.message = params.message;
    this.displayMode = params.displayMode ?? 'fallback';
  }
}
