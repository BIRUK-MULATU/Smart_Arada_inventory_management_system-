import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { lazy, Suspense } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import { LoadingSpinner } from "./components/LoadingSpinner";
import { AuthProvider } from "./features/auth/AuthProvider";
import { AdminLayout } from "./layouts/AdminLayout";
import { EmployeeLayout } from "./layouts/EmployeeLayout";
import { LoginPage } from "./pages/LoginPage";
import { NotFoundPage } from "./pages/NotFoundPage";
import { RequireAuth } from "./routes/RequireAuth";
import { RequireRole } from "./routes/RequireRole";
import { RootRedirect } from "./routes/RootRedirect";

// Route-level code splitting: AdminDashboardPage alone pulls in recharts (the single largest
// dependency in the app), and no employee ever navigates there, so it shouldn't be in everyone's
// initial bundle. Splitting every page consistently, not just the dashboard, keeps this uniform.
const AdminDashboardPage = lazy(() => import("./pages/AdminDashboardPage").then((m) => ({ default: m.AdminDashboardPage })));
const AdminEmployeesPage = lazy(() => import("./pages/AdminEmployeesPage").then((m) => ({ default: m.AdminEmployeesPage })));
const AdminInventoryPage = lazy(() => import("./pages/AdminInventoryPage").then((m) => ({ default: m.AdminInventoryPage })));
const AdminProductsPage = lazy(() => import("./pages/AdminProductsPage").then((m) => ({ default: m.AdminProductsPage })));
const AdminSalesPage = lazy(() => import("./pages/AdminSalesPage").then((m) => ({ default: m.AdminSalesPage })));
const EmployeeMySalesPage = lazy(() => import("./pages/EmployeeMySalesPage").then((m) => ({ default: m.EmployeeMySalesPage })));
const EmployeeNewSalePage = lazy(() => import("./pages/EmployeeNewSalePage").then((m) => ({ default: m.EmployeeNewSalePage })));
const EmployeeProductsPage = lazy(() =>
  import("./pages/EmployeeProductsPage").then((m) => ({ default: m.EmployeeProductsPage })),
);

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30_000,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <Suspense fallback={<LoadingSpinner />}>
            <Routes>
              <Route path="/login" element={<LoginPage />} />

              <Route element={<RequireAuth />}>
                <Route path="/" element={<RootRedirect />} />

                <Route element={<RequireRole role="ADMIN" />}>
                  <Route element={<AdminLayout />}>
                    <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
                    <Route path="/admin/products" element={<AdminProductsPage />} />
                    <Route path="/admin/inventory" element={<AdminInventoryPage />} />
                    <Route path="/admin/sales" element={<AdminSalesPage />} />
                    <Route path="/admin/employees" element={<AdminEmployeesPage />} />
                  </Route>
                </Route>

                <Route element={<RequireRole role="EMPLOYEE" />}>
                  <Route element={<EmployeeLayout />}>
                    <Route path="/products" element={<EmployeeProductsPage />} />
                    <Route path="/sales/new" element={<EmployeeNewSalePage />} />
                    <Route path="/sales" element={<EmployeeMySalesPage />} />
                  </Route>
                </Route>
              </Route>

              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </Suspense>
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
