import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Clock3, LockKeyhole, Mail, MoreHorizontal, Pencil, Plus, ShieldCheck, UserMinus, UsersRound } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { usersApi } from '@/api/services';
import { invalidateWorkspaceData } from '@/app/queryClient';
import { useAuth } from '@/auth/AuthContext';
import {
  Avatar,
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  ErrorState,
  Field,
  Input,
  Modal,
  PageHeader,
  PageSkeleton,
  Pagination,
  SearchInput,
  Select,
} from '@/components/ui';
import { useToast } from '@/components/ui/Toast';
import { canManageTeamMember, canManageUsers, ROLE_LABELS } from '@/lib/constants';
import { formatDate, getApiMessage, personName, relativeTime } from '@/lib/utils';
import type { Role, User, UserStatus } from '@/types';

const standardRoles: Role[] = ['COMPANY_ADMIN', 'HR', 'RECRUITER', 'EMPLOYEE'];
const allRoles: Role[] = ['SUPER_ADMIN', ...standardRoles];
const inviteSchema = z.object({
  firstName: z.string().min(2, 'Enter a first name'),
  lastName: z.string().min(2, 'Enter a last name'),
  email: z.email('Enter a valid email'),
  role: z.enum(['SUPER_ADMIN', 'COMPANY_ADMIN', 'HR', 'RECRUITER', 'EMPLOYEE']),
});
const editSchema = z.object({
  firstName: z.string().min(2, 'Enter a first name'),
  lastName: z.string().min(2, 'Enter a last name'),
  role: z.enum(['SUPER_ADMIN', 'COMPANY_ADMIN', 'HR', 'RECRUITER', 'EMPLOYEE']),
  status: z.enum(['INVITED', 'ACTIVE', 'SUSPENDED']),
});

function MemberForm({ member, onDone }: { member?: User | null; onDone: () => void }) {
  const { user: actor } = useAuth();
  const { toast } = useToast();
  const editing = Boolean(member);
  const assignableRoles = actor?.role === 'SUPER_ADMIN' ? allRoles : standardRoles;
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<any>({
    resolver: zodResolver(editing ? editSchema : inviteSchema),
    defaultValues: member
      ? { firstName: member.firstName, lastName: member.lastName, role: member.role, status: member.status ?? 'ACTIVE' }
      : { firstName: '', lastName: '', email: '', role: 'RECRUITER' },
  });

  const submit = async (values: any) => {
    if (!canManageTeamMember(actor?.role, member?.role) || (values.role === 'SUPER_ADMIN' && actor?.role !== 'SUPER_ADMIN')) {
      toast('This account is protected', { kind: 'error', description: 'Only a platform administrator can manage the platform-admin role.' });
      return;
    }
    try {
      if (member) await usersApi.update(member.id, values);
      else await usersApi.invite(values);
      await invalidateWorkspaceData('users');
      toast(member ? 'Team member updated' : 'Invitation sent', {
        description: member ? undefined : `${values.firstName} will receive a secure account setup email.`,
      });
      onDone();
    } catch (error) {
      toast(member ? 'Could not update member' : 'Could not send invitation', { kind: 'error', description: getApiMessage(error) });
    }
  };

  return (
    <form onSubmit={handleSubmit(submit)} className="space-y-5">
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="First name" error={errors.firstName?.message as string | undefined}><Input autoFocus {...register('firstName')} /></Field>
        <Field label="Last name" error={errors.lastName?.message as string | undefined}><Input {...register('lastName')} /></Field>
        {!editing && <Field label="Work email" className="sm:col-span-2" error={errors.email?.message as string | undefined}><Input type="email" placeholder="teammate@company.com" {...register('email')} /></Field>}
        <Field label="Role" className={editing ? '' : 'sm:col-span-2'} error={errors.role?.message as string | undefined}>
          <Select {...register('role')}>{assignableRoles.map((role) => <option value={role} key={role}>{ROLE_LABELS[role]}</option>)}</Select>
        </Field>
        {editing && <Field label="Account status"><Select {...register('status')}><option value="ACTIVE">Active</option><option value="INVITED">Invited</option><option value="SUSPENDED">Suspended</option></Select></Field>}
      </div>
      <div className="rounded-xl bg-ink-50 p-3 text-xs leading-5 text-ink-500 dark:bg-ink-800/70 dark:text-ink-300"><ShieldCheck className="mr-1 inline h-3.5 w-3.5 text-brand-600" />Company admins manage team access; platform-admin accounts remain protected from non-platform administrators.</div>
      <div className="flex justify-end gap-3 border-t border-ink-100 pt-5 dark:border-ink-800"><Button variant="secondary" onClick={onDone}>Cancel</Button><Button type="submit" disabled={isSubmitting}>{isSubmitting ? 'Saving…' : member ? 'Save changes' : 'Send invitation'}</Button></div>
    </form>
  );
}

