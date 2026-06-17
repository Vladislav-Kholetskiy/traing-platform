import { toApiError } from './apiError';
import { getEffectiveDemoActorId } from '../../features/auth/model/demoActor';

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';

type RequestBody = BodyInit | Record<string, unknown> | unknown[] | null;

type RequestOptions = {
  body?: RequestBody;
  headers?: HeadersInit;
  signal?: AbortSignal;
};

const baseUrl = import.meta.env.VITE_API_BASE_URL;

function isFormData(value: unknown): value is FormData {
  return typeof FormData !== 'undefined' && value instanceof FormData;
}

function isBodyInitValue(value: unknown): value is BodyInit {
  return (
    typeof value === 'string' ||
    value instanceof Blob ||
    value instanceof URLSearchParams ||
    value instanceof ArrayBuffer ||
    ArrayBuffer.isView(value) ||
    isFormData(value)
  );
}

function buildHeaders(customHeaders?: HeadersInit, body?: RequestBody): Headers {
  const headers = new Headers(customHeaders);
  headers.set('Accept', 'application/json');

  if (body !== undefined && body !== null && !isFormData(body) && !isBodyInitValue(body)) {
    headers.set('Content-Type', 'application/json');
  }

  const demoActorId = getEffectiveDemoActorId();
  if (demoActorId) {
    headers.set('X-Demo-Actor-Id', demoActorId);
  }

  return headers;
}

async function request<T>(method: HttpMethod, path: string, options: RequestOptions = {}): Promise<T> {
  if (!baseUrl) {
    throw new Error('VITE_API_BASE_URL is not configured');
  }

  const hasBody = options.body !== undefined;
  const requestBody = !hasBody
    ? undefined
    : isBodyInitValue(options.body)
      ? options.body
      : JSON.stringify(options.body);
  const response = await fetch(new URL(path, baseUrl).toString(), {
    method,
    headers: buildHeaders(options.headers, options.body),
    body: requestBody,
    signal: options.signal,
  });

  if (!response.ok) {
    throw await toApiError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  if (response.headers.get('content-length') === '0') {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export const apiClient = {
  get: <T>(path: string, options?: Omit<RequestOptions, 'body'>) => request<T>('GET', path, options),
  post: <T>(path: string, body?: RequestBody, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('POST', path, { ...options, body }),
  put: <T>(path: string, body?: RequestBody, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('PUT', path, { ...options, body }),
  patch: <T>(path: string, body?: RequestBody, options?: Omit<RequestOptions, 'body'>) =>
    request<T>('PATCH', path, { ...options, body }),
  delete: <T>(path: string, options?: Omit<RequestOptions, 'body'>) => request<T>('DELETE', path, options),
};
