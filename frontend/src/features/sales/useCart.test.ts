import { act, renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import type { Product } from "../../types/product";
import { useCart } from "./useCart";

function makeProduct(overrides: Partial<Product> = {}): Product {
  return {
    id: "product-1",
    name: "Widget",
    sku: "WIDGET-1",
    imageUrl: null,
    categoryId: "category-1",
    categoryName: "General",
    basePrice: 10,
    costPrice: null,
    stockQuantity: 5,
    lowStockThreshold: 2,
    lowStock: false,
    active: true,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    ...overrides,
  };
}

describe("useCart", () => {
  it("starts empty with a zero total", () => {
    const { result } = renderHook(() => useCart());
    expect(result.current.items).toEqual([]);
    expect(result.current.total).toBe(0);
  });

  it("adds a product using its base price and available stock", () => {
    const { result } = renderHook(() => useCart());
    act(() => result.current.addProduct(makeProduct()));

    expect(result.current.items).toHaveLength(1);
    expect(result.current.items[0]).toMatchObject({
      productId: "product-1",
      productName: "Widget",
      sellingPrice: 10,
      quantity: 1,
      availableStock: 5,
    });
    expect(result.current.total).toBe(10);
  });

  it("does not add the same product twice", () => {
    const { result } = renderHook(() => useCart());
    act(() => {
      result.current.addProduct(makeProduct());
      result.current.addProduct(makeProduct());
    });

    expect(result.current.items).toHaveLength(1);
  });

  it("recomputes the total when quantity or price changes", () => {
    const { result } = renderHook(() => useCart());
    act(() => result.current.addProduct(makeProduct()));
    act(() => result.current.updateQuantity("product-1", 3));
    act(() => result.current.updateSellingPrice("product-1", 8));

    expect(result.current.items[0].quantity).toBe(3);
    expect(result.current.items[0].sellingPrice).toBe(8);
    expect(result.current.total).toBe(24);
  });

  it("removes an item and updates the total", () => {
    const { result } = renderHook(() => useCart());
    act(() => result.current.addProduct(makeProduct()));
    act(() => result.current.removeItem("product-1"));

    expect(result.current.items).toEqual([]);
    expect(result.current.total).toBe(0);
  });

  it("sums multiple distinct items correctly", () => {
    const { result } = renderHook(() => useCart());
    act(() => {
      result.current.addProduct(makeProduct({ id: "product-1", basePrice: 10 }));
      result.current.addProduct(makeProduct({ id: "product-2", name: "Gadget", basePrice: 5 }));
    });
    act(() => result.current.updateQuantity("product-2", 4));

    expect(result.current.total).toBe(10 * 1 + 5 * 4);
  });

  it("clears all items", () => {
    const { result } = renderHook(() => useCart());
    act(() => {
      result.current.addProduct(makeProduct({ id: "product-1" }));
      result.current.addProduct(makeProduct({ id: "product-2" }));
    });
    act(() => result.current.clear());

    expect(result.current.items).toEqual([]);
  });
});
