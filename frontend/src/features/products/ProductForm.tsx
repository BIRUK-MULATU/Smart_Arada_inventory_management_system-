import { zodResolver } from "@hookform/resolvers/zod";
import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import { Select } from "../../components/Select";
import type { Category } from "../../types/category";
import type { Product } from "../../types/product";

// z.url() rejects anything without an explicit http(s):// prefix - but a pasted link often
// doesn't have one ("www.example.com/photo.jpg", "example.com/photo.jpg"), which silently failed
// validation with an easy-to-miss inline error and made the field look broken. Prepending https://
// when it's missing makes the field accept what people actually paste.
function normalizeImageUrl(value: string): string {
  const trimmed = value.trim();
  if (!trimmed || /^https?:\/\//i.test(trimmed)) {
    return trimmed;
  }
  return `https://${trimmed}`;
}

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
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<ProductFormInput, unknown, ProductFormValues>({
    resolver: zodResolver(productSchema),
    defaultValues: {
      name: product?.name ?? "",
      sku: product?.sku ?? "",
      imageUrl: product?.imageUrl ?? "",
      categoryId: product?.categoryId ?? "",
      // Empty, not 0: a number input pre-filled with the literal digit "0" means the user's
      // first keystroke lands after it rather than replacing it - typing "3444" becomes "03444"
      // instead of "3444". Leaving it blank on create avoids that; editing an existing product
      // still shows its real value above.
      basePrice: product?.basePrice ?? "",
      costPrice: product?.costPrice ?? "",
      lowStockThreshold: product?.lowStockThreshold ?? "",
      active: product?.active ?? true,
    },
  });

  // useForm's defaultValues are only read once, at mount - uploading or removing a photo via
  // ProductImageUpload updates `product` from outside this form (a separate mutation, not a form
  // field), so without this the stale imageUrl already in the form would silently overwrite the
  // upload the next time this form is saved.
  useEffect(() => {
    setValue("imageUrl", product?.imageUrl ?? "");
  }, [product?.imageUrl, setValue]);

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(onSubmit)} noValidate>
      <Input label="Name" error={errors.name?.message} {...register("name")} />
      <Input label="SKU (optional)" error={errors.sku?.message} {...register("sku")} />
      <Input
        label="Image URL (optional)"
        placeholder="example.com/photo.jpg"
        error={errors.imageUrl?.message}
        {...register("imageUrl", { setValueAs: normalizeImageUrl })}
      />
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
        label="Wholesale price (what you paid the supplier - used for profit tracking, never shown to employees)"
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
        <label className="flex items-center gap-2 text-sm font-medium text-ink-700">
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
