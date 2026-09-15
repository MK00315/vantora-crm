import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, GripVertical, KanbanSquare, LayoutList, Mail, MoreHorizontal, Pencil, Plus, Trash2, UserRound } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useSearchParams } from 'react-router-dom';
import { leadsApi, usersApi } from '@/api/services';
import { invalidateWorkspaceData } from '@/app/queryClient';
import { useAuth } from '@/auth/AuthContext';
import { Avatar, Badge, Button, Card, ConfirmDialog, EmptyState, ErrorState, Field, Input, Modal, PageHeader, PageSkeleton, Pagination, SearchInput, Select, Textarea } from '@/components/ui';
import { useToast } from '@/components/ui/Toast';
import { canDeleteRecords, canManage, LEAD_STAGES } from '@/lib/constants';
import { cn, formatCurrency, getApiMessage, personName } from '@/lib/utils';
import type { Lead, LeadStage, User } from '@/types';

const leadSchema = z.object({
  firstName: z.string().min(2, 'Enter a first name'),
  lastName: z.string().min(2, 'Enter a last name'),
  company: z.string().optional(),
  title: z.string().optional(),
  email: z.union([z.literal(''), z.email('Enter a valid email')]).optional(),
  phone: z.string().optional(),
  stage: z.enum(['NEW', 'CONTACTED', 'QUALIFIED', 'PROPOSAL', 'NEGOTIATION', 'WON', 'LOST']),
  estimatedValue: z.number().min(0, 'Value cannot be negative').optional(),
  source: z.string().optional(),
  ownerId: z.string().optional(),
  notes: z.string().max(5000).optional(),
});
type LeadFormValues = z.infer<typeof leadSchema>;

function displayName(lead: Lead) { return `${lead.firstName} ${lead.lastName}`.trim(); }

function LeadForm({ lead, onDone }: { lead?: Lead | null; onDone: () => void }) {
  const { toast } = useToast();
  const users = useQuery({ queryKey: ['users', 'lead-form'], queryFn: () => usersApi.list({ size: 100 }) });
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<LeadFormValues>({ resolver: zodResolver(leadSchema), defaultValues: lead ? { ...lead } : { firstName: '', lastName: '', company: '', title: '', email: '', phone: '', stage: 'NEW', estimatedValue: 0, source: '', ownerId: '', notes: '' } });
  const submit = async (values: LeadFormValues) => {
    try { if (lead) await leadsApi.update(lead.id, values); else await leadsApi.create(values); await invalidateWorkspaceData('leads'); toast(lead ? 'Lead updated' : 'Lead added', { description: `${values.firstName} ${values.lastName} is now in ${LEAD_STAGES.find((s) => s.value === values.stage)?.label}.` }); onDone(); }
    catch (error) { toast('Could not save lead', { kind: 'error', description: getApiMessage(error) }); }
  };
  return <form onSubmit={handleSubmit(submit)} className="space-y-5"><div className="grid gap-4 sm:grid-cols-2"><Field label="First name" error={errors.firstName?.message}><Input autoFocus placeholder="Olivia" {...register('firstName')} /></Field><Field label="Last name" error={errors.lastName?.message}><Input placeholder="Martin" {...register('lastName')} /></Field><Field label="Company"><Input placeholder="Atlas Works" {...register('company')} /></Field><Field label="Job title"><Input placeholder="VP of Revenue" {...register('title')} /></Field><Field label="Email" error={errors.email?.message}><Input type="email" placeholder="olivia@company.com" {...register('email')} /></Field><Field label="Phone"><Input type="tel" placeholder="+1 555 0100" {...register('phone')} /></Field><Field label="Pipeline stage"><Select {...register('stage')}>{LEAD_STAGES.map((stage) => <option key={stage.value} value={stage.value}>{stage.label}</option>)}</Select></Field><Field label="Estimated value" error={errors.estimatedValue?.message}><Input type="number" min="0" step="100" {...register('estimatedValue', { setValueAs: (value) => value === '' ? undefined : Number(value) })} /></Field><Field label="Lead source"><Input placeholder="Referral, website, event…" {...register('source')} /></Field><Field label="Owner" hint={users.data && users.data.totalElements > users.data.content.length ? `Showing the newest ${users.data.content.length} of ${users.data.totalElements} members.` : undefined}><Select {...register('ownerId')}><option value="">Unassigned</option>{users.data?.content.filter((member) => member.status !== 'SUSPENDED').map((member) => <option key={member.id} value={member.id}>{personName(member)}</option>)}</Select></Field><Field label="Notes" className="sm:col-span-2"><Textarea placeholder="Context, requirements, next steps…" {...register('notes')} /></Field></div><div className="flex justify-end gap-3 border-t border-ink-100 pt-5 dark:border-ink-800"><Button variant="secondary" onClick={onDone}>Cancel</Button><Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Saving…' : lead ? 'Save changes' : 'Add lead'}</Button></div></form>;
}

