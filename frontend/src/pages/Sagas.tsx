import React, { useEffect, useState } from 'react';
import axios from 'axios';
import config from '../config';

interface Saga {
  id: string;
  sagaId: string;
  orderId: string;
  status: string;
  currentStepId: string;
  completedSteps: number;
  totalSteps: number;
  createdAt: string;
}

function Sagas() {
  const [sagas, setSagas] = useState<Saga[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchSagas = async () => {
      try {
        const response = await axios.get(`${config.apiGateway}/api/sagas`);
        setSagas(response.data);
      } catch (error) {
        console.error('Error fetching sagas:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchSagas();
  }, []);

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return '#4caf50';
      case 'IN_PROGRESS': return '#2196f3';
      case 'COMPENSATING': return '#ff9800';
      case 'COMPENSATED': return '#ff5722';
      case 'FAILED': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading...</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Saga Orchestrator</h1>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #ddd' }}>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Saga ID</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Order ID</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Status</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Progress</th>
            <th style={{ padding: '0.5rem', textAlign: 'left' }}>Created</th>
          </tr>
        </thead>
        <tbody>
          {sagas.map((saga) => (
            <tr key={saga.id} style={{ borderBottom: '1px solid #ddd' }}>
              <td style={{ padding: '0.5rem' }}>{saga.sagaId?.substring(0, 8)}...</td>
              <td style={{ padding: '0.5rem' }}>{saga.orderId?.substring(0, 8)}...</td>
              <td style={{ padding: '0.5rem' }}>
                <span style={{
                  padding: '0.25rem 0.5rem',
                  borderRadius: '4px',
                  background: getStatusColor(saga.status),
                  color: 'white'
                }}>
                  {saga.status}
                </span>
              </td>
              <td style={{ padding: '0.5rem' }}>
                {saga.completedSteps}/{saga.totalSteps}
              </td>
              <td style={{ padding: '0.5rem' }}>{new Date(saga.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default Sagas;
