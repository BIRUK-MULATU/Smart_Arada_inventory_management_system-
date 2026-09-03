import { Navigate, Outlet } from "react-router-dom";
import type { Role } from "../types/user";
import { useAuth } from "../features/auth/useAuth";
import { homeRouteFor } from "./homeRoute";

export function RequireRole({ role }: { role: Role }) {
  const { user } = useAuth();

  // RequireAuth already guarantees `user` is set for any route nested under it.
  if (user && user.role !== role) {
    return <Navigate to={homeRouteFor(user.role)} replace />;
  }

  return <Outlet />;
}
