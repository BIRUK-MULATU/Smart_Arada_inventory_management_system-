import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { inventoryApi } from "../../api/inventoryApi";
import type { StockInRequest } from "../../types/inventory";

export function useInventoryList() {
  return useQuery({ queryKey: ["inventory"], queryFn: () => inventoryApi.list() });
}

export function useInventoryHistory(productId: string | undefined, page: number) {
  return useQuery({
    queryKey: ["inventory-history", { productId, page }],
    queryFn: () => inventoryApi.history(productId, page),
  });
}

export function useStockIn() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: StockInRequest) => inventoryApi.stockIn(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      queryClient.invalidateQueries({ queryKey: ["inventory-history"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
