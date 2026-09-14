import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { authApi } from '@/api/services';
import { VerifyEmailPage } from '@/pages/auth/AuthPages';

afterEach(() => {
  vi.restoreAllMocks();
  window.history.replaceState({}, '', '/');
});

function renderVerification() {
  return render(<MemoryRouter><VerifyEmailPage /></MemoryRouter>);
}

describe('one-time auth links', () => {
  it('reads a verification token from the URL fragment and scrubs it', async () => {
    window.history.replaceState({}, '', '/verify-email#token=fragment-secret');
    const verify = vi.spyOn(authApi, 'verifyEmail').mockResolvedValue(undefined);

    renderVerification();

    await waitFor(() => expect(verify).toHaveBeenCalledWith('fragment-secret'));
    expect(window.location.hash).toBe('');
    expect(await screen.findByText('You’re verified')).toBeInTheDocument();
  });

  it('supports a legacy query token while removing it from the address bar', async () => {
    window.history.replaceState({}, '', '/verify-email?token=legacy-secret&source=email');
    const verify = vi.spyOn(authApi, 'verifyEmail').mockResolvedValue(undefined);

    renderVerification();

    await waitFor(() => expect(verify).toHaveBeenCalledWith('legacy-secret'));
    expect(window.location.search).toBe('?source=email');
  });
});
