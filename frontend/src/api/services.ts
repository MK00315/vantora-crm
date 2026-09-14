import { http, refreshSession, setAccessToken } from '@/api/http';
import {
  delay,
  demoActivities,
  demoCustomers,
  demoDashboard,
  demoFiles,
  demoLeads,
  demoTasks,
  demoUser,
  demoUsers,
  isDemoMode,
  page,
} from '@/api/demo';
import type {
  AuthResponse,
  AuthUserSummary,
  Customer,
  DashboardData,
  FileDetails,
  FileSummary,
  Lead,
  LeadStage,
  PageResponse,
  Role,
  StoredObject,
  Task,
  TaskStatus,
  User,
} from '@/types';

const verificationRequests = new Map<string, Promise<void>>();

export interface ListParams {
  page?: number;
  size?: number;
  search?: string;
  sort?: string;
  [key: string]: string | number | boolean | undefined;
}

function apiParams(params?: ListParams) {
  if (!params) return undefined;
  const { search, ...rest } = params;
  return { ...rest, q: search };
}

const filter = <T>(items: T[], search?: string) => {
  if (!search) return items;
  const query = search.toLowerCase();
  return items.filter((item) => JSON.stringify(item).toLowerCase().includes(query));
};

export const authApi = {
  async login(payload: { email: string; password: string }): Promise<AuthResponse> {
    if (isDemoMode()) return delay({ accessToken: 'demo-access-token', user: { ...demoUser, email: payload.email } });
    const { data } = await http.post<AuthResponse>('/auth/login', payload);
    setAccessToken(data.accessToken);
    return data;
  },
  async register(payload: { organizationName: string; firstName: string; lastName: string; email: string; password: string }): Promise<void> {
    if (isDemoMode()) return delay(undefined);
    const { organizationName, ...rest } = payload;
    await http.post('/auth/register', { ...rest, companyName: organizationName });
  },
  async me(): Promise<AuthUserSummary> {
    if (isDemoMode()) return delay(demoUser);
    const { data } = await http.get<AuthUserSummary>('/auth/me');
    return data;
  },
  async refresh(): Promise<AuthResponse> {
    if (isDemoMode()) return delay({ accessToken: 'demo-access-token', user: demoUser });
    return refreshSession();
  },
  async logout() {
    try {
      if (!isDemoMode()) await http.post('/auth/logout');
    } finally {
      setAccessToken(null);
    }
  },
  async forgotPassword(email: string) {
    if (!isDemoMode()) await http.post('/auth/forgot-password', { email });
    else await delay(undefined);
  },
  async resendVerification(email: string) {
    if (!isDemoMode()) await http.post('/auth/resend-verification', { email });
    else await delay(undefined);
  },
  async resetPassword(token: string, password: string) {
    if (!isDemoMode()) await http.post('/auth/reset-password', { token, password });
    else await delay(undefined);
  },
  async verifyEmail(token: string) {
    const existing = verificationRequests.get(token);
    if (existing) return existing;

    const request = (isDemoMode()
      ? delay(undefined)
      : http.post('/auth/verify-email', { token }).then(() => undefined)
    ).finally(() => {
      verificationRequests.delete(token);
    });

    verificationRequests.set(token, request);
    return request;
  },
};

