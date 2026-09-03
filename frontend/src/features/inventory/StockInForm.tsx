import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";
import type { Product } from "../../types/product";

const stockInSchema = z.object({
  productId: z.string().min(1, "Choose a product"),
  quantity: z.coerce.number().int().positive("Quantity must be at least 1"),
  reason: z.string().optional(),
});

// See ProductForm for why this needs separate input/output types: z.coerce.number() accepts a
// broader input than its validated (numeric) output.
type StockInFormInput = z.input<typeof stockInSchema>;
export type StockInFormValues = z.output<typeof stockInSchema>;

interface StockInFormProps {
  products: Product[];
  onSubmit: (values: StockInFormValues) => Promise<void>;
  onCancel: () => void;
}

export function StockInForm({ products, onSubmit, onCancel }: StockInFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<StockInFormInput, unknown, StockInFormValues>({
    resolver: zodResolver(stockInSchema),
    defaultValues: { productId: "", quantity: 1, reason: "" },
  });

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Select label="Product" error={errors.productId?.message} {...register("productId")}>
        <option value="">Select a product…</option>
        {products.map((product) => (
          <option key={product.id} value={product.id}>
            {product.name} ({product.stockQuantity} in stock)
          </option>
        ))}
      </Select>
      <Input label="Quantity" type="number" min="1" error={errors.quantity?.message} {...register("quantity")} />
      <Input label="Reason (optional)" error={errors.reason?.message} {...register("reason")} />
      <div className="mt-2 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving…" : "Stock in"}
        </Button>
      </div>
    </form>
  );
}
