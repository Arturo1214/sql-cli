# Proposal: Terminal Raw TUI Navigation

## Intent

Replace the line-oriented workspace with a Lanterna-backed full-screen TUI so QA users can navigate connections, SQL, results, and help/status without leaving the terminal. Keep script-friendly commands and non-TTY fallback behavior intact, while introducing a SQL editor abstraction for future highlighting, autocomplete, and correction.

## Scope

### In Scope

- Full-screen Lanterna workspace with left connections/actions, main SQL editor, bottom results, and help/status overlay/bar.
- Keyboard navigation: `Ctrl+Left`/`Ctrl+J` to the left panel, `Ctrl+Right`/`Ctrl+L` to the SQL editor, arrows/`j`/`k` for selection, `Ctrl+R` or double Enter to run, `Ctrl+H` for help.
- Oracle/PostgreSQL connection switching/creation, metadata commands, safety confirmation, and query execution/results.

### Out of Scope

- SQL syntax highlighting, autocomplete, and correction.
- New database types or non-terminal integrations.
- Removing script-oriented commands such as `run-current`.

## Capabilities

### New Capabilities

- `raw-terminal-workspace`: full-screen Lanterna navigation and panel layout.
- `workspace-connection-management`: connection switching/creation plus metadata access from the TUI.
- `workspace-query-execution`: safe execution, result rendering, and status/help feedback.

### Modified Capabilities

- None

## Approach

Use Lanterna as the primary UI shell and reuse the existing connection registry, safety guard, metadata providers, and SQL/result rendering instead of duplicating database logic. Keep a compact fallback for unsupported terminals and isolate the SQL editor behind an abstraction so later editing features can be added without changing the TUI contract.

## Affected Areas

| Area                                                                     | Impact   | Description                                                          |
| ------------------------------------------------------------------------ | -------- | -------------------------------------------------------------------- |
| `src/main/java/com/example/oraclescript/OracleScriptCli.java`            | Modified | Route default interactive mode to the new raw TUI entry point.       |
| `src/main/java/com/example/oraclescript/InteractiveWorkspace.java`       | Modified | Replace the line-oriented loop with panel-focused TUI session logic. |
| `src/main/java/com/example/oraclescript/WorkspaceDashboardRenderer.java` | Modified | Render the full-screen panel layout and fallback status/help areas.  |
| `src/main/java/com/example/oraclescript/WorkspaceCommand.java`           | Modified | Extend command handling for navigation and TUI actions.              |
| `src/main/java/com/example/oraclescript/SqlConsoleRenderer.java`         | Modified | Keep result formatting reusable inside the new results pane.         |

## Risks

| Risk                                                   | Likelihood | Mitigation                                                                 |
| ------------------------------------------------------ | ---------- | -------------------------------------------------------------------------- |
| Lanterna terminal behavior varies across environments  | Medium     | Keep compact fallback mode and detect unsupported terminals early.         |
| UI refactor regresses query or safety behavior         | Medium     | Reuse existing execution/safety services; do not change command semantics. |
| Keyboard shortcuts conflict with terminal/OS shortcuts | Low        | Document bindings and keep command-line fallback available.                |

## Rollback Plan

Restore the previous ANSI/line-oriented workspace entry path and remove the Lanterna wiring; all script commands remain available as the safe fallback.

## Dependencies

- Lanterna dependency and terminal support for full-screen rendering.

## Success Criteria

- [ ] The workspace can switch connections, edit SQL, run queries, and view results in a full-screen terminal UI for Oracle and PostgreSQL.
- [ ] Script-oriented commands and fallback behavior still work in non-TTY or compact terminal environments.
