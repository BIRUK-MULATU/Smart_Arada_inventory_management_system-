import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { salesApi } from "../../api/salesApi";
import type { CreateSaleRequest } from "../../types/sale";

export function useSales(page: number) {
  return useQuery({ queryKey: ["sales", { page }], queryFn: () => salesApi.list(page) });
}

export function useCreateSale() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateSaleRequest) => salesApi.create(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["sales"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
    },
  });
}
