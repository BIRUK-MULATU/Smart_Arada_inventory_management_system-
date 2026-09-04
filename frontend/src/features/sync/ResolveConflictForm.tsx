import { useState, type FormEvent } from "react";
import { Button } from "../../components/Button";
import { Input } from "../../components/Input";
import type { Conflict, ResolveConflictRequest } from "../../types/sync";

interface ResolveConflictFormProps {
  conflict: Conflict;
  onSubmit: (request: ResolveConflictRequest) => Promise<void>;
  onCancel: () => void;
}

export function ResolveConflictForm({ conflict, onSubmit, onCancel }: ResolveConflictFormProps) {
  const [quantities, setQuantities] = useState<Record<string, number>>(() =>
    Object.fromEntries(conflict.items.map((item) => [item.productId, item.shortfall])),
  );
  const [note, setNote] = useState("");
  const [noteError, setNoteError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (!note.trim()) {
      setNoteError("A note is required.");
      return;
    }
    setNoteError(null);
    setIsSubmitting(true);
    try {
      await onSubmit({
        adjustments: conflict.items.map((item) => ({
          productId: item.productId,
          restockQuantity: quantities[item.productId] ?? 0,
        })),
        note: note.trim(),
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
      <div className="flex flex-col gap-3">
        {conflict.items.map((item) => (
          <div key={item.productId} className="flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-medium text-ink-900">{item.productName}</p>
              <p className="text-xs text-ink-500">Short by {item.shortfall}</p>
            </div>
            <input
              type="number"
              min="0"
              aria-label={`Restock quantity for ${item.productName}`}
              className="min-h-11 w-24 rounded-md border border-ink-300 px-2 py-1 text-sm"
              value={quantities[item.productId]}
              onChange={(event) =>
                setQuantities((current) => ({
                  ...current,
                  [item.productId]: Number(event.target.value),
                }))
              }
            />
          </div>
        ))}
      </div>
      <Input
        label="Resolution note"
        placeholder="Why - e.g. restocked from back room, confirmed as a loss…"
        value={note}
        onChange={(event) => setNote(event.target.value)}
        error={noteError ?? undefined}
      />
      <div className="mt-2 flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {isSubmitting ? "Resolving…" : "Resolve conflict"}
        </Button>
      </div>
    </form>
  );
}
