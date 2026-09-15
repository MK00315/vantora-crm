import { ArrowDown, ArrowRight, CornerDownLeft, Search, X } from 'lucide-react';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { DialogSurface } from '@/components/ui/DialogSurface';
import { searchablePages } from './navigation';

export function QuickSearch({ open, onClose }: { open: boolean; onClose: () => void }) {
  return <DialogSurface open={open} onClose={onClose} label="Search and navigate" className="quick-search-dialog">
    <SearchContent onClose={onClose} />
  </DialogSurface>;
}

function SearchContent({ onClose }: { onClose: () => void }) {
  const [query, setQuery] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const resultsRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();
  const term = query.trim();
  const pages = searchablePages.filter((page) => `${page.label} ${page.description}`.toLowerCase().includes(term.toLowerCase()));
  const go = (path: string) => { onClose(); navigate(path); };
  const searchLeads = () => { if (term) go(`/app/leads?search=${encodeURIComponent(term)}`); };

  return <>
    <form className="flex items-center gap-3 border-b border-ink-200 px-4 py-4 sm:px-5" onSubmit={(event) => { event.preventDefault(); searchLeads(); }}>
      <Search size={20} className="shrink-0 text-ink-400" />
      <input ref={inputRef} type="search" value={query} maxLength={200} onChange={(event) => setQuery(event.target.value)}
        onKeyDown={(event) => { if (event.key === 'ArrowDown') { event.preventDefault(); resultsRef.current?.querySelector<HTMLButtonElement>('button')?.focus(); } }}
        aria-label="Search leads or workspace pages" placeholder="Search leads or jump to a page…" className="min-w-0 flex-1 bg-white py-1 text-sm text-ink-900 outline-none placeholder:text-ink-500" />
      <button type="button" onClick={onClose} aria-label="Close search" className="rounded-md p-1.5 text-ink-500 hover:bg-ink-100"><X size={18} /></button>
    </form>
    <div ref={resultsRef} className="max-h-[55dvh] overflow-y-auto p-2 sm:p-3" onKeyDown={(event) => {
      if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
      const buttons = Array.from(event.currentTarget.querySelectorAll<HTMLButtonElement>('button'));
      const index = buttons.indexOf(document.activeElement as HTMLButtonElement);
      if (index < 0) return;
      event.preventDefault();
      if (event.key === 'ArrowUp' && index === 0) inputRef.current?.focus();
      else buttons[event.key === 'Home' ? 0 : event.key === 'End' ? buttons.length - 1 : Math.max(0, Math.min(buttons.length - 1, index + (event.key === 'ArrowDown' ? 1 : -1)))]?.focus();
    }}>
      {term && <button type="button" onClick={searchLeads} className="search-result mb-2 bg-brand-50 text-brand-800">
        <span className="search-result-icon bg-white text-brand-700"><Search size={18} /></span>
        <span className="min-w-0 flex-1 text-left"><span className="block truncate text-sm font-semibold">Search leads for “{term}”</span><span className="mt-0.5 block text-xs text-ink-500">Match contact names, companies, or emails</span></span>
        <CornerDownLeft size={16} className="shrink-0" />
      </button>}
      <p className="px-3 pb-2 pt-2 text-[11px] font-semibold uppercase tracking-wider text-ink-500">Go to page</p>
      {pages.map(({ to, label, description, icon: Icon }) => <button type="button" key={to} onClick={() => go(to)} className="search-result group">
        <span className="search-result-icon bg-ink-50 text-ink-600"><Icon size={18} /></span>
        <span className="min-w-0 flex-1 text-left"><span className="block text-sm font-medium text-ink-900">{label}</span><span className="mt-0.5 block text-xs text-ink-500">{description}</span></span>
        <ArrowRight size={16} className="shrink-0 text-ink-400 group-hover:text-brand-700" />
      </button>)}
      {!pages.length && <p className="px-3 py-6 text-center text-sm text-ink-500">No matching pages. You can still search your leads above.</p>}
    </div>
    <div className="flex flex-wrap items-center gap-x-4 gap-y-2 border-t border-ink-100 bg-ink-50 px-5 py-3 text-[11px] text-ink-500">
      <span className="inline-flex items-center gap-1"><ArrowDown size={12} />Navigate</span><span className="inline-flex items-center gap-1"><CornerDownLeft size={12} />Open / search</span><span className="ml-auto">Esc to close</span>
    </div>
  </>;
}