export const filesApi = {
  async list(params?: Pick<ListParams, 'page' | 'size'>): Promise<PageResponse<FileSummary>> {
    if (isDemoMode()) return delay(page(demoFiles, params?.page, params?.size ?? 20));
    return (await http.get<PageResponse<FileSummary>>('/files', { params })).data;
  },
  async get(id: string): Promise<FileDetails> {
    if (isDemoMode()) {
      const file = demoFiles.find((item) => item.id === id);
      if (!file) throw new Error('File not found');
      return delay({ ...file, url: `demo://${file.key}` });
    }
    return (await http.get<FileDetails>(`/files/${id}`)).data;
  },
  async upload(file: File, onProgress?: (percent: number) => void): Promise<StoredObject> {
    if (isDemoMode()) {
      onProgress?.(35);
      await delay(undefined, 180);
      const key = `tenant-demo/${crypto.randomUUID()}-${file.name}`;
      demoFiles.unshift({
        id: crypto.randomUUID(),
        key,
        filename: file.name,
        contentType: file.type,
        size: file.size,
        createdAt: new Date().toISOString(),
      });
      onProgress?.(100);
      return delay({ key, url: `demo://${key}`, filename: file.name, contentType: file.type, size: file.size });
    }

    const formData = new FormData();
    formData.append('file', file);
    const { data } = await http.post<StoredObject>('/files', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (event) => {
        if (event.total) onProgress?.(Math.min(100, Math.round((event.loaded / event.total) * 100)));
      },
    });
    return data;
  },
  async content(file: FileDetails): Promise<Blob> {
    if (isDemoMode()) return delay(new Blob(['Demo file content'], { type: 'text/plain' }));

    const target = new URL(file.url, window.location.origin);
    if (!['http:', 'https:'].includes(target.protocol)) throw new Error('The download URL is not valid');

    // Local storage is served by the authenticated backend. Always rewrite that URL
    // through the configured API client, even when the backend reports its dev host.
    // This keeps local downloads working through Vite/nginx without trusting the host
    // supplied in the response.
    const contentPrefix = '/api/v1/files/content/';
    if (target.pathname.startsWith(contentPrefix)) {
      const apiPath = `/files/content/${target.pathname.slice(contentPrefix.length)}${target.search}`;
      return (await http.get<Blob>(apiPath, { responseType: 'blob' })).data;
    }

    // S3-compatible providers return a short-lived presigned URL. Fetch it without
    // axios, cookies, or the in-memory Authorization header so credentials can never
    // be disclosed to an object-storage origin.
    const response = await fetch(target, {
      credentials: 'omit',
      referrerPolicy: 'no-referrer',
    });
    if (!response.ok) throw new Error('The file could not be downloaded');
    return response.blob();
  },
  async remove(key: string) {
    if (isDemoMode()) {
      const index = demoFiles.findIndex((file) => file.key === key);
      if (index >= 0) demoFiles.splice(index, 1);
      return delay(undefined);
    }
    await http.delete('/files', { params: { key } });
  },
};

export const dashboardApi = {
  async get(): Promise<DashboardData> {
    if (isDemoMode()) return delay(demoDashboard);
    return (await http.get<DashboardData>('/dashboard')).data;
  },
  async activities(params?: ListParams) {
    if (isDemoMode()) return delay(page(filter(demoActivities, params?.search), params?.page, params?.size));
    return (await http.get('/activities', { params: apiParams(params) })).data;
  },
};

export const leadsApi = {
  async list(params?: ListParams): Promise<PageResponse<Lead>> {
    if (isDemoMode()) {
      const rows = filter(demoLeads, params?.search).filter((l) => !params?.stage || l.stage === params.stage);
      return delay(page(rows, params?.page, params?.size ?? 100));
    }
    return (await http.get<PageResponse<Lead>>('/leads', { params: apiParams(params) })).data;
  },
  async create(payload: Partial<Lead>): Promise<Lead> {
    if (isDemoMode()) {
      const row = { ...payload, id: crypto.randomUUID(), createdAt: new Date().toISOString() } as Lead;
      demoLeads.unshift(row);
      return delay(row);
    }
    return (await http.post<Lead>('/leads', payload)).data;
  },
  async update(id: string, payload: Partial<Lead>): Promise<Lead> {
    if (isDemoMode()) {
      const index = demoLeads.findIndex((l) => l.id === id);
      demoLeads[index] = { ...demoLeads[index], ...payload };
      return delay(demoLeads[index]);
    }
    return (await http.put<Lead>(`/leads/${id}`, payload)).data;
  },
  async updateStage(id: string, stage: LeadStage): Promise<Lead> {
    if (isDemoMode()) return this.update(id, { stage });
    return (await http.patch<Lead>(`/leads/${id}/stage`, { stage })).data;
  },
  async remove(id: string) {
    if (isDemoMode()) {
      const index = demoLeads.findIndex((lead) => lead.id === id);
      if (index >= 0) demoLeads.splice(index, 1);
      return delay(undefined);
    }
    await http.delete(`/leads/${id}`);
  },
};

