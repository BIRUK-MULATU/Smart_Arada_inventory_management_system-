import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { syncApi } from "../../api/syncApi";
import type { ResolveConflictRequest } from "../../types/sync";

export function useConflicts(page: number) {
  return useQuery({ queryKey: ["conflicts", { page }], queryFn: () => syncApi.listConflicts(page) });
}

export function useResolveConflict() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ saleId, request }: { saleId: string; request: ResolveConflictRequest }) =>
      syncApi.resolveConflict(saleId, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["conflicts"] });
      queryClient.invalidateQueries({ queryKey: ["products"] });
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      queryClient.invalidateQueries({ queryKey: ["dashboard"] });
      queryClient.invalidateQueries({ queryKey: ["sales"] });
    },
  });
}
