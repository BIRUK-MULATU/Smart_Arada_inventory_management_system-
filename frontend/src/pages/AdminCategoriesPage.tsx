import { useState, type FormEvent } from "react";
import { extractErrorMessage } from "../api/errors";
import { Button } from "../components/Button";
import { EmptyState } from "../components/EmptyState";
import { ErrorMessage } from "../components/ErrorMessage";
import { Input } from "../components/Input";
import { LoadingSpinner } from "../components/LoadingSpinner";
import { useCategories, useCreateCategory, useDeleteCategory, useRenameCategory } from "../features/categories/useCategories";
import type { Category } from "../types/category";

export function AdminCategoriesPage() {
  const { data: categories, isLoading, isError } = useCategories();
  const createCategory = useCreateCategory();
  const renameCategory = useRenameCategory();
  const deleteCategory = useDeleteCategory();

  const [newName, setNewName] = useState("");
  const [editingId, setEditingId] = useState<string | null>(null);
  const [editingName, setEditingName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const handleCreate = async (event: FormEvent) => {
    event.preventDefault();
    if (!newName.trim()) {
      return;
    }
    setError(null);
    try {
      await createCategory.mutateAsync({ name: newName.trim() });
      setNewName("");
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const startEditing = (category: Category) => {
    setEditingId(category.id);
    setEditingName(category.name);
  };

  const handleRename = async (id: string) => {
    if (!editingName.trim()) {
      return;
    }
    setError(null);
    try {
      await renameCategory.mutateAsync({ id, request: { name: editingName.trim() } });
      setEditingId(null);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  const handleDelete = async (category: Category) => {
    if (!window.confirm(`Delete "${category.name}"? Products in this category must be moved first.`)) {
      return;
    }
    setError(null);
    try {
      await deleteCategory.mutateAsync(category.id);
    } catch (err) {
      setError(extractErrorMessage(err));
    }
  };

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-xl font-semibold text-slate-900">Categories</h1>
      <p className="text-sm text-slate-500">
        Group products (Electronics, Household, Glassware…) so they're easy to tell apart across the catalog,
        inventory, and sales.
      </p>

      <form onSubmit={handleCreate} className="flex items-end gap-2">
        <div className="flex-1">
          <Input label="New category" value={newName} onChange={(event) => setNewName(event.target.value)} />
        </div>
        <Button type="submit" disabled={createCategory.isPending}>
          Add
        </Button>
      </form>

      {error && <ErrorMessage message={error} />}
      {isLoading && <LoadingSpinner />}
      {isError && <ErrorMessage message="Couldn't load categories." />}
      {categories && categories.length === 0 && <EmptyState message="No categories yet. Add the first one above." />}

      {categories && categories.length > 0 && (
        <div className="overflow-x-auto rounded-lg border border-slate-200 bg-white">
          <table className="min-w-full divide-y divide-slate-200 text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="px-4 py-2 text-left font-medium text-slate-600">Name</th>
                <th className="px-4 py-2" />
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {categories.map((category) => (
                <tr key={category.id}>
                  <td className="px-4 py-2 font-medium text-slate-900">
                    {editingId === category.id ? (
                      <input
                        aria-label={`Rename ${category.name}`}
                        value={editingName}
                        onChange={(event) => setEditingName(event.target.value)}
                        className="min-h-11 w-full rounded-md border border-slate-300 px-2 py-1 text-sm"
                      />
                    ) : (
                      category.name
                    )}
                  </td>
                  <td className="px-4 py-2 text-right">
                    <div className="flex justify-end gap-2">
                      {editingId === category.id ? (
                        <>
                          <Button variant="secondary" onClick={() => setEditingId(null)}>
                            Cancel
                          </Button>
                          <Button onClick={() => handleRename(category.id)}>Save</Button>
                        </>
                      ) : (
                        <>
                          <Button variant="secondary" onClick={() => startEditing(category)}>
                            Rename
                          </Button>
                          <Button variant="danger" onClick={() => handleDelete(category)}>
                            Delete
                          </Button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
