import { describe, expect, it } from 'vitest';
import { formatCurrency, fromDateTimeLocal, initials, personName, safeHttpUrl, toDateTimeLocal } from '@/lib/utils';

describe('formatting helpers', () => {
  it('formats people consistently', () => {
    expect(personName({ firstName: 'Maya', lastName: 'Chen' })).toBe('Maya Chen');
    expect(initials('Maya', 'Chen')).toBe('MC');
  });

  it('renders a currency value', () => {
    expect(formatCurrency(1250)).toContain('1,250');
  });

  it('round-trips UTC task deadlines through a datetime-local field', () => {
    const instant = '2026-08-24T12:30:00.000Z';
    expect(fromDateTimeLocal(toDateTimeLocal(instant))).toBe(instant);
  });

  it('allows only HTTP and HTTPS customer websites', () => {
    expect(safeHttpUrl('https://example.com/customer')).toBe('https://example.com/customer');
    expect(safeHttpUrl('javascript:alert(1)')).toBeUndefined();
    expect(safeHttpUrl('ftp://example.com/file')).toBeUndefined();
  });
});
