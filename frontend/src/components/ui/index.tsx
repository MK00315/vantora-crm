import { AlertTriangle, ChevronLeft, ChevronRight, Inbox, LoaderCircle, Search, X } from 'lucide-react';
import { forwardRef, useEffect, type ButtonHTMLAttributes, type HTMLAttributes, type InputHTMLAttributes, type PropsWithChildren, type SelectHTMLAttributes } from 'react';
import { cn, initials } from '@/lib/utils';

export const Button = forwardRef<HTMLButtonElement, ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'ghost' | 'danger'; size?: 'sm' | 'md' | 'lg' }>(
  ({ className, variant = 'primary', size = 'md', type = 'button', children, ...props }, ref) => (
    <button
      ref={ref}
      type={type}
      className={cn(
        'inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 dark:focus-visible:ring-offset-ink-950',
        variant === 'primary' && 'bg-gradient-to-r from-brand-600 to-sky-700 text-white shadow-sm hover:from-brand-700 hover:to-sky-800 hover:shadow-md',
        variant === 'secondary' && 'border border-ink-200 bg-white text-ink-700 shadow-sm hover:border-ink-300 hover:bg-ink-50 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-100 dark:hover:bg-ink-800',
        variant === 'ghost' && 'text-ink-600 hover:bg-ink-100 hover:text-ink-950 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-white',
        variant === 'danger' && 'bg-rose-600 text-white hover:bg-rose-700',
        size === 'sm' && 'h-9 px-3 text-xs',
        size === 'md' && 'h-11 px-4 text-sm',
        size === 'lg' && 'h-12 px-5 text-sm',
        className,
      )}
      {...props}
    >
      {children}
    </button>
  ),
);
Button.displayName = 'Button';

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & { invalid?: boolean }>(
  ({ className, invalid, ...props }, ref) => (
    <input
      ref={ref}
      className={cn('h-11 w-full rounded-lg border border-ink-300 bg-white px-3.5 text-sm text-ink-900 shadow-sm outline-none transition placeholder:text-ink-500 focus:border-brand-500 focus:ring-4 focus:ring-brand-500/10 disabled:cursor-not-allowed disabled:bg-ink-50 disabled:text-ink-500 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-50 dark:placeholder:text-ink-500', invalid && 'border-rose-500 focus:border-rose-500 focus:ring-rose-500/10', className)}
      aria-invalid={invalid || undefined}
      {...props}
    />
  ),
);
Input.displayName = 'Input';

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement> & { invalid?: boolean }>(
  ({ className, invalid, children, ...props }, ref) => (
    <select ref={ref} className={cn('h-11 w-full appearance-none rounded-lg border border-ink-300 bg-white px-3.5 text-sm text-ink-900 shadow-sm outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-500/10 disabled:cursor-not-allowed disabled:bg-ink-50 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-50', invalid && 'border-rose-500', className)} aria-invalid={invalid || undefined} {...props}>{children}</select>
  ),
);
Select.displayName = 'Select';

export function Textarea({ className, invalid, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { invalid?: boolean }) {
  return <textarea className={cn('min-h-28 w-full resize-y rounded-lg border border-ink-300 bg-white px-3.5 py-3 text-sm text-ink-900 shadow-sm outline-none transition placeholder:text-ink-500 focus:border-brand-500 focus:ring-4 focus:ring-brand-500/10 disabled:cursor-not-allowed disabled:bg-ink-50 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-50', invalid && 'border-rose-500', className)} aria-invalid={invalid || undefined} {...props} />;
}

export function Field({ label, error, hint, children, className }: PropsWithChildren<{ label: string; error?: string; hint?: string; className?: string }>) {
  return (
    <label className={cn('block', className)}>
      <span className="mb-1.5 block text-sm font-medium text-ink-800 dark:text-ink-100">{label}</span>
      {children}
      {error ? <span className="mt-1.5 block text-xs font-medium text-rose-600">{error}</span> : hint ? <span className="mt-1.5 block text-xs text-ink-500">{hint}</span> : null}
    </label>
  );
}

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('rounded-xl border border-ink-200 bg-white shadow-soft dark:border-ink-800 dark:bg-ink-900', className)} {...props} />;
}

