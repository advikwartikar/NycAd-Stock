# Project documentation

This folder is the home for maintained project documentation. Keep the root README focused on what the project is, how to run it, and where to find more detail.

## Documentation maintenance

- Keep one canonical guide for each topic.
- Prefer Markdown (`.md`) for documentation.
- Remove obsolete instructions only after checking that they are no longer needed.
- Ensure commands, environment variables, deployment providers, database settings, and screenshots match the current code.
- Never include real credentials, access tokens, or private connection strings.

## Existing notes

The repository root contains historical setup, feature, database, deployment, and troubleshooting notes. They have not been deleted or moved automatically because their contents need to be reviewed before consolidation. When reviewing them, consolidate current guidance into a small set of maintained documents, for example:

- `setup.md` — local development and configuration
- `architecture.md` — modules and data flow
- `database.md` — supported database profiles and schema
- `deployment.md` — one verified deployment path
- `troubleshooting.md` — current, reproducible fixes

Do not treat an old guide as authoritative until it has been checked against the current application.
