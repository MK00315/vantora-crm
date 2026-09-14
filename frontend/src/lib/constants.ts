import type { LeadStage, Priority, Role, TaskStatus } from '@/types';

export const LEAD_STAGES: Array<{ value: LeadStage; label: string; color: string }> = [
  { value: 'NEW', label: 'New', color: 'bg-sky-600' },
  { value: 'CONTACTED', label: 'Contacted', color: 'bg-cyan-600' },
  { value: 'QUALIFIED', label: 'Qualified', color: 'bg-cyan-700' },
  { value: 'PROPOSAL', label: 'Proposal', color: 'bg-teal-600' },
  { value: 'NEGOTIATION', label: 'Negotiation', color: 'bg-teal-700' },
  { value: 'WON', label: 'Won', color: 'bg-emerald-600' },
  { value: 'LOST', label: 'Lost', color: 'bg-rose-600' },
];

export const TASK_STATUSES: Array<{ value: TaskStatus; label: string }> = [
  { value: 'TODO', label: 'To do' },
  { value: 'IN_PROGRESS', label: 'In progress' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export const PRIORITIES: Array<{ value: Priority; label: string }> = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
  { value: 'URGENT', label: 'Urgent' },
];

export const ROLE_LABELS: Record<Role, string> = {
  SUPER_ADMIN: 'Platform admin',
  COMPANY_ADMIN: 'Company admin',
  HR: 'HR',
  RECRUITER: 'Recruiter',
  EMPLOYEE: 'Employee',
};

export const canManage = (role?: Role) => role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'HR' || role === 'RECRUITER';
export const canDeleteRecords = (role?: Role) => role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN' || role === 'HR';
export const canManageUsers = (role?: Role) => role === 'SUPER_ADMIN' || role === 'COMPANY_ADMIN';
export const canManageTeamMember = (actorRole?: Role, targetRole?: Role) =>
  canManageUsers(actorRole) && (targetRole !== 'SUPER_ADMIN' || actorRole === 'SUPER_ADMIN');
