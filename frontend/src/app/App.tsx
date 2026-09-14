import { QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthContext';
import { ThemeProvider } from '@/app/ThemeProvider';
import { ToastProvider } from '@/components/ui/Toast';
import { queryClient } from '@/app/queryClient';
import { router } from '@/app/router';

export function App() {
  return <ThemeProvider><QueryClientProvider client={queryClient}><AuthProvider><ToastProvider><a href="#main-content" className="fixed left-3 top-3 z-[200] -translate-y-20 rounded-lg bg-ink-950 px-3 py-2 text-sm font-semibold text-white focus:translate-y-0">Skip to content</a><RouterProvider router={router} /></ToastProvider></AuthProvider></QueryClientProvider></ThemeProvider>;
}
