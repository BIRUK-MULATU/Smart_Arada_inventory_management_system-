import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { financeApi } from "../../api/financeApi";
import type { CreateBudgetRequest, CreateExpenseRequest } from "../../types/finance";

export function useFinanceSummary(from?: string, to?: string) {
  return useQuery({
    queryKey: ["finance", "summary", { from, to }],
    queryFn: () => financeApi.getSummary(from, to),
  });
}

export function useExpenses(from: string, to: string, page: number) {
  return useQuery({
    queryKey: ["finance", "expenses", { from, to, page }],
    queryFn: () => financeApi.listExpenses(from, to, page),
  });
}

export function useRecordExpense() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateExpenseRequest) => financeApi.recordExpense(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["finance"] });
    },
  });
}

export function useDeleteExpense() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => financeApi.deleteExpense(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["finance"] }),
  });
}

export function useBudgets() {
  return useQuery({ queryKey: ["finance", "budgets"], queryFn: () => financeApi.listBudgets() });
}

export function useCreateBudget() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (request: CreateBudgetRequest) => financeApi.createBudget(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["finance", "budgets"] }),
  });
}

export function useUpdateBudgetAmount() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, amount }: { id: string; amount: number }) => financeApi.updateBudgetAmount(id, amount),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["finance", "budgets"] }),
  });
}

export function useDeleteBudget() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => financeApi.deleteBudget(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["finance", "budgets"] }),
  });
}
