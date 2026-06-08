# Tasks: Terminal Raw TUI Navigation

## Review Workload Forecast

| Field                   | Value                                                                       |
| ----------------------- | --------------------------------------------------------------------------- |
| Estimated changed lines | 900-1300                                                                    |
| 400-line budget risk    | High                                                                        |
| Chained PRs recommended | Yes                                                                         |
| Suggested split         | PR 1 foundation/tests → PR 2 panels/execution → PR 3 launcher/smoke/package |
| Delivery strategy       | ask-on-risk with automatic continuation                                     |
| Chain strategy          | stacked-to-main                                                             |

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal                                                        | Likely PR | Notes                                               |
| ---- | ----------------------------------------------------------- | --------- | --------------------------------------------------- |
| 1    | Lanterna dependency, launcher boundary, reducer/editor core | PR 1      | Base main; pure tests included.                     |
| 2    | Connection, result, help panels plus execution wiring       | PR 2      | Base PR 1 branch; component/session tests included. |
| 3    | CLI routing, fallback, smoke, package verification          | PR 3      | Base PR 2 branch; smoke/build checks included.      |

## Phase 1: Foundation TDD

- [x] 1.1 RED: Add `oracle-script-cli/src/test/java/com/example/oraclescript/TuiEventRouterTest.java` for Ctrl focus, Ctrl+R, Ctrl+H, double Enter, Escape/Ctrl+C.
- [x] 1.2 GREEN: Add Lanterna `3.1.2` to `oracle-script-cli/pom.xml`; create `TuiEventRouter`, `WorkspaceAction`, and Java 8 key mapping.
- [x] 1.3 RED: Add `WorkspaceStateReducerTest.java` for focus order, refresh preserving state, help toggle, compact/full resize state.
- [x] 1.4 GREEN: Create `WorkspaceFocus` and `WorkspaceStateReducer` with no terminal side effects.

## Phase 2: Editor and Panels TDD

- [x] 2.1 RED: Add `BasicSqlEditorComponentTest.java` for buffer persistence, cursor movement, deletion, current-statement selection, empty execution rejection.
- [x] 2.2 GREEN: Create `SqlEditorComponent` and `BasicSqlEditorComponent`; include empty decoration/extension hooks only, no highlighting/autocomplete/correction.
- [x] 2.3 RED: Add `ConnectionListComponentTest.java` for selection, Oracle schema ignored, PostgreSQL `public` default action, metadata action labels.
- [x] 2.4 GREEN: Create `ConnectionListComponent`; expose selected connection/action without duplicating registry logic.
- [x] 2.5 RED: Add `ResultsPanelComponentTest.java` and `HelpStatusComponentTest.java` for result/error clipping, focus markers, help overlay shortcuts.
- [x] 2.6 GREEN: Create `ResultsPanelComponent` and `HelpStatusComponent` using existing `SqlConsoleRenderer` strings.

## Phase 3: Shell and Integration TDD

- [x] 3.1 RED: Extend `InteractiveWorkspaceTest.java` for cursor-aware `Session.runCurrentBuffer(text, cursorOffset)` and preserved history/result status.
- [x] 3.2 GREEN: Modify `InteractiveWorkspace.java` `Session` APIs for raw TUI execution, connection switching, create-connection actions, and REPL fallback preservation.
- [x] 3.3 RED: Add `WorkspaceLauncherTest.java` for supported terminal launching raw mode and unsupported/dumb terminal using compact REPL fallback.
- [x] 3.4 GREEN: Create `WorkspaceLauncher` and `RawTerminalWorkspace` with screen lifecycle, resize handling, render loop, and `finally` cleanup.

## Phase 4: Routing, Smoke, Build

- [x] 4.1 RED: Update `OracleScriptCliSmokeTest.java` for no-args/workspace TUI entry, explicit commands bypassing TUI, and Lanterna `Class.forName` dependency smoke.
- [x] 4.2 GREEN: Modify `OracleScriptCli.java` to route no-args and bare `workspace` through `WorkspaceLauncher`; keep `run-current`, `exec`, metadata, script flows unchanged.
- [x] 4.3 Update/delete `WorkspaceDashboardRendererTest.java` so `WorkspaceDashboardRenderer` covers compact fallback only.
- [x] 4.4 Verify `mvn test` and `mvn package`; confirm shaded JAR starts with Java 8 target.
