# SQL Metadata Catalog Specification

## Purpose

Expose a read-only metadata snapshot that can feed table-name autocomplete.

## Requirements

### Requirement: Provide a read-only catalog snapshot

The system MUST expose a read-only catalog snapshot containing table names available for autocomplete.

#### Scenario: Build a snapshot from metadata

- GIVEN metadata discovery returns table names
- WHEN the catalog snapshot is created
- THEN the snapshot contains those table names in a read-only form

#### Scenario: Keep snapshot isolated

- GIVEN the active metadata source changes after snapshot creation
- WHEN autocomplete uses the existing snapshot
- THEN the previous snapshot remains usable until refreshed

### Requirement: Degrade safely when metadata is unavailable

The system SHOULD return an empty or partial snapshot when table metadata cannot be discovered, without blocking editor use.

#### Scenario: Metadata source is unavailable

- GIVEN the database metadata cannot be read
- WHEN the catalog snapshot is requested
- THEN the editor still works and keyword autocomplete remains available

#### Scenario: Partial metadata is available

- GIVEN only some table metadata can be discovered
- WHEN the snapshot is built
- THEN the available table names are exposed and missing entries are ignored
