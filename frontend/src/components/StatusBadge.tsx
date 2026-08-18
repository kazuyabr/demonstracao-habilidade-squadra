import React from 'react';

type BadgeTone = 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'primary';

const TONE_BY_STATUS: Record<string, BadgeTone> = {
  CONFIRMED: 'success',
  COMPLETED: 'success',
  PAID: 'success',
  AUTHORIZED: 'success',
  RESERVED: 'info',
  PENDING: 'warning',
  IN_PROGRESS: 'info',
  EXECUTING: 'info',
  COMPENSATING: 'warning',
  COMPENSATED: 'warning',
  CANCELLED: 'danger',
  FAILED: 'danger',
  REJECTED: 'danger'
};

export default function StatusBadge({ status }: { status?: string }) {
  const normalized = (status || '').toUpperCase();
  const tone = TONE_BY_STATUS[normalized] || 'neutral';
  const label = status || '—';
  return (
    <span className={`badge badge-${tone}`}>
      <span className="dot" />
      {label}
    </span>
  );
}
