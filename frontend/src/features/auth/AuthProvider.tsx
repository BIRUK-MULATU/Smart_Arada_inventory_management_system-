import { useCallback, useEffect, useState, type ReactNode } from "react";
import { authApi } from "../../api/authApi";
import { startSyncEngine } from "../sync/syncEngine";
import { clearToken, getToken, setToken } from "../../services/tokenStorage";
import type { User } from "../../types/user";
import { AuthContext } from "./AuthContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(() => Boolean(getToken()));

  useEffect(() => {
    if (!getToken()) {
      return;
    }
    authApi
      .me()
      .then(setUser)
      .catch(() => clearToken())
      .finally(() => setIsLoading(false));
  }, []);

  // The sync engine needs a valid JWT (it calls POST /api/sync/sales), so it only runs while
  // someone is signed in - it starts once a user is known and stops on logout.
  useEffect(() => {
    if (!user) {
      return;
    }
    return startSyncEngine();
  }, [user]);

  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login({ email, password });
    setToken(response.token);
    setUser(response.user);
    return response.user;
  }, []);

  const logout = useCallback(() => {
    clearToken();
    setUser(null);
    authApi.logout().catch(() => {
      // Best-effort - the token is already cleared client-side regardless.
    });
  }, []);

  return <AuthContext value={{ user, isLoading, login, logout }}>{children}</AuthContext>;
}
