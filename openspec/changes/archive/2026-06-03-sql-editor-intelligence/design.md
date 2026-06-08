# Design: SQL Editor Intelligence

## Technical Approach

Add a small, Java 8-friendly editor intelligence layer around the existing `BasicSqlEditorComponent`. Keep the editor as the buffer owner, but replace the current string-only decoration placeholder with typed render spans and completion data. SQL highlighting is produced by a lightweight tokenizer, then mapped to Lanterna colors at the terminal boundary. Autocomplete uses a provider abstraction that combines SQL keywords with a read-only metadata catalog snapshot, so unit tests can run without Oracle/PostgreSQL.

## Architecture Decisions

| Option                                           | Tradeoff                                                                                                      | Decision                                                                                       |
| ------------------------------------------------ | ------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| Lightweight tokenizer vs parser                  | Tokenizer avoids dialect false positives; parser enables deeper correction but exceeds MVP                    | Use tokenizer now; expose `SqlAnalysisProvider` extension for future parser-backed correction. |
| Editor-owned spans vs renderer-only styling      | Editor-owned spans keep tests independent from Lanterna; renderer-only styling couples logic to terminal APIs | `SqlEditorComponent` exposes `render(int,int)` returning text lines plus spans.                |
| Snapshot catalog vs live per-keystroke DB lookup | Snapshot is fast/testable; live lookup is fresher but risky in TUI input loop                                 | Use immutable `MetadataCatalogSnapshot`; later refresh can rebuild it from provider queries.   |
| Tab autocomplete vs Ctrl+Space                   | Tab is easy to test with Lanterna `KeyType.Tab`; Ctrl+Space portability is terminal-dependent                 | Route Tab to autocomplete trigger first; keep action extensible for Ctrl+Space later.          |

## Data Flow

```text
KeyStroke ─→ TuiEventRouter ─→ WorkspaceAction
                              └─ autocomplete trigger

BasicSqlEditorComponent ─→ SqlTokenizer ─→ SqlRenderModel
          │                         │
          └→ CompletionProvider ← MetadataCatalogSnapshot

WorkspaceViewModel ─→ RenderedWorkspace.editorRows ─→ Lanterna style mapper
```

## File Changes

