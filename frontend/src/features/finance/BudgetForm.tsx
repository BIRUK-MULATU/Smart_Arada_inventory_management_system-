import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";

const budgetSchema = z.object({
  category: z.string().optional(),
  periodType: z.enum(["MONTHLY", "YEARLY"]),
  periodStart: z.string().min(1, "Date is required"),
  amount: z.coerce.number().min(0, "Amount must be zero or more"),
});

type BudgetFormInput = z.input<typeof budgetSchema>;
export type BudgetFormValues = z.output<typeof budgetSchema>;

interface BudgetFormProps {
  onSubmit: (values: BudgetFormValues) => Promise<void>;
}

const today = () => new Date().toISOString().slice(0, 10);

export function BudgetForm({ onSubmit }: BudgetFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<BudgetFormInput, unknown, BudgetFormValues>({
    resolver: zodResolver(budgetSchema),
    defaultValues: { category: "", periodType: "MONTHLY", periodStart: today(), amount: 0 },
  });

  const submit = async (values: BudgetFormValues) => {
    await onSubmit(values);
    reset({ category: "", periodType: "MONTHLY", periodStart: today(), amount: 0 });
  };

  return (
    <form className="grid grid-cols-1 gap-3 sm:grid-cols-5 sm:items-end" onSubmit={handleSubmit(submit)} noValidate>
      <Input label="Category (optional)" placeholder="Rent, Utilities…" error={errors.category?.message} {...register("category")} />
      <Select label="Period" error={errors.periodType?.message} {...register("periodType")}>
        <option value="MONTHLY">Monthly</option>
        <option value="YEARLY">Yearly</option>
      </Select>
      <Input label="Period start" type="date" error={errors.periodStart?.message} {...register("periodStart")} />
      <Input label="Budget amount" type="number" step="0.01" min="0" error={errors.amount?.message} {...register("amount")} />
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving…" : "Set budget"}
      </Button>
    </form>
  );
}
