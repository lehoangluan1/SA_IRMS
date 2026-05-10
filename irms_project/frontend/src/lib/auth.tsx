import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { apiFetch, clearStoredToken, getStoredToken, setStoredToken } from "./api/client";
import { ENDPOINTS } from "./api/endpoints";
import type { AuthUser, LoginResponse } from "./api/types";

type SignInInput = {
  email: string;
  password: string;
  role?: string;
};

const DEVICE_ID_KEY = "irms.device.id";

function getOrCreateDeviceId() {
  if (typeof window === "undefined") return "web:ssr";

  const existing = window.localStorage.getItem(DEVICE_ID_KEY);
  if (existing) return existing.slice(0, 120);

  const randomPart = typeof crypto !== "undefined" && "randomUUID" in crypto
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  const deviceId = `web:${randomPart}`.slice(0, 120);
  window.localStorage.setItem(DEVICE_ID_KEY, deviceId);
  return deviceId;
}

type AuthContextValue = {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  signIn: (input: SignInInput) => Promise<void>;
  signOut: () => Promise<void>;
  refresh: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(getStoredToken());
  const [isLoading, setIsLoading] = useState(true);

  const refresh = useCallback(async () => {
    const storedToken = getStoredToken();
    if (!storedToken) {
      setUser(null);
      setToken(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    try {
      const me = await apiFetch<AuthUser>(ENDPOINTS.auth.me);
      setUser(me);
      setToken(storedToken);
    } catch {
      clearStoredToken();
      setUser(null);
      setToken(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const signIn = useCallback(async ({ email, password, role }: SignInInput) => {
    const response = await apiFetch<LoginResponse>(ENDPOINTS.auth.login, {
      method: "POST",
      body: JSON.stringify({
        email: email.trim(),
        password,
        role,
        deviceId: getOrCreateDeviceId(),
      }),
    });
    setStoredToken(response.token);
    setToken(response.token);
    setUser(response.user);
  }, []);

  const signOut = useCallback(async () => {
    try {
      await apiFetch(ENDPOINTS.auth.logout, {
        method: "POST",
      });
    } catch {
      // Ignore logout errors and clear the client session anyway.
    } finally {
      clearStoredToken();
      setToken(null);
      setUser(null);
    }
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      token,
      isLoading,
      signIn,
      signOut,
      refresh,
    }),
    [isLoading, refresh, signIn, signOut, token, user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }
  return context;
}
