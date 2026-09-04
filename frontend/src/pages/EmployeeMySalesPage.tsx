import { useState } from "react";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Pagination } from "../components/Pagination";
import { useAuth } from "../features/auth/useAuth";
import { LocalSalesTable } from "../features/sales/LocalSalesTable";
import { SalesTable } from "../features/sales/SalesTable";
import { useLocalSales } from "../features/sales/useLocalSales";
import { useSales } from "../features/sales/useSales";

export function EmployeeMySalesPage() {
  const [page, setPage] = useState(0);
  const { user } = useAuth();
  const { data, isLoading, isError } = useSales(page);
  const localSales = useLocalSales(user?.id);

  const hasLocalSales = !!localSales && localSales.length > 0;
  const hasRecordedSales = !!data && data.content.length > 0;

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-ink-900">My sales</h1>

      {hasLocalSales && (
        <div className="flex flex-col gap-2">
          <h2 className="text-lg font-semibold text-ink-900">Not yet recorded</h2>
          <LocalSalesTable sales={localSales} />
        </div>
      )}

      <div className="flex flex-col gap-2">
        {hasLocalSales && <h2 className="text-lg font-semibold text-ink-900">Recorded</h2>}
        {isLoading && <LoadingSpinner />}
        {isError && <ErrorMessage message="Couldn't load your recorded sales." />}
        {!isLoading && !isError && !hasRecordedSales && !hasLocalSales && (
          <EmptyState message="You haven't recorded any sales yet." />
        )}
        {hasRecordedSales && data && (
          <div className="flex flex-col gap-3">
            <SalesTable sales={data.content} showEmployee={false} />
            <Pagination page={page} totalPages={data.page.totalPages} onPageChange={setPage} />
          </div>
        )}
      </div>
    </div>
  );
}
