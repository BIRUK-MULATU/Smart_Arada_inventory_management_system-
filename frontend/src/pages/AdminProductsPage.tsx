import { useState } from "react";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { Modal } from "../components/Modal";
import { useCategories } from "../features/categories/useCategories";
import type { ProductFormValues } from "../features/products/ProductForm";
import { ProductForm } from "../features/products/ProductForm";
import { ProductImageUpload } from "../features/products/ProductImageUpload";
import { ProductTable } from "../features/products/ProductTable";
import { useCreateProduct, useDeactivateProduct, useProducts, useUpdateProduct } from "../features/products/useProducts";
import type { Product } from "../types/product";

type DialogState = { mode: "create" } | { mode: "edit"; product: Product } | null;

export function AdminProductsPage() {
  const [categoryFilter, setCategoryFilter] = useState("");
  const { data: categories } = useCategories();
  const { data: products, isLoading, isError } = useProducts(true, categoryFilter || undefined);
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
            categoryId: values.categoryId,
            basePrice: values.basePrice,
            costPrice: values.costPrice,
            lowStockThreshold: values.lowStockThreshold,
            active: values.active,
          },
        });
      } else {
        await createProduct.mutateAsync({
          name: values.name,
          sku: values.sku || undefined,
          imageUrl: values.imageUrl || undefined,
          categoryId: values.categoryId,
          basePrice: values.basePrice,
          costPrice: values.costPrice,
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

  const handleDownloadPdf = async () => {
    if (!products) {
      return;
    }
    const { addTable, createReport, dateStamp, savePdf } = await import("../utils/pdfExport");
    const categoryName = categoryFilter ? categories?.find((c) => c.id === categoryFilter)?.name : undefined;
    const { doc, startY } = createReport("Product catalog", categoryName ? `Category: ${categoryName}` : "All categories");
    addTable(doc, {
      head: [["Name", "Category", "SKU", "Base price", "Wholesale price", "Stock", "Status"]],
      body: products.map((product) => [
        product.name,
        product.categoryName,
        product.sku ?? "—",
        `$${product.basePrice.toFixed(2)}`,
        product.costPrice !== null ? `$${product.costPrice.toFixed(2)}` : "—",
        String(product.stockQuantity),
        !product.active ? "Inactive" : product.lowStock ? "Low stock" : "Active",
      ]),
      startY,
    });
    savePdf(doc, `products-${dateStamp()}.pdf`);
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-semibold text-ink-900">Products</h1>
        <div className="flex items-center gap-3">
          <select
            aria-label="Filter by category"
            value={categoryFilter}
            onChange={(event) => setCategoryFilter(event.target.value)}
            className="min-h-11 rounded-md border border-ink-300 bg-white px-3 py-2 text-sm shadow-sm"
          >
            <option value="">All categories</option>
            {categories?.map((category) => (
              <option key={category.id} value={category.id}>
                {category.name}
              </option>
            ))}
          </select>
          <Button variant="secondary" onClick={handleDownloadPdf} disabled={!products}>
            Download PDF
          </Button>
          <Button onClick={() => setDialog({ mode: "create" })}>New product</Button>
        </div>
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
          {dialog.mode === "edit" && (
            <div className="mb-4">
              <ProductImageUpload
                product={dialog.product}
                onUpdated={(product) => setDialog({ mode: "edit", product })}
              />
            </div>
          )}
          <ProductForm
            product={dialog.mode === "edit" ? dialog.product : undefined}
            categories={categories ?? []}
            onSubmit={handleSubmit}
            onCancel={() => setDialog(null)}
          />
        </Modal>
      )}
    </div>
  );
}
