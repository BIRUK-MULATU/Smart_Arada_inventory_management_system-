import { Link } from "react-router-dom";

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-2 bg-ink-950 px-4 text-center">
      <h1 className="text-2xl font-semibold text-gold-400">Page not found</h1>
      <p className="text-sm text-ink-400">The page you're looking for doesn't exist.</p>
      <Link to="/" className="mt-2 text-sm font-medium text-gold-400 underline hover:text-gold-300">
        Go back home
      </Link>
    </div>
  );
}
