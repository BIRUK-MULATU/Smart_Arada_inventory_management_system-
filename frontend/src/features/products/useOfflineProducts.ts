import { useLiveQuery } from "dexie-react-hooks";
import { useEffect } from "react";
import { db } from "../../db/db";
import { useOnlineStatus } from "../../hooks/useOnlineStatus";
import { refreshProductSnapshot } from "./offlineProducts";

/**
 * Reads the product catalog from the local Dexie cache (always available, works offline), and
 * opportunistically refreshes that cache from the server on mount and whenever the browser
 * regains connectivity. The employee-facing product/sale pages read through this hook instead of
 * calling the API directly, so they work with no network per spec §11. Admin pages are
 * unaffected - they keep using the plain online-only useProducts hook from Phase 8.
 */
export function useOfflineProducts() {
  const isOnline = useOnlineStatus();

  useEffect(() => {
    if (isOnline) {
      refreshProductSnapshot();
    }
  }, [isOnline]);

  // refreshProductSnapshot only ever caches active products (it fetches with
  // includeInactive=false, matching the employee-facing default from Phase 4), so no further
  // filtering is needed here.
  const products = useLiveQuery(() => db.products.toArray(), []);
  const snapshot = useLiveQuery(() => db.inventorySnapshot.get("current"), []);

  return { products, fetchedAt: snapshot?.fetchedAt ?? null };
}
