import { Navigate } from "react-router-dom";
import { useAuth } from "../features/auth/useAuth";
import { homeRouteFor } from "./homeRoute";

export function RootRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? homeRouteFor(user.role) : "/login"} replace />;
}
