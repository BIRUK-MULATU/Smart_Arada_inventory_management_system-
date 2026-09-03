import { useState } from "react";
import { Badge } from "../components/Badge";
import { Card } from "../components/Card";
import { EmptyState } from "../components/EmptyState";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { useOfflineProducts } from "../features/products/useOfflineProducts";

export function EmployeeProductsPage() {
  const { products, fetchedAt } = useOfflineProducts();
  const [search, setSearch] = useState("");

  const filtered = products?.filter((product) => product.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">Products</h1>
      <Input label="Search products" placeholder="Search by name…" value={search} onChange={(e) => setSearch(e.target.value)} />

      {!products && <LoadingSpinner />}
      {products && fetchedAt === null && (
        <p className="text-sm text-slate-500">Showing products from this device. Prices and stock may be out of date.</p>
      )}
      {filtered && filtered.length === 0 && <EmptyState message="No products found." />}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {filtered?.map((product) => (
          <Card key={product.id} className="flex items-center justify-between">
            <div>
              <p className="font-medium text-slate-900">{product.name}</p>
              <p className="text-sm text-slate-500">${product.basePrice.toFixed(2)}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-slate-600">{product.stockQuantity} in stock</p>
              {product.lowStock && <Badge tone="warning">Low stock</Badge>}
            </div>
          </Card>
        ))}
      </div>
    </div>
  );
}
