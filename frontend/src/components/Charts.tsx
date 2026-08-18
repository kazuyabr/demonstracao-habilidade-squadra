import React from 'react';

const COLORS = ['#4f46e5', '#16a34a', '#d97706', '#dc2626', '#2563eb', '#64748b'];

export interface DonutSlice {
  label: string;
  value: number;
  color: string;
}

export default function DonutChart({ slices, totalLabel = 'Total' }: { slices: DonutSlice[]; totalLabel?: string }) {
  const total = slices.reduce((acc, s) => acc + s.value, 0);
  const radius = 70;
  const circumference = 2 * Math.PI * radius;
  let offset = 0;

  if (total === 0) {
    return (
      <div className="donut-wrap">
        <div className="donut-center">
          <div className="donut-value">0</div>
          <div className="donut-label">{totalLabel}</div>
        </div>
      </div>
    );
  }

  return (
    <div className="donut-wrap">
      <svg width="180" height="180" viewBox="0 0 180 180">
        <g transform="rotate(-90 90 90)">
          <circle cx="90" cy="90" r={radius} fill="none" stroke="#f1f5f9" strokeWidth="22" />
          {slices.map((slice, i) => {
            const fraction = slice.value / total;
            const dash = fraction * circumference;
            const element = (
              <circle
                key={i}
                cx="90"
                cy="90"
                r={radius}
                fill="none"
                stroke={slice.color}
                strokeWidth="22"
                strokeDasharray={`${dash} ${circumference - dash}`}
                strokeDashoffset={-offset}
              />
            );
            offset += dash;
            return element;
          })}
        </g>
        <text x="90" y="86" textAnchor="middle" style={{ fontSize: '30px', fontWeight: 700, fill: '#0f172a' }}>
          {total}
        </text>
        <text x="90" y="106" textAnchor="middle" style={{ fontSize: '12px', fill: '#64748b' }}>
          {totalLabel}
        </text>
      </svg>
      <ul className="legend">
        {slices.map((slice, i) => (
          <li key={i}>
            <span className="legend-dot" style={{ background: slice.color }} />
            {slice.label}
            <span className="legend-value">{slice.value}</span>
          </li>
        ))}
      </ul>
    </div>
  );
}

export function BarRow({ label, value, max, color = COLORS[0] }: { label: string; value: number; max: number; color?: string }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div className="bar-row">
      <span className="bar-label">{label}</span>
      <div className="bar-track">
        <div className="bar-fill" style={{ width: `${pct}%`, background: color }} />
      </div>
      <span className="bar-val">{value}</span>
    </div>
  );
}
