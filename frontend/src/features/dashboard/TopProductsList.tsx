import { Card } from "../../components/Card";
import { EmptyState } from "../../components/EmptyState";
import type { TopProduct } from "../../types/dashboard";

export function TopProductsList({ products }: { products: TopProduct[] }) {
  return (
    <Card>
      <h2 className="mb-2 text-sm font-medium text-ink-600">Top products this period</h2>
      {products.length === 0 && <EmptyState message="No sales in this period yet." />}
      {products.length > 0 && (
        <ol className="divide-y divide-ink-100">
          {products.map((product, index) => (
            <li key={product.productId} className="flex items-center justify-between py-2 text-sm">
              <span className="font-medium text-ink-900">
                {index + 1}. {product.productName}
              </span>
              <span className="text-ink-600">{product.unitsSold} units · ${product.revenue.toFixed(2)}</span>
            </li>
          ))}
        </ol>
      )}
    </Card>
  );
}
