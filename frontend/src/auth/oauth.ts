import config from '../config';

interface TokenResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  token_type: string;
}

const TOKEN_KEY = 'eop_access_token';
const REFRESH_KEY = 'eop_refresh_token';
const EXPIRY_KEY = 'eop_token_expiry';

const keycloakAuthEndpoint = () =>
  `${config.keycloak.url}/realms/${config.keycloak.realm}/protocol/openid-connect/auth`;

const keycloakTokenEndpoint = () =>
  `${config.keycloak.url}/realms/${config.keycloak.realm}/protocol/openid-connect/token`;

const keycloakLogoutEndpoint = () =>
  `${config.keycloak.url}/realms/${config.keycloak.realm}/protocol/openid-connect/logout`;

function generateCodeVerifier(): string {
  const array = new Uint8Array(64);
  crypto.getRandomValues(array);
  let binary = '';
  array.forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest('SHA-256', data);
  let binary = '';
  new Uint8Array(digest).forEach((b) => { binary += String.fromCharCode(b); });
  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
}

function generateState(): string {
  const array = new Uint8Array(16);
  crypto.getRandomValues(array);
  return Array.from(array, (b) => b.toString(16).padStart(2, '0')).join('');
}

export function startLogin(): void {
  const verifier = generateCodeVerifier();
  const state = generateState();
  sessionStorage.setItem('eop_pkce_verifier', verifier);
  sessionStorage.setItem('eop_oauth_state', state);

  generateCodeChallenge(verifier).then((challenge) => {
    const params = new URLSearchParams({
      client_id: config.keycloak.clientId,
      redirect_uri: config.keycloak.redirectUri,
      response_type: 'code',
      scope: 'openid profile email',
      code_challenge: challenge,
      code_challenge_method: 'S256',
      state
    });
    window.location.href = `${keycloakAuthEndpoint()}?${params.toString()}`;
  });
}

export async function handleCallback(code: string, state: string): Promise<void> {
  const storedState = sessionStorage.getItem('eop_oauth_state');
  if (!state || state !== storedState) {
    throw new Error('OAuth state mismatch');
  }

  const verifier = sessionStorage.getItem('eop_pkce_verifier');
  if (!verifier) {
    throw new Error('Missing PKCE verifier');
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    client_id: config.keycloak.clientId,
    code,
    redirect_uri: config.keycloak.redirectUri,
    code_verifier: verifier
  });

  const response = await fetch(keycloakTokenEndpoint(), {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString()
  });

  if (!response.ok) {
    throw new Error('Failed to exchange authorization code');
  }

  const tokens: TokenResponse = await response.json();
  saveTokens(tokens);
  sessionStorage.removeItem('eop_pkce_verifier');
  sessionStorage.removeItem('eop_oauth_state');
}

export function saveTokens(tokens: TokenResponse): void {
  const expiresAt = Date.now() + tokens.expires_in * 1000;
  localStorage.setItem(TOKEN_KEY, tokens.access_token);
  localStorage.setItem(REFRESH_KEY, tokens.refresh_token);
  localStorage.setItem(EXPIRY_KEY, String(expiresAt));
}

export function getAccessToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY);
}

export function isTokenValid(): boolean {
  const expiresAt = localStorage.getItem(EXPIRY_KEY);
  if (!expiresAt) return false;
  // Refresh a bit early to avoid edge races
  return Date.now() < Number(expiresAt) - 30_000;
}

export async function refreshAccessToken(): Promise<string | null> {
  const refreshToken = getRefreshToken();
  if (!refreshToken) return null;

  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: config.keycloak.clientId,
    refresh_token: refreshToken
  });

  try {
    const response = await fetch(keycloakTokenEndpoint(), {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: body.toString()
    });
    if (!response.ok) return null;
    const tokens: TokenResponse = await response.json();
    saveTokens(tokens);
    return tokens.access_token;
  } catch {
    return null;
  }
}

export function logout(): void {
  const idToken = sessionStorage.getItem('eop_id_token');
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(EXPIRY_KEY);

  const params = new URLSearchParams({
    client_id: config.keycloak.clientId,
    post_logout_redirect_uri: config.keycloak.redirectUri.replace('/callback', '')
  });
  if (idToken) params.append('id_token_hint', idToken);

  window.location.href = `${keycloakLogoutEndpoint()}?${params.toString()}`;
}
