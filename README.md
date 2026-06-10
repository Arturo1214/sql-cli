# Oracle Script CLI

Terminal-first SQL script CLI/TUI for inspecting Oracle and PostgreSQL databases from a server shell. It is built as a Java/Maven module, packaged as a runnable JAR, and designed for keyboard-heavy QA, support, and contributor workflows.

## Quick Start

Requirements:

- Java 8 or newer runtime.
- Maven 3.x.
- Live Oracle or PostgreSQL credentials for real integration use.

Build and run:

```bash
mvn test
mvn package
java -jar target/oracle-script-cli.jar
```

The shaded executable JAR is generated at `target/oracle-script-cli.jar` with main class `bo.ahosoft.sqlscript.cli.OracleScriptCli`.

## What You Get

| Area                  | Capability                                                                                  |
| --------------------- | ------------------------------------------------------------------------------------------- |
| Interactive workspace | Full-terminal TUI named `Database Script Workspace`.                                        |
| Databases             | Oracle and PostgreSQL through JDBC.                                                         |
| Connections           | Saved connections with environment labels, active selection, and Oracle/PostgreSQL wizards. |
| SQL editor            | Editable SQL buffer with non-destructive diagnostics and suggestions.                       |
| Metadata              | Database-neutral commands: `tables`, `describe <table>`, `indexes <table>`.                 |
| Query library         | Local named SQL snippets with list/search/load/delete/favorite workflows.                   |
| Results               | Row numbers, horizontal/vertical scrolling, DB-level pagination for SELECT-like queries.    |
| i18n                  | English/Spanish in-session UI switching; command keywords remain English.                   |
| License               | MIT.                                                                                        |

## Interactive Workspace

Run the packaged CLI with no arguments to open the TUI:

```bash
java -jar target/oracle-script-cli.jar
```

`workspace` is an explicit alias for the same full-terminal workspace:

```bash
java -jar target/oracle-script-cli.jar workspace
```

The workspace is split into:

- Left menu: saved Oracle/PostgreSQL connections, connection creation actions, and language switching.
- SQL editor: the editable SQL buffer.
- Results/logs: query output, metadata output, diagnostics, or errors.
- Status/help: active hints and keyboard guidance.

When raw terminal mode is unavailable, `TERM=dumb`/`NO_COLOR` is active, output is captured, or the terminal is too small, the CLI uses a compact fallback that avoids cursor-control escape sequences.

The compact fallback accepts text commands such as `help`, `connections`, `use <name>`, `buffer set <sql>`, `run`, metadata commands, and `exit`.

## Keyboard Shortcuts

| Shortcut                              | Action                                                                                       |
| ------------------------------------- | -------------------------------------------------------------------------------------------- |
| `F1` or `?`                           | Open help. `?` is handled outside the SQL editor so it remains usable while typing SQL.      |
| `F2` or `Ctrl+B`                      | Focus the left connections/menu pane.                                                        |
| `F3` or `Ctrl+E`                      | Focus the SQL editor.                                                                        |
| `Tab` / `Shift+Tab`                   | Cycle focus through menu, editor, results, and status areas.                                 |
| `F5` or `Ctrl+R`                      | Execute the current SQL buffer or metadata command.                                          |
| `F6`                                  | Load a `.sql` file from a typed server filesystem path into the editor.                      |
| `F9`                                  | Save the current SQL editor text to the local query library.                                 |
| `F10`                                 | Open/search/load the local query library.                                                    |
| `F7`                                  | Export the current visible result page as CSV.                                               |
| `F8`                                  | Export all result pages as CSV after confirmation.                                           |
| `Enter` in the menu                   | Select the active connection/action; selecting a saved connection moves focus to the editor. |
| `ArrowUp` / `ArrowDown` in results    | Scroll results vertically.                                                                   |
| `ArrowLeft` / `ArrowRight` in results | Scroll results horizontally.                                                                 |
| `PageDown` / `PageUp`                 | Move to the next/previous result page.                                                       |
| `Esc`                                 | Close/exit the current workspace view.                                                       |

`Ctrl+H` may be unreliable in some terminals because it is often interpreted as Backspace. Use `F1` or `?` for dependable help access.

## Support Workflow Toolkit

Support workflows are terminal-only and SSH-friendly. Paths are typed as server filesystem paths, so they work the same in a local shell and over SSH. No desktop file picker, browser download, GUI-native clipboard flow, or desktop-only interaction is required.

