import React, { ReactNode } from 'react';

export default function StatCard({
  label,
  value,
  icon,
  accent = 'primary'
}: {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
  accent?: 'success' | 'warning' | 'danger' | 'info' | 'primary';
}) {
  return (
    <div className={`stat-card accent-${accent}`}>
      <div className="stat-label">{label}</div>
      <div className="stat-value">{value}</div>
      {icon && <div className="stat-icon">{icon}</div>}
    </div>
  );
}
