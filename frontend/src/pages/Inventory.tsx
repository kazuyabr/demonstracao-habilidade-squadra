import React, { useEffect, useState } from 'react';
import axios from 'axios';
import config from '../config';

interface Product {
  id: string;
  name: string;
  sku: string;
  quantityAvailable: number;
  quantityReserved: number;
}

function Inventory() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchProducts = async () => {
      try {
        const response = await axios.get(`${config.apiGateway}/api/inventory/products`);
        setProducts(response.data);
      } catch (error) {
        console.error('Error fetching products:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchProducts();
  }, []);

  if (loading) return <div style={{ padding: '2rem' }}>Loading...</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Inventory</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd' }}>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Name</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>SKU</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Available</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Reserved</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Status</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '0.5rem' }}>{product.name}</td>
              <td style={{ padding: '0.5rem' }}>{product.sku}</td>
              <td style={{ padding: '0.5rem' }}>{product.quantityAvailable}</td>
              <td style={{ padding: '0.5rem' }}>{product.quantityReserved}</td>
              <td style={{ padding: '0.5rem' }}>
                <span style={{
                  padding: '0.25rem 0.5rem',
                  borderRadius: '4px',
                  background: product.quantityAvailable > 0 ? '#4caf50' : '#f44336',
                  color: 'white'
                }}>
                  {product.quantityAvailable > 0 ? 'In Stock' : 'Out of Stock'}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Inventory;
