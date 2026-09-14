import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from '@/auth/ProtectedRoute';

const auth = vi.hoisted(() => ({ loading: false, isAuthenticated: false }));
vi.mock('@/auth/AuthContext', () => ({ useAuth: () => auth }));

function Subject() {
  return <MemoryRouter initialEntries={['/app']}><Routes><Route path="/login" element={<p>Sign in screen</p>} /><Route element={<ProtectedRoute />}><Route path="/app" element={<p>Private workspace</p>} /></Route></Routes></MemoryRouter>;
}

describe('ProtectedRoute', () => {
  beforeEach(() => { auth.loading = false; auth.isAuthenticated = false; });

  it('redirects guests to sign in', () => {
    render(<Subject />);
    expect(screen.getByText('Sign in screen')).toBeInTheDocument();
  });

  it('renders the workspace for an authenticated user', () => {
    auth.isAuthenticated = true;
    render(<Subject />);
    expect(screen.getByText('Private workspace')).toBeInTheDocument();
  });
});
