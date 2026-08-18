import React from 'react';
import { useAuth } from '../auth/AuthContext';

export default function Login() {
  const { login } = useAuth();

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-logo">E</div>
        <h1 className="login-title">Enterprise Order Platform</h1>
        <p className="login-desc">
          Sign in to access the order processing dashboard. Authentication is handled by Keycloak
          using OAuth 2.0 with PKCE.
        </p>
        <button className="btn btn-primary" onClick={login}>
          Sign in with Keycloak
        </button>
        <div className="login-footer">
          Order · Payment · Inventory · Saga orchestration
        </div>
      </div>
    </div>
  );
}
