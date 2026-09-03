import { useQuery } from "@tanstack/react-query";
import { salesApi } from "../../api/salesApi";

export function useSales(page: number) {
  return useQuery({ queryKey: ["sales", { page }], queryFn: () => salesApi.list(page) });
}
