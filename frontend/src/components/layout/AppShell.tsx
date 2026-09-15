import { Building2, ChevronDown, ChevronRight, LogOut, Menu, Search, UserRound, X } from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { isDemoMode } from '@/api/demo';
import { useAuth } from '@/auth/AuthContext';
import { Avatar, Badge } from '@/components/ui';
import { DialogSurface } from '@/components/ui/DialogSurface';
import { QuickSearch } from './QuickSearch';
import { searchablePages, supportPages as secondary, workspacePages as nav } from './navigation';
import { ROLE_LABELS } from '@/lib/constants';
import { cn, personName } from '@/lib/utils';

function Logo({ compact = false }: { compact?: boolean }) {
  return (
    <div className="flex items-center gap-3">
      <div className="brand-monogram grid h-9 w-9 shrink-0 place-items-center rounded-lg bg-brand-600 text-sm font-bold text-white">
        V
      </div>
      {!compact && <span className="font-display text-lg font-bold tracking-tight text-ink-900 dark:text-white">Vantora CRM</span>}
    </div>
  );
}

function SidebarContent({ close, onSearch }: { close?: () => void; onSearch: () => void }) {
  const { user } = useAuth();
  const navClass = ({ isActive }: { isActive: boolean }) => cn(
    'workspace-nav-link group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
    isActive
      ? 'bg-brand-50 text-brand-700 dark:bg-brand-950/70 dark:text-brand-200 dark:ring-brand-900'
      : 'text-ink-600 hover:bg-ink-50 hover:text-ink-900 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-white',
  );

  return (
    <>
      <div className="flex h-16 shrink-0 items-center border-b border-ink-100 px-5 dark:border-ink-800"><Logo /></div>
      <div className="workspace-surface mx-3 mb-5 mt-4 rounded-lg border border-ink-200 p-3 dark:border-brand-900 dark:bg-ink-900 dark:bg-none">
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
      <div className="m-3 mt-5 border-t border-ink-200 pt-3">
        <button type="button" onClick={onSearch} className="flex w-full items-center gap-2 rounded-lg px-3 py-2.5 text-xs font-medium text-ink-600 hover:bg-ink-100"><Search size={15} />Quick navigation<kbd className="ml-auto rounded border border-ink-200 bg-white px-1.5 py-0.5 text-[10px]">Ctrl K</kbd></button>
      </div>
    </>
  );
}

