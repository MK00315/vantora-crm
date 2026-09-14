import {
  Building2,
  CheckSquare2,
  ChevronDown,
  CircleHelp,
  ContactRound,
  LayoutDashboard,
  LogOut,
  Menu,
  Paperclip,
  Search,
  Settings,
  UserRound,
  UsersRound,
  X,
} from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { isDemoMode } from '@/api/demo';
import { useAuth } from '@/auth/AuthContext';
import { Avatar, Badge } from '@/components/ui';
import { ROLE_LABELS } from '@/lib/constants';
import { cn, personName } from '@/lib/utils';

const nav = [
  { to: '/app', label: 'Overview', icon: LayoutDashboard, end: true },
  { to: '/app/leads', label: 'Leads', icon: ContactRound },
  { to: '/app/customers', label: 'Customers', icon: Building2 },
  { to: '/app/tasks', label: 'Tasks', icon: CheckSquare2 },
  { to: '/app/files', label: 'Files', icon: Paperclip },
  { to: '/app/team', label: 'Team', icon: UsersRound },
];

const secondary = [
  { to: '/app/settings', label: 'Settings', icon: Settings },
  { to: '/app/help', label: 'Help center', icon: CircleHelp },
];

function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <div className="grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-gradient-to-br from-brand-600 to-sky-700 text-sm font-bold text-white shadow-sm ring-1 ring-white/40">
        V
      </div>
      {!compact && <span className="font-display text-lg font-bold tracking-tight text-ink-900 dark:text-white">Vantora CRM</span>}
    </div>
  );
}

function SidebarContent({ close }: { close?: () => void }) {
  const { user } = useAuth();
  const navClass = ({ isActive }: { isActive: boolean }) => cn(
    'group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
    isActive
      ? 'bg-gradient-to-r from-brand-50 to-sky-50 text-brand-700 ring-1 ring-brand-100 dark:from-brand-950/70 dark:to-sky-950/40 dark:text-brand-200 dark:ring-brand-900'
      : 'text-ink-600 hover:bg-ink-50 hover:text-ink-900 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-white',
  );

  return (
    <>
      <div className="flex h-20 items-center border-b border-ink-100 px-5 dark:border-ink-800"><Logo /></div>
      <div className="enterprise-gradient mx-3 mb-5 mt-4 rounded-xl border border-brand-100 p-3 dark:border-brand-900 dark:bg-ink-900 dark:bg-none">
        <div className="flex items-center gap-2.5">
          <div className="grid h-9 w-9 place-items-center rounded-lg border border-white bg-white text-brand-700 shadow-sm dark:border-ink-700 dark:bg-ink-800 dark:text-brand-300"><Building2 className="h-4 w-4" /></div>
          <div className="min-w-0"><p className="truncate text-sm font-semibold text-ink-900 dark:text-white">{user?.tenantName ?? 'Your workspace'}</p><p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">Tenant workspace</p></div>
        </div>
      </div>
      <nav className="flex-1 space-y-1 px-3" aria-label="Main navigation">
        <p className="px-3 pb-2 pt-1 text-xs font-semibold uppercase tracking-[.12em] text-ink-500">Workspace</p>
        {nav.map(({ to, label, icon: Icon, end }) => <NavLink key={to} to={to} end={end} onClick={close} className={navClass}><Icon className="h-[18px] w-[18px]" />{label}</NavLink>)}
        <p className="px-3 pb-2 pt-6 text-xs font-semibold uppercase tracking-[.12em] text-ink-500">Support</p>
        {secondary.map(({ to, label, icon: Icon }) => <NavLink key={to} to={to} onClick={close} className={navClass}><Icon className="h-[18px] w-[18px]" />{label}</NavLink>)}
      </nav>
      <div className="m-3 rounded-xl border border-ink-200 bg-ink-50 p-4 dark:border-ink-800 dark:bg-ink-900">
        <p className="text-sm font-semibold text-ink-800 dark:text-white">Keep work moving</p>
        <p className="mt-1 text-xs leading-5 text-ink-500 dark:text-ink-400">Review overdue tasks and next steps for your team.</p>
        <NavLink to="/app/tasks" onClick={close} className="mt-3 inline-flex text-xs font-semibold text-brand-700 hover:text-brand-800 dark:text-brand-300">View tasks →</NavLink>
      </div>
    </>
  );
}

