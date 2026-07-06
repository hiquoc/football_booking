# Frontend development instructions

Before creating, reviewing, or modifying any file under this directory, read and follow [`CODE_STRUCTURE_RULES.md`](./CODE_STRUCTURE_RULES.md) completely.

These rules are mandatory for all frontend work. In particular:

- Keep pages, UI components, hooks, client API functions, BFF routes, and server data access separated.
- Browser code must call same-origin `/api/**` routes only.
- Put React Query query/mutation logic in custom hooks.
- Keep all user-facing display text in Vietnamese.
- Run typecheck, lint, relevant tests, and a production build after structural changes.
