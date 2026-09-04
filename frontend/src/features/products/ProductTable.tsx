import { Badge } from "../../components/Badge";
import { Button } from "../../components/Button";
import type { Product } from "../../types/product";

interface ProductTableProps {
  products: Product[];
  onEdit?: (product: Product) => void;
  onDeactivate?: (product: Product) => void;
}

export function ProductTable({ products, onEdit, onDeactivate }: ProductTableProps) {
  const showActions = Boolean(onEdit || onDeactivate);

  return (
    <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
      <table className="min-w-full divide-y divide-slate-200 text-sm">
        <thead className="bg-slate-50">
          <tr>
            <th className="px-4 py-2" />
            <th className="px-4 py-2 text-left font-medium text-slate-600">Name</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Category</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">SKU</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Base price</th>
            <th className="px-4 py-2 text-right font-medium text-slate-600">Stock</th>
            <th className="px-4 py-2 text-left font-medium text-slate-600">Status</th>
            {showActions && <th className="px-4 py-2" />}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {products.map((product) => (
            <tr key={product.id}>
              <td className="px-4 py-2">
                {product.imageUrl ? (
                  <img
                    src={product.imageUrl}
                    alt=""
                    className="h-10 w-10 rounded-md border border-slate-200 object-cover"
                  />
                ) : (
                  <div className="h-10 w-10 rounded-md border border-dashed border-slate-200" />
                )}
              </td>
              <td className="px-4 py-2 font-medium text-slate-900">{product.name}</td>
              <td className="px-4 py-2 text-slate-600">{product.categoryName}</td>
              <td className="px-4 py-2 text-slate-600">{product.sku ?? "—"}</td>
              <td className="px-4 py-2 text-right text-slate-600">${product.basePrice.toFixed(2)}</td>
              <td className="px-4 py-2 text-right text-slate-600">{product.stockQuantity}</td>
              <td className="px-4 py-2">
                <div className="flex flex-wrap gap-1">
                  {!product.active && <Badge tone="neutral">Inactive</Badge>}
                  {product.active && product.lowStock && <Badge tone="warning">Low stock</Badge>}
                  {product.active && !product.lowStock && <Badge tone="success">Active</Badge>}
                </div>
              </td>
              {showActions && (
                <td className="px-4 py-2 text-right">
                  <div className="flex justify-end gap-2">
                    {onEdit && (
                      <Button variant="secondary" onClick={() => onEdit(product)}>
                        Edit
                      </Button>
                    )}
                    {onDeactivate && product.active && (
                      <Button variant="danger" onClick={() => onDeactivate(product)}>
                        Deactivate
                      </Button>
                    )}
                  </div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
