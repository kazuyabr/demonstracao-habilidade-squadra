import React, { useEffect, useState } from 'react';
import Layout from '../components/Layout';
import StatusBadge from '../components/StatusBadge';
import { api } from '../api/client';

interface Product {
  id: string;
  name: string;
  sku: string;
  category?: string;
  price?: number;
  quantityAvailable?: number;
  quantityReserved?: number;
  totalQuantity?: number;
  reservedQuantity?: number;
  availableQuantity?: number;
}

function available(product: Product): number {
  return product.quantityAvailable ?? product.availableQuantity ?? 0;
}

function reserved(product: Product): number {
  return product.quantityReserved ?? product.reservedQuantity ?? 0;
}

export default function Inventory() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        const res = await api.get('/api/inventory/products');
        const data = res.data?.content ?? res.data ?? [];
        setProducts(Array.isArray(data) ? data : []);
      } catch {
        setError(true);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <Layout title="Inventory">
      <h1 className="page-title">Inventory</h1>
      <p className="page-subtitle">Product catalog and stock levels (MongoDB).</p>

      <div className="card">
        {loading ? (
          <div className="loading-row"><span className="spinner" /> Loading inventory…</div>
        ) : error ? (
          <div className="empty-state">
            <div className="empty-icon">⚠️</div>
            <h3>Could not load inventory</h3>
          </div>
        ) : products.length === 0 ? (
          <div className="empty-state">
            <div className="empty-icon">▥</div>
            <h3>No products in the catalog</h3>
            <p>Seed the MongoDB catalog to see products here.</p>
          </div>
        ) : (
          <div className="table-wrap">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Product</th>
                  <th>SKU</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Available</th>
                  <th>Reserved</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {products.map((p) => {
                  const avail = available(p);
                  const resv = reserved(p);
                  const inStock = avail > 0;
                  return (
                    <tr key={p.id}>
                      <td>{p.name}</td>
                      <td className="mono">{p.sku}</td>
                      <td className="muted">{p.category || '—'}</td>
                      <td>{p.price != null ? `R$ ${Number(p.price).toLocaleString('pt-BR')}` : '—'}</td>
                      <td>{avail}</td>
                      <td className="muted">{resv}</td>
                      <td><StatusBadge status={inStock ? 'In stock' : 'Out of stock'} /></td>
                    </tr>
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
