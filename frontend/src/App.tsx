import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import Orders from './pages/Orders';
import Inventory from './pages/Inventory';
import Sagas from './pages/Sagas';

function App() {
  return (
    <Router>
      <div className="App">
        <nav style={{ padding: '1rem', background: '#f5f5f5' }}>
          <a href="/" style={{ marginRight: '1rem' }}>Dashboard</a>
          <a href="/orders" style={{ marginRight: '1rem' }}>Orders</a>
          <a href="/inventory" style={{ marginRight: '1rem' }}>Inventory</a>
          <a href="/sagas">Sagas</a>
        </nav>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/inventory" element={<Inventory />} />
          <Route path="/sagas" element={<Sagas />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
