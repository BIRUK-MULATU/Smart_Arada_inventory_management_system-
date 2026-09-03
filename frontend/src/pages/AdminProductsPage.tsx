import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Modal } from "../components/Modal";
import type { ProductFormValues } from "../features/products/ProductForm";
import { ProductForm } from "../features/products/ProductForm";
import { ProductTable } from "../features/products/ProductTable";
import { useCreateProduct, useDeactivateProduct, useProducts, useUpdateProduct } from "../features/products/useProducts";
import type { Product } from "../types/product";

type DialogState = { mode: "create" } | { mode: "edit"; product: Product } | null;

export function AdminProductsPage() {
  const { data: products, isLoading, isError } = useProducts(true);
  const createProduct = useCreateProduct();
  const updateProduct = useUpdateProduct();
  const deactivateProduct = useDeactivateProduct();
  const [dialog, setDialog] = useState<DialogState>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const handleSubmit = async (values: ProductFormValues) => {
    setFormError(null);
    try {
      if (dialog?.mode === "edit") {
        await updateProduct.mutateAsync({
          id: dialog.product.id,
          request: {
            name: values.name,
            sku: values.sku || undefined,
            imageUrl: values.imageUrl || undefined,
            basePrice: values.basePrice,
            lowStockThreshold: values.lowStockThreshold,
            active: values.active,
          },
        });
      } else {
        await createProduct.mutateAsync({
          name: values.name,
          sku: values.sku || undefined,
          imageUrl: values.imageUrl || undefined,
          basePrice: values.basePrice,
          lowStockThreshold: values.lowStockThreshold,
        });
      }
      setDialog(null);
    } catch (error) {
      setFormError(extractErrorMessage(error));
    }
  };

  const handleDeactivate = (product: Product) => {
    if (window.confirm(`Deactivate ${product.name}? It can be reactivated later by editing it.`)) {
      deactivateProduct.mutate(product.id);
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between">
        <h1 className="text-xl font-semibold text-slate-900">Products</h1>
        <Button onClick={() => setDialog({ mode: "create" })}>New product</Button>
      </div>

      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load products." />}
      {products && products.length === 0 && <EmptyState message="No products yet. Create the first one." />}
      {products && products.length > 0 && (
        <ProductTable products={products} onEdit={(product) => setDialog({ mode: "edit", product })} onDeactivate={handleDeactivate} />
      )}

      {dialog && (
        <Modal title={dialog.mode === "edit" ? "Edit product" : "New product"} onClose={() => setDialog(null)}>
          {formError && (
            <div className="mb-4">
              <ErrorMessage message={formError} />
            </div>
          )}
          <ProductForm
            product={dialog.mode === "edit" ? dialog.product : undefined}
            onSubmit={handleSubmit}
            onCancel={() => setDialog(null)}
          />
        </Modal>
      )}
    </div>
  );
}
