import { forwardRef, type InputHTMLAttributes } from "react";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, id, className = "", ...props },
  ref,
) {
  const inputId = id ?? props.name;
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={inputId} className="text-sm font-medium text-ink-700">
        {label}
      </label>
      <input
        ref={ref}
        id={inputId}
        className={`min-h-11 rounded-md border bg-white px-3 py-2 text-sm text-ink-900 shadow-sm
          transition-colors duration-150 focus:outline focus:outline-2 focus:outline-offset-1
          focus:outline-gold-500 ${error ? "border-red-500" : "border-ink-300"} ${className}`}
        aria-invalid={error ? true : undefined}
        {...props}
      />
      {error && <p className="text-sm text-red-600">{error}</p>}
    </div>
  );
});
