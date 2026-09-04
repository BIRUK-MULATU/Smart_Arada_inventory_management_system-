import { productsApi } from "../../api/productsApi";
import { db } from "../../db/db";

/**
 * Refreshes the local product cache from the server. Failures (offline, server unreachable) are
 * swallowed silently - the last-known-good cache in Dexie stays exactly as it was, which is the
 * whole point of caching it locally in the first place.
 */
export async function refreshProductSnapshot(): Promise<void> {
  let products;
  try {
    products = await productsApi.list(false);
  } catch {
    return;
  }

  const fetchedAt = new Date().toISOString();

  await db.transaction("rw", db.products, db.inventorySnapshot, async () => {
    await db.products.clear();
    await db.products.bulkAdd(
      products.map((product) => ({
        id: product.id,
        name: product.name,
        sku: product.sku,
        imageUrl: product.imageUrl,
        categoryId: product.categoryId,
        categoryName: product.categoryName,
        basePrice: product.basePrice,
        stockQuantity: product.stockQuantity,
        lowStockThreshold: product.lowStockThreshold,
        lowStock: product.lowStock,
        active: product.active,
      })),
    );
    await db.inventorySnapshot.put({ id: "current", fetchedAt, productCount: products.length });
  });
}