| File                                                                       | Action | Description                                                                                                                                                          |
| -------------------------------------------------------------------------- | ------ | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `src/main/java/com/example/oraclescript/SqlTokenType.java`                 | Create | Token categories: keyword, identifier, literal, comment, punctuation, whitespace, unknown.                                                                           |
| `src/main/java/com/example/oraclescript/SqlToken.java`                     | Create | Immutable token with type, text, start offset, end offset.                                                                                                           |
| `src/main/java/com/example/oraclescript/SqlTokenizer.java`                 | Create | Lightweight scanner for SQL keywords, identifiers, quoted strings, comments, numbers, punctuation.                                                                   |
| `src/main/java/com/example/oraclescript/SqlStyle.java`                     | Create | Renderer-neutral style enum mapped to Lanterna colors only in `LanternaScreenDriver`.                                                                                |
| `src/main/java/com/example/oraclescript/SqlRenderSpan.java`                | Create | Line-relative span with start column, length, and `SqlStyle`.                                                                                                        |
| `src/main/java/com/example/oraclescript/SqlRenderLine.java`                | Create | Plain line text plus immutable spans.                                                                                                                                |
| `src/main/java/com/example/oraclescript/SqlRenderModel.java`               | Create | Editor render result: visible lines, cursor offset, completion popup data.                                                                                           |
| `src/main/java/com/example/oraclescript/CompletionCandidate.java`          | Create | Suggestion value, display label, kind, replacement range.                                                                                                            |
| `src/main/java/com/example/oraclescript/CompletionProvider.java`           | Create | Interface for prefix-based suggestions.                                                                                                                              |
| `src/main/java/com/example/oraclescript/SqlKeywordCompletionProvider.java` | Create | Static SQL keyword suggestions.                                                                                                                                      |
| `src/main/java/com/example/oraclescript/MetadataCatalogSnapshot.java`      | Create | Immutable schemas/tables snapshot for table suggestions.                                                                                                             |
| `src/main/java/com/example/oraclescript/CatalogCompletionProvider.java`    | Create | Table-name suggestions from snapshots.                                                                                                                               |
| `src/main/java/com/example/oraclescript/CompositeCompletionProvider.java`  | Create | Combines keyword and catalog providers with deterministic ordering.                                                                                                  |
| `src/main/java/com/example/oraclescript/MetadataCatalogLoader.java`        | Create | Interface for future JDBC-backed snapshot refresh; tests use in-memory snapshots.                                                                                    |
| `src/main/java/com/example/oraclescript/SqlEditorComponent.java`           | Modify | Replace/augment `decorations()` with typed `renderModel(width,height)` and `completionCandidates()`. Keep `renderLines` for fallback compatibility during migration. |
| `src/main/java/com/example/oraclescript/BasicSqlEditorComponent.java`      | Modify | Inject completion provider/catalog snapshot; produce spans and apply selected completion.                                                                            |
| `src/main/java/com/example/oraclescript/WorkspaceAction.java`              | Modify | Add `TRIGGER_AUTOCOMPLETE` and optional `ACCEPT_COMPLETION` if popup navigation lands in this slice.                                                                 |
| `src/main/java/com/example/oraclescript/TuiEventRouter.java`               | Modify | Map `KeyType.Tab` to autocomplete while editor-focused handling remains in `RawTerminalWorkspace`.                                                                   |
| `src/main/java/com/example/oraclescript/WorkspaceViewModel.java`           | Modify | Carry `SqlRenderModel`/`SqlRenderLine` while preserving plain `editorLines()` flattening for tests and compact fallback.                                             |
| `src/main/java/com/example/oraclescript/RawTerminalWorkspace.java`         | Modify | Render spans by applying `SqlStyle` to Lanterna `TextGraphics`; render completion popup as simple rows below editor.                                                 |
| `src/main/java/com/example/oraclescript/HelpStatusComponent.java`          | Modify | Document Tab autocomplete.                                                                                                                                           |
| `src/main/java/com/example/oraclescript/MetadataProvider*.java`            | Modify | Add metadata query helpers only if needed to feed `MetadataCatalogLoader`; do not execute live queries during typing.                                                |

## Interfaces / Contracts

```java
interface CompletionProvider {
  List<CompletionCandidate> suggest(String text, int cursorOffset, MetadataCatalogSnapshot catalog);
}

interface MetadataCatalogLoader {
  MetadataCatalogSnapshot load(ConnectionConfig connectionConfig) throws SQLException;
}

```

## Testing Strategy

| Layer           | What to Test                        | Approach                                                                                    |
| --------------- | ----------------------------------- | ------------------------------------------------------------------------------------------- |
| Unit            | Token boundaries and token types    | `SqlTokenizerTest` with keywords, strings, comments, punctuation.                           |
| Unit            | Render spans and plain fallback     | Extend `BasicSqlEditorComponentTest`; assert spans independent of Lanterna.                 |
| Unit            | Keyword/table suggestions           | `CompletionProviderTest` using `MetadataCatalogSnapshot.ofTables(...)`.                     |
| Unit            | Catalog immutability and filtering  | `MetadataCatalogSnapshotTest`, no live DB.                                                  |
| Integration-ish | Key routing and view model contract | Extend `TuiEventRouterTest` and `WorkspaceViewModelTest`; assert Tab action and popup rows. |
| Renderer        | Lanterna style mapping              | Unit-test mapper function, not terminal drawing.                                            |

## Migration / Rollout

No data migration required. Keep plain text rendering as fallback until all call sites consume `SqlRenderModel`.

## Open Questions

- [ ] Whether first implementation should accept the top completion immediately on Tab or show suggestions first and require Enter.
