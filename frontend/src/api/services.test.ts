import axios from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { http, refreshSession } from '@/api/http';
import { authApi, filesApi } from '@/api/services';
import type { FileDetails } from '@/types';

const summary = {
  id: 'user-1',
  firstName: 'Maya',
  lastName: 'Chen',
  email: 'maya@example.com',
  role: 'COMPANY_ADMIN' as const,
  tenantId: 'tenant-1',
  tenantName: 'Aurora Labs',
  emailVerifiedAt: '2026-08-25T10:00:00Z',
};

afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

describe('authentication requests', () => {

  it('shares an in-flight request for the same verification token', async () => {
    let release!: () => void;
    const response = new Promise((resolve) => {
      release = () => resolve({ data: {} });
    });
    const post = vi.spyOn(http, 'post').mockImplementation(() => response as any);

    const first = authApi.verifyEmail('same-token');
    const second = authApi.verifyEmail('same-token');

    expect(post).toHaveBeenCalledTimes(1);
    release();
    await Promise.all([first, second]);

    post.mockResolvedValueOnce({ data: {} });
    await authApi.verifyEmail('same-token');
    expect(post).toHaveBeenCalledTimes(2);
  });

  it('puts a 15-second timeout on the raw refresh request', async () => {
    const post = vi.spyOn(axios, 'post').mockResolvedValue({ data: { accessToken: 'fresh-token', user: summary } });

    await refreshSession();

    expect(post).toHaveBeenCalledWith('/api/v1/auth/refresh', undefined, expect.objectContaining({
      withCredentials: true,
      timeout: 15_000,
    }));
  });

  it('uses the generic resend-verification endpoint', async () => {
    const post = vi.spyOn(http, 'post').mockResolvedValue({ data: {} });

    await authApi.resendVerification('pending@example.com');

    expect(post).toHaveBeenCalledWith('/auth/resend-verification', { email: 'pending@example.com' });
  });
});

describe('file downloads', () => {
  const file: FileDetails = {
    id: 'file-1',
    key: 'tenant-1/file.pdf',
    filename: 'file.pdf',
    contentType: 'application/pdf',
    size: 128,
    createdAt: '2026-08-25T10:00:00Z',
    url: 'http://localhost:8080/api/v1/files/content/tenant-1/file.pdf',
  };

  it('rewrites backend content URLs through the authenticated API client', async () => {
    const expected = new Blob(['local file'], { type: 'application/pdf' });
    const get = vi.spyOn(http, 'get').mockResolvedValue({ data: expected });

    const result = await filesApi.content(file);

    expect(result).toBe(expected);
    expect(get).toHaveBeenCalledWith('/files/content/tenant-1/file.pdf', { responseType: 'blob' });
  });

  it('fetches presigned external URLs without cookies or an Authorization header', async () => {
    const expected = new Blob(['remote file'], { type: 'application/pdf' });
    const fetchMock = vi.fn().mockResolvedValue({ ok: true, blob: vi.fn().mockResolvedValue(expected) });
    vi.stubGlobal('fetch', fetchMock);
    const get = vi.spyOn(http, 'get');

    const result = await filesApi.content({
      ...file,
      url: 'https://objects.example.com/tenant-1/file.pdf?X-Amz-Signature=temporary',
    });

    expect(result).toBe(expected);
    expect(get).not.toHaveBeenCalled();
    expect(fetchMock).toHaveBeenCalledWith(
      expect.objectContaining({ protocol: 'https:', hostname: 'objects.example.com' }),
      { credentials: 'omit', referrerPolicy: 'no-referrer' },
    );
  });
});
