import { useMemo, useState } from "react";
import { Badge } from "../components/Badge";
import { Card } from "../components/Card";
import { EmptyState } from "../components/EmptyState";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { ZoomableImage } from "../components/ZoomableImage";
import { useOfflineProducts } from "../features/products/useOfflineProducts";

export function EmployeeProductsPage() {
  const { products, fetchedAt } = useOfflineProducts();
  const [search, setSearch] = useState("");
  const [categoryId, setCategoryId] = useState("");

  const categories = useMemo(() => {
    const byId = new Map<string, string>();
    products?.forEach((product) => byId.set(product.categoryId, product.categoryName));
    return Array.from(byId, ([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name));
  }, [products]);

  const filtered = products?.filter(
    (product) =>
      product.name.toLowerCase().includes(search.toLowerCase()) &&
      (categoryId === "" || product.categoryId === categoryId),
  );

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">Products</h1>
      <div className="flex flex-col gap-3 sm:flex-row">
        <div className="flex-1">
          <Input label="Search products" placeholder="Search by name…" value={search} onChange={(e) => setSearch(e.target.value)} />
        </div>
        <div className="flex flex-col gap-1 sm:w-56">
          <label htmlFor="category-filter" className="text-sm font-medium text-slate-700">
            Category
          </label>
          <select
            id="category-filter"
            value={categoryId}
            onChange={(event) => setCategoryId(event.target.value)}
            className="min-h-11 rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm"
          >
            <option value="">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {!products && <LoadingSpinner />}
      {products && fetchedAt === null && (
        <p className="text-sm text-slate-500">Showing products from this device. Prices and stock may be out of date.</p>
      )}
      {filtered && filtered.length === 0 && <EmptyState message="No products found." />}

      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {filtered?.map((product) => (
          <Card key={product.id} className="flex items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              {product.imageUrl ? (
                <ZoomableImage
                  src={product.imageUrl}
                  alt=""
                  className="h-20 w-20 shrink-0 rounded-md border border-slate-200"
                />
              ) : (
                <div className="h-20 w-20 shrink-0 rounded-md border border-dashed border-slate-200" />
              )}
              <div>
                <p className="font-medium text-slate-900">{product.name}</p>
                <p className="text-xs uppercase tracking-wide text-slate-400">{product.categoryName}</p>
                <p className="text-sm text-slate-500">${product.basePrice.toFixed(2)}</p>
              </div>
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
