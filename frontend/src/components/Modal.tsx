import type { ReactNode } from "react";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

export function Modal({ title, onClose, children }: ModalProps) {
  return (
    <div className="fixed inset-0 z-50 flex animate-fade-in items-center justify-center bg-ink-950/60 p-4">
      <div
        className="max-h-[90vh] w-full max-w-md animate-scale-in overflow-y-auto rounded-lg border border-ink-200
          bg-white p-6 shadow-2xl"
        role="dialog"
        aria-modal="true"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink-900">{title}</h2>
          <button
            onClick={onClose}
            aria-label="Close"
            className="flex h-8 w-8 items-center justify-center rounded-md text-ink-500 transition-colors
              duration-150 hover:bg-ink-100 hover:text-gold-600"
          >
            ✕
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