function RoleBadge({ role }: { role: Role }) {
  return <Badge tone={role === 'SUPER_ADMIN' ? 'danger' : role === 'COMPANY_ADMIN' ? 'brand' : role === 'HR' ? 'info' : role === 'RECRUITER' ? 'warning' : 'neutral'}>{ROLE_LABELS[role]}</Badge>;
}

function StatusBadge({ status }: { status?: UserStatus }) {
  return <Badge tone={status === 'ACTIVE' ? 'success' : status === 'INVITED' ? 'warning' : 'danger'}>{status === 'ACTIVE' ? 'Active' : status === 'INVITED' ? 'Invited' : 'Suspended'}</Badge>;
}

export function TeamPage() {
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [editing, setEditing] = useState<User | null | undefined>(undefined);
  const [deactivating, setDeactivating] = useState<User | null>(null);
  const [menu, setMenu] = useState<string | null>(null);
  const { user } = useAuth();
  const permitted = canManageUsers(user?.role);
  const { toast } = useToast();
  const query = useQuery({ queryKey: ['users', page, search, role], queryFn: () => usersApi.list({ page, size: 20, search, role, sort: 'createdAt', direction: 'desc' }) });
  const deactivate = useMutation({
    mutationFn: (id: string) => usersApi.remove(id),
    onSuccess: async () => {
      await invalidateWorkspaceData('users');
      setDeactivating(null);
      toast('Access suspended', { description: 'The user’s active sessions have been revoked.' });
    },
    onError: (error) => toast('Could not suspend access', { kind: 'error', description: getApiMessage(error) }),
  });
  const rows = query.data?.content ?? [];

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Workspace access" title="Team" description="Invite teammates, assign responsibilities, and keep permissions intentional." actions={permitted ? <Button onClick={() => setEditing(null)}><Plus className="h-4 w-4" />Invite member</Button> : undefined} />
      <div className="grid gap-4 sm:grid-cols-3">
        <Card className="p-4"><p className="text-xs font-medium text-ink-500">Total members</p><p className="mt-2 font-display text-2xl font-bold text-ink-950 dark:text-white">{query.data?.totalElements ?? '—'}</p></Card>
        <Card className="p-4"><p className="text-xs font-medium text-ink-500">Active on this page</p><p className="mt-2 font-display text-2xl font-bold text-brand-700 dark:text-brand-300">{rows.filter((member) => member.status === 'ACTIVE').length}</p></Card>
        <Card className="p-4"><p className="text-xs font-medium text-ink-500">Pending invitations</p><p className="mt-2 font-display text-2xl font-bold text-amber-600">{rows.filter((member) => member.status === 'INVITED').length}</p></Card>
      </div>
      <Card className="flex flex-col gap-3 p-3 sm:flex-row"><SearchInput value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Search people or email…" className="flex-1" /><Select value={role} onChange={(event) => { setRole(event.target.value); setPage(0); }} className="sm:w-48"><option value="">All roles</option>{allRoles.map((item) => <option key={item} value={item}>{ROLE_LABELS[item]}</option>)}</Select></Card>
      {query.isLoading ? <PageSkeleton /> : query.isError ? <ErrorState onRetry={() => void query.refetch()} /> : !rows.length ? <Card><EmptyState icon={UsersRound} title="No team members found" description="Try another filter or invite your first teammate." action={permitted ? <Button onClick={() => setEditing(null)}>Invite member</Button> : undefined} /></Card> : (
        <div className="table-shell overflow-x-auto">
          <table className="data-table min-w-[850px]">
            <thead><tr><th>Member</th><th>Role</th><th>Status</th><th>Last active</th><th>Joined</th>{permitted && <th className="w-16" />}</tr></thead>
            <tbody>{rows.map((member) => {
              const manageable = canManageTeamMember(user?.role, member.role);
              return <tr key={member.id}>
                <td><div className="flex items-center gap-3"><Avatar firstName={member.firstName} lastName={member.lastName} src={member.avatarUrl} /><div><p className="font-semibold text-ink-900 dark:text-white">{personName(member)}{member.id === user?.id && <span className="ml-2 text-[10px] font-medium text-ink-400">You</span>}</p><p className="mt-0.5 flex items-center gap-1 text-xs text-ink-400"><Mail className="h-3 w-3" />{member.email}</p></div></div></td>
                <td><div className="flex items-center gap-2"><RoleBadge role={member.role} />{member.role === 'SUPER_ADMIN' && <span title="Protected platform account"><LockKeyhole className="h-3.5 w-3.5 text-ink-400" /></span>}</div></td>
                <td><StatusBadge status={member.status} /></td>
                <td><span className="flex items-center gap-1.5 text-xs text-ink-500"><Clock3 className="h-3.5 w-3.5" />{member.lastLoginAt ? relativeTime(member.lastLoginAt) : 'Never'}</span></td>
                <td className="text-xs text-ink-500">{formatDate(member.createdAt)}</td>
                {permitted && <td>{manageable ? <div className="relative"><button onClick={() => setMenu(menu === member.id ? null : member.id)} className="rounded-lg p-2 text-ink-400 hover:bg-ink-100 dark:hover:bg-ink-800" aria-label={`Actions for ${personName(member)}`}><MoreHorizontal className="h-4 w-4" /></button>{menu === member.id && <div className="absolute right-0 top-9 z-20 w-36 rounded-xl border border-ink-200 bg-white p-1 shadow-lift dark:border-ink-700 dark:bg-ink-900"><button onClick={() => { setEditing(member); setMenu(null); }} className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs hover:bg-ink-50 dark:hover:bg-ink-800"><Pencil className="h-3.5 w-3.5" />Edit member</button>{member.id !== user?.id && member.status !== 'SUSPENDED' && <button onClick={() => { setDeactivating(member); setMenu(null); }} className="flex w-full items-center gap-2 rounded-lg px-2.5 py-2 text-xs text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950"><UserMinus className="h-3.5 w-3.5" />Suspend</button>}</div>}</div> : <span className="inline-grid h-8 w-8 place-items-center text-ink-300 dark:text-ink-600" aria-label="Protected platform-admin account" title="Only a platform administrator can manage this account"><LockKeyhole className="h-4 w-4" /></span>}</td>}
              </tr>;
            })}</tbody>
          </table>
          <Pagination page={query.data?.page ?? page} totalPages={query.data?.totalPages ?? 1} onChange={setPage} />
        </div>
      )}
      <Modal open={editing !== undefined} onClose={() => setEditing(undefined)} title={editing ? 'Edit team member' : 'Invite a teammate'} description={editing ? 'Adjust this person’s role and workspace status.' : 'They’ll receive a secure link to set up their account.'}><MemberForm member={editing} onDone={() => setEditing(undefined)} /></Modal>
      <ConfirmDialog open={Boolean(deactivating)} onClose={() => setDeactivating(null)} onConfirm={() => deactivating && deactivate.mutate(deactivating.id)} busy={deactivate.isPending} title="Suspend this member?" description={`${personName(deactivating ?? undefined)} will immediately lose workspace access and all active sessions will be revoked.`} />
    </div>
  );
}
