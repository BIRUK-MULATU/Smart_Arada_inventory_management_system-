import { Button } from "./Button";

interface PaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="flex items-center justify-between">
      <Button variant="secondary" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
        Previous
      </Button>
      <span className="text-sm text-ink-500">
        Page {page + 1} of {totalPages}
      </span>
      <Button variant="secondary" onClick={() => onPageChange(page + 1)} disabled={page + 1 >= totalPages}>
        Next
      </Button>
    </div>
  );
}
