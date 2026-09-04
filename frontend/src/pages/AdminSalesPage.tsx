import { useState } from "react";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Pagination } from "../components/Pagination";
import { SalesTable } from "../features/sales/SalesTable";
import { useSales } from "../features/sales/useSales";

export function AdminSalesPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useSales(page);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-ink-900">Sales</h1>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load sales." />}
      {data && data.content.length === 0 && <EmptyState message="No sales yet." />}
      {data && data.content.length > 0 && (
        <div className="flex flex-col gap-3">
          <SalesTable sales={data.content} showEmployee />
          <Pagination page={page} totalPages={data.page.totalPages} onPageChange={setPage} />
        </div>
      )}
    </div>
  );
}