export function AppShell() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const profileRef = useRef<HTMLDivElement>(null);

  useEffect(() => setMobileOpen(false), [location.pathname]);
  useEffect(() => {
    const close = (event: MouseEvent) => !profileRef.current?.contains(event.target as Node) && setProfileOpen(false);
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="app-canvas min-h-screen dark:bg-ink-950 dark:bg-none">
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col overflow-y-auto border-r border-ink-200/80 bg-white/95 backdrop-blur-xl dark:border-ink-800 dark:bg-ink-950/95 lg:flex"><SidebarContent /></aside>
      {mobileOpen && <div className="fixed inset-0 z-50 bg-ink-950/35 lg:hidden" onMouseDown={(event) => event.target === event.currentTarget && setMobileOpen(false)}><aside className="relative flex h-full w-[min(18rem,86vw)] animate-fade-up flex-col overflow-y-auto border-r border-ink-200 bg-white dark:border-ink-800 dark:bg-ink-950"><button className="absolute right-3 top-3 rounded-lg p-2 text-ink-500 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800" onClick={() => setMobileOpen(false)} aria-label="Close navigation"><X className="h-5 w-5" /></button><SidebarContent close={() => setMobileOpen(false)} /></aside></div>}
      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center border-b border-ink-200/80 bg-white/85 px-4 backdrop-blur-xl dark:border-ink-800 dark:bg-ink-950/85 sm:px-6 lg:px-8">
          <button onClick={() => setMobileOpen(true)} className="mr-3 rounded-lg p-2 text-ink-600 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800 lg:hidden" aria-label="Open navigation"><Menu className="h-5 w-5" /></button>
          <div className="sm:hidden"><Logo /></div>
          <button onClick={() => navigate('/app/leads')} className="hidden h-10 min-w-0 max-w-sm flex-1 items-center gap-2.5 rounded-lg border border-ink-200 bg-white px-3 text-left text-sm text-ink-500 shadow-sm hover:border-brand-300 dark:border-ink-800 dark:bg-ink-900 sm:flex" aria-label="Search leads"><Search className="h-4 w-4" /><span className="truncate">Search leads…</span></button>
          <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
            {isDemoMode() && <Badge tone="warning" className="hidden sm:inline-flex">Demo mode</Badge>}
            <div className="relative ml-1" ref={profileRef}>
              <button className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-ink-100 dark:hover:bg-ink-800" onClick={() => setProfileOpen((open) => !open)} aria-expanded={profileOpen}><Avatar firstName={user?.firstName} lastName={user?.lastName} src={user?.avatarUrl} /><div className="hidden text-left xl:block"><p className="max-w-28 truncate text-xs font-semibold text-ink-900 dark:text-white">{personName(user ?? undefined)}</p><p className="text-xs text-ink-500">{user ? ROLE_LABELS[user.role] : ''}</p></div><ChevronDown className="hidden h-4 w-4 text-ink-400 sm:block" /></button>
              {profileOpen && <div className="absolute right-0 mt-2 w-56 animate-fade-up rounded-xl border border-ink-200 bg-white p-2 shadow-lift dark:border-ink-700 dark:bg-ink-900"><div className="border-b border-ink-100 px-3 py-2 dark:border-ink-800"><p className="truncate text-sm font-semibold text-ink-900 dark:text-white">{personName(user ?? undefined)}</p><p className="truncate text-xs text-ink-500">{user?.email}</p></div><button onClick={() => { navigate('/app/profile'); setProfileOpen(false); }} className="mt-1 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-ink-600 hover:bg-ink-50 dark:text-ink-300 dark:hover:bg-ink-800"><UserRound className="h-4 w-4" />Your profile</button><button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40"><LogOut className="h-4 w-4" />Sign out</button></div>}
            </div>
          </div>
        </header>
        <main id="main-content" className="mx-auto w-full max-w-[1600px] p-4 sm:p-6 lg:p-8"><Outlet /></main>
      </div>
    </div>
  );
}