export function AppShell() {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const profileRef = useRef<HTMLDivElement>(null);
  const profileButtonRef = useRef<HTMLButtonElement>(null);
  const currentPage = searchablePages.find((page) => page.to === location.pathname)?.label ?? 'Workspace';
  const openSearch = useCallback(() => { setMobileOpen(false); setProfileOpen(false); setSearchOpen(true); }, []);
  const closeSearch = useCallback(() => setSearchOpen(false), []);
  const closeNavigation = useCallback(() => setMobileOpen(false), []);

  useEffect(() => { setMobileOpen(false); setProfileOpen(false); }, [location.pathname]);
  useEffect(() => {
    const shortcut = (event: KeyboardEvent) => {
      if (event.key.toLowerCase() === 'k' && (event.ctrlKey || event.metaKey) && !event.altKey) {
        // Do not steal focus from an open form dialog.
        if (document.querySelector('dialog[open], [role="dialog"]')) return;
        event.preventDefault(); openSearch();
      }
      if (event.key === 'Escape' && profileOpen) { setProfileOpen(false); profileButtonRef.current?.focus(); }
    };
    document.addEventListener('keydown', shortcut);
    return () => document.removeEventListener('keydown', shortcut);
  }, [openSearch, profileOpen]);
  useEffect(() => {
    const wide = window.matchMedia('(min-width: 1024px)');
    const closeAtDesktop = () => { if (wide.matches) setMobileOpen(false); };
    wide.addEventListener('change', closeAtDesktop);
    return () => wide.removeEventListener('change', closeAtDesktop);
  }, []);
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
      <aside className="fixed inset-y-0 left-0 z-40 hidden w-64 flex-col overflow-y-auto border-r border-ink-200/80 bg-ink-50 dark:border-ink-800 dark:bg-ink-950/95 lg:flex"><SidebarContent onSearch={openSearch} /></aside>
      <DialogSurface open={mobileOpen} onClose={closeNavigation} label="Workspace navigation" className="navigation-dialog">
        <div className="relative flex min-h-full flex-col"><button className="absolute right-2 top-4 rounded-lg p-2 text-ink-500 hover:bg-ink-100" onClick={closeNavigation} aria-label="Close navigation"><X className="h-4 w-4" /></button><SidebarContent close={closeNavigation} onSearch={openSearch} /></div>
      </DialogSurface>
      <QuickSearch open={searchOpen} onClose={closeSearch} />
      <div className="lg:pl-64">
        <header className="sticky top-0 z-30 flex h-16 items-center border-b border-ink-200/80 bg-white px-4 dark:border-ink-800 dark:bg-ink-950/85 sm:px-6 lg:px-8">
          <button onClick={() => setMobileOpen(true)} className="mr-3 rounded-lg p-2 text-ink-600 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800 lg:hidden" aria-label="Open navigation"><Menu className="h-5 w-5" /></button>
          <nav aria-label="Breadcrumb" className="min-w-0 flex-1"><ol className="flex min-w-0 items-center gap-2 text-sm"><li className="hidden truncate text-ink-500 xl:block">{user?.tenantName ?? 'Workspace'}</li><li aria-hidden="true" className="hidden text-ink-300 xl:block"><ChevronRight size={14} /></li><li aria-current="page" className="truncate font-medium text-ink-800">{currentPage}</li></ol></nav>
          <button onClick={openSearch} className="mr-2 flex h-9 shrink-0 items-center gap-2 rounded-lg border border-ink-200 bg-white px-2.5 text-sm text-ink-500 hover:border-ink-300 hover:bg-ink-50 sm:mr-4 sm:w-56" aria-label="Search and navigate" aria-keyshortcuts="Control+k Meta+k"><Search className="h-4 w-4 shrink-0" /><span className="hidden min-w-0 flex-1 truncate text-left sm:inline">Search workspace</span><kbd className="ml-auto hidden shrink-0 whitespace-nowrap rounded border border-ink-200 bg-ink-50 px-1 text-[10px] sm:inline">Ctrl K</kbd></button>
          <div className="ml-auto flex items-center gap-1.5 sm:gap-2">
            {isDemoMode() && <Badge tone="warning" className="hidden sm:inline-flex">Demo mode</Badge>}
            <div className="relative ml-1" ref={profileRef}>
              <button ref={profileButtonRef} aria-label="Account options" aria-controls="account-options" className="flex items-center gap-2 rounded-lg p-1.5 hover:bg-ink-100 dark:hover:bg-ink-800" onClick={() => setProfileOpen((open) => !open)} aria-expanded={profileOpen}><Avatar firstName={user?.firstName} lastName={user?.lastName} src={user?.avatarUrl} /><div className="hidden text-left xl:block"><p className="max-w-28 truncate text-xs font-semibold text-ink-900 dark:text-white">{personName(user ?? undefined)}</p><p className="text-xs text-ink-500">{user ? ROLE_LABELS[user.role] : ''}</p></div><ChevronDown className="hidden h-4 w-4 text-ink-400 sm:block" /></button>
              {profileOpen && <div id="account-options" className="absolute right-0 mt-2 w-56 animate-fade-up rounded-xl border border-ink-200 bg-white p-2 shadow-lift dark:border-ink-700 dark:bg-ink-900"><div className="border-b border-ink-100 px-3 py-2 dark:border-ink-800"><p className="truncate text-sm font-semibold text-ink-900 dark:text-white">{personName(user ?? undefined)}</p><p className="truncate text-xs text-ink-500">{user?.email}</p></div><button onClick={() => { navigate('/app/profile'); setProfileOpen(false); }} className="mt-1 flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-ink-600 hover:bg-ink-50 dark:text-ink-300 dark:hover:bg-ink-800"><UserRound className="h-4 w-4" />Your profile</button><button onClick={handleLogout} className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40"><LogOut className="h-4 w-4" />Sign out</button></div>}
            </div>
          </div>
        </header>
        <main id="main-content" className="mx-auto w-full max-w-[1600px] p-4 sm:p-6 lg:p-8"><Outlet /></main>
      </div>
    </div>
  );
}
