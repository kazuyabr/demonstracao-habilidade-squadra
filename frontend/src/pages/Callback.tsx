import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import * as oauth from '../auth/oauth';
import { useAuth } from '../auth/AuthContext';

export default function Callback() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { onTokenAcquired } = useAuth();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const code = searchParams.get('code');
    const state = searchParams.get('state');

    (async () => {
      if (!code) {
        navigate('/login', { replace: true });
        return;
      }
      try {
        await oauth.handleCallback(code, state || '');
        onTokenAcquired();
        navigate('/', { replace: true });
      } catch (e) {
        console.error('OAuth callback failed:', e);
        navigate('/login', { replace: true });
      }
    })();
  }, [navigate, searchParams, onTokenAcquired]);

  return (
    <div className="loading-screen">
      <div className="spinner" />
    </div>
  );
}
