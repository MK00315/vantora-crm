import { cleanup, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { leadsApi } from '@/api/services';
import { LeadsPage } from './LeadsPage';

vi.mock('@/auth/AuthContext', () => ({ useAuth: () => ({ user: { role: 'COMPANY_ADMIN' } }) }));
vi.mock('@/components/ui/Toast', () => ({ useToast: () => ({ toast: vi.fn() }) }));
vi.mock('@/api/services', () => ({ leadsApi: { list: vi.fn() }, usersApi: { list: vi.fn().mockResolvedValue({ content: [] }) } }));
const lead = { id: 'one', firstName: 'Olivia', lastName: 'Martin', company: 'Atlas Works', stage: 'NEW', estimatedValue: 100 };
beforeEach(() => {
  localStorage.setItem('vantora_leads_view', 'list');
  vi.mocked(leadsApi.list).mockImplementation(async (args) => ({ content: args?.search === 'missing' ? [] : [lead], page: 0, totalPages: 1, totalElements: args?.search === 'missing' ? 0 : 1, size: 35 }) as Awaited<ReturnType<typeof leadsApi.list>>);
});
afterEach(() => { cleanup(); vi.clearAllMocks(); localStorage.removeItem('vantora_leads_view'); });

function renderLeads(url: string) {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter initialEntries={[url]}><LeadsPage /></MemoryRouter></QueryClientProvider>);
}

describe('lead workspace filters', () => {
  it('uses the search passed by the command panel', async () => {
    renderLeads('/app/leads?search=Olivia');
    expect(screen.getByRole('searchbox', { name: 'Search leads' })).toHaveValue('Olivia');
    expect(await screen.findByText('Olivia Martin')).toBeInTheDocument();
    expect(leadsApi.list).toHaveBeenCalledWith(expect.objectContaining({ search: 'Olivia', page: 0 }));
  });

  it('clears empty-result filters and returns to the first page', async () => {
    const user = userEvent.setup();
    renderLeads('/app/leads?search=missing&stage=WON&page=2');
    await user.click(await screen.findByRole('button', { name: 'Reset search and filters' }));
    expect(screen.getByRole('searchbox')).toHaveValue('');
    expect(screen.getByRole('combobox', { name: 'Filter by stage' })).toHaveValue('');
    await waitFor(() => expect(leadsApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ search: '', stage: '', page: 0 })));
    expect(await screen.findByText('Olivia Martin')).toBeInTheDocument();
  });
});
