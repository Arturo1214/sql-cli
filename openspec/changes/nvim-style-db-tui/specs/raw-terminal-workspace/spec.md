# Delta for Raw Terminal Workspace

## ADDED Requirements

### Requirement: Split the terminal into focused panes

The system MUST present a Lanterna-backed full-screen workspace with a left explorer/menu pane, a right SQL editor pane, a bottom results/logs pane, and a status/help area.

#### Scenario: Render the workspace layout

- GIVEN raw terminal mode starts
- WHEN the workspace draws
- THEN the core panes are visible
- AND the status/help area shows current hints

#### Scenario: Stay usable on constrained terminals

- GIVEN the terminal is smaller than the ideal layout
- WHEN the workspace starts
- THEN the UI remains usable with a safe fallback layout or fallback mode

### Requirement: Move focus between real widgets

The system MUST move focus between the explorer/menu and the SQL editor using real widget focus, and SHOULD make the active pane clear to the user.

#### Scenario: Focus the SQL editor

- GIVEN the explorer is focused
- WHEN the user navigates to the editor
- THEN typing affects the editor only

#### Scenario: Focus the explorer/menu

- GIVEN the editor is focused
- WHEN the user navigates to the explorer/menu
- THEN selection keys act on the left pane only

### Requirement: Interpret Enter by active context

The system MUST interpret Enter according to the focused pane or selected action.

#### Scenario: Enter activates a connection

- GIVEN a connection is highlighted in the explorer/menu
- WHEN the user presses Enter
- THEN the connection becomes active

#### Scenario: Enter runs a highlighted action

- GIVEN an action item is highlighted
- WHEN the user presses Enter
- THEN the action executes instead of inserting text

### Requirement: Create connections from the explorer

The system MUST provide a connection creation dialog reachable from the explorer/menu.

#### Scenario: Save a valid connection

- GIVEN the connection dialog is open
- WHEN required fields are completed and confirmed
- THEN the connection is saved and listed in the explorer/menu

#### Scenario: Cancel invalid connection entry

- GIVEN the connection dialog is open
- WHEN the user cancels or submits incomplete data
- THEN nothing is saved and validation feedback remains visible

### Requirement: Preserve script-friendly commands

The system MUST preserve existing script-oriented commands and one-shot execution behavior when the TUI is introduced.

#### Scenario: Run a one-shot command

- GIVEN the user invokes an existing script command
- WHEN the command runs
- THEN it behaves as before without requiring the TUI shell

#### Scenario: Use fallback when raw mode is unsupported

- GIVEN raw terminal mode is unsupported
- WHEN interactive mode starts
- THEN a safe non-Lanterna fallback remains available

### Requirement: Defer SQL intelligence features

The system MUST NOT add syntax highlighting, autocomplete, or parser-based correction in this change.

#### Scenario: Keep the editor plain

- GIVEN the user types SQL into the editor
- WHEN the buffer changes
- THEN the text remains plain and uncorrected

#### Scenario: Leave invalid SQL untouched

- GIVEN the current SQL is incomplete or invalid
- WHEN the user navigates or executes workspace actions
- THEN the system does not auto-correct the SQL