function LeadCard({ lead, owner, canEdit, canDelete, onEdit, onDelete }: { lead: Lead; owner?: User; canEdit: boolean; canDelete: boolean; onEdit: (lead: Lead) => void; onDelete: (lead: Lead) => void }) {
  const [menu, setMenu] = useState(false); const name = displayName(lead);
  return <article draggable={canEdit} onDragStart={(e) => { if (!canEdit) return; e.dataTransfer.setData('application/x-vantora-lead', lead.id); e.dataTransfer.effectAllowed = 'move'; }} className={cn('group rounded-xl border border-ink-200 bg-white p-4 shadow-soft transition hover:border-brand-300 dark:border-ink-700 dark:bg-ink-900', canEdit && 'cursor-grab active:cursor-grabbing')}><div className="flex items-start gap-2">{canEdit && <GripVertical className="mt-0.5 h-4 w-4 shrink-0 text-ink-300 opacity-0 transition group-hover:opacity-100" />}<div className="min-w-0 flex-1"><h3 className="truncate text-sm font-bold text-ink-900 dark:text-white">{name}</h3><p className="mt-1 flex items-center gap-1.5 truncate text-xs text-ink-500"><Building2 className="h-3 w-3" />{lead.company || 'Independent'}</p></div>{(canEdit || canDelete) && <div className="relative"><button onClick={() => setMenu((v) => !v)} className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-800" aria-label={`Actions for ${name}`}><MoreHorizontal className="h-4 w-4" /></button>{menu && <div className="absolute right-0 top-7 z-20 w-32 rounded-xl border border-ink-200 bg-white p-1 shadow-lift dark:border-ink-700 dark:bg-ink-900">{canEdit && <button onClick={() => { onEdit(lead); setMenu(false); }} className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs text-ink-700 hover:bg-ink-50 dark:text-ink-200 dark:hover:bg-ink-800"><Pencil className="h-3.5 w-3.5" />Edit</button>}{canDelete && <button onClick={() => { onDelete(lead); setMenu(false); }} className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950"><Trash2 className="h-3.5 w-3.5" />Delete</button>}</div>}</div>}</div><div className="mt-4"><span className="font-display text-sm font-bold text-ink-900 dark:text-white">{formatCurrency(lead.estimatedValue)}</span>{lead.title && <p className="mt-1 truncate text-xs text-ink-400">{lead.title}</p>}</div><div className="mt-4 flex items-center gap-2 border-t border-ink-100 pt-3 dark:border-ink-800"><Avatar size="sm" firstName={owner?.firstName} lastName={owner?.lastName} /><span className="truncate text-xs text-ink-400">{personName(owner)}</span></div></article>;
}

function Kanban({ leads, users, canEdit, canDelete, onStage, onEdit, onDelete }: { leads: Lead[]; users: User[]; canEdit: boolean; canDelete: boolean; onStage: (id: string, stage: LeadStage) => void; onEdit: (lead: Lead) => void; onDelete: (lead: Lead) => void }) {
  const [over, setOver] = useState<LeadStage | null>(null); const owner = (id?: string) => users.find((user) => user.id === id);
  return <div className="-mx-4 overflow-x-auto px-4 pb-4 sm:-mx-6 sm:px-6 lg:-mx-8 lg:px-8" tabIndex={0} role="region" aria-label="Lead pipeline board"><div className="grid min-w-[1380px] grid-cols-7 gap-4">{LEAD_STAGES.map((stage) => { const items = leads.filter((lead) => lead.stage === stage.value); return <section key={stage.value} onDragOver={(e) => { if (canEdit) { e.preventDefault(); setOver(stage.value); } }} onDragLeave={(e) => { if (!e.currentTarget.contains(e.relatedTarget as Node)) setOver(null); }} onDrop={(e) => { e.preventDefault(); const id = e.dataTransfer.getData('application/x-vantora-lead'); if (id && canEdit) onStage(id, stage.value); setOver(null); }} className={cn('min-h-[28rem] rounded-xl border border-ink-200/80 bg-ink-100/55 p-3 transition dark:border-ink-800 dark:bg-ink-900/40', over === stage.value && 'border-brand-400 bg-brand-50 ring-2 ring-brand-400/20 dark:bg-brand-950/30')}><div className="mb-3 flex items-center justify-between px-1"><div className="flex items-center gap-2"><span className={cn('h-2.5 w-2.5 rounded-full', stage.color)} /><h2 className="text-xs font-bold text-ink-800 dark:text-ink-100">{stage.label}</h2><span className="rounded-full bg-white px-1.5 py-0.5 text-xs font-bold text-ink-500 dark:bg-ink-800">{items.length}</span></div></div><p className="mb-3 px-1 text-xs font-semibold text-ink-400">{formatCurrency(items.reduce((sum, lead) => sum + (lead.estimatedValue ?? 0), 0))}</p><div className="space-y-3">{items.map((lead) => <LeadCard key={lead.id} lead={lead} owner={owner(lead.ownerId)} canEdit={canEdit} canDelete={canDelete} onEdit={onEdit} onDelete={onDelete} />)}{!items.length && <div className="grid h-28 place-items-center rounded-xl border border-dashed border-ink-300 text-xs text-ink-400 dark:border-ink-700">{canEdit ? 'Drop leads here' : 'No leads'}</div>}</div></section>; })}</div></div>;
}

function LeadsTable({ leads, users, canEdit, canDelete, onEdit, onDelete }: { leads: Lead[]; users: User[]; canEdit: boolean; canDelete: boolean; onEdit: (lead: Lead) => void; onDelete: (lead: Lead) => void }) {
  return <div className="table-shell overflow-x-auto" tabIndex={0} role="region" aria-label="Lead records"><table className="data-table min-w-[820px]"><thead><tr><th>Contact</th><th>Stage</th><th>Value</th><th>Owner</th><th>Source</th>{(canEdit || canDelete) && <th className="w-24">Actions</th>}</tr></thead><tbody>{leads.map((lead) => { const stage = LEAD_STAGES.find((item) => item.value === lead.stage); const owner = users.find((user) => user.id === lead.ownerId); const name = displayName(lead); return <tr key={lead.id}><td><div className="flex items-center gap-3"><div className="grid h-9 w-9 place-items-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-950"><UserRound className="h-4 w-4" /></div><div><p className="font-semibold text-ink-900 dark:text-white">{name}</p><p className="mt-0.5 flex items-center gap-1 text-xs text-ink-400"><Mail className="h-3 w-3" />{lead.email || 'No email'}</p></div></div></td><td><Badge tone={lead.stage === 'WON' ? 'success' : lead.stage === 'LOST' ? 'danger' : 'brand'}><span className={cn('mr-1.5 h-1.5 w-1.5 rounded-full', stage?.color)} />{stage?.label}</Badge></td><td className="font-semibold">{formatCurrency(lead.estimatedValue)}</td><td><div className="flex items-center gap-2"><Avatar size="sm" firstName={owner?.firstName} lastName={owner?.lastName} /><span className="text-xs">{personName(owner)}</span></div></td><td>{lead.source || '—'}</td>{(canEdit || canDelete) && <td><div className="flex gap-1">{canEdit && <Button size="sm" variant="ghost" onClick={() => onEdit(lead)} aria-label={`Edit ${name}`}><Pencil className="h-4 w-4" /></Button>}{canDelete && <Button size="sm" variant="ghost" className="text-rose-600" onClick={() => onDelete(lead)} aria-label={`Delete ${name}`}><Trash2 className="h-4 w-4" /></Button>}</div></td>}</tr>; })}</tbody></table></div>;
}

export function LeadsPage() {
  const [view, setView] = useState<'board' | 'list'>(() => {
    try { return localStorage.getItem('vantora_leads_view') === 'list' ? 'list' : 'board'; } catch { return 'board'; }
  });
  const [params, setParams] = useSearchParams();
  const search = params.get('search') ?? '';
  const stage = LEAD_STAGES.some((item) => item.value === params.get('stage')) ? params.get('stage')! : '';
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const updateFilter = (key: 'search' | 'stage', value: string) => setParams((current) => {
    const next = new URLSearchParams(current);
    if (value) next.set(key, value); else next.delete(key);
    next.delete('page');
    return next;
  }, { replace: true, preventScrollReset: true });
  const setPage = (nextPage: number) => setParams((current) => {
    const next = new URLSearchParams(current);
    if (nextPage > 0) next.set('page', String(nextPage)); else next.delete('page');
    return next;
  }, { replace: true, preventScrollReset: true });
  const clearFilters = () => setParams((current) => {
    const next = new URLSearchParams(current);
    ['search', 'stage', 'page'].forEach((key) => next.delete(key));
    return next;
  }, { replace: true, preventScrollReset: true });
  const [editing, setEditing] = useState<Lead | null | undefined>(undefined); const [deleting, setDeleting] = useState<Lead | null>(null);
  const { user } = useAuth(); const { toast } = useToast(); const queryClient = useQueryClient(); const editable = canManage(user?.role); const deletable = canDeleteRecords(user?.role);
  const query = useQuery({ queryKey: ['leads', page, search, stage], queryFn: () => leadsApi.list({ page, search, stage, size: 35, sort: 'updatedAt', direction: 'desc' }) });
  const users = useQuery({ queryKey: ['users', 'lead-owners'], queryFn: () => usersApi.list({ size: 100 }) });
  const stageMutation = useMutation({ mutationFn: ({ id, next }: { id: string; next: LeadStage }) => leadsApi.updateStage(id, next), onMutate: async ({ id, next }) => { await queryClient.cancelQueries({ queryKey: ['leads'] }); const snapshots = queryClient.getQueriesData({ queryKey: ['leads'] }); queryClient.setQueriesData({ queryKey: ['leads'] }, (old: any) => old ? { ...old, content: old.content.map((lead: Lead) => lead.id === id ? { ...lead, stage: next } : lead) } : old); return { snapshots }; }, onError: (error, _variables, context) => { context?.snapshots.forEach(([key, value]) => queryClient.setQueryData(key, value)); toast('Stage update failed', { kind: 'error', description: getApiMessage(error) }); }, onSettled: () => invalidateWorkspaceData('leads') });
  const remove = useMutation({ mutationFn: (id: string) => leadsApi.remove(id), onSuccess: async () => { if (page > 0 && query.data?.content.length === 1) setPage(Math.max(0, page - 1)); await invalidateWorkspaceData('leads'); toast('Lead deleted'); setDeleting(null); }, onError: (error) => toast('Could not delete lead', { kind: 'error', description: getApiMessage(error) }) });
  const leads = useMemo(() => query.data?.content ?? [], [query.data]); const members = users.data?.content ?? [];
  const chooseView = (next: 'board' | 'list') => { setView(next); try { localStorage.setItem('vantora_leads_view', next); } catch { /* The selected view still works for this visit. */ } };
  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Sales workspace" title="Lead pipeline" description="Manage contacts, track deal stages, and plan your next conversation." actions={editable ? <Button onClick={() => setEditing(null)}><Plus className="h-4 w-4" />Add lead</Button> : undefined} />
      <Card className="flex flex-col gap-3 p-3 sm:flex-row sm:items-center">
        <SearchInput aria-label="Search leads" value={search} onChange={(event) => { updateFilter('search', event.target.value); }} placeholder="Search name, company, email…" className="flex-1" />
        <Select aria-label="Filter by stage" value={stage} onChange={(event) => { updateFilter('stage', event.target.value); }} className="sm:w-44"><option value="">All stages</option>{LEAD_STAGES.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}</Select>
        <div role="group" aria-label="Lead view" className="flex shrink-0 rounded-lg bg-ink-100 p-1 dark:bg-ink-800"><button aria-pressed={view === 'board'} onClick={() => chooseView('board')} className={cn('flex h-9 items-center gap-1.5 rounded-lg px-3 text-xs font-semibold', view === 'board' ? 'bg-white text-ink-900 shadow-sm dark:bg-ink-700 dark:text-white' : 'text-ink-500')}><KanbanSquare className="h-4 w-4" />Board</button><button aria-pressed={view === 'list'} onClick={() => chooseView('list')} className={cn('flex h-9 items-center gap-1.5 rounded-lg px-3 text-xs font-semibold', view === 'list' ? 'bg-white text-ink-900 shadow-sm dark:bg-ink-700 dark:text-white' : 'text-ink-500')}><LayoutList className="h-4 w-4" />List</button></div>
      </Card>
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
        <p className="text-ink-500" role="status">{query.isFetching ? 'Updating results…' : query.isError ? 'Results unavailable' : `Showing ${leads.length} of ${query.data?.totalElements ?? 0} leads`}{view === 'board' && editable && !query.isFetching && <span className="ml-2 hidden text-ink-400 md:inline">· Drag cards to change stage</span>}</p>
        {(search || stage) && <div className="flex min-w-0 flex-wrap items-center gap-2"><span className="text-ink-500">Filtered by</span>{search && <span className="max-w-44 truncate rounded-md border border-ink-200 bg-white px-2 py-1" title={search}>“{search}”</span>}{stage && <span className="rounded-md border border-ink-200 bg-white px-2 py-1">{LEAD_STAGES.find((item) => item.value === stage)?.label}</span>}<button type="button" onClick={clearFilters} className="rounded px-2 py-1 font-semibold text-brand-700 hover:bg-brand-50">Clear filters</button></div>}
      </div>
      {query.isLoading ? <PageSkeleton /> : query.isError ? <ErrorState onRetry={() => void query.refetch()} /> : !leads.length ? <Card><EmptyState title="No leads found" description={search || stage ? 'Try a different search or clear your filters to see all leads.' : 'Add your first lead to start building your pipeline.'} action={search || stage ? <Button variant="secondary" onClick={clearFilters}>Reset search and filters</Button> : editable ? <Button onClick={() => setEditing(null)}><Plus className="h-4 w-4" />Add lead</Button> : undefined} /></Card> : view === 'board' ? <Kanban leads={leads} users={members} canEdit={editable} canDelete={deletable} onStage={(id, next) => stageMutation.mutate({ id, next })} onEdit={(lead) => setEditing(lead)} onDelete={setDeleting} /> : <LeadsTable leads={leads} users={members} canEdit={editable} canDelete={deletable} onEdit={(lead) => setEditing(lead)} onDelete={setDeleting} />}
      {query.data && query.data.totalPages > 1 && <Card className="overflow-hidden"><Pagination page={query.data.page} totalPages={query.data.totalPages} onChange={setPage} /></Card>}
      <Modal open={editing !== undefined} onClose={() => setEditing(undefined)} title={editing ? 'Edit lead' : 'Add a new lead'} description="Keep contact and pipeline details clear for your whole team."><LeadForm lead={editing} onDone={() => setEditing(undefined)} /></Modal>
      <ConfirmDialog open={Boolean(deleting)} onClose={() => setDeleting(null)} onConfirm={() => deleting && remove.mutate(deleting.id)} busy={remove.isPending} title="Delete this lead?" description={`${deleting ? displayName(deleting) : 'This lead'} will be permanently removed from the pipeline. This action cannot be undone.`} />
    </div>
  );
}
