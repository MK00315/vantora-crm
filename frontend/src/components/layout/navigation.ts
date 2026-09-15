import { Building2, CheckSquare2, CircleHelp, ContactRound, LayoutDashboard, Paperclip, Settings, UserRound, UsersRound } from 'lucide-react';

export const workspacePages = [
  { to: '/app', label: 'Overview', description: 'Workspace metrics and recent activity', icon: LayoutDashboard, end: true },
  { to: '/app/leads', label: 'Leads', description: 'Your contacts and sales pipeline', icon: ContactRound },
  { to: '/app/customers', label: 'Customers', description: 'Companies and customer relationships', icon: Building2 },
  { to: '/app/tasks', label: 'Tasks', description: 'Deadlines and next steps', icon: CheckSquare2 },
  { to: '/app/files', label: 'Files', description: 'Documents and shared resources', icon: Paperclip },
  { to: '/app/team', label: 'Team', description: 'Members and workspace access', icon: UsersRound },
];

export const supportPages = [
  { to: '/app/settings', label: 'Settings', description: 'Display and workspace preferences', icon: Settings },
  { to: '/app/help', label: 'Help center', description: 'Guidance for using Vantora', icon: CircleHelp },
];

export const searchablePages = [...workspacePages, ...supportPages,
  { to: '/app/profile', label: 'Profile & security', description: 'Your details and password', icon: UserRound },
];
