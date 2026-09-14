import { createContext, useCallback, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react';
import { queryClient } from '@/app/queryClient';
import { authApi } from '@/api/services';
import { setAccessToken } from '@/api/http';
import type { User } from '@/types';

interface AuthContextValue {
  user: User | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (email: string, password: string) => Promise<User>;
  register: (payload: { organizationName: string; firstName: string; lastName: string; email: string; password: string }) => Promise<void>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);
const USER_KEY = 'vantora_user';

function getStoredUser(): User | null {
  try {
    const value = localStorage.getItem(USER_KEY);
    return value ? (JSON.parse(value) as User) : null;
  } catch {
    return null;
  }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUserState] = useState<User | null>(getStoredUser);
  const [loading, setLoading] = useState(true);

  const setUser = useCallback((next: User | null) => {
    setUserState(next);
    if (next) localStorage.setItem(USER_KEY, JSON.stringify(next));
    else {
      localStorage.removeItem(USER_KEY);
      queryClient.clear();
    }
  }, []);

  useEffect(() => {
    let active = true;
    const restore = async () => {
      try {
        const response = await authApi.refresh();
        if (active) setUser(response.user);
      } catch {
        if (active) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        if (active) setLoading(false);
      }
    };
    void restore();
    return () => {
      active = false;
    };
  }, [setUser]);

  useEffect(() => {
    const expired = () => setUser(null);
    window.addEventListener('vantora:session-expired', expired);
    return () => window.removeEventListener('vantora:session-expired', expired);
  }, [setUser]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loading,
      isAuthenticated: Boolean(user),
      setUser,
      login: async (email, password) => {
        const result = await authApi.login({ email, password });
        setAccessToken(result.accessToken);
        queryClient.clear();
        setUser(result.user);
        return result.user;
      },
      register: async (payload) => {
        await authApi.register(payload);
      },
      logout: async () => {
        try {
          await authApi.logout();
        } finally {
          setUser(null);
        }
      },
    }),
    [loading, setUser, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error('useAuth must be used inside AuthProvider');
  return value;
}