export const customersApi = {
  async list(params?: ListParams): Promise<PageResponse<Customer>> {
    if (isDemoMode()) {
      const rows = filter(demoCustomers, params?.search).filter((c) => !params?.status || c.status === params.status);
      return delay(page(rows, params?.page, params?.size ?? 10));
    }
    return (await http.get<PageResponse<Customer>>('/customers', { params: apiParams(params) })).data;
  },
  async create(payload: Partial<Customer>): Promise<Customer> {
    if (isDemoMode()) {
      const row = { ...payload, id: crypto.randomUUID(), createdAt: new Date().toISOString() } as Customer;
      demoCustomers.unshift(row);
      return delay(row);
    }
    return (await http.post<Customer>('/customers', payload)).data;
  },
  async update(id: string, payload: Partial<Customer>): Promise<Customer> {
    if (isDemoMode()) {
      const index = demoCustomers.findIndex((c) => c.id === id);
      demoCustomers[index] = { ...demoCustomers[index], ...payload };
      return delay(demoCustomers[index]);
    }
    return (await http.put<Customer>(`/customers/${id}`, payload)).data;
  },
  async remove(id: string) {
    if (isDemoMode()) {
      const index = demoCustomers.findIndex((customer) => customer.id === id);
      if (index >= 0) demoCustomers.splice(index, 1);
      return delay(undefined);
    }
    await http.delete(`/customers/${id}`);
  },
  async exportCsv(): Promise<Blob> {
    if (isDemoMode()) {
      const lines = ['Name,Company,Email,Status,Website', ...demoCustomers.map((c) => [c.name, c.company, c.email, c.status, c.website].map((v) => `"${v ?? ''}"`).join(','))];
      return delay(new Blob([lines.join('\n')], { type: 'text/csv' }));
    }
    return (await http.get('/customers/export', { responseType: 'blob' })).data;
  },
};

export const tasksApi = {
  async list(params?: ListParams): Promise<PageResponse<Task>> {
    if (isDemoMode()) {
      const rows = filter(demoTasks, params?.search).filter((t) => !params?.status || t.status === params.status);
      return delay(page(rows, params?.page, params?.size ?? 100));
    }
    return (await http.get<PageResponse<Task>>('/tasks', { params: apiParams(params) })).data;
  },
  async create(payload: Partial<Task>): Promise<Task> {
    if (isDemoMode()) {
      const row = { ...payload, id: crypto.randomUUID(), createdAt: new Date().toISOString() } as Task;
      demoTasks.unshift(row);
      return delay(row);
    }
    return (await http.post<Task>('/tasks', payload)).data;
  },
  async update(id: string, payload: Partial<Task>): Promise<Task> {
    if (isDemoMode()) {
      const index = demoTasks.findIndex((t) => t.id === id);
      demoTasks[index] = { ...demoTasks[index], ...payload };
      return delay(demoTasks[index]);
    }
    return (await http.put<Task>(`/tasks/${id}`, payload)).data;
  },
  async updateStatus(id: string, status: TaskStatus): Promise<Task> {
    if (isDemoMode()) return this.update(id, { status });
    return (await http.patch<Task>(`/tasks/${id}/status`, { status })).data;
  },
  async remove(id: string) {
    if (isDemoMode()) {
      const index = demoTasks.findIndex((task) => task.id === id);
      if (index >= 0) demoTasks.splice(index, 1);
      return delay(undefined);
    }
    await http.delete(`/tasks/${id}`);
  },
};

export const usersApi = {
  async list(params?: ListParams): Promise<PageResponse<User>> {
    if (isDemoMode()) return delay(page(filter(demoUsers, params?.search), params?.page, params?.size ?? 20));
    return (await http.get<PageResponse<User>>('/users', { params: apiParams(params) })).data;
  },
  async invite(payload: { firstName: string; lastName: string; email: string; role: Role }): Promise<User> {
    if (isDemoMode()) {
      const member: User = { ...payload, id: crypto.randomUUID(), status: 'INVITED', createdAt: new Date().toISOString() };
      demoUsers.unshift(member);
      return delay(member);
    }
    return (await http.post<User>('/users', payload)).data;
  },
  async update(id: string, payload: Partial<User>): Promise<User> {
    if (isDemoMode()) {
      const index = demoUsers.findIndex((user) => user.id === id);
      demoUsers[index] = { ...demoUsers[index], ...payload };
      return delay(demoUsers[index]);
    }
    return (await http.put<User>(`/users/${id}`, payload)).data;
  },
  async remove(id: string) {
    if (!isDemoMode()) await http.delete(`/users/${id}`);
    else {
      const member = demoUsers.find((user) => user.id === id);
      if (member) member.status = 'SUSPENDED';
      await delay(undefined);
    }
  },
};

export const profileApi = {
  async update(payload: Pick<User, 'firstName' | 'lastName'>): Promise<User> {
    if (isDemoMode()) {
      Object.assign(demoUser, payload);
      return delay({ ...demoUser });
    }
    return (await http.put<User>('/users/me', payload)).data;
  },
  async changePassword(payload: { currentPassword: string; newPassword: string }) {
    if (!isDemoMode()) await http.put('/users/me/password', payload);
    else await delay(undefined);
  },
};
