import { NavLink, Outlet } from "react-router-dom";
import { Badge } from "../components/Badge";
import { OnlineStatusBanner } from "../components/OnlineStatusBanner";
import { useAuth } from "../features/auth/useAuth";
import { usePendingSyncCounts } from "../features/sync/usePendingSyncCounts";

const navItems = [
  { to: "/products", label: "Products" },
  { to: "/sales/new", label: "New Sale" },
  { to: "/sales", label: "My Sales" },
  { to: "/notes", label: "Notes" },
];

export function EmployeeLayout() {
  const { user, logout } = useAuth();
  const syncCounts = usePendingSyncCounts();

  return (
    <div className="flex min-h-screen flex-col bg-ink-50">
      <header className="flex items-center justify-between border-b border-ink-800 bg-ink-950 px-4 py-3">
        <div>
          <p className="text-sm font-semibold text-gold-400">Inventory &amp; Sales</p>
          <p className="text-xs text-ink-400">{user?.name}</p>
          {(syncCounts?.pending ?? 0) > 0 || (syncCounts?.failed ?? 0) > 0 ? (
            <div className="mt-1 flex gap-1">
              {syncCounts && syncCounts.pending > 0 && (
                <Badge tone="warning">{syncCounts.pending} pending sync</Badge>
              )}
              {syncCounts && syncCounts.failed > 0 && (
                <Badge tone="danger">{syncCounts.failed} sync failed</Badge>
              )}
            </div>
          ) : null}
        </div>
        <button
          onClick={logout}
          className="min-h-11 rounded-md border border-ink-700 px-4 py-2 text-sm font-medium text-ink-300
            transition-colors duration-150 hover:border-gold-600/50 hover:bg-ink-900 hover:text-gold-300"
        >
          Log out
        </button>
      </header>
      <OnlineStatusBanner />
      <main className="flex-1 overflow-y-auto p-4 pb-24">
        <Outlet />
      </main>
      <nav className="fixed inset-x-0 bottom-0 flex border-t border-ink-800 bg-ink-950">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              `flex flex-1 flex-col items-center justify-center gap-1 py-3 text-sm font-medium transition-colors duration-150 ${
                isActive ? "text-gold-400" : "text-ink-400 hover:text-gold-300"
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
