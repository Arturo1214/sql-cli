# Tasks: SQL Editor Intelligence

## Review Workload Forecast

| Field                   | Value                                                                                |
| ----------------------- | ------------------------------------------------------------------------------------ |
| Estimated changed lines | 900-1300                                                                             |
| 400-line budget risk    | High                                                                                 |
| Chained PRs recommended | Yes                                                                                  |
| Suggested split         | PR 1 tokenizer/render model → PR 2 catalog/completion → PR 3 TUI integration/package |
| Delivery strategy       | ask-on-risk with user-approved automatic continuation                                |
| Chain strategy          | stacked-to-main                                                                      |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal                                                       | Likely PR | Notes                                          |
| ---- | ---------------------------------------------------------- | --------- | ---------------------------------------------- |
| 1    | Tokenizer, styles, render spans, editor render model       | PR 1      | Base main; pure Java tests included.           |
| 2    | Catalog snapshot and keyword/table completion providers    | PR 2      | Base PR 1 branch; no live DB typing path.      |
| 3    | Lanterna mapping, Tab/event integration, help/docs/package | PR 3      | Base PR 2 branch; TUI contract tests included. |

## Phase 1: Tokenizer and Render Model TDD

- [x] 1.1 RED: Add `oracle-script-cli/src/test/java/com/example/oraclescript/SqlTokenizerTest.java` for keywords, identifiers, literals, comments, punctuation, whitespace, unknown text.
- [x] 1.2 GREEN: Create `SqlTokenType`, `SqlToken`, and `SqlTokenizer` in `oracle-script-cli/src/main/java/com/example/oraclescript/`.
- [x] 1.3 RED: Extend `BasicSqlEditorComponentTest.java` for highlighted spans and readable plain fallback.
- [x] 1.4 GREEN: Create `SqlStyle`, `SqlRenderSpan`, `SqlRenderLine`, `SqlRenderModel`; update `SqlEditorComponent` and `BasicSqlEditorComponent` render APIs.

## Phase 2: Catalog and Completion TDD

- [x] 2.1 RED: Add `MetadataCatalogSnapshotTest.java` for immutable table snapshots, filtering, unavailable metadata, and partial metadata.
- [x] 2.2 GREEN: Create `MetadataCatalogSnapshot` and `MetadataCatalogLoader`; adapt `SchemaDiscoveryService`/`MetadataProvider` only for snapshot loading.
- [x] 2.3 RED: Add `CompletionProviderTest.java` for keyword suggestions, table suggestions, deterministic ordering, no column/function suggestions, and no SQL rewrite.
- [x] 2.4 GREEN: Create `CompletionCandidate`, `CompletionProvider`, `SqlKeywordCompletionProvider`, `CatalogCompletionProvider`, and `CompositeCompletionProvider`.

## Phase 3: Editor Integration TDD

- [x] 3.1 RED: Extend `BasicSqlEditorComponentTest.java` for prefix replacement range and requested autocomplete candidates.
- [x] 3.2 GREEN: Inject completion provider/catalog into `BasicSqlEditorComponent`; expose candidates without parser correction or live DB lookup.
- [x] 3.3 RED: Extend `WorkspaceViewModelTest.java` for carrying `SqlRenderModel` while preserving `editorLines()` fallback.
- [x] 3.4 GREEN: Update `WorkspaceViewModel.java` to include styled editor rows and completion popup data.

## Phase 4: TUI Wiring, Docs, Verification

- [x] 4.1 RED: Extend `TuiEventRouterTest.java` for `KeyType.Tab` mapping to `TRIGGER_AUTOCOMPLETE`.
- [x] 4.2 GREEN: Update `WorkspaceAction`, `TuiEventRouter`, `RawTerminalWorkspace`, and `HelpStatusComponent` for Tab autocomplete and popup rendering.
- [x] 4.3 RED: Add renderer mapping tests for `SqlStyle` to Lanterna colors without terminal drawing.
- [x] 4.4 GREEN: Implement Lanterna style mapping in `RawTerminalWorkspace` or extracted mapper; keep plain rendering fallback.
- [x] 4.5 Update `oracle-script-cli/README.md`, `OracleScriptCliDocumentationTest.java`, and verify `mvn test` plus `mvn package`.
