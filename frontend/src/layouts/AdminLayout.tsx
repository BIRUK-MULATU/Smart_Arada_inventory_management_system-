import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";
import { OnlineStatusBanner } from "../components/OnlineStatusBanner";
import { useAuth } from "../features/auth/useAuth";

const navItems = [
  { to: "/admin/dashboard", label: "Dashboard" },
  { to: "/admin/products", label: "Products" },
  { to: "/admin/categories", label: "Categories" },
  { to: "/admin/inventory", label: "Inventory" },
  { to: "/admin/sales", label: "Sales" },
  { to: "/admin/sync", label: "Sync & Conflicts" },
  { to: "/admin/finance", label: "Finance" },
  { to: "/admin/employees", label: "Employees" },
  { to: "/admin/notes", label: "Notes" },
];

export function AdminLayout() {
  const { user, logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="flex min-h-screen bg-slate-50">
      {sidebarOpen && (
        <button
          type="button"
          aria-label="Close menu"
          onClick={() => setSidebarOpen(false)}
          className="fixed inset-0 z-30 bg-black/40 md:hidden"
        />
      )}

      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 -translate-x-full flex-col border-r
          border-slate-200 bg-white transition-transform duration-200 md:static md:translate-x-0 ${
            sidebarOpen ? "translate-x-0" : ""
          }`}
      >
        <div className="border-b border-slate-200 px-4 py-4">
          <p className="text-sm font-semibold text-slate-900">Inventory &amp; Sales</p>
          <p className="text-xs text-slate-500">Admin</p>
        </div>
        <nav className="flex-1 space-y-1 overflow-y-auto p-2">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `block rounded-md px-3 py-2 text-sm font-medium ${
                  isActive ? "bg-slate-900 text-white" : "text-slate-700 hover:bg-slate-100"
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200 p-3">
          <p className="truncate text-xs text-slate-500">{user?.name}</p>
          <button
            onClick={logout}
            className="mt-2 min-h-11 w-full rounded-md border border-slate-300 px-3 py-2 text-sm font-medium
              text-slate-700 hover:bg-slate-50"
          >
            Log out
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex items-center gap-3 border-b border-slate-200 bg-white px-4 py-3 md:hidden">
          <button
            type="button"
            aria-label="Open menu"
            onClick={() => setSidebarOpen(true)}
            className="flex h-11 w-11 items-center justify-center rounded-md border border-slate-300 text-slate-700"
          >
            ☰
          </button>
          <p className="text-sm font-semibold text-slate-900">Inventory &amp; Sales</p>
        </header>
        <main className="flex-1 overflow-y-auto">
          <OnlineStatusBanner />
          <div className="p-4 sm:p-6">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
