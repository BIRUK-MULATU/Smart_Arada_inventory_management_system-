import { zodResolver } from "@hookform/resolvers/zod";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";
import type { Category } from "../../types/category";
import type { Product } from "../../types/product";

const productSchema = z.object({
  name: z.string().min(1, "Name is required"),
  sku: z.string().optional(),
  imageUrl: z.union([z.url("Enter a valid URL"), z.literal("")]).optional(),
  categoryId: z.string().min(1, "Choose a category"),
  basePrice: z.coerce.number().min(0, "Price must be zero or more"),
  costPrice: z.coerce.number().min(0, "Cost must be zero or more"),
  lowStockThreshold: z.coerce.number().int().min(0, "Threshold must be zero or more"),
  active: z.boolean(),
});

// z.coerce.number() accepts a broader input (what raw HTML fields actually produce) than its
// validated output (a real number), so the form's field-value type and its post-validation
// submit-handler type differ - useForm's third generic carries the resolver's actual output type
// through to handleSubmit's callback instead of forcing both ends to the same shape.
type ProductFormInput = z.input<typeof productSchema>;
export type ProductFormValues = z.output<typeof productSchema>;

interface ProductFormProps {
  product?: Product;
  categories: Category[];
  onSubmit: (values: ProductFormValues) => Promise<void>;
  onCancel: () => void;
}

export function ProductForm({ product, categories, onSubmit, onCancel }: ProductFormProps) {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormInput, unknown, ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: product?.name ?? "",
      sku: product?.sku ?? "",
      imageUrl: product?.imageUrl ?? "",
      categoryId: product?.categoryId ?? "",
      basePrice: product?.basePrice ?? 0,
      costPrice: product?.costPrice ?? 0,
      lowStockThreshold: product?.lowStockThreshold ?? 0,
      active: product?.active ?? true,
    },
  });

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Input label="Name" error={errors.name?.message} {...register("name")} />
      <Input label="SKU (optional)" error={errors.sku?.message} {...register("sku")} />
      <Input label="Image URL (optional)" error={errors.imageUrl?.message} {...register("imageUrl")} />
      <Select label="Category" error={errors.categoryId?.message} {...register("categoryId")}>
        <option value="">Select a category…</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.name}
          </option>
        ))}
      </Select>
      <Input
        label="Base price (selling price)"
        type="number"
        step="0.01"
        min="0"
        error={errors.basePrice?.message}
        {...register("basePrice")}
      />
      <Input
        label="Cost price (what you paid - used for profit tracking, never shown to employees)"
        type="number"
        step="0.01"
        min="0"
        error={errors.costPrice?.message}
        {...register("costPrice")}
      />
      <Input
        label="Low stock threshold"
        type="number"
        min="0"
        error={errors.lowStockThreshold?.message}
        {...register("lowStockThreshold")}
      />
      {product && (
        <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
          <input type="checkbox" className="h-4 w-4" {...register("active")} />
          Active
        </label>
      )}
      <div className="mt-2 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Saving…" : "Save"}
        </Button>
      </div>
    </form>
  );
}
