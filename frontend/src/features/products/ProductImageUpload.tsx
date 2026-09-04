import { useState, type ChangeEvent } from "react";
import { extractErrorMessage } from "../../api/errors";
import type { Product } from "../../types/product";
import { useRemoveProductImage, useUploadProductImage } from "./useProducts";

interface ProductImageUploadProps {
  product: Product;
  onUpdated: (product: Product) => void;
}

export function ProductImageUpload({ product, onUpdated }: ProductImageUploadProps) {
  const uploadImage = useUploadProductImage();
  const removeImage = useRemoveProductImage();
  const [error, setError] = useState<string | null>(null);

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }
    setError(null);
    try {
      const updated = await uploadImage.mutateAsync({ id: product.id, file });
      onUpdated(updated);
    } catch (err) {
      setError(extractErrorMessage(err, "Couldn't upload the image."));
    } finally {
      event.target.value = "";
    }
  };

  const handleRemove = async () => {
    setError(null);
    try {
      const updated = await removeImage.mutateAsync(product.id);
      onUpdated(updated);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="flex flex-col gap-2">
      <span className="text-sm font-medium text-slate-700">Photo</span>
      <div className="flex items-center gap-3">
        {product.imageUrl ? (
          <img
            src={product.imageUrl}
            alt={product.name}
            className="h-16 w-16 rounded-md border border-slate-200 object-cover"
          />
        ) : (
          <div className="flex h-16 w-16 items-center justify-center rounded-md border border-dashed border-slate-300 text-center text-xs text-slate-400">
            No photo
          </div>
        )}
        <div className="flex flex-col gap-1">
          <label className="min-h-11 cursor-pointer rounded-md border border-slate-300 px-3 py-2 text-center text-sm font-medium text-slate-700 hover:bg-slate-50">
            {uploadImage.isPending ? "Uploading…" : "Upload from device"}
            <input
              type="file"
              accept="image/jpeg,image/png,image/webp"
              className="hidden"
              onChange={handleFileChange}
              disabled={uploadImage.isPending}
            />
          </label>
          {product.imageUrl && (
            <button
              type="button"
              onClick={handleRemove}
              disabled={removeImage.isPending}
              className="min-h-6 text-sm text-red-600 hover:underline disabled:opacity-50"
            >
              {removeImage.isPending ? "Removing…" : "Remove photo"}
            </button>
          )}
        </div>
      </div>
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
}
