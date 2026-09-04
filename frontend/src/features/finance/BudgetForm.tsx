import { zodResolver } from "@hookform/resolvers/zod";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";

const budgetSchema = z
  .object({
    category: z.string().optional(),
    periodType: z.enum(["MONTHLY", "QUARTERLY", "YEARLY", "CUSTOM"]),
    periodStart: z.string().min(1, "Date is required"),
    periodEnd: z.string().optional(),
    amount: z.coerce.number().min(0, "Amount must be zero or more"),
  })
  .refine((values) => values.periodType !== "CUSTOM" || !!values.periodEnd, {
    message: "End date is required for a custom period",
    path: ["periodEnd"],
  })
  .refine(
    (values) => values.periodType !== "CUSTOM" || !values.periodEnd || values.periodEnd >= values.periodStart,
    { message: "End date can't be before the start date", path: ["periodEnd"] },
  );

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
    control,
    formState: { errors, isSubmitting },
  } = useForm<BudgetFormInput, unknown, BudgetFormValues>({
    resolver: zodResolver(budgetSchema),
    defaultValues: { category: "", periodType: "MONTHLY", periodStart: today(), periodEnd: "", amount: 0 },
  });

  const periodType = useWatch({ control, name: "periodType" });
  const isCustom = periodType === "CUSTOM";

  const submit = async (values: BudgetFormValues) => {
    await onSubmit(values);
    reset({ category: "", periodType: "MONTHLY", periodStart: today(), periodEnd: "", amount: 0 });
  };

  return (
    <form className="grid grid-cols-1 gap-3 sm:grid-cols-5 sm:items-end" onSubmit={handleSubmit(submit)} noValidate>
      <Input label="Category (optional)" placeholder="Rent, Utilities…" error={errors.category?.message} {...register("category")} />
      <Select label="Period" error={errors.periodType?.message} {...register("periodType")}>
        <option value="MONTHLY">Monthly</option>
        <option value="QUARTERLY">Quarterly</option>
        <option value="YEARLY">Yearly</option>
        <option value="CUSTOM">Custom</option>
      </Select>
      <Input
        label={isCustom ? "Start date" : "Period start"}
        type="date"
        error={errors.periodStart?.message}
        {...register("periodStart")}
      />
      {isCustom && (
        <Input label="End date" type="date" error={errors.periodEnd?.message} {...register("periodEnd")} />
      )}
      <Input label="Budget amount" type="number" step="0.01" min="0" error={errors.amount?.message} {...register("amount")} />
      <Button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving…" : "Set budget"}
      </Button>
    </form>
  );
}
