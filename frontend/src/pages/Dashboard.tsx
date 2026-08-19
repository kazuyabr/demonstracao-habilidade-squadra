import React, { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import StatCard from '../components/StatCard';
import DonutChart, { BarRow } from '../components/Charts';
import { api } from '../api/client';

interface Stats {
  total: number;
  created: number;
  inProgress: number;
  completed: number;
  compensating: number;
  compensated: number;
  failed: number;
}

export default function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [forbidden, setForbidden] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/api/sagas/stats');
        setStats(res.data);
      } catch (e: any) {
        if (e?.response?.status === 403) setForbidden(true);
        else setError(true);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  if (loading) {
    return (
      <Layout title="Dashboard">
        <div className="loading-row"><span className="spinner" /> Loading metrics…</div>
      </Layout>
    );
  }

  if (forbidden) {
    return (
      <Layout title="Dashboard">
        <div className="empty-state">
          <div className="empty-icon">🔒</div>
          <h3>Operational metrics require the OPERATOR role</h3>
          <p>Your account has the <b>CUSTOMER</b> role. Explore <a href="/orders">Orders</a> and <a href="/inventory">Inventory</a>, or sign in with the demo account to see the full Saga overview.</p>
        </div>
      </Layout>
    );
  }

  if (error || !stats) {
    return (
      <Layout title="Dashboard">
        <div className="empty-state">
          <div className="empty-icon">⚠️</div>
          <h3>Could not load metrics</h3>
          <p>Make sure the platform is running and you are signed in.</p>
        </div>
      </Layout>
    );
  }

  const donutSlices = [
    { label: 'Completed', value: stats.completed, color: '#16a34a' },
    { label: 'In progress', value: stats.inProgress, color: '#2563eb' },
    { label: 'Compensating', value: stats.compensating, color: '#d97706' },
    { label: 'Compensated', value: stats.compensated, color: '#9333ea' },
    { label: 'Failed', value: stats.failed, color: '#dc2626' },
    { label: 'Created', value: stats.created, color: '#64748b' }
  ].filter((s) => s.value > 0);

  const barMax = Math.max(stats.completed, stats.inProgress, stats.failed, stats.compensated, 1);

  return (
    <Layout title="Dashboard">
      <h1 className="page-title">Order Processing Overview</h1>
      <p className="page-subtitle">Live view of the distributed order platform and its Sagas.</p>

      <div className="stats-grid">
        <StatCard label="Total Orders" value={stats.total} accent="primary" icon={<span>∑</span>} />
        <StatCard label="Completed" value={stats.completed} accent="success" icon={<span>✓</span>} />
        <StatCard label="In Progress" value={stats.inProgress} accent="info" icon={<span>↻</span>} />
        <StatCard label="Failed / Compensated" value={stats.failed + stats.compensated} accent="danger" icon={<span>✕</span>} />
      </div>

      <div className="grid-2">
        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Saga status</div>
              <div className="card-subtitle">Distribution by current state</div>
            </div>
          </div>
          <DonutChart slices={donutSlices} totalLabel="Sagas" />
        </div>

        <div className="card">
          <div className="card-header">
            <div>
              <div className="card-title">Saga outcomes</div>
              <div className="card-subtitle">Final and transitional states</div>
            </div>
          </div>
          <BarRow label="Completed" value={stats.completed} max={barMax} color="#16a34a" />
          <BarRow label="In progress" value={stats.inProgress} max={barMax} color="#2563eb" />
          <BarRow label="Compensated" value={stats.compensated} max={barMax} color="#9333ea" />
          <BarRow label="Failed" value={stats.failed} max={barMax} color="#dc2626" />
        </div>
      </div>
    </Layout>
  );
}
