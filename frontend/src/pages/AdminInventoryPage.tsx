import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { InventoryHistoryTable } from "../features/inventory/InventoryHistoryTable";
import type { StockInFormValues } from "../features/inventory/StockInForm";
import { StockInForm } from "../features/inventory/StockInForm";
import { useInventoryHistory, useInventoryList, useStockIn } from "../features/inventory/useInventory";
import { ProductTable } from "../features/products/ProductTable";

export function AdminInventoryPage() {
  const { data: products, isLoading, isError } = useInventoryList();
  const [historyPage, setHistoryPage] = useState(0);
  const { data: history } = useInventoryHistory(undefined, historyPage);
  const stockIn = useStockIn();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (values: StockInFormValues) => {
    setFormError(null);
    try {
      await stockIn.mutateAsync({
        productId: values.productId,
        quantity: values.quantity,
        reason: values.reason || undefined,
      });
      setDialogOpen(false);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-ink-900">Inventory</h1>
        <Button onClick={() => setDialogOpen(true)}>Stock in</Button>
      </div>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load inventory." />}
      {products && products.length === 0 && <EmptyState message="No products yet." />}
      {products && products.length > 0 && <ProductTable products={products} />}

      <div>
        <h2 className="mb-2 text-lg font-semibold text-ink-900">Recent movements</h2>
        {history && history.content.length === 0 && <EmptyState message="No inventory movements yet." />}
        {history && history.content.length > 0 && (
          <div className="flex flex-col gap-3">
            <InventoryHistoryTable transactions={history.content} />
            <Pagination page={historyPage} totalPages={history.page.totalPages} onPageChange={setHistoryPage} />
          </div>
        )}
      </div>

      {dialogOpen && (
        <Modal title="Stock in" onClose={() => setDialogOpen(false)}>
          {formError && (
            <div className="mb-4">
              <ErrorMessage message={formError} />
            </div>
          )}
          <StockInForm products={products ?? []} onSubmit={handleSubmit} onCancel={() => setDialogOpen(false)} />
        </Modal>
      )}
    </div>
  );
}
