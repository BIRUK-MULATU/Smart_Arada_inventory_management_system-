import { Card } from "../../components/Card";
import { EmptyState } from "../../components/EmptyState";
import type { Product } from "../../types/product";

export function LowStockList({ products }: { products: Product[] }) {
  return (
    <Card>
      <h2 className="mb-2 text-sm font-medium text-slate-600">Low stock</h2>
      {products.length === 0 && <EmptyState message="Nothing is low on stock." />}
      {products.length > 0 && (
        <ul className="divide-y divide-slate-100">
          {products.map((product) => (
            <li key={product.id} className="flex items-center justify-between py-2 text-sm">
              <span className="font-medium text-slate-900">{product.name}</span>
              <span className="text-amber-700">
                {product.stockQuantity} / {product.lowStockThreshold}
              </span>
            </li>
          ))}
        </ul>
      )}
    </Card>
  );
}
