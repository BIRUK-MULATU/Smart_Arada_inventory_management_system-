import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";

const expenseSchema = z.object({
  category: z.string().optional(),
  description: z.string().min(1, "Description is required"),
  amount: z.coerce.number().min(0, "Amount must be zero or more"),
  incurredOn: z.string().min(1, "Date is required"),
});

type ExpenseFormInput = z.input<typeof expenseSchema>;
export type ExpenseFormValues = z.output<typeof expenseSchema>;

interface ExpenseFormProps {
  onSubmit: (values: ExpenseFormValues) => Promise<void>;
}

const today = () => new Date().toISOString().slice(0, 10);

export function ExpenseForm({ onSubmit }: ExpenseFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<ExpenseFormInput, unknown, ExpenseFormValues>({
    resolver: zodResolver(expenseSchema),
    defaultValues: { category: "", description: "", amount: "", incurredOn: today() },
  });

  const submit = async (values: ExpenseFormValues) => {
    await onSubmit(values);
    reset({ category: "", description: "", amount: "", incurredOn: today() });
  };

  return (
    <form className="grid grid-cols-1 gap-3 sm:grid-cols-5 sm:items-end" onSubmit={handleSubmit(submit)} noValidate>
      <Input label="Category (optional)" placeholder="Rent, Utilities…" error={errors.category?.message} {...register("category")} />
      <Input label="Description" error={errors.description?.message} {...register("description")} />
      <Input label="Amount" type="number" step="0.01" min="0" error={errors.amount?.message} {...register("amount")} />
      <Input label="Date" type="date" error={errors.incurredOn?.message} {...register("incurredOn")} />
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving…" : "Add expense"}
      </Button>
    </form>
  );
}