- `F6` prompts for a `.sql` file path and loads it into the SQL editor. If the editor is dirty, confirm before replacing the current buffer.
- `F9` prompts for query name, description, comma-separated tags, and favorite flag, then saves the current editor SQL.
- `F10` opens a keyboard-only Query library dialog for list/search/load/delete/favorite workflows.
- `F7` prompts for an export path and writes the current visible result page as CSV, preserving headers and column order.
- `F8` prompts for an export path and asks for confirmation before exporting all paginated result pages as CSV.
- Existing export targets require overwrite confirmation. Missing parent directories, directory targets, and permission errors are reported without treating partial output as success.

### Query library

The Query library stores named SQL snippets locally at `~/.oracle-script-cli/query-library.properties`. Entries include name, SQL text, description, tags, timestamps, favorite status, and the active environment/connection labels when saved.

Terminal fallback commands use English keywords in every UI language:

```text
lib save <name> [--desc <text>] [--tags support,qa] [--favorite] [--overwrite]
lib save <name> --template [--desc <text>] [--tags support,qa] [--favorite] [--overwrite]
lib list
lib search <text>
lib load <id> --replace
lib preview <id> --param name=value
lib fill <id> --replace --param name=value
lib delete <id> --yes
lib favorite <id>
lib unfavorite <id>
```

Saved SQL may contain sensitive data. Review query text before storing shared or regulated information. The library file is local text storage and applies user-only permissions where the operating system supports them.

Loading a query never executes it. It only replaces the editor/buffer after dirty-buffer confirmation; any later execution still goes through SafetyGuard and the active environment confirmation rules.

### Dangerous SQL confirmation

The TUI keeps SafetyGuard enabled by default. When you explicitly run dangerous SQL from the editor with `F5` or `Ctrl+R` (`UPDATE`, `DELETE`, `DROP`, `TRUNCATE`, `ALTER`, and other SafetyGuard-blocked statements), the workspace opens a keyboard-only confirmation modal instead of executing immediately.

- UPDATE and DELETE require a top-level WHERE clause before any confirmation can appear. A WHERE inside a comment, string, parenthesized expression, or subquery does not satisfy this guard.
- Non-PROD connections require typing `RUN`, then activating `Run anyway`. The confirmation applies to the current statement once only.
- PROD connections require you to type the active connection name exactly, then activate `Run anyway`. A mismatch or cancel does not execute anything.
- DROP, TRUNCATE, and ALTER keep the existing confirmation behavior; they do not require a WHERE clause.
- Loading files, saved queries, favorites, or rendered templates never auto-executes SQL. Loaded text must still be run manually and still goes through this confirmation flow.
- `--unsafe` and `--confirm-risk` are CLI mode only. Use no arguments or `workspace` to open the TUI; `java -jar target/oracle-script-cli.jar --unsafe` remains a CLI invocation and does not silently launch the workspace.

#### Parameterized query templates

Parameterized query templates are saved query-library entries whose SQL contains `{{name}}` placeholders. Names must start with a letter or underscore and may contain letters, numbers, or underscores. Duplicate placeholders are prompted once and reused for every occurrence.

Example template:

```sql
select *
from customers
where customer_id = {{customer_id}}
  and status = {{status}}
```

Terminal fallback commands:

```text
lib save Customer Template --template --tags support
lib preview <id> --param customer_id=42 --param status='ACTIVE'
lib fill <id> --replace --param customer_id=42 --param status='ACTIVE'
```

In the full TUI, use `F9` and `Save Template` to store the current editor SQL as a template. Use `F10`, choose a template id, and `Fill Template` to open a keyboard-only modal with one field per unique placeholder. `Preview` renders the SQL in Results/Logs; `Load` replaces the editor after the same dirty-buffer confirmation used by normal query loading.

Raw substitution warning: template values are inserted as text. Quote and escape values in the template or input before running. Rendered templates load into the editor only and never auto-execute; manual execution still goes through SafetyGuard.

## Connection Management

The left menu lists saved connections and includes:

- `New Oracle connection`.
- `New PostgreSQL connection`.
- `Language: English` / `Idioma: Espanol` action for in-session UI switching.

The connection wizard prompts for name, JDBC URL, username, password, and schemas, and uses a constrained environment selector. Required fields are validated before save. PostgreSQL can use schema input; Oracle ignores schema input.

