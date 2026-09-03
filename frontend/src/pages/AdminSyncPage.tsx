import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { Card } from "../components/Card";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { ResolveConflictForm } from "../features/sync/ResolveConflictForm";
import { useConflicts, useResolveConflict } from "../features/sync/useSyncConflicts";
import type { Conflict, ResolveConflictRequest } from "../types/sync";

export function AdminSyncPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useConflicts(page);
  const resolveConflict = useResolveConflict();
  const [selected, setSelected] = useState<Conflict | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const handleResolve = async (request: ResolveConflictRequest) => {
    if (!selected) {
      return;
    }
    setFormError(null);
    try {
      await resolveConflict.mutateAsync({ saleId: selected.saleId, request });
      setSelected(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="text-xl font-semibold text-slate-900">Sync &amp; conflicts</h1>
        <p className="text-sm text-slate-500">
          Offline sales that synced with a stock shortfall. Nothing here auto-resolves - review each one and
          record a stock adjustment.
        </p>
      </div>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load conflicts." />}
      {data && data.content.length === 0 && <EmptyState message="No open conflicts. Everything's reconciled." />}

      {data && data.content.length > 0 && (
        <div className="flex flex-col gap-3">
          {data.content.map((conflict) => (
            <Card key={conflict.saleId} className="flex flex-col gap-3">
              <div className="flex items-start justify-between">
                <div>
                  <p className="font-medium text-slate-900">{conflict.employeeName}</p>
                  <p className="text-sm text-slate-500">
                    Synced {new Date(conflict.syncedAt).toLocaleString()} · ${conflict.totalAmount.toFixed(2)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setSelected(conflict)}
                  className="min-h-11 rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800"
                >
                  Resolve
                </button>
              </div>
              <ul className="text-sm text-slate-600">
                {conflict.items
                  .filter((item) => item.shortfall > 0)
                  .map((item) => (
                    <li key={item.productId}>
                      {item.productName}: requested {item.quantityRequested}, short by {item.shortfall}
                    </li>
                  ))}
              </ul>
            </Card>
          ))}
          <Pagination page={page} totalPages={data.page.totalPages} onPageChange={setPage} />
        </div>
      )}

      {selected && (
        <Modal title={`Resolve conflict - ${selected.employeeName}`} onClose={() => setSelected(null)}>
          {formError && (
            <div className="mb-4">
              <ErrorMessage message={formError} />
            </div>
          )}
          <ResolveConflictForm conflict={selected} onSubmit={handleResolve} onCancel={() => setSelected(null)} />
        </Modal>
      )}
    </div>
  );
}
