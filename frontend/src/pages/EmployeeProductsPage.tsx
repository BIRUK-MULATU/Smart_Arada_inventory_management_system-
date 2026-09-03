import { useState } from "react";
import { Badge } from "../components/Badge";
import { Card } from "../components/Card";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { useProducts } from "../features/products/useProducts";

export function EmployeeProductsPage() {
  const { data: products, isLoading, isError } = useProducts(false);
  const [search, setSearch] = useState("");

  const filtered = products?.filter((product) => product.name.toLowerCase().includes(search.toLowerCase()));

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">Products</h1>
      <Input label="Search products" placeholder="Search by name…" value={search} onChange={(e) => setSearch(e.target.value)} />

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load products." />}
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
