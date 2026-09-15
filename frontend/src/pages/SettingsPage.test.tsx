import { act, cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { PREFERENCES_KEY, updatePreferences } from '@/app/preferences';
import { ThemeProvider } from '@/app/ThemeProvider';
import { SettingsPage } from './SettingsPage';

function renderSettings() {
  return render(<MemoryRouter><ThemeProvider><SettingsPage /></ThemeProvider></MemoryRouter>);
}

describe('workspace display preferences', () => {
  beforeEach(() => {
    localStorage.removeItem(PREFERENCES_KEY);
    updatePreferences({ density: 'comfortable', reduceMotion: false });
  });
  afterEach(() => { cleanup(); vi.restoreAllMocks(); });

  it('saves density and motion choices and applies them to the document', async () => {
    const user = userEvent.setup();
    const view = renderSettings();
    await user.click(screen.getByRole('radio', { name: 'Compact' }));
    await user.click(screen.getByRole('switch', { name: 'Reduce motion' }));
    expect(document.documentElement.dataset.density).toBe('compact');
    expect(document.documentElement.dataset.reduceMotion).toBe('true');
    expect(JSON.parse(localStorage.getItem(PREFERENCES_KEY)!)).toEqual({ density: 'compact', reduceMotion: true });
    view.unmount();
    renderSettings();
    expect(screen.getByRole('radio', { name: 'Compact' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Reduce motion' })).toHaveAttribute('aria-checked', 'true');
  });

  it('keeps controls working and explains when browser storage is blocked', () => {
    renderSettings();
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new Error('Storage blocked'); });
    fireEvent.click(screen.getByRole('radio', { name: 'Compact' }));
    expect(document.documentElement.dataset.density).toBe('compact');
    expect(screen.getByRole('status')).toHaveTextContent('Applied for this visit');
  });

  it('handles invalid stored preferences and updates from another tab', () => {
    renderSettings();
    act(() => {
      localStorage.setItem(PREFERENCES_KEY, 'invalid JSON');
      window.dispatchEvent(new StorageEvent('storage', { key: PREFERENCES_KEY }));
    });
    expect(screen.getByRole('radio', { name: 'Comfortable' })).toBeChecked();
    act(() => {
      localStorage.setItem(PREFERENCES_KEY, JSON.stringify({ density: 'compact', reduceMotion: true }));
      window.dispatchEvent(new StorageEvent('storage', { key: PREFERENCES_KEY }));
    });
    expect(screen.getByRole('radio', { name: 'Compact' })).toBeChecked();
    expect(screen.getByRole('switch', { name: 'Reduce motion' })).toHaveAttribute('aria-checked', 'true');
  });

  it('links to the existing profile and omits deployment configuration', () => {
    renderSettings();
    expect(screen.getByRole('link', { name: /Profile & security/ })).toHaveAttribute('href', '/app/profile');
    expect(screen.queryByText('Platform services')).not.toBeInTheDocument();
    expect(screen.queryByText('Email notifications')).not.toBeInTheDocument();
  });
});
