import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { Card } from "../components/Card";
import { ErrorMessage } from "../components/ErrorMessage";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { CartTable } from "../features/sales/CartTable";
import { useCart } from "../features/sales/useCart";
import { useCreateSale } from "../features/sales/useSales";
import { useProducts } from "../features/products/useProducts";

export function EmployeeNewSalePage() {
  const { data: products, isLoading } = useProducts(false);
  const { items, addProduct, removeItem, updateQuantity, updateSellingPrice, clear, total } = useCart();
  const createSale = useCreateSale();
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const [submitError, setSubmitError] = useState<string | null>(null);

  const filtered = products?.filter((product) => product.name.toLowerCase().includes(search.toLowerCase()));
  const hasOverStockItem = items.some((item) => item.quantity > item.availableStock || item.quantity < 1);

  const handleSubmit = async () => {
    setSubmitError(null);
    try {
      await createSale.mutateAsync({
        clientTransactionId: crypto.randomUUID(),
        items: items.map((item) => ({
          productId: item.productId,
          quantity: item.quantity,
          sellingPrice: item.sellingPrice,
        })),
      });
      clear();
      navigate("/sales");
    } catch (error) {
      setSubmitError(extractErrorMessage(error, "Couldn't complete the sale. Please review and try again."));
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">New sale</h1>

      <Input label="Add a product" placeholder="Search by name…" value={search} onChange={(e) => setSearch(e.target.value)} />

      {isLoading && <LoadingSpinner />}

      {search && (
        <div className="flex flex-col gap-2">
          {filtered?.map((product) => (
            <Card key={product.id} className="flex items-center justify-between">
              <div>
                <p className="font-medium text-slate-900">{product.name}</p>
                <p className="text-sm text-slate-500">
                  ${product.basePrice.toFixed(2)} · {product.stockQuantity} in stock
                </p>
              </div>
              <Button
                variant="secondary"
                onClick={() => {
                  addProduct(product);
                  setSearch("");
                }}
                disabled={product.stockQuantity < 1}
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
          {submitError && (
            <div className="mt-3">
              <ErrorMessage message={submitError} />
            </div>
          )}
          <Button
            className="mt-3 w-full"
            onClick={handleSubmit}
            disabled={createSale.isPending || hasOverStockItem}
          >
            {createSale.isPending ? "Completing sale…" : "Complete sale"}
          </Button>
        </div>
      )}
    </div>
  );
}
