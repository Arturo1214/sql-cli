# Design: Terminal Raw TUI Navigation

## Technical Approach

Introduce a Lanterna `Screen` shell for the interactive workspace while keeping the existing command parser and one-shot CLI paths intact. `OracleScriptCli` will still route explicit commands (`exec`, `run-current`, metadata, `connections`) through the current services; only no-args and `workspace` with no extra options enter the raw TUI. Database behavior remains delegated to `ConnectionRegistry`, `MetadataProviderFactory`, `SqlScriptRunner`, `SafetyGuard`, `SqlStatementSelector`, `EditorStateStore`, and `SqlConsoleRenderer`.

## Architecture Decisions

| Decision          | Choice                                                                                   | Alternatives considered                 | Rationale                                                                                                                   |
| ----------------- | ---------------------------------------------------------------------------------------- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| Terminal library  | Add `com.googlecode.lanterna:lanterna:3.1.2` to `pom.xml`                                | Keep ANSI renderer; use JLine           | Lanterna 3.1.x targets Java 8 and supports full-screen screen/key events; ANSI/JLine cannot model panels and focus cleanly. |
| Shell boundary    | Create `RawTerminalWorkspace` and keep `InteractiveWorkspace` as fallback/session facade | Rewrite `InteractiveWorkspace` entirely | Preserves existing REPL tests and non-TTY behavior while isolating terminal lifecycle risk.                                 |
| State transitions | Pure reducer classes for keys/focus/editor state                                         | Put all behavior in Lanterna callbacks  | Unit-testable reducers protect shortcut behavior without needing a real terminal.                                           |
| SQL editor        | Define `SqlEditorComponent` interface with a basic text-buffer implementation            | Directly embed Lanterna `TextBox`       | Keeps extension points for highlighting/autocomplete/correction without coupling future features to Lanterna widgets.       |

## Data Flow

```text
OracleScriptCli(no args/workspace) -> WorkspaceLauncher
    -> RawTerminalWorkspace -> TuiEventRouter -> WorkspaceStateReducer
    -> components render with Lanterna Screen
    -> InteractiveWorkspace.Session services -> DB/metadata/safety/rendering
```

`Ctrl+Left`/`Ctrl+J` focuses the connection/actions panel. `Ctrl+Right`/`Ctrl+L` focuses the SQL editor. Arrows and `j`/`k` move selection in the active panel. `Ctrl+R` or double Enter executes the current editor statement. `Ctrl+H` toggles help. EOF/Escape exits cleanly.

## File Changes

| File                                                                     | Action | Description                                                                                |
| ------------------------------------------------------------------------ | ------ | ------------------------------------------------------------------------------------------ |
| `pom.xml`                                                                | Modify | Add Lanterna dependency; keep Maven Shade transformers.                                    |
| `src/main/java/com/example/oraclescript/OracleScriptCli.java`            | Modify | Route interactive entry through `WorkspaceLauncher`; preserve one-shot commands.           |
| `src/main/java/com/example/oraclescript/WorkspaceLauncher.java`          | Create | Chooses raw TUI when terminal is supported, otherwise uses existing REPL/compact fallback. |
| `src/main/java/com/example/oraclescript/RawTerminalWorkspace.java`       | Create | Owns Lanterna terminal/screen lifecycle, resize handling, event loop, and cleanup.         |
| `src/main/java/com/example/oraclescript/TuiEventRouter.java`             | Create | Maps Lanterna `KeyStroke` values to domain actions.                                        |
| `src/main/java/com/example/oraclescript/WorkspaceFocus.java`             | Create | Enum/model for `CONNECTIONS`, `EDITOR`, `RESULTS`, `HELP`.                                 |
| `src/main/java/com/example/oraclescript/WorkspaceStateReducer.java`      | Create | Applies navigation, selection, help, execute, and status transitions.                      |
| `src/main/java/com/example/oraclescript/SqlEditorComponent.java`         | Create | Editor abstraction: text, cursor offset, insert/delete/move, render lines.                 |
| `src/main/java/com/example/oraclescript/BasicSqlEditorComponent.java`    | Create | Initial Java 8 text-buffer editor; delegates current-statement selection by cursor.        |
| `src/main/java/com/example/oraclescript/ConnectionListComponent.java`    | Create | Renders saved connections/actions and exposes selected connection/action.                  |
| `src/main/java/com/example/oraclescript/ResultsPanelComponent.java`      | Create | Displays `SqlConsoleRenderer` output and errors.                                           |
| `src/main/java/com/example/oraclescript/HelpStatusComponent.java`        | Create | Renders shortcut help overlay and status bar.                                              |
| `src/main/java/com/example/oraclescript/InteractiveWorkspace.java`       | Modify | Reuse `Session` execution methods from raw TUI; retain REPL fallback.                      |
| `src/main/java/com/example/oraclescript/WorkspaceDashboardRenderer.java` | Modify | Keep compact/fallback renderer; no raw screen responsibility.                              |
| `src/test/java/com/example/oraclescript/*Tui*Test.java`                  | Create | Reducer/router/editor/component tests.                                                     |
| `src/test/java/com/example/oraclescript/OracleScriptCliSmokeTest.java`   | Modify | Update dependency assertion and add launcher/fallback smoke coverage.                      |

## Interfaces / Contracts

```java
interface SqlEditorComponent {
  String text();
  int cursorOffset();
  void handle(EditorAction action);
  List<String> renderLines(int width, int height);
}

```

`TuiEventRouter` returns domain actions, never mutates state. Execution actions call `Session.runCurrentBuffer(...)` after syncing editor text/cursor.

## Testing Strategy

| Layer       | What to Test                                                                             | Approach                                          |
| ----------- | ---------------------------------------------------------------------------------------- | ------------------------------------------------- |
| Unit        | Key mapping, focus transitions, selection, double Enter, editor cursor/current statement | JUnit 4 pure reducer/router tests.                |
| Unit        | Component rendering clips and marks focus/selection                                      | String/line assertions without terminal IO.       |
| Integration | Raw launcher falls back when no console/dumb terminal; script commands bypass TUI        | Existing process smoke tests with stdin.          |
| Smoke       | Lanterna dependency loads in shaded/runtime classpath                                    | `Class.forName` and package smoke where feasible. |

## Migration / Rollout

No data migration required. Roll out behind the interactive launcher path only; keep REPL fallback and `run-current` as rollback-safe paths.

## Open Questions

- [ ] Exact typed-confirmation UX for destructive SQL in raw mode: modal prompt vs status-bar command input.
- [ ] Whether connection creation in the first slice uses a simple modal form or delegates to existing text command fallback.

## Risks

- Terminal key combinations may be swallowed by OS/terminal; keep aliases and command fallback.
- Lanterna resize/private-screen cleanup can leave terminals dirty; `RawTerminalWorkspace` must close in `finally`.
- Current tests assert no heavy TUI dependencies; they must be intentionally updated for the accepted Lanterna direction.
