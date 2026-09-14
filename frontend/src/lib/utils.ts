import { clsx, type ClassValue } from 'clsx';
import { format, formatDistanceToNow, isPast, parseISO } from 'date-fns';

export function cn(...inputs: ClassValue[]) {
  return clsx(inputs);
}

export function formatCurrency(value?: number, currency = 'USD') {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    notation: Math.abs(value ?? 0) >= 1_000_000 ? 'compact' : 'standard',
    maximumFractionDigits: 0,
  }).format(value ?? 0);
}

export function formatDate(value?: string, pattern = 'MMM d, yyyy') {
  if (!value) return '—';
  try {
    return format(parseISO(value), pattern);
  } catch {
    return '—';
  }
}

export function relativeTime(value?: string) {
  if (!value) return '';
  try {
    return formatDistanceToNow(parseISO(value), { addSuffix: true });
  } catch {
    return '';
  }
}

export function isOverdue(value?: string) {
  if (!value) return false;
  try {
    return isPast(parseISO(value));
  } catch {
    return false;
  }
}

/** Converts a UTC/offset timestamp into the wall-clock value expected by datetime-local. */
export function toDateTimeLocal(value?: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return local.toISOString().slice(0, 16);
}

/** Converts a datetime-local wall-clock value into the UTC ISO timestamp used by the API. */
export function fromDateTimeLocal(value?: string) {
  if (!value) return undefined;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? undefined : date.toISOString();
}

export function safeHttpUrl(value?: string) {
  if (!value) return undefined;
  try {
    const url = new URL(value);
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : undefined;
  } catch {
    return undefined;
  }
}

export function initials(firstName?: string, lastName?: string) {
  return `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase() || '?';
}

export function personName(person?: { firstName?: string; lastName?: string }) {
  return [person?.firstName, person?.lastName].filter(Boolean).join(' ') || 'Unassigned';
}

export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export function getApiMessage(error: unknown, fallback = 'Something went wrong. Please try again.') {
  if (typeof error === 'object' && error && 'response' in error) {
    const response = (error as { response?: { data?: { message?: string; detail?: string } } }).response;
    return response?.data?.message ?? response?.data?.detail ?? fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
}
