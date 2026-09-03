import type { CartItem } from "./useCart";

interface CartTableProps {
  items: CartItem[];
  onQuantityChange: (productId: string, quantity: number) => void;
  onPriceChange: (productId: string, price: number) => void;
  onRemove: (productId: string) => void;
}

export function CartTable({ items, onQuantityChange, onPriceChange, onRemove }: CartTableProps) {
  return (
    <div className="flex flex-col gap-3">
      {items.map((item) => {
        const overStock = item.quantity > item.availableStock;
        return (
          <div key={item.productId} className="rounded-lg border border-slate-200 bg-white p-3">
            <div className="flex items-center justify-between">
              <p className="font-medium text-slate-900">{item.productName}</p>
              <button
                onClick={() => onRemove(item.productId)}
                className="text-sm font-medium text-red-600 hover:underline"
                aria-label={`Remove ${item.productName}`}
              >
                Remove
              </button>
            </div>
            <div className="mt-2 grid grid-cols-2 gap-3">
              <label className="flex flex-col gap-1 text-sm text-slate-600">
                Quantity
                <input
                  type="number"
                  min={1}
                  max={item.availableStock}
                  value={item.quantity}
                  onChange={(e) => onQuantityChange(item.productId, Number(e.target.value))}
                  className="min-h-11 rounded-md border border-slate-300 px-3 py-2 text-sm"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-600">
                Selling price
                <input
                  type="number"
                  min={0}
                  step="0.01"
                  value={item.sellingPrice}
                  onChange={(e) => onPriceChange(item.productId, Number(e.target.value))}
                  className="min-h-11 rounded-md border border-slate-300 px-3 py-2 text-sm"
                />
              </label>
            </div>
            {overStock && <p className="mt-1 text-sm text-red-600">Only {item.availableStock} in stock.</p>}
            <p className="mt-2 text-right text-sm font-medium text-slate-900">
              Subtotal: ${(item.sellingPrice * item.quantity).toFixed(2)}
            </p>
          </div>
        );
      })}
    </div>
  );
}
