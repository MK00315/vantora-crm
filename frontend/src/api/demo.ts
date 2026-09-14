import type { Activity, AuthUserSummary, Customer, DashboardData, FileSummary, Lead, PageResponse, Task, User } from '@/types';

const now = new Date();
const date = (offset: number) => new Date(now.getTime() + offset * 86_400_000).toISOString();

export const demoUser: User & AuthUserSummary = {
  id: 'usr-demo-1',
  firstName: 'Maya',
  lastName: 'Chen',
  email: 'maya@vantora.demo',
  role: 'COMPANY_ADMIN',
  status: 'ACTIVE',
  tenantId: 'tenant-demo',
  tenantName: 'Aurora Labs',
};

export const demoLeads: Lead[] = [
  { id: 'l1', firstName: 'Olivia', lastName: 'Martin', company: 'Atlas Works', email: 'olivia@atlas.works', stage: 'NEW', estimatedValue: 18000, source: 'Referral', ownerId: demoUser.id, createdAt: date(-5) },
  { id: 'l2', firstName: 'Noah', lastName: 'Williams', company: 'Vanta North', email: 'noah@vantanorth.io', stage: 'CONTACTED', estimatedValue: 32000, source: 'Website', ownerId: demoUser.id, createdAt: date(-12) },
  { id: 'l3', firstName: 'Ava', lastName: 'Patel', company: 'Solace Health', email: 'ava@solace.health', stage: 'QUALIFIED', estimatedValue: 46000, source: 'Conference', ownerId: demoUser.id, createdAt: date(-17) },
  { id: 'l4', firstName: 'Ethan', lastName: 'Brooks', company: 'Ember Finance', email: 'ethan@ember.finance', stage: 'PROPOSAL', estimatedValue: 72500, source: 'Outbound', ownerId: demoUser.id, createdAt: date(-21) },
  { id: 'l5', firstName: 'Sophia', lastName: 'Reed', company: 'Luma Design', email: 'sophia@luma.design', stage: 'WON', estimatedValue: 28000, source: 'Partner', ownerId: demoUser.id, createdAt: date(-36) },
  { id: 'l6', firstName: 'Liam', lastName: 'Carter', company: 'Harbor AI', email: 'liam@harbor.ai', stage: 'QUALIFIED', estimatedValue: 54000, source: 'LinkedIn', ownerId: demoUser.id, createdAt: date(-9) },
];

export const demoCustomers: Customer[] = [
  { id: 'c1', name: 'Sophia Reed', company: 'Luma Design', email: 'sophia@luma.design', phone: '+1 415 555 0134', status: 'ACTIVE', website: 'https://luma.design', ownerId: demoUser.id, createdAt: date(-120) },
  { id: 'c2', name: 'James Kim', company: 'Mosaic Systems', email: 'james@mosaic.systems', phone: '+1 617 555 0177', status: 'ACTIVE', ownerId: demoUser.id, createdAt: date(-84) },
  { id: 'c3', name: 'Amelia Jones', company: 'Evergreen Co.', email: 'amelia@evergreen.co', phone: '+1 206 555 0109', status: 'PROSPECT', ownerId: demoUser.id, createdAt: date(-42) },
  { id: 'c4', name: 'Lucas Silva', company: 'Nimble Cloud', email: 'lucas@nimble.cloud', phone: '+44 20 7946 0111', status: 'ACTIVE', ownerId: demoUser.id, createdAt: date(-66) },
  { id: 'c5', name: 'Isabella Moore', company: 'Fieldstone', email: 'isabella@fieldstone.co', status: 'INACTIVE', ownerId: demoUser.id, createdAt: date(-210) },
];

export const demoTasks: Task[] = [
  { id: 't1', title: 'Prepare Ember Finance proposal', description: 'Finalize pricing and security appendix.', status: 'IN_PROGRESS', priority: 'URGENT', dueAt: date(1), assigneeId: demoUser.id, leadId: 'l4', createdAt: date(-4) },
  { id: 't2', title: 'Discovery call with Harbor AI', status: 'TODO', priority: 'HIGH', dueAt: date(3), assigneeId: demoUser.id, leadId: 'l6', createdAt: date(-2) },
  { id: 't3', title: 'Send QBR notes to Mosaic', status: 'TODO', priority: 'MEDIUM', dueAt: date(5), assigneeId: demoUser.id, createdAt: date(-1) },
  { id: 't4', title: 'Update onboarding playbook', status: 'COMPLETED', priority: 'LOW', dueAt: date(-1), assigneeId: demoUser.id, createdAt: date(-8) },
  { id: 't5', title: 'Follow up with Atlas Works', status: 'TODO', priority: 'MEDIUM', dueAt: date(-2), assigneeId: demoUser.id, leadId: 'l1', createdAt: date(-6) },
];

