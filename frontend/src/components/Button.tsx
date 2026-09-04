import type { ButtonHTMLAttributes } from "react";

type Variant = "primary" | "secondary" | "danger";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
}

const variantClasses: Record<Variant, string> = {
  primary:
    "bg-ink-950 text-gold-400 border border-gold-600/40 hover:bg-ink-900 hover:text-gold-300 hover:border-gold-500/60 focus-visible:outline-gold-500",
  secondary: "bg-white text-ink-900 border border-ink-300 hover:bg-ink-50 hover:border-gold-500/50 focus-visible:outline-gold-500",
  danger: "bg-red-600 text-white hover:bg-red-500 focus-visible:outline-red-600",
};

export function Button({ variant = "primary", className = "", disabled, ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex min-h-11 items-center justify-center rounded-md px-4 py-2 text-sm font-medium
        transition-all duration-150 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2
        disabled:cursor-not-allowed disabled:opacity-50 ${variantClasses[variant]} ${className}`}
      disabled={disabled}
      {...props}
    />
  );
}
