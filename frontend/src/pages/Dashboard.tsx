import React, { useEffect, useState } from 'react';
import axios from 'axios';
import config from '../config';

interface Stats {
  total: number;
  created: number;
  inProgress: number;
  completed: number;
  compensating: number;
  compensated: number;
  failed: number;
}

function Dashboard() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await axios.get(`${config.apiGateway}/api/sagas/stats`);
        setStats(response.data);
      } catch (error) {
        console.error('Error fetching stats:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) return <div style={{ padding: '2rem' }}>Loading...</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Enterprise Order Platform - Dashboard</h1>
      <p>Welcome to the Enterprise Order Processing Platform</p>

      <h2>Saga Statistics</h2>
      {stats ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '1rem' }}>
          <div style={{ padding: '1rem', background: '#e3f2fd', borderRadius: '8px' }}>
            <h3>Total</h3>
            <p style={{ fontSize: '2rem', margin: 0 }}>{stats.total}</p>
          </div>
          <div style={{ padding: '1rem', background: '#e8f5e9', borderRadius: '8px' }}>
            <h3>Completed</h3>
            <p style={{ fontSize: '2rem', margin: 0 }}>{stats.completed}</p>
          </div>
          <div style={{ padding: '1rem', background: '#fff3e0', borderRadius: '8px' }}>
            <h3>In Progress</h3>
            <p style={{ fontSize: '2rem', margin: 0 }}>{stats.inProgress}</p>
          </div>
          <div style={{ padding: '1rem', background: '#ffebee', borderRadius: '8px' }}>
            <h3>Failed</h3>
            <p style={{ fontSize: '2rem', margin: 0 }}>{stats.failed}</p>
          </div>
        </div>
      ) : (
        <p>No statistics available</p>
      )}

      <h2>Services</h2>
      <ul>
        <li>Order Service (port 8081)</li>
        <li>Payment Service (port 8082)</li>
        <li>Inventory Service (port 8083)</li>
        <li>Notification Service (port 8084)</li>
        <li>Legacy Integration Service (port 8085)</li>
        <li>Saga Orchestrator (port 8086)</li>
        <li>API Gateway (port 8080)</li>
      </ul>
    </div>
  );
}

export default Dashboard;