Environment values are stable labels: `DEV`, `QA`, `STAGING`, and `PROD`. Legacy configs without an environment load as `DEV` so existing profiles keep working; newly saved configs persist `environment=DEV` unless another value is selected.

The TUI shows environment in the connection list and status/footer, for example `[PROD] billing-db`. Production connections are text-marked as `!! PROD !! [PROD] ...` so the warning remains visible even without terminal styling.

Registered connections are stored under `~/.oracle-script-cli/connections/`. Passwords in registered connection files are protected by `ProtectedSecretStore` with local Java encryption key material under `~/.oracle-script-cli/connections/secrets`; connection listings never print passwords.

List registered connections:

```bash
java -jar target/oracle-script-cli.jar connections list
```

Legacy profile-style configuration is also supported:

```bash
java -jar target/oracle-script-cli.jar init \
  'jdbc:oracle:thin:@//localhost:1521/XEPDB1' \
  'system' \
  'oracle' \
  --type oracle \
  --environment dev

java -jar target/oracle-script-cli.jar init \
  'jdbc:postgresql://localhost:5432/appdb' \
  'app_user' \
  'secret' \
  --type postgresql \
  --environment qa \
  --schemas app,audit \
  --profile pg-qa

java -jar target/oracle-script-cli.jar profiles
java -jar target/oracle-script-cli.jar validate --profile pg-qa
```

Default and named profiles are stored in `~/.oracle-script-cli.properties` or `~/.oracle-script-cli-<profile>.properties`. Legacy profiles without a stored database `type` are treated as Oracle, and profiles without `environment` are treated as `DEV`.

## SQL Editor And Metadata

The SQL editor keeps user text under user control. Diagnostics and suggestions are non-destructive: they report possible issues but do not rewrite SQL automatically.

The project includes a SQL render/highlight model for keywords, strings, numbers, comments, and operators. Current limitation: the Lanterna `TextBox` editor is still plain text, so true colored editing is not directly rendered inside the editable field yet.

Metadata commands are database-neutral and keep English command keywords in both UI languages:

```text
tables
tables customer
describe customers
indexes customers
```

The shorter legacy alias `desc <table>` is also available for command-line metadata use.

## Results And Pagination

SELECT-like queries (`SELECT` and `WITH`) use database-level pagination with a maximum of 100 rows per page.

| Database   | Pagination strategy                                         |
| ---------- | ----------------------------------------------------------- |
| PostgreSQL | Wraps the query with `LIMIT` / `OFFSET`.                    |
| Oracle     | Wraps the query with `OFFSET ... FETCH NEXT ... ROWS ONLY`. |

The CLI also runs a `COUNT` wrapper query to calculate total pages. If the count fails, it falls back without doing a full result fetch; page count is shown as unknown and navigation continues while pages return rows.

Result pages are cached for about 5 minutes. Re-running a query creates a fresh result and clears the previous page cache. Results include a row number column and support horizontal/vertical scrolling in the TUI.

Use deterministic `ORDER BY` clauses for stable pagination. Without a stable order, the database may return rows in different orders between pages.

## Command-Line Usage

Execute SQL with a saved profile:

```bash
java -jar target/oracle-script-cli.jar exec 'select * from dual'
java -jar target/oracle-script-cli.jar exec @script.sql --profile qa
java -jar target/oracle-script-cli.jar export 'select * from clientes where rownum <= 100' clientes.csv
java -jar target/oracle-script-cli.jar exec @consulta.sql --csv salida.csv
```

Run metadata commands:

```bash
java -jar target/oracle-script-cli.jar search CLIENTE
java -jar target/oracle-script-cli.jar tables [filter]
java -jar target/oracle-script-cli.jar sample CLIENTES [limit]
java -jar target/oracle-script-cli.jar desc CLIENTES
java -jar target/oracle-script-cli.jar detail CLIENTES
java -jar target/oracle-script-cli.jar indexes CLIENTES
java -jar target/oracle-script-cli.jar constraints CLIENTES
java -jar target/oracle-script-cli.jar fk-out CLIENTES
java -jar target/oracle-script-cli.jar fk-in CLIENTES
java -jar target/oracle-script-cli.jar count CLIENTES
java -jar target/oracle-script-cli.jar explain 'select * from clientes where id = 1'
```

Choose a saved Oracle or PostgreSQL profile:

```bash
java -jar target/oracle-script-cli.jar tables --profile pg-qa
java -jar target/oracle-script-cli.jar desc clientes --profile oracle-qa
```

Use direct JDBC credentials without saved configuration:

```bash
java -jar target/oracle-script-cli.jar \
  'jdbc:oracle:thin:@//localhost:1521/XEPDB1' \
  'system' \
  'oracle' \
  'select * from dual'
```

Use `run-current` for script-friendly one-shot current-statement execution:

```bash
java -jar target/oracle-script-cli.jar run-current @work.sql --cursor 42 --profile qa
java -jar target/oracle-script-cli.jar run-current --buffer 'select * from dual; select sysdate from dual' --cursor 5 --dry-run
```

Statement selection rules:

- Semicolons delimit statements.
- A final semicolon ends the current statement.
- Later statements in the buffer are ignored.
- Unfinished trailing text after the selected statement is not executed.

## Safety Mode

Safety mode blocks dangerous SQL by default before execution. This applies to CLI execution and SQL entered in the TUI editor.

Blocked statements include `insert`, `update`, `delete`, `merge`, `drop`, `truncate`, `alter`, `create`, `grant`, and `revoke`. `select` and metadata commands such as `tables`, `describe <table>`, and `indexes <table>` continue normally.

For non-PROD CLI use, bypass requires explicit intent with `--unsafe`:

```bash
java -jar target/oracle-script-cli.jar run-current @update.sql --unsafe --profile qa
```

For PROD CLI use, bypass also requires typing the active profile/config name:

```bash
java -jar target/oracle-script-cli.jar run-current @update.sql --unsafe --confirm-risk prod --profile prod
```

The older `--force --confirm-risk YES` path is still accepted for non-PROD compatibility, but new automation should use `--unsafe`. The TUI currently blocks dangerous editor SQL and shows a localized Results/Logs safety message instead of opening a confirmation modal.

## Limitations

- Non-SELECT behavior is preserved. Multi-statement or non-SELECT execution does not use the SELECT pagination path.
- Real Oracle/PostgreSQL integration validation requires live database credentials and reachable database instances.
- Use deterministic `ORDER BY` clauses for stable paginated results.
- `Ctrl+H` may be terminal-unreliable; prefer `F1` or `?` for help.

Command history is stored in `~/.oracle-script-cli.history` and does not include passwords.

## Architecture For Contributors

The Java package root is `bo.ahosoft.sqlscript`:

| Package  | Responsibility                                                                              |
| -------- | ------------------------------------------------------------------------------------------- |
| `cli`    | Main entry point, command parsing, console rendering, and safety guard.                     |
| `config` | Profiles, registered connections, protected local secrets, and editor state.                |
| `db`     | JDBC connection factory, metadata providers, schema discovery, and SQL execution.           |
| `domain` | Shared domain objects such as connection config, database type, and execution results.      |
| `sql`    | Statement selection, tokenization, render model, diagnostics, and completion providers.     |
| `tui`    | Lanterna workspace, keyboard routing, connection dialogs, i18n messages, and result panels. |

Important entry points:

- `bo.ahosoft.sqlscript.cli.OracleScriptCli` is the packaged main class.
- `bo.ahosoft.sqlscript.tui.Gui2WorkspaceController` handles the main TUI keyboard workflow.
- `bo.ahosoft.sqlscript.db.SqlScriptRunner` handles SQL execution and SELECT pagination.
- `bo.ahosoft.sqlscript.domain.SqlExecutionResult` handles page state, row numbering, and page cache behavior.

## Testing Expectations

Before opening a PR or sharing a patch, run:

```bash
mvn test
mvn package
```

For TUI changes, also smoke test manually in a real terminal:

```bash
java -jar target/oracle-script-cli.jar
```

Recommended smoke checks:

- Resize the terminal.
- Cycle focus with `Tab` and `Shift+Tab`.
- Open help with `F1` or `?`.
- Create or cancel Oracle/PostgreSQL connection wizards.
- Select a saved connection with `Enter`.
- Run `tables`, `describe <table>`, and `indexes <table>`.
- Execute a harmless SELECT and navigate results with arrows plus `PageDown`/`PageUp`.

## Proposing Improvements

Issues and pull requests are welcome. A useful report includes:

- Operating system, terminal emulator, Java version, and Maven version.
- Database type and JDBC URL shape without secrets.
- Exact command or shortcut used.
- Expected behavior and actual behavior.
- Screenshots or terminal output when safe to share.

Never include passwords, production credentials, private hostnames, or sensitive query results in public issues.

## License

This project is licensed under the MIT License. See `LICENSE` for details.
