# Proposal: SQL Editor Intelligence

## Intent

Add a lightweight intelligence layer to the terminal SQL editor so users get immediate feedback while typing without introducing heavy parsing or validation. This improves readability and query entry speed for Oracle/PostgreSQL while keeping the MVP small and testable.

## Scope

### In Scope

- Token/keyword highlighting in the editor.
- Autocomplete model for SQL keywords and metadata-backed table names.
- Span/decorations abstraction for terminal rendering.
- Metadata catalog/snapshot abstraction fed from existing metadata paths where possible.

### Out of Scope

- Parser-based syntax correction.
- Deep dialect-aware validation.
- Column/function autocomplete and full context-sensitive completion.

## Capabilities

### New Capabilities

- `sql-editor-rendering`: span/decorations model plus SQL token highlighting.
- `sql-autocomplete`: keyword and table-name completion suggestions.
- `sql-metadata-catalog`: read-only catalog/snapshot for table suggestions.

### Modified Capabilities

- None

## Approach

Keep the current editor buffer, but introduce a typed render model that separates text storage from styling and suggestions. Build a lightweight tokenizer for keywords, identifiers, literals, and punctuation; surface highlighted spans through the editor contract and render them in the terminal workspace. Back autocomplete with a read-only catalog snapshot assembled from existing metadata discovery paths and provider-adjacent data, so table suggestions work without a parser or per-keystroke live introspection.

## Affected Areas

| Area                                                                                           | Impact   | Description                                                     |
| ---------------------------------------------------------------------------------------------- | -------- | --------------------------------------------------------------- |
| `src/main/java/com/example/oraclescript/SqlEditorComponent.java`                               | Modified | Extend the editor contract for spans and completion candidates. |
| `src/main/java/com/example/oraclescript/BasicSqlEditorComponent.java`                          | Modified | Produce token spans and keyword/table suggestions.              |
| `src/main/java/com/example/oraclescript/WorkspaceViewModel.java`                               | Modified | Carry styled editor data instead of flattened strings only.     |
| `src/main/java/com/example/oraclescript/RawTerminalWorkspace.java`                             | Modified | Render spans/decorations in the terminal UI.                    |
| `src/main/java/com/example/oraclescript/MetadataProvider*.java`, `SchemaDiscoveryService.java` | Modified | Add catalog/snapshot support for completion sources.            |
| `src/test/java/com/example/oraclescript/*Test.java`                                            | Modified | Cover highlighting, completion, and catalog snapshot behavior.  |

## Risks

| Risk                                             | Likelihood | Mitigation                                                                                |
| ------------------------------------------------ | ---------- | ----------------------------------------------------------------------------------------- |
| Render model changes ripple through the TUI      | Medium     | Keep the abstraction small and preserve plain-text fallback paths.                        |
| Metadata sources do not expose enough table data | Medium     | Start with snapshot adapters on existing discovery paths and keep completion best-effort. |
| Autocomplete UX feels incomplete without parsing | Low        | Explicitly defer parser-based correction and dialect-aware validation.                    |

## Rollback Plan

Revert the editor contract and workspace rendering changes, then fall back to plain text editing with the existing metadata/query behavior intact.

## Dependencies

- Existing Oracle/PostgreSQL metadata discovery paths.

## Success Criteria

- [ ] SQL keywords render with token highlighting in the terminal editor.
- [ ] Tab/shortcut-driven autocomplete suggests SQL keywords and table names from metadata snapshots.
- [ ] No parser-based correction or deep validation is introduced in this change.
