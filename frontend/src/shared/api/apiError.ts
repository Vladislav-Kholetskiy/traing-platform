export type ApiError = Error & {
  status: number;
  code?: string;
  details?: unknown;
};

type BackendErrorShape = {
  message?: string;
  error?: string;
  code?: string;
  details?: unknown;
  path?: string;
};

export async function toApiError(response: Response): Promise<ApiError> {
  const fallbackMessage = `Request failed with status ${response.status}`;
  let parsedBody: BackendErrorShape | string | null = null;

  try {
    parsedBody = await response.json();
  } catch {
    try {
      parsedBody = await response.text();
    } catch {
      parsedBody = null;
    }
  }

  const message =
    typeof parsedBody === 'string'
      ? parsedBody || fallbackMessage
      : parsedBody?.message ?? parsedBody?.error ?? fallbackMessage;

  const error = new Error(message) as ApiError;
  error.name = 'ApiError';
  error.status = response.status;
  error.code = typeof parsedBody === 'object' && parsedBody ? parsedBody.code : undefined;
  error.details = typeof parsedBody === 'object' && parsedBody ? parsedBody.details ?? parsedBody.path : parsedBody;

  return error;
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return 'Unknown error';
}

export function hasErrorStatus(error: unknown, status: number): error is ApiError {
  return typeof error === 'object' && error !== null && 'status' in error && error.status === status;
}

export function isInteractiveActorResolutionError(error: unknown): error is ApiError {
  if (!error || typeof error !== 'object') {
    return false;
  }

  const message = getErrorMessage(error);
  const isExpectedStatus = hasErrorStatus(error, 401) || hasErrorStatus(error, 403);

  return isExpectedStatus && message.includes('Authenticated principal is required for interactive actor resolution');
}
