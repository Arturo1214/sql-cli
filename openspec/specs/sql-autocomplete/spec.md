# SQL Autocomplete Specification

## Purpose

Offer lightweight autocomplete for SQL keywords and metadata-backed table names.

## Requirements

### Requirement: Suggest SQL keywords and table names

The system MUST provide completion candidates for SQL keywords and table names when the user requests autocomplete.

#### Scenario: Suggest a keyword

- GIVEN the user has typed a partial SQL keyword
- WHEN autocomplete is requested
- THEN matching SQL keywords are returned

#### Scenario: Suggest a table name

- GIVEN the metadata catalog contains table names
- WHEN autocomplete is requested at a table-reference position
- THEN matching table names are returned

### Requirement: Limit completion scope

The system MUST NOT provide parser-based corrections, deep dialect validation, column autocomplete, or function autocomplete in this MVP.

#### Scenario: Avoid column suggestions

- GIVEN the user is inside a column list
- WHEN autocomplete is requested
- THEN column suggestions are not offered

#### Scenario: Keep invalid SQL uncorrected

- GIVEN the current SQL is syntactically incomplete
- WHEN autocomplete is requested
- THEN the system offers candidates only and does not rewrite the query
