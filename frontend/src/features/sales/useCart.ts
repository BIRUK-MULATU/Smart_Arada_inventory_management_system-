import { useMemo, useState } from "react";
import type { Product } from "../../types/product";

export interface CartItem {
  productId: string;
  productName: string;
  sellingPrice: number;
  quantity: number;
  availableStock: number;
}

export function useCart() {
  const [items, setItems] = useState<CartItem[]>([]);

  const addProduct = (product: Product) => {
    setItems((current) => {
      if (current.some((item) => item.productId === product.id)) {
        return current;
      }
      return [
        ...current,
        {
          productId: product.id,
          productName: product.name,
          sellingPrice: product.basePrice,
          quantity: 1,
          availableStock: product.stockQuantity,
        },
      ];
    });
  };

  const removeItem = (productId: string) => {
    setItems((current) => current.filter((item) => item.productId !== productId));
  };

  const updateQuantity = (productId: string, quantity: number) => {
    setItems((current) => current.map((item) => (item.productId === productId ? { ...item, quantity } : item)));
  };

  const updateSellingPrice = (productId: string, sellingPrice: number) => {
    setItems((current) => current.map((item) => (item.productId === productId ? { ...item, sellingPrice } : item)));
  };

  const clear = () => setItems([]);

  const total = useMemo(() => items.reduce((sum, item) => sum + item.sellingPrice * item.quantity, 0), [items]);

  return { items, addProduct, removeItem, updateQuantity, updateSellingPrice, clear, total };
}
