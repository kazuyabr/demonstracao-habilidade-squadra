import React, { ReactNode } from 'react';
import { Link, NavLink } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: '▦', end: true },
  { to: '/orders', label: 'Orders', icon: '▤' },
  { to: '/inventory', label: 'Inventory', icon: '▥' },
  { to: '/sagas', label: 'Sagas', icon: '◈' }
];

function getInitials(name?: string): string {
  if (!name) return 'U';
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((n) => n[0])
    .join('')
    .toUpperCase();
}

export default function Layout({ title, children }: { title: string; children: ReactNode }) {
  const { user, logout } = useAuth();

  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <Link to="/" className="sidebar-logo">
            <span className="logo-mark">E</span>
            <span>
              Enterprise Ops
              <span className="logo-sub">Order Platform</span>
            </span>
          </Link>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">Distributed microservices demo · Java 21</div>
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="topbar-title">{title}</div>
          <div className="topbar-right">
            <div className="user-chip">
              <div className="user-avatar">{getInitials(user?.name || user?.preferred_username)}</div>
              <div className="user-meta">
                <span className="user-name">{user?.name || user?.preferred_username}</span>
                <span className="user-roles">{(user?.roles || []).slice(0, 2).join(' · ')}</span>
              </div>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={logout}>Sign out</button>
          </div>
        </header>
        <main className="content">{children}</main>
      </div>
    </div>
  );
}
