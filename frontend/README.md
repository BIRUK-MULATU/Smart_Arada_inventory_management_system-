# Frontend — Inventory & Sales

React + TypeScript + Vite + Tailwind CSS. See the root `README.md` and
`CLAUDE.md` for full project context and architecture rules.

## Commands

```bash
npm install
npm run dev         # Vite dev server on http://localhost:5173
npm run typecheck   # tsc --noEmit
npm run lint         # ESLint
npm test             # Vitest
npm run build         # Type-checked production build
```

## Structure

- `src/api/` — typed HTTP client and endpoint wrappers
- `src/features/` — feature-scoped logic (auth, products, inventory, sales, employees, dashboard, sync)
- `src/components/` — shared presentational components
- `src/layouts/`, `src/pages/`, `src/routes/` — routing and page shells
- `src/db/` — IndexedDB (Dexie) schema and access
- `src/services/` — cross-cutting application services
- `src/types/`, `src/utils/`, `src/hooks/` — shared types, helpers, hooks
