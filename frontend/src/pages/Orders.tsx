import React, { useCallback, useEffect, useState } from 'react';
import Layout from '../components/Layout';
import StatusBadge from '../components/StatusBadge';
import { api } from '../api/client';

interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  status: string;
  totalAmount: number;
  currency: string;
  createdAt: string;
}

export default function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState('');

  const [productName, setProductName] = useState('Laptop');
  const [productId, setProductId] = useState('PROD-001');
  const [quantity, setQuantity] = useState('1');
  const [unitPrice, setUnitPrice] = useState('4999.99');

  const load = useCallback(async () => {
    try {
      const res = await api.get('/api/orders');
      const data = res.data?.content ?? res.data ?? [];
      setOrders(Array.isArray(data) ? data : []);
    } catch {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const createOrder = async () => {
    setFormError('');
    setCreating(true);
    try {
      const body = {
        customerId: '3f2504e0-4f89-41d3-9a0c-0305e82c3301',
        items: [
          {
            productId,
            productName,
            quantity: Number(quantity),
            unitPrice: Number(unitPrice)
          }
        ]
      };
      await api.post('/api/orders', body);
      setShowForm(false);
      setLoading(true);
      await load();
    } catch (e: any) {
      setFormError(e?.response?.data?.message || 'Failed to create order');
    } finally {
      setCreating(false);
    }
  };

  return (
    <Layout title="Orders">
      <div className="flex-between mb-3">
        <div>
          <h1 className="page-title">Orders</h1>
          <p className="page-subtitle">Orders are processed through the Saga orchestrator.</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
          {showForm ? 'Cancel' : '+ New order'}
        </button>
      </div>

      {showForm && (
        <div className="card mb-3">
          <div className="card-header">
            <div className="card-title">Create order</div>
            <div className="card-subtitle">This triggers the payment → inventory → confirm Saga</div>
          </div>
          <div className="form-grid">
            <div className="form-field">
              <label>Product name</label>
              <input value={productName} onChange={(e) => setProductName(e.target.value)} />
            </div>
            <div className="form-field">
              <label>Product ID</label>
              <input value={productId} onChange={(e) => setProductId(e.target.value)} />
            </div>
            <div className="form-field">
              <label>Quantity</label>
              <input type="number" min="1" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            </div>
            <div className="form-field">
              <label>Unit price (BRL)</label>
              <input type="number" step="0.01" value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} />
            </div>
          </div>
          {formError && <p className="text-sm" style={{ color: 'var(--danger)' }}>{formError}</p>}
          <div className="form-actions">
            <button className="btn btn-outline" onClick={() => setShowForm(false)}>Cancel</button>
            <button className="btn btn-primary" disabled={creating} onClick={createOrder}>
              {creating ? 'Creating…' : 'Create order'}
            </button>
          </div>
        </div>
      )}

      <div className="card">
        {loading ? (
          <div className="loading-row"><span className="spinner" /> Loading orders…</div>
        ) : error ? (
          <div className="empty-state">
            <div className="empty-icon">⚠️</div>
            <h3>Could not load orders</h3>
          </div>
        ) : orders.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">▤</div>
            <h3>No orders yet</h3>
            <p>Create an order to see the Saga in action.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Order number</th>
                  <th>Status</th>
                  <th>Amount</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {orders.map((order) => (
                  <tr key={order.id}>
                    <td className="mono">{order.orderNumber}</td>
                    <td><StatusBadge status={order.status} /></td>
                    <td>{order.currency} {Number(order.totalAmount).toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</td>
                    <td className="muted">{order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </Layout>
  );
}
