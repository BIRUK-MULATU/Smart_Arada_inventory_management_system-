import type { Role } from "../types/user";

export function homeRouteFor(role: Role): string {
  return role === "ADMIN" ? "/admin/dashboard" : "/sales/new";
}
