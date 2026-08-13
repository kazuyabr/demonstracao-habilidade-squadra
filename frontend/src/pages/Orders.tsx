import React, { useEffect, useState } from 'react';
import axios from 'axios';
import config from '../config';

interface Order {
  id: string;
  orderNumber: string;
  customerId: string;
  status: string;
  totalAmount: number;
  createdAt: string;
}

function Orders() {
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchOrders = async () => {
      try {
        const response = await axios.get(`${config.apiGateway}/api/orders`);
        setOrders(response.data);
      } catch (error) {
        console.error('Error fetching orders:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'CONFIRMED': return '#4caf50';
      case 'PENDING': return '#ff9800';
      case 'CANCELLED': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading...</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Orders</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd' }}>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Order Number</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Customer</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Status</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Amount</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Created</th>
          </tr>
        </thead>
        <tbody>
          {orders.map((order) => (
            <tr key={order.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '0.5rem' }}>{order.orderNumber}</td>
              <td style={{ padding: '0.5rem' }}>{order.customerId}</td>
              <td style={{ padding: '0.5rem' }}>
                <span style={{
                  padding: '0.25rem 0.5rem',
                  borderRadius: '4px',
                  background: getStatusColor(order.status),
                  color: 'white'
                }}>
                  {order.status}
                </span>
              </td>
              <td style={{ padding: '0.5rem' }}>${order.totalAmount?.toFixed(2)}</td>
              <td style={{ padding: '0.5rem' }}>{new Date(order.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Orders;
