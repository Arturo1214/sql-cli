# Oracle Script CLI

Standalone Java 8 Maven CLI for QA users who need to inspect Oracle and PostgreSQL databases from a server terminal.

## Build and test

```bash
jenv local 1.8.0.292
mvn test
mvn package
```

The executable JAR is generated at:

```text
target/oracle-script-cli.jar
```

## Configure a connection

Oracle remains the default database type for legacy compatibility:

```bash
java -jar target/oracle-script-cli.jar init \
  'jdbc:oracle:thin:@//localhost:1521/XEPDB1' \
  'system' \
  'oracle' \
  --type oracle
```

PostgreSQL profiles use `--type postgresql`. Select schemas with a comma-separated `--schemas` list:

```bash
java -jar target/oracle-script-cli.jar init \
  'jdbc:postgresql://localhost:5432/appdb' \
  'app_user' \
  'secret' \
  --type postgresql \
  --schemas app,audit \
  --profile pg-qa
```

If a PostgreSQL profile is saved without `--schemas`, the CLI discovers available schemas and stores `public` when it is available.

Environment profile:

```bash
java -jar target/oracle-script-cli.jar init '<jdbcUrl>' '<user>' '<password>' --profile qa
java -jar target/oracle-script-cli.jar profiles
java -jar target/oracle-script-cli.jar validate --profile qa
```

Default and named profiles are stored in `~/.oracle-script-cli.properties` or `~/.oracle-script-cli-<profile>.properties`. Legacy profiles without a stored `type` are treated as Oracle.

Registered connections are stored under `~/.oracle-script-cli/connections/`. Passwords in registered connection files are protected by `ProtectedSecretStore` with local Java encryption key material under `~/.oracle-script-cli/connections/secrets`; connection listings never print passwords.

```bash
java -jar target/oracle-script-cli.jar connections list
```

## QA terminal workflow

Running the packaged CLI with no arguments opens the interactive workspace by default:

```bash
java -jar target/oracle-script-cli.jar
```

`workspace` is an explicit alias for the same full-terminal Lanterna GUI2 split-pane workspace:

```bash
java -jar target/oracle-script-cli.jar workspace
```

The GUI2 workspace uses Lanterna widgets when the current terminal supports them. It opens as a full-screen window, recomputes panel sizes on terminal resize, and keeps separate visual regions for:

- left explorer — saved Oracle/PostgreSQL connections plus metadata and creation actions.
- SQL editor — the current editable SQL text.
- bottom results/logs — the latest query output, error, or metadata result.
- Status/help — command hints remain visible while the editor keeps focus-aware input available.

When raw terminal mode is unavailable, `TERM=dumb`/`NO_COLOR` is active, output is captured, or the terminal is too small, the CLI uses a compact fallback that keeps connections, SQL buffer, results, status, and command hints readable without cursor-control escape sequences.

GUI2 workspace shortcuts:

- `Tab` / `Shift+Tab` cycles focus between explorer, editor, results, and footer/status areas.
- `F1` or `?` shows help. `?` is handled outside the SQL editor so it remains available for SQL text while typing.
- `F2` or `Ctrl+M` runs a metadata command (`tables`, `describe`, or `indexes`) from the SQL editor.
- `F3` or `Ctrl+L` returns focus to the left explorer.
- `F5` or `Ctrl+R` executes the visible SQL buffer/current SQL and renders results in the bottom pane without reusing stale metadata commands such as `tables`.
- `Enter` activates the selected connection or action in the explorer.
- `Escape` or `Ctrl+C` exits cleanly.

Metadata can be started from either the SQL editor or the left explorer menu. In the editor, type `tables [filter]`, `describe <table>`, or `indexes <table>`, then press `F2`/`Ctrl+M`. From the explorer, choose `Tables`, `Describe`, or `Indexes`; table-specific actions reuse the current editor argument when present.

Connection creation actions in the left panel include `New Oracle connection` and `New PostgreSQL connection`. Selecting either action opens a wizard-style form that prompts for name, JDBC URL, username, password, and schemas. Required fields are validated before save and validation feedback stays visible in the workspace. PostgreSQL defaults to `public` when schemas are empty and `public` is available, while Oracle ignores schema input. Cancel or Back returns to the workspace without saving.

