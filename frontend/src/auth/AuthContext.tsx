import React, { createContext, useContext, useEffect, useState, ReactNode } from 'react';
import * as oauth from './oauth';

interface UserInfo {
  preferred_username: string;
  name?: string;
  email?: string;
  roles?: string[];
}

interface AuthContextValue {
  isAuthenticated: boolean;
  user: UserInfo | null;
  token: string | null;
  login: () => void;
  logout: () => void;
  onTokenAcquired: () => void;
}

const AuthContext = createContext<AuthContextValue>({
  isAuthenticated: false,
  user: null,
  token: null,
  login: () => {},
  logout: () => {},
  onTokenAcquired: () => {}
});

function decodeToken(token: string): { payload: Record<string, unknown> } | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
    const json = atob(padded);
    return { payload: JSON.parse(json) };
  } catch {
    return null;
  }
}

function buildUser(token: string): UserInfo | null {
  const decoded = decodeToken(token);
  if (!decoded) return null;
  const p = decoded.payload;
  const realmAccess = p.realm_access as { roles?: string[] } | undefined;
  return {
    preferred_username: (p.preferred_username as string) || 'user',
    name: (p.name as string) || undefined,
    email: (p.email as string) || undefined,
    roles: realmAccess?.roles || []
  };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => oauth.getAccessToken());
  const [user, setUser] = useState<UserInfo | null>(() => {
    const t = oauth.getAccessToken();
    return t ? buildUser(t) : null;
  });

  useEffect(() => {
    const interval = setInterval(() => {
      if (oauth.getAccessToken() && !oauth.isTokenValid()) {
        oauth.refreshAccessToken().then((newToken) => {
          if (newToken) {
            setToken(newToken);
            setUser(buildUser(newToken));
          } else {
            oauth.logout();
          }
        });
      }
    }, 30_000);
    return () => clearInterval(interval);
  }, []);

  const login = () => oauth.startLogin();

  const onTokenAcquired = () => {
    const t = oauth.getAccessToken();
    setToken(t);
    setUser(t ? buildUser(t) : null);
  };

  const handleLogout = () => {
    oauth.logout();
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: Boolean(token),
        user,
        token,
        login,
        logout: handleLogout,
        onTokenAcquired
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}
