import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios';
import type { AuthResponse } from '@/types';

const baseURL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';
let accessToken: string | null = null;
let refreshPromise: Promise<string> | null = null;
let sessionRefreshPromise: Promise<AuthResponse> | null = null;

export const http = axios.create({
  baseURL,
  withCredentials: true,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
});

export function getAccessToken() {
  return accessToken;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
}

async function requestSessionRefresh(): Promise<AuthResponse> {
  const { data } = await axios.post<AuthResponse>(`${baseURL}/auth/refresh`, undefined, {
    withCredentials: true,
    timeout: 15_000,
  });
  setAccessToken(data.accessToken);
  return data;
}

export function refreshSession(): Promise<AuthResponse> {
  if (!sessionRefreshPromise) {
    const request = typeof navigator !== 'undefined' && navigator.locks
      ? navigator.locks.request('vantora-refresh-session', requestSessionRefresh)
      : requestSessionRefresh();
    sessionRefreshPromise = request.finally(() => {
      sessionRefreshPromise = null;
    });
  }
  return sessionRefreshPromise;
}

http.interceptors.request.use((config) => {
  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`;
  return config;
});

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean };

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as RetriableConfig | undefined;
    const isAuthCall = original?.url?.includes('/auth/refresh') || original?.url?.includes('/auth/login');

    if (error.response?.status !== 401 || !original || original._retry || isAuthCall) {
      return Promise.reject(error);
    }

    original._retry = true;
    try {
      refreshPromise ??= refreshSession()
        .then((data) => data.accessToken)
        .finally(() => {
          refreshPromise = null;
        });

      const token = await refreshPromise;
      original.headers.Authorization = `Bearer ${token}`;
      return http(original);
    } catch (refreshError) {
      setAccessToken(null);
      window.dispatchEvent(new CustomEvent('vantora:session-expired'));
      return Promise.reject(refreshError);
    }
  },
);
