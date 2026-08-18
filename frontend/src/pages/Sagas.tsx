import React, { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import StatusBadge from '../components/StatusBadge';
import { api } from '../api/client';

interface SagaStepView {
  id: number;
  stepId: string;
  name: string;
  status: string;
  type: string;
  stepOrder?: number;
  startedAt?: number[];
  completedAt?: number[];
}

interface Saga {
  id: number;
  sagaId: string;
  orderId: string;
  orderNumber?: string;
  status: string;
  currentStepId?: string;
  completedSteps: number;
  totalSteps: number;
  createdAt?: number[];
  steps?: SagaStepView[];
}

function formatDate(value?: number[]): string {
  if (!value) return '—';
  const [y, m, d, h, min] = value;
  return new Date(y, m - 1, d, h, min).toLocaleString();
}

function toDateTime(value?: number[]): Date | null {
  if (!value || value.length < 3) return null;
  return new Date(value[0], value[1] - 1, value[2], value[3] || 0, value[4] || 0, value[5] || 0);
}

export default function Sagas() {
  const [sagas, setSagas] = useState<Saga[]>([]);
  const [expanded, setExpanded] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/api/sagas');
        const data = res.data?.content ?? res.data ?? [];
        setSagas(Array.isArray(data) ? data : []);
      } catch {
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    load();
    const timer = setInterval(load, 8000);
    return () => clearInterval(timer);
  }, []);

  const toggle = (id: number) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <Layout title="Sagas">
      <h1 className="page-title">Saga Orchestrator</h1>
      <p className="page-subtitle">Distributed transactions with compensating steps. Auto-refreshes.</p>

      <div className="card">
        {loading ? (
          <div className="loading-row"><span className="spinner" /> Loading sagas…</div>
        ) : error ? (
          <div className="empty-state">
            <div className="empty-icon">⚠️</div>
            <h3>Could not load sagas</h3>
          </div>
        ) : sagas.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">◈</div>
            <h3>No sagas yet</h3>
            <p>Create an order to start an order-processing Saga.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th></th>
                  <th>Saga ID</th>
                  <th>Order</th>
                  <th>Status</th>
                  <th>Progress</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {sagas.map((saga) => {
                  const expandedThis = expanded.has(saga.id);
                  const started = toDateTime(saga.createdAt);
                  return (
                    <React.Fragment key={saga.id}>
                      <tr onClick={() => toggle(saga.id)} style={{ cursor: 'pointer' }}>
                        <td>{expandedThis ? '▾' : '▸'}</td>
                        <td className="mono">{saga.sagaId?.substring(0, 8)}…</td>
                        <td className="mono">{saga.orderNumber || saga.orderId?.substring(0, 8) + '…'}</td>
                        <td><StatusBadge status={saga.status} /></td>
                        <td>
                          <div className="progress-track">
                            <div className="progress-fill" style={{ width: `${saga.totalSteps ? Math.round((saga.completedSteps / saga.totalSteps) * 100) : 0}%` }} />
                          </div>
                          <div className="progress-label">{saga.completedSteps}/{saga.totalSteps} steps</div>
                        </td>
                        <td className="muted">{started ? started.toLocaleString() : '—'}</td>
                      </tr>
                      {expandedThis && (
                        <tr>
                          <td colSpan={6} style={{ background: 'var(--row-hover)' }}>
                            <div style={{ padding: '0.5rem 0' }}>
                              <strong className="text-sm">Steps</strong>
                              {(saga.steps || []).length === 0 && <div className="text-sm muted mt-1">No step details available.</div>}
                              {(saga.steps || []).map((step) => (
                                <div key={step.id} className="flex items-center gap-2 mt-1 text-sm">
                                  <span className="muted" style={{ width: 12 }}>{step.stepOrder || ''}</span>
                                  <span style={{ minWidth: 180 }}>{step.name}</span>
                                  <StatusBadge status={step.status} />
                                  <span className="muted" style={{ marginLeft: 'auto' }}>{step.type}</span>
                                </div>
                              ))}
                            </div>
                          </td>
                        </tr>
                      )}
                    </React.Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