export const demoUsers: User[] = [
  demoUser,
  { id: 'u2', firstName: 'Jonah', lastName: 'Park', email: 'jonah@auroralabs.io', role: 'HR', status: 'ACTIVE', lastLoginAt: date(-1) },
  { id: 'u3', firstName: 'Priya', lastName: 'Nair', email: 'priya@auroralabs.io', role: 'RECRUITER', status: 'ACTIVE', lastLoginAt: date(-2) },
  { id: 'u4', firstName: 'Marcus', lastName: 'Lee', email: 'marcus@auroralabs.io', role: 'EMPLOYEE', status: 'ACTIVE', lastLoginAt: date(-4) },
  { id: 'u5', firstName: 'Elena', lastName: 'Garcia', email: 'elena@auroralabs.io', role: 'EMPLOYEE', status: 'SUSPENDED', lastLoginAt: date(-30) },
];

export const demoActivities: Activity[] = [
  { id: 'a1', actorId: demoUser.id, action: 'STAGE_CHANGED', entityType: 'LEAD', entityId: 'l4', summary: 'Moved Ember Finance to Proposal', createdAt: new Date(now.getTime() - 22 * 60_000).toISOString() },
  { id: 'a2', actorId: demoUsers[1].id, action: 'CREATED', entityType: 'CUSTOMER', entityId: 'c4', summary: 'Added Nimble Cloud as a customer', createdAt: new Date(now.getTime() - 3 * 3_600_000).toISOString() },
  { id: 'a3', actorId: demoUsers[2].id, action: 'STATUS_CHANGED', entityType: 'TASK', entityId: 't4', summary: 'Completed onboarding playbook update', createdAt: new Date(now.getTime() - 7 * 3_600_000).toISOString() },
  { id: 'a4', actorId: demoUser.id, action: 'CREATED', entityType: 'LEAD', entityId: 'l6', summary: 'Captured Harbor AI from LinkedIn', createdAt: date(-1) },
];

export const demoFiles: FileSummary[] = [
  { id: 'f1', key: 'tenant-demo/revenue-playbook.pdf', filename: 'Revenue playbook.pdf', contentType: 'application/pdf', size: 2_483_712, createdAt: date(-3) },
  { id: 'f2', key: 'tenant-demo/customer-import.csv', filename: 'Customer import.csv', contentType: 'text/csv', size: 84_920, createdAt: date(-8) },
  { id: 'f3', key: 'tenant-demo/brand-reference.webp', filename: 'Brand reference.webp', contentType: 'image/webp', size: 1_196_440, createdAt: date(-14) },
];

export const demoDashboard: DashboardData = {
  totalLeads: 38,
  totalCustomers: 24,
  activeCustomers: 21,
  openTasks: 17,
  overdueTasks: 3,
  wonValue: 384500,
  conversionRate: 24.8,
  pipeline: [
    { stage: 'NEW', count: 11, value: 128000 },
    { stage: 'CONTACTED', count: 9, value: 186000 },
    { stage: 'QUALIFIED', count: 8, value: 224000 },
    { stage: 'PROPOSAL', count: 6, value: 198000 },
    { stage: 'NEGOTIATION', count: 5, value: 176000 },
    { stage: 'WON', count: 4, value: 142000 },
    { stage: 'LOST', count: 3, value: 68000 },
  ],
  upcomingTasks: demoTasks.filter((task) => task.status !== 'COMPLETED' && task.status !== 'CANCELLED').slice(0, 5),
};

export function page<T>(items: T[], pageNumber = 0, size = 10): PageResponse<T> {
  return {
    content: items.slice(pageNumber * size, pageNumber * size + size),
    page: pageNumber,
    size,
    totalElements: items.length,
    totalPages: Math.max(1, Math.ceil(items.length / size)),
  };
}

export function isDemoMode() {
  return import.meta.env.VITE_DEMO_MODE === 'true';
}

export function delay<T>(value: T, ms = 240): Promise<T> {
  return new Promise((resolve) => window.setTimeout(() => resolve(value), ms));
}
