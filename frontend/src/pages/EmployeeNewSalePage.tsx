import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "../components/Button";
import { Card } from "../components/Card";
import { ErrorMessage } from "../components/ErrorMessage";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { CartTable } from "../features/sales/CartTable";
import { useCart } from "../features/sales/useCart";
import { recordSaleOffline } from "../features/sales/offlineSales";
import { useOfflineProducts } from "../features/products/useOfflineProducts";
import { triggerSync } from "../features/sync/syncEngine";
import { useAuth } from "../features/auth/useAuth";

export function EmployeeNewSalePage() {
  const { products, fetchedAt } = useOfflineProducts();
  const { user } = useAuth();
  const { items, addProduct, removeItem, updateQuantity, updateSellingPrice, clear, total } = useCart();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const filtered = products?.filter((product) => product.name.toLowerCase().includes(search.toLowerCase()));
  const hasInvalidQuantityItem = items.some((item) => item.quantity < 1);
  const hasOverStockItem = items.some((item) => item.quantity > item.availableStock);

  const handleSubmit = async () => {
    if (!user) {
      return;
    }
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      await recordSaleOffline({ employeeId: user.id, items });
      clear();
      triggerSync();
      navigate("/sales");
    } catch {
      setSubmitError("Couldn't save the sale on this device. Please try again.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">New sale</h1>

      <Input label="Add a product" placeholder="Search by name…" value={search} onChange={(e) => setSearch(e.target.value)} />

      {!products && <LoadingSpinner />}
      {products && fetchedAt === null && (
        <p className="text-sm text-slate-500">Showing products from this device. Prices and stock may be out of date.</p>
      )}

      {search && (
        <div className="flex flex-col gap-2">
          {filtered?.map((product) => (
            <Card key={product.id} className="flex items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                {product.imageUrl ? (
                  <img
                    src={product.imageUrl}
                    alt=""
                    className="h-16 w-16 shrink-0 rounded-md border border-slate-200 object-cover"
                  />
                ) : (
                  <div className="h-16 w-16 shrink-0 rounded-md border border-dashed border-slate-200" />
                )}
                <div>
                  <p className="font-medium text-slate-900">{product.name}</p>
                  <p className="text-xs uppercase tracking-wide text-slate-400">{product.categoryName}</p>
                  <p className="text-sm text-slate-500">
                    ${product.basePrice.toFixed(2)} · {product.stockQuantity} in stock
                  </p>
                </div>
              </div>
              <Button
                variant="secondary"
                onClick={() => {
                  addProduct(product);
                  setSearch("");
                }}
              >
                Add
              </Button>
            </Card>
          ))}
        </div>
      )}

      <div>
        <h2 className="mb-2 text-lg font-semibold text-slate-900">Cart</h2>
        {items.length === 0 && <p className="text-sm text-slate-500">No items yet. Search above to add products.</p>}
        {items.length > 0 && (
          <CartTable items={items} onQuantityChange={updateQuantity} onPriceChange={updateSellingPrice} onRemove={removeItem} />
        )}
      </div>

      {items.length > 0 && (
        <div className="sticky bottom-20 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between text-lg font-semibold text-slate-900">
            <span>Total</span>
            <span>${total.toFixed(2)}</span>
          </div>
          {hasOverStockItem && (
            <p className="mt-2 text-sm text-amber-700">
              One or more items exceed the last known stock count. The sale will still be saved and reviewed if stock is short.
            </p>
          )}
          {submitError && (
            <div className="mt-3">
              <ErrorMessage message={submitError} />
            </div>
          )}
          <Button className="mt-3 w-full" onClick={handleSubmit} disabled={isSubmitting || hasInvalidQuantityItem}>
            {isSubmitting ? "Saving…" : "Complete sale"}
          </Button>
        </div>
      )}
    </div>
  );
}
