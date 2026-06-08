# SQL Editor Rendering Specification

## Purpose

Provide lightweight SQL token highlighting and a span/decorations render model for the terminal editor.

## Requirements

### Requirement: Expose styled editor spans

The system MUST represent editor content as plain text plus an ordered span/decorations model that can be rendered in the terminal.

#### Scenario: Render highlighted SQL text

- GIVEN the editor contains SQL text
- WHEN the render model is built
- THEN the output includes spans for styled tokens

#### Scenario: Preserve plain text fallback

- GIVEN no token styling is available
- WHEN the editor renders
- THEN the plain SQL text remains readable without decoration

### Requirement: Highlight SQL tokens lightly

The system MUST highlight SQL keywords and SHOULD distinguish literals, identifiers, and punctuation without performing parser-based correction.

#### Scenario: Highlight a keyword

- GIVEN the user types `select`
- WHEN the render model is produced
- THEN the keyword is styled distinctly from normal text

#### Scenario: Leave unknown text unchanged

- GIVEN the editor contains an unrecognized token
- WHEN highlighting runs
- THEN the token remains visible and unmodified except for normal text rendering
