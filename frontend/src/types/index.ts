export type Role = 'SUPER_ADMIN' | 'COMPANY_ADMIN' | 'HR' | 'RECRUITER' | 'EMPLOYEE';
export type LeadStage = 'NEW' | 'CONTACTED' | 'QUALIFIED' | 'PROPOSAL' | 'NEGOTIATION' | 'WON' | 'LOST';
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type UserStatus = 'ACTIVE' | 'INVITED' | 'SUSPENDED';

export interface User {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  avatarUrl?: string | null;
  role: Role;
  status?: UserStatus;
  lastLoginAt?: string | null;
  emailVerifiedAt?: string | null;
  tenantId?: string;
  tenantName?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** The intentionally small identity payload returned by authentication endpoints. */
export type AuthUserSummary = Pick<User, 'id' | 'firstName' | 'lastName' | 'email' | 'role'> & {
  tenantId: string;
  tenantName: string;
  emailVerifiedAt?: string | null;
};

export interface AuthResponse {
  accessToken: string;
  expiresIn?: number;
  user: AuthUserSummary;
}

export interface Customer {
  id: string;
  name: string;
  company?: string;
  email: string;
  phone?: string;
  status?: 'ACTIVE' | 'INACTIVE' | 'PROSPECT';
  website?: string;
  notes?: string;
  ownerId?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Lead {
  id: string;
  firstName: string;
  lastName: string;
  company?: string;
  email: string;
  phone?: string;
  stage: LeadStage;
  title?: string;
  estimatedValue?: number;
  source?: string;
  ownerId?: string;
  notes?: string;
  createdAt: string;
  updatedAt?: string;
}

export interface Task {
  id: string;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: Priority;
  dueAt?: string;
  assigneeId?: string;
  leadId?: string;
  completedAt?: string;
  createdAt: string;
}

export interface Activity {
  id: string;
  actorId?: string;
  action: string;
  entityType?: string;
  entityId?: string;
  summary: string;
  createdAt: string;
}

export interface FileSummary {
  id: string;
  key: string;
  filename: string;
  contentType: string;
  size: number;
  createdAt: string;
}

export interface FileDetails extends FileSummary {
  url: string;
}

export interface StoredObject {
  key: string;
  url: string;
  filename: string;
  contentType: string;
  size: number;
}

export interface DashboardData {
  totalLeads: number;
  totalCustomers: number;
  activeCustomers: number;
  openTasks: number;
  overdueTasks: number;
  wonValue: number;
  conversionRate: number;
  pipeline: Array<{ stage: LeadStage; count: number; value: number }>;
  upcomingTasks: Array<Pick<Task, 'id' | 'title' | 'status' | 'priority' | 'dueAt' | 'assigneeId'>>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiErrorPayload {
  message?: string;
  detail?: string;
  errors?: Record<string, string>;
  status?: number;
  timestamp?: string;
}
