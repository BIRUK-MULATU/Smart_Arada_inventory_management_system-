import { useState } from "react";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { CategoryBreakdownChart } from "../features/dashboard/CategoryBreakdownChart";
import { LowStockList } from "../features/dashboard/LowStockList";
import { RecentActivity } from "../features/dashboard/RecentActivity";
import { SalesRevenueChart } from "../features/dashboard/SalesRevenueChart";
import { SummaryCards } from "../features/dashboard/SummaryCards";
import { TopProductsList } from "../features/dashboard/TopProductsList";
import {
  useDashboardCategoryBreakdown,
  useDashboardLowStock,
  useDashboardSales,
  useDashboardSummary,
  useDashboardTopProducts,
} from "../features/dashboard/useDashboard";
import type { SalesGranularity } from "../types/dashboard";

export function AdminDashboardPage() {
  const [granularity, setGranularity] = useState<SalesGranularity>("DAILY");
  const { data: summary, isLoading: summaryLoading, isError: summaryError } = useDashboardSummary();
  const { data: sales, isLoading: salesLoading } = useDashboardSales(granularity);
  const { data: categories, isLoading: categoriesLoading } = useDashboardCategoryBreakdown();
  const { data: topProducts, isLoading: topProductsLoading } = useDashboardTopProducts();
  const { data: lowStock, isLoading: lowStockLoading } = useDashboardLowStock();

  if (summaryLoading) {
    return <LoadingSpinner label="Loading dashboard…" />;
  }

  if (summaryError || !summary) {
    return <ErrorMessage message="Couldn't load the dashboard." />;
  }

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Dashboard</h1>
        <p className="text-sm text-slate-500">
          {summary.periodFrom} – {summary.periodTo}
        </p>
      </div>

      <SummaryCards summary={summary} />

      {salesLoading ? (
        <LoadingSpinner />
      ) : (
        <SalesRevenueChart points={sales ?? []} granularity={granularity} onGranularityChange={setGranularity} />
      )}

      {categoriesLoading ? <LoadingSpinner /> : <CategoryBreakdownChart categories={categories ?? []} />}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        {lowStockLoading ? <LoadingSpinner /> : <LowStockList products={lowStock ?? []} />}
        {topProductsLoading ? <LoadingSpinner /> : <TopProductsList products={topProducts ?? []} />}
      </div>

      <RecentActivity summary={summary} />
    </div>
  );
}
