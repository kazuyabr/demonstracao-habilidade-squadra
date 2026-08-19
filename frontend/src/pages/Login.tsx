import React from 'react';
import { useAuth } from '../auth/AuthContext';
import config from '../config';

function registrationUrl(): string {
  const params = new URLSearchParams({
    client_id: config.keycloak.clientId,
    redirect_uri: config.keycloak.redirectUri,
    response_type: 'code',
    scope: 'openid profile email'
  });
  return `${config.keycloak.url}/realms/${config.keycloak.realm}/protocol/openid-connect/registrations?${params.toString()}`;
}

export default function Login() {
  const { login } = useAuth();

  return (
    <div className="login-screen">
      <div className="login-card">
        <div className="login-logo">E</div>
        <h1 className="login-title">Enterprise Order Platform</h1>
        <p className="login-desc">
          Sign in to explore the distributed order platform. Authentication is handled by
          Keycloak using OAuth 2.0 with PKCE.
        </p>

        <button className="btn btn-primary" onClick={login}>
          Sign in with Keycloak
        </button>

        <div className="login-divider">or</div>

        <div className="demo-creds">
          <div className="demo-creds-title">Try the demo account</div>
          {config.demo.password ? (
            <div className="demo-creds-row">
              <span className="demo-creds-key">User</span>
              <code>{config.demo.username}</code>
            </div>
          ) : (
            <div className="demo-creds-row">
              <span className="demo-creds-key">User</span>
              <code>{config.demo.username}</code>
            </div>
          )}
          {config.demo.password ? (
            <div className="demo-creds-row">
              <span className="demo-creds-key">Password</span>
              <code>{config.demo.password}</code>
            </div>
          ) : (
            <div className="demo-creds-row">
              <span className="demo-creds-key">Password</span>
              <span className="demo-creds-hint">{config.demo.hint}</span>
            </div>
          )}
        </div>

        <a className="btn btn-outline btn-block" href={registrationUrl()}>
          Create account
        </a>

        <div className="login-footer">
          Order · Payment · Inventory · Saga orchestration
        </div>
      </div>
    </div>
  );
}