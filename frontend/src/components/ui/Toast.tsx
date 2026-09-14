import { CheckCircle2, CircleAlert, Info, X } from 'lucide-react';
import { createContext, useCallback, useContext, useMemo, useState, type PropsWithChildren } from 'react';
import { cn } from '@/lib/utils';

type ToastKind = 'success' | 'error' | 'info';
interface ToastItem { id: string; title: string; description?: string; kind: ToastKind }
interface ToastContextValue { toast: (title: string, options?: { description?: string; kind?: ToastKind }) => void }
const ToastContext = createContext<ToastContextValue | null>(null);

export function ToastProvider({ children }: PropsWithChildren) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const remove = useCallback((id: string) => setItems((current) => current.filter((item) => item.id !== id)), []);
  const toast = useCallback((title: string, options?: { description?: string; kind?: ToastKind }) => {
    const item = { id: crypto.randomUUID(), title, description: options?.description, kind: options?.kind ?? 'success' };
    setItems((current) => [...current, item]);
    window.setTimeout(() => remove(item.id), 4200);
  }, [remove]);

  const value = useMemo(() => ({ toast }), [toast]);
  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[100] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2" aria-live="polite">
        {items.map((item) => {
          const Icon = item.kind === 'success' ? CheckCircle2 : item.kind === 'error' ? CircleAlert : Info;
          return (
            <div key={item.id} className={cn('pointer-events-auto flex animate-fade-up items-start gap-3 rounded-xl border border-ink-200 border-l-4 bg-white p-4 shadow-lift dark:border-ink-700 dark:bg-ink-900', item.kind === 'success' ? 'border-l-brand-500' : item.kind === 'error' ? 'border-l-rose-500' : 'border-l-sky-500')}>
              <Icon className={cn('mt-0.5 h-5 w-5 shrink-0', item.kind === 'success' ? 'text-brand-600' : item.kind === 'error' ? 'text-rose-500' : 'text-sky-500')} />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-ink-900 dark:text-white">{item.title}</p>
                {item.description && <p className="mt-0.5 text-xs leading-5 text-ink-500 dark:text-ink-300">{item.description}</p>}
              </div>
              <button onClick={() => remove(item.id)} className="rounded-lg p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700 dark:hover:bg-ink-800" aria-label="Dismiss notification"><X className="h-4 w-4" /></button>
            </div>
          );
        })}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const value = useContext(ToastContext);
  if (!value) throw new Error('useToast must be used inside ToastProvider');
  return value;
}
