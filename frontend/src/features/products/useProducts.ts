import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { productsApi } from "../../api/productsApi";
import type { CreateProductRequest, UpdateProductRequest } from "../../types/product";

const productsKey = (includeInactive: boolean) => ["products", { includeInactive }] as const;

export function useProducts(includeInactive = false) {
  return useQuery({
    queryKey: productsKey(includeInactive),
    queryFn: () => productsApi.list(includeInactive),
  });
}

export function useCreateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateProductRequest) => productsApi.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products"] }),
  });
}

export function useUpdateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, request }: { id: string; request: UpdateProductRequest }) => productsApi.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products"] }),
  });
}

export function useDeactivateProduct() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => productsApi.deactivate(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products"] }),
  });
}
