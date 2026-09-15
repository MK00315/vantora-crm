import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { afterAll, afterEach, beforeAll, describe, expect, it } from 'vitest';
import { QuickSearch } from './QuickSearch';

const showModal = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'showModal');
const close = Object.getOwnPropertyDescriptor(HTMLDialogElement.prototype, 'close');
beforeAll(() => {
  // jsdom has no top layer; real focus trapping is checked in the browser.
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value() { this.setAttribute('open', ''); } });
  Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value() { this.removeAttribute('open'); } });
});
afterEach(cleanup);
afterAll(() => {
  if (showModal) Object.defineProperty(HTMLDialogElement.prototype, 'showModal', showModal); else Reflect.deleteProperty(HTMLDialogElement.prototype, 'showModal');
  if (close) Object.defineProperty(HTMLDialogElement.prototype, 'close', close); else Reflect.deleteProperty(HTMLDialogElement.prototype, 'close');
});

function Harness() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  return <><button onClick={() => setOpen(true)}>Open search</button><output aria-label="Current route">{location.pathname}{location.search}</output><QuickSearch open={open} onClose={() => setOpen(false)} /></>;
}

async function openSearch() {
  const user = userEvent.setup();
  render(<MemoryRouter initialEntries={['/app']}><Harness /></MemoryRouter>);
  await user.click(screen.getByRole('button', { name: 'Open search' }));
  return user;
}

describe('quick search', () => {
  it('submits an encoded lead search and closes the panel', async () => {
    const user = await openSearch();
    await user.type(screen.getByRole('searchbox'), 'Atlas & Co{Enter}');
    expect(screen.getByLabelText('Current route')).toHaveTextContent('/app/leads?search=Atlas%20%26%20Co');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('filters page shortcuts and opens a matching page', async () => {
    const user = await openSearch();
    await user.type(screen.getByRole('searchbox'), 'settings');
    await user.click(screen.getByRole('button', { name: /Settings Display/ }));
    expect(screen.getByLabelText('Current route')).toHaveTextContent('/app/settings');
  });

  it('supports arrow-key navigation and Escape cancellation', async () => {
    const user = await openSearch();
    const input = screen.getByRole('searchbox');
    await user.click(input);
    await user.keyboard('{ArrowDown}');
    expect(screen.getByRole('button', { name: /Overview Workspace metrics/ })).toHaveFocus();
    await user.keyboard('{ArrowUp}');
    expect(input).toHaveFocus();
    fireEvent(screen.getByRole('dialog'), new Event('cancel', { cancelable: true }));
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Open search' })).toHaveFocus();
    expect(document.body.style.overflow).toBe('');
  });

  it('does not submit a blank query and keeps lead search available without matching pages', async () => {
    const user = await openSearch();
    await user.type(screen.getByRole('searchbox'), '   {Enter}');
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    await user.clear(screen.getByRole('searchbox'));
    await user.type(screen.getByRole('searchbox'), 'unmatched-company');
    expect(screen.getByText(/No matching pages/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Search leads for/ })).toBeInTheDocument();
  });
});