WezTerm smoke steps after packaging:

```bash
mvn package
java -jar target/oracle-script-cli.jar
```

In the workspace, resize the terminal, cycle focus with `Tab`/`Shift+Tab`, press `F1`, run `tables` with `F2`, execute a harmless query with `F5`, and open/cancel both connection wizards from the explorer.

The SQL editor is plain text in this GUI2 slice. This change does not add syntax highlighting, autocomplete, or SQL correction; invalid SQL remains untouched until the user explicitly executes it.

Inside the workspace, use plain commands:

```text
help
connections
use <name>
new connection postgresql pg-qa jdbc:postgresql://localhost:5432/app app_user secret
buffer set select * from customers; select * from orders
buffer append where created_at >= current_date - interval '7 days'
buffer show
run
run --force --confirm-risk YES
tables customer
desc customers
sample customers 20
history
exit
```

The workspace dashboard shows the active connection, selected schemas, SQL buffer preview, bounded history, last result/error, and command hints. Use `help` to print the command list again and `exit` or `quit` to terminate cleanly.

Connection flow:

- `connections` lists saved registered connections from `~/.oracle-script-cli/connections/`.
- `use <name>` switches the active connection for subsequent metadata and `run` commands.
- `new connection postgresql ...` stores PostgreSQL connections and defaults schemas to `public` when no schema is provided and `public` is available.
- `new connection oracle ...` stores Oracle connections without schema selection.

Use `run-current` for script-friendly one-shot current-statement execution. It selects the SQL statement at the cursor and sends only that statement to the active database without entering the REPL.

```bash
java -jar target/oracle-script-cli.jar run-current @work.sql --cursor 42 --profile qa
java -jar target/oracle-script-cli.jar run-current --buffer 'select * from dual; select sysdate from dual' --cursor 5 --dry-run
```

Statement rules:

- Semicolons delimit statements.
- A final semicolon ends the current statement.
- Later statements in the buffer are ignored.
- Unfinished trailing text after the selected statement is not executed.

Editor state is stored separately from result output. `EditorStateStore` persists the current buffer and bounded SQL history in an editor properties file, encoded for safe properties storage. Failed queries remain eligible for history so QA users can correct and retry them.

## Execute SQL

```bash
java -jar target/oracle-script-cli.jar exec 'select * from dual'
java -jar target/oracle-script-cli.jar exec @script.sql --profile qa
java -jar target/oracle-script-cli.jar export 'select * from clientes where rownum <= 100' clientes.csv
java -jar target/oracle-script-cli.jar exec @consulta.sql --csv salida.csv
```

The CLI blocks destructive SQL by default, including `insert`, `update`, `delete`, `merge`, `drop`, `truncate`, `alter`, `create`, `grant`, and `revoke`.

To run destructive SQL non-interactively, use both `--force` and the exact typed confirmation `--confirm-risk YES`:

```bash
java -jar target/oracle-script-cli.jar run-current @update.sql --force --confirm-risk YES --profile qa
```

## Explore metadata

The same metadata commands work with Oracle and PostgreSQL profiles. SQL is routed through the active profile's database provider.

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

Use `--profile` or `--config` to choose the saved Oracle or PostgreSQL connection for metadata commands:

```bash
java -jar target/oracle-script-cli.jar tables --profile pg-qa
java -jar target/oracle-script-cli.jar desc clientes --profile oracle-qa
```

The packaged JAR includes Oracle and PostgreSQL JDBC drivers and preserves JDBC service entries during shading.

## Help and history

```bash
java -jar target/oracle-script-cli.jar help
java -jar target/oracle-script-cli.jar history
```

Command history is stored in `~/.oracle-script-cli.history` and does not include passwords.

## Direct use without saved configuration

```bash
java -jar target/oracle-script-cli.jar \
  'jdbc:oracle:thin:@//localhost:1521/XEPDB1' \
  'system' \
  'oracle' \
  'select * from dual'
```
