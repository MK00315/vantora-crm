import { zodResolver } from '@hookform/resolvers/zod';
import { BookOpen, CheckCircle2, ChevronRight, CircleHelp, Clock3, ExternalLink, Globe2, KeyRound, LifeBuoy, Mail, MessageSquareText, Palette, Search, ServerCog, ShieldCheck, Sun, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { profileApi } from '@/api/services';
import { invalidateWorkspaceData } from '@/app/queryClient';
import { useAuth } from '@/auth/AuthContext';
import { Avatar, Badge, Button, Card, Field, Input, PageHeader } from '@/components/ui';
import { useToast } from '@/components/ui/Toast';
import { ROLE_LABELS } from '@/lib/constants';
import { getApiMessage, personName } from '@/lib/utils';

const profileSchema = z.object({ firstName: z.string().min(2, 'Enter your first name'), lastName: z.string().min(2, 'Enter your last name') });
const passwordSchema = z.object({ currentPassword: z.string().min(1, 'Enter your current password'), newPassword: z.string().min(10, 'Use at least 10 characters').regex(/[a-z]/, 'Include a lowercase letter').regex(/[A-Z]/, 'Include an uppercase letter').regex(/[0-9]/, 'Include a number'), confirmPassword: z.string() }).refine((value) => value.newPassword === value.confirmPassword, { path: ['confirmPassword'], message: 'Passwords do not match' });

export function ProfilePage() {
  const { user, setUser, logout } = useAuth(); const { toast } = useToast(); const navigate = useNavigate();
  const profile = useForm<z.infer<typeof profileSchema>>({ resolver: zodResolver(profileSchema), defaultValues: { firstName: user?.firstName ?? '', lastName: user?.lastName ?? '' } });
  const password = useForm<z.infer<typeof passwordSchema>>({ resolver: zodResolver(passwordSchema), defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' } });
  const saveProfile = async (values: z.infer<typeof profileSchema>) => { try { const updated = await profileApi.update(values); setUser(user ? { ...user, ...updated, tenantId: user.tenantId, tenantName: user.tenantName } : updated); await invalidateWorkspaceData('users'); toast('Profile updated'); } catch (error) { toast('Could not update profile', { kind: 'error', description: getApiMessage(error) }); } };
  const changePassword = async (form: z.infer<typeof passwordSchema>) => { try { await profileApi.changePassword({ currentPassword: form.currentPassword, newPassword: form.newPassword }); password.reset(); await invalidateWorkspaceData('users').catch(() => undefined); try { await logout(); } catch { /* Local auth state is cleared by logout even if the network request fails. */ } toast('Password changed', { description: 'All sessions were revoked. Sign in again to continue securely.' }); navigate('/login', { replace: true }); } catch (error) { toast('Could not change password', { kind: 'error', description: getApiMessage(error) }); } };
  return <div className="space-y-6"><PageHeader eyebrow="Your account" title="Profile & security" description="Manage your identity and keep your account protected." /><div className="grid gap-5 xl:grid-cols-[.75fr_1.25fr]"><Card className="h-fit p-6"><div className="flex flex-col items-center text-center"><Avatar size="lg" firstName={user?.firstName} lastName={user?.lastName} src={user?.avatarUrl} className="h-20 w-20 text-xl" /><h2 className="mt-4 font-display text-xl font-bold text-ink-950 dark:text-white">{personName(user ?? undefined)}</h2><p className="mt-1 text-sm text-ink-500">{user?.email}</p><Badge tone="brand" className="mt-3">{user ? ROLE_LABELS[user.role] : ''}</Badge></div><div className="mt-6 space-y-3 border-t border-ink-100 pt-5 text-sm dark:border-ink-800"><div className="flex items-center justify-between"><span className="text-ink-500">Workspace</span><strong className="text-ink-800 dark:text-white">{user?.tenantName ?? '—'}</strong></div><div className="flex items-center justify-between"><span className="text-ink-500">Email</span>{user?.emailVerifiedAt ? <span className="inline-flex items-center gap-1 text-brand-700 dark:text-brand-400"><CheckCircle2 className="h-3.5 w-3.5" />Verified</span> : <span className="inline-flex items-center gap-1 text-amber-700 dark:text-amber-400"><Clock3 className="h-3.5 w-3.5" />Pending verification</span>}</div></div></Card><div className="space-y-5"><Card className="p-5 sm:p-6"><div className="mb-5 flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-brand-100 text-brand-700 dark:bg-brand-950 dark:text-brand-300"><UserRound className="h-5 w-5" /></div><div><h2 className="font-display font-bold text-ink-950 dark:text-white">Personal details</h2><p className="text-xs text-ink-500">How your name appears across the workspace.</p></div></div><form onSubmit={profile.handleSubmit(saveProfile)}><div className="grid gap-4 sm:grid-cols-2"><Field label="First name" error={profile.formState.errors.firstName?.message}><Input {...profile.register('firstName')} /></Field><Field label="Last name" error={profile.formState.errors.lastName?.message}><Input {...profile.register('lastName')} /></Field><Field label="Email" className="sm:col-span-2" hint="Contact a company admin to change your sign-in email."><Input value={user?.email ?? ''} disabled /></Field></div><div className="mt-5 flex justify-end"><Button type="submit" disabled={profile.formState.isSubmitting}>{profile.formState.isSubmitting ? 'Saving…' : 'Save profile'}</Button></div></form></Card><Card className="p-5 sm:p-6"><div className="mb-5 flex items-center gap-3"><div className="grid h-10 w-10 place-items-center rounded-xl bg-violet-100 text-violet-700 dark:bg-violet-950 dark:text-violet-300"><KeyRound className="h-5 w-5" /></div><div><h2 className="font-display font-bold text-ink-950 dark:text-white">Change password</h2><p className="text-xs text-ink-500">Use 10+ characters with upper-case, lower-case, and a number.</p></div></div><form onSubmit={password.handleSubmit(changePassword)} className="space-y-4"><Field label="Current password" error={password.formState.errors.currentPassword?.message}><Input type="password" autoComplete="current-password" {...password.register('currentPassword')} /></Field><div className="grid gap-4 sm:grid-cols-2"><Field label="New password" error={password.formState.errors.newPassword?.message}><Input type="password" autoComplete="new-password" {...password.register('newPassword')} /></Field><Field label="Confirm new password" error={password.formState.errors.confirmPassword?.message}><Input type="password" autoComplete="new-password" {...password.register('confirmPassword')} /></Field></div><div className="flex justify-end"><Button type="submit" disabled={password.formState.isSubmitting}>{password.formState.isSubmitting ? 'Updating…' : 'Update password'}</Button></div></form></Card></div></div></div>;
}

export function SettingsPage() {
  const browserTimezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'Browser default';
  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Workspace preferences" title="Settings" description="Review your local workspace experience and services managed by the CRM deployment." />
      <div className="grid gap-5 lg:grid-cols-2">
        <Card className="p-5 sm:p-6">
          <div className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded-lg bg-brand-100 text-brand-700"><Palette className="h-5 w-5" /></div>
            <div><h2 className="font-display font-bold text-ink-950">Appearance</h2><p className="text-xs text-ink-500">Optimized for clear daily work.</p></div>
          </div>
          <div className="enterprise-gradient mt-6 flex items-center gap-4 rounded-xl border border-brand-100 p-4">
            <div className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-white text-brand-700 shadow-sm"><Sun className="h-5 w-5" /></div>
            <div className="min-w-0 flex-1"><p className="text-sm font-semibold text-ink-900">Professional light workspace</p><p className="mt-1 text-xs leading-5 text-ink-500">Consistent high-contrast surfaces for dashboards, tables, forms, and team workflows.</p></div>
            <Badge tone="brand">Active</Badge>
          </div>
        </Card>
        <Card className="p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-sky-100 text-sky-700"><Mail className="h-5 w-5" /></div>
            <div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-2"><h2 className="font-display font-bold text-ink-950">Email notifications</h2><Badge tone="neutral">Deployment managed</Badge></div><p className="mt-1 text-xs leading-5 text-ink-500">Password recovery, invitations, onboarding, and due-task reminders are sent by the backend when SMTP is configured. This release does not expose per-user email switches.</p></div>
          </div>
          <div className="mt-6 rounded-lg bg-ink-50 p-4 text-xs leading-5 text-ink-600">Ask your workspace administrator to confirm mail delivery settings for this environment.</div>
        </Card>
        <Card className="p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-amber-100 text-amber-700"><Globe2 className="h-5 w-5" /></div>
            <div><h2 className="font-display font-bold text-ink-950">Date & time</h2><p className="mt-1 text-xs leading-5 text-ink-500">Deadlines are stored by the API in UTC and automatically displayed in your browser time zone.</p></div>
          </div>
          <div className="mt-6 flex items-center justify-between rounded-lg border border-ink-200 p-4"><span className="text-xs text-ink-500">Detected time zone</span><strong className="text-xs text-ink-900">{browserTimezone}</strong></div>
        </Card>
        <Card className="p-5 sm:p-6">
          <div className="flex items-start gap-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-violet-100 text-violet-700"><ServerCog className="h-5 w-5" /></div>
            <div><div className="flex flex-wrap items-center gap-2"><h2 className="font-display font-bold text-ink-950">Platform services</h2><Badge tone="neutral">Administrator managed</Badge></div><p className="mt-1 text-xs leading-5 text-ink-500">Database, cache, object storage, mail transport, and retention policies are configured through secured deployment environment settings—not from this browser.</p></div>
          </div>
        </Card>
      </div>
    </div>
  );
}

const helpTopics = [
  { icon: UserRound, title: 'Getting started', text: 'Set up your workspace, team, and daily flow.' },
  { icon: ShieldCheck, title: 'Roles & security', text: 'Understand access controls and tenant isolation.' },
  { icon: MessageSquareText, title: 'Leads & pipeline', text: 'Move opportunities forward with confidence.' },
  { icon: BookOpen, title: 'Customers & tasks', text: 'Keep context and next actions connected.' },
];

export function HelpPage() {
  const [query, setQuery] = useState(''); const filtered = helpTopics.filter((topic) => `${topic.title} ${topic.text}`.toLowerCase().includes(query.toLowerCase()));
  return <div className="space-y-6"><div className="enterprise-gradient rounded-2xl border border-brand-100 px-6 py-10 text-center shadow-soft dark:border-brand-900 dark:bg-ink-900 dark:bg-none sm:px-10 sm:py-14"><CircleHelp className="mx-auto h-8 w-8 text-brand-600" /><h1 className="mt-4 font-display text-3xl font-bold text-ink-950 dark:text-white">How can we help?</h1><p className="mt-2 text-sm text-ink-600 dark:text-ink-300">Find quick guidance for your Vantora CRM workspace.</p><div className="relative mx-auto mt-6 max-w-xl"><Search className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" /><Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search help topics…" className="h-12 border-ink-200 bg-white pl-11 text-ink-900 shadow-sm" /></div></div><div className="grid gap-4 sm:grid-cols-2">{filtered.map(({ icon: Icon, title, text }) => <Card key={title} className="group flex items-center gap-4 p-5 transition hover:border-brand-300 hover:shadow-sm"><div className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700 dark:bg-brand-950 dark:text-brand-300"><Icon className="h-5 w-5" /></div><div className="min-w-0 flex-1"><h2 className="text-sm font-bold text-ink-900 dark:text-white">{title}</h2><p className="mt-1 text-xs leading-5 text-ink-500">{text}</p></div><ChevronRight className="h-4 w-4 text-ink-300 transition group-hover:translate-x-1 group-hover:text-brand-500" /></Card>)}</div><Card className="flex flex-col items-start gap-4 bg-gradient-to-br from-brand-50 to-white p-6 dark:from-brand-950/40 dark:to-ink-900 sm:flex-row sm:items-center"><div className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-gradient-to-br from-brand-600 to-sky-600 text-white"><LifeBuoy className="h-5 w-5" /></div><div className="flex-1"><h2 className="font-display font-bold text-ink-950 dark:text-white">Still need a hand?</h2><p className="mt-1 text-sm text-ink-500">Reach your workspace administrator or review the live API reference.</p></div><a href="/swagger-ui.html" target="_blank" className="inline-flex items-center gap-2 text-sm font-bold text-brand-700 dark:text-brand-300">Open API docs <ExternalLink className="h-4 w-4" /></a></Card></div>;
}

export function NotFoundPage() { return <div className="grid min-h-[70vh] place-items-center text-center"><div><p className="font-display text-7xl font-bold text-brand-500">404</p><h1 className="mt-4 font-display text-2xl font-bold text-ink-950 dark:text-white">This page drifted off course</h1><p className="mt-2 text-sm text-ink-500">The route does not exist or moved somewhere else.</p><Link to="/app" className="mt-6 inline-flex h-11 items-center rounded-lg bg-gradient-to-r from-brand-600 to-sky-700 px-4 text-sm font-semibold text-white shadow-sm hover:from-brand-700 hover:to-sky-800">Back to dashboard</Link></div></div>; }
