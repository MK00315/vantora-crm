import { lazy, Suspense } from 'react';
import { createBrowserRouter, Navigate } from 'react-router-dom';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { AppShell } from '@/components/layout/AppShell';
import { PageSkeleton } from '@/components/ui';
import { LeadsPage } from '@/pages/LeadsPage';
import { CustomersPage } from '@/pages/CustomersPage';
import { TasksPage } from '@/pages/TasksPage';
import { TeamPage } from '@/pages/TeamPage';
import { FilesPage } from '@/pages/FilesPage';
import { HelpPage, NotFoundPage, ProfilePage, SettingsPage } from '@/pages/AccountPages';
import { ForgotPasswordPage, LoginPage, RegisterPage, ResendVerificationPage, ResetPasswordPage, VerifyEmailPage } from '@/pages/auth/AuthPages';

const DashboardPage = lazy(() => import('@/pages/DashboardPage').then((module) => ({ default: module.DashboardPage })));

export const router = createBrowserRouter([
  { path: '/', element: <Navigate to="/app" replace /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/resend-verification', element: <ResendVerificationPage /> },
  { path: '/reset-password', element: <ResetPasswordPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  {
    element: <ProtectedRoute />,
    children: [{
      path: '/app',
      element: <AppShell />,
      children: [
        { index: true, element: <Suspense fallback={<PageSkeleton />}><DashboardPage /></Suspense> },
        { path: 'leads', element: <LeadsPage /> },
        { path: 'customers', element: <CustomersPage /> },
        { path: 'tasks', element: <TasksPage /> },
        { path: 'files', element: <FilesPage /> },
        { path: 'team', element: <TeamPage /> },
        { path: 'profile', element: <ProfilePage /> },
        { path: 'settings', element: <SettingsPage /> },
        { path: 'help', element: <HelpPage /> },
      ],
    }],
  },
  { path: '*', element: <NotFoundPage /> },
]);