export function Badge({ className, tone = 'neutral', ...props }: HTMLAttributes<HTMLSpanElement> & { tone?: 'neutral' | 'brand' | 'success' | 'warning' | 'danger' | 'info' }) {
  return <span className={cn('inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold', tone === 'neutral' && 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300', tone === 'brand' && 'bg-brand-100 text-brand-800 dark:bg-brand-950 dark:text-brand-300', tone === 'success' && 'bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300', tone === 'warning' && 'bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300', tone === 'danger' && 'bg-rose-100 text-rose-700 dark:bg-rose-950 dark:text-rose-300', tone === 'info' && 'bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300', className)} {...props} />;
}

export function Avatar({ firstName, lastName, src, size = 'md', className }: { firstName?: string; lastName?: string; src?: string | null; size?: 'sm' | 'md' | 'lg'; className?: string }) {
  return (
    <div className={cn('grid shrink-0 place-items-center overflow-hidden rounded-full bg-brand-100 font-bold text-brand-700 ring-2 ring-white dark:bg-brand-900 dark:text-brand-200 dark:ring-ink-900', size === 'sm' && 'h-7 w-7 text-[10px]', size === 'md' && 'h-9 w-9 text-xs', size === 'lg' && 'h-12 w-12 text-sm', className)}>
      {src ? <img src={src} alt="" className="h-full w-full object-cover" /> : initials(firstName, lastName)}
    </div>
  );
}

export function SearchInput({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <div className={cn('relative', className)}><Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" /><Input className="pl-10" type="search" {...props} /></div>;
}

export function Modal({ open, onClose, title, description, children, width = 'max-w-xl' }: PropsWithChildren<{ open: boolean; onClose: () => void; title: string; description?: string; width?: string }>) {
  useEffect(() => {
    if (!open) return;
    const handler = (event: KeyboardEvent) => event.key === 'Escape' && onClose();
    document.addEventListener('keydown', handler);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', handler);
      document.body.style.overflow = '';
    };
  }, [onClose, open]);
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-ink-950/35 p-0 sm:items-center sm:p-4" role="presentation" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className={cn('max-h-[92vh] w-full overflow-y-auto rounded-t-2xl border border-ink-200 bg-white shadow-lift dark:border-ink-700 dark:bg-ink-900 sm:rounded-2xl', width)} role="dialog" aria-modal="true" aria-labelledby="modal-title">
        <div className="sticky top-0 z-10 flex items-start justify-between border-b border-ink-100 bg-white/95 px-5 py-4 backdrop-blur dark:border-ink-800 dark:bg-ink-900/95 sm:px-6">
          <div><h2 id="modal-title" className="font-display text-lg font-bold text-ink-950 dark:text-white">{title}</h2>{description && <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">{description}</p>}</div>
          <button onClick={onClose} className="rounded-lg p-2 text-ink-400 hover:bg-ink-100 hover:text-ink-700 dark:hover:bg-ink-800" aria-label="Close"><X className="h-5 w-5" /></button>
        </div>
        <div className="p-5 sm:p-6">{children}</div>
      </div>
    </div>
  );
}

export function ConfirmDialog({ open, onClose, onConfirm, title = 'Are you sure?', description, busy }: { open: boolean; onClose: () => void; onConfirm: () => void; title?: string; description: string; busy?: boolean }) {
  return <Modal open={open} onClose={onClose} title={title} width="max-w-md"><div className="flex gap-3 rounded-2xl bg-rose-50 p-4 dark:bg-rose-950/40"><AlertTriangle className="h-5 w-5 shrink-0 text-rose-600" /><p className="text-sm leading-6 text-rose-900 dark:text-rose-200">{description}</p></div><div className="mt-6 flex justify-end gap-3"><Button variant="secondary" onClick={onClose}>Cancel</Button><Button variant="danger" onClick={onConfirm} disabled={busy}>{busy && <LoaderCircle className="h-4 w-4 animate-spin" />}Delete</Button></div></Modal>;
}

export function EmptyState({ title, description, action, icon: Icon = Inbox }: { title: string; description: string; action?: React.ReactNode; icon?: React.ComponentType<{ className?: string }> }) {
  return <div className="flex min-h-64 flex-col items-center justify-center px-6 py-12 text-center"><div className="grid h-14 w-14 place-items-center rounded-2xl bg-ink-100 text-ink-500 dark:bg-ink-800 dark:text-ink-300"><Icon className="h-6 w-6" /></div><h3 className="mt-4 font-display font-bold text-ink-900 dark:text-white">{title}</h3><p className="mt-1 max-w-sm text-sm leading-6 text-ink-500 dark:text-ink-400">{description}</p>{action && <div className="mt-5">{action}</div>}</div>;
}

export function ErrorState({ onRetry, title = 'We could not load this data', description = 'Check your connection and try again.' }: { onRetry?: () => void; title?: string; description?: string }) {
  return <EmptyState icon={AlertTriangle} title={title} description={description} action={onRetry ? <Button variant="secondary" onClick={onRetry}>Try again</Button> : undefined} />;
}

export function PageSkeleton() {
  return <div className="animate-pulse space-y-5" aria-label="Loading"><div className="h-8 w-48 rounded-lg bg-ink-200 dark:bg-ink-800" /><div className="grid gap-4 md:grid-cols-4">{Array.from({ length: 4 }).map((_, i) => <div key={i} className="h-32 rounded-xl bg-ink-100 dark:bg-ink-900" />)}</div><div className="h-80 rounded-xl bg-ink-100 dark:bg-ink-900" /></div>;
}

export function Spinner({ className }: { className?: string }) {
  return <LoaderCircle className={cn('h-5 w-5 animate-spin', className)} aria-hidden="true" />;
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (page: number) => void }) {
  if (totalPages <= 1) return null;
  return <div className="flex items-center justify-between border-t border-ink-100 px-4 py-3 text-sm dark:border-ink-800"><span className="text-ink-500">Page {page + 1} of {totalPages}</span><div className="flex gap-2"><Button variant="secondary" size="sm" onClick={() => onChange(page - 1)} disabled={page <= 0} aria-label="Previous page"><ChevronLeft className="h-4 w-4" /></Button><Button variant="secondary" size="sm" onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1} aria-label="Next page"><ChevronRight className="h-4 w-4" /></Button></div></div>;
}

export function PageHeader({ eyebrow, title, description, actions }: { eyebrow?: string; title: string; description?: string; actions?: React.ReactNode }) {
  return <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div>{eyebrow && <p className="mb-1.5 text-xs font-semibold uppercase tracking-[0.14em] text-brand-700 dark:text-brand-400">{eyebrow}</p>}<h1 className="font-display text-2xl font-bold tracking-tight text-ink-950 dark:text-white sm:text-3xl">{title}</h1>{description && <p className="mt-1.5 max-w-2xl text-sm leading-6 text-ink-500 dark:text-ink-400">{description}</p>}</div>{actions && <div className="flex shrink-0 flex-wrap gap-2">{actions}</div>}</div>;
}
