# Proposal: PostgreSQL Test Hardening

## Intent

Close the last verification gaps from `postgres-support` without changing product behavior: harden tests around missing pgJDBC and PostgreSQL init with omitted schemas so the prior warnings become a clean PASS.

## Scope

### In Scope

- Add a process-level smoke test for the missing-driver / no suitable driver failure.
- Add an end-to-end CLI init test proving omitted PostgreSQL schemas default to `public`.
- Add only test-harness helpers needed to run the CLI with a filtered classpath.

### Out of Scope

- No feature expansion.
- No metadata/query behavior changes.
- No dependency/version upgrades.

## Capabilities

### New Capabilities

- None

### Modified Capabilities

- None

## Approach

Use the existing smoke-test JVM launcher to exercise the CLI as a child process. For the missing-driver case, run without the PostgreSQL jar and assert the current `SQLException` failure path. For omitted schemas, initialize a PostgreSQL config with no `--schemas` and assert the persisted config/output contains `public`. Keep any seam test-only.

## Affected Areas

| Area                                                                     | Impact   | Description                                                   |
| ------------------------------------------------------------------------ | -------- | ------------------------------------------------------------- |
| `src/test/java/com/example/oraclescript/OracleScriptCliSmokeTest.java`   | Modified | Add the two hardening scenarios and any JVM/classpath helper. |
| `src/test/java/com/example/oraclescript/SchemaDiscoveryServiceTest.java` | Modified | Extend coverage for `public` defaulting if needed.            |

## Risks

| Risk                                    | Likelihood | Mitigation                                               |
| --------------------------------------- | ---------- | -------------------------------------------------------- |
| Process-classpath test is brittle on CI | Medium     | Keep assertions narrow: exit code + stable error text.   |
| CLI init test becomes integration-heavy | Low        | Reuse temp files and the existing child-process harness. |

## Rollback Plan

Remove the new smoke tests/helpers only; no production rollback is expected.

## Dependencies

- Existing PostgreSQL driver availability for positive-path runtime tests.

## Success Criteria

- [ ] Missing-driver failure is covered by a deterministic test.
- [ ] Omitted-schema PostgreSQL init is covered end to end and persists/selects `public`.
- [ ] The suite passes without changing CLI behavior.
