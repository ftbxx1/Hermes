# Contributing to Hermes

First of all, thank you for taking the time to contribute to Hermes!

The following is a set of guidelines for contributing to Hermes. These are
guidelines, not rules — use your best judgement, and feel free to propose
changes to this document in a pull request.

## Table of contents

- [What should I know before I get started?](#what-should-i-know-before-i-get-started)
  - [Hermes and plugins](#hermes-and-plugins)
  - [The language](#the-language)
- [How can I contribute?](#how-can-i-contribute)
  - [Reporting bugs](#reporting-bugs)
  - [Suggesting enhancements](#suggesting-enhancements)
  - [Your first code contribution](#your-first-code-contribution)
  - [Pull requests](#pull-requests)
- [Style guide](#style-guide)
  - [Code style](#code-style)
  - [Commit messages](#commit-messages)

## What should I know before I get started?

### Hermes and plugins

Hermes is a Paper plugin that interprets plain-text `.her` scripts at
runtime. There are no third-party plugin dependencies. The in-game docs live
in `src/main/resources/help/variables.md` and are copied into the plugins
folder on first run.

### The language

Scripts are made of triggers (`when player joins`), states (`when player is
flying`), actions (`give player 1 diamond`), commands, and variables. The
full reference is documented in the README's feature table and in
`help/variables.md`.

## How can I contribute?

### Reporting bugs

Before creating a bug report, please check that the issue isn't already
reported (search the [issues](https://github.com/ftbxx1/Hermes/issues)).
When you create a bug report, use the
[bug report template](.github/ISSUE_TEMPLATE/bug_report.md) and include as
much detail as possible:

- Paper version and Java version
- The exact steps to reproduce
- The `.her` script that triggers the bug, if applicable
- The full server log / stack trace, if any

### Suggesting enhancements

Open a [feature request](.github/ISSUE_TEMPLATE/feature_request.md). Explain
the use case: what a script would look like with the new feature and why
existing features aren't enough.

### Your first code contribution

Unsure where to start? Look for issues labelled `good first issue`. If you
want to work on an issue, leave a comment on it so others know it's taken.

### Pull requests

1. Fork the repository and create your branch from `master`.
2. Make your changes. Follow the [style guide](#style-guide).
3. Run `mvn test` to make sure nothing is broken.
4. Build with `mvn package` and smoke-test the jar on a Paper 1.21+ server
   if possible.
5. Push to your fork and open a pull request using the
   [pull request template](.github/PULL_REQUEST_TEMPLATE.md).
6. Keep changes focused: one pull request per feature or bugfix.

## Style guide

### Code style

- Indent with 4 spaces, no tabs.
- Follow the style of surrounding code in the file you edit.
- Use descriptive names; the language itself is meant to read like English,
  so keep trigger/action keywords lowercase and readable.
- Don't introduce dependencies without a good reason — Hermes stays
  dependency-free by design.
- No `//` comments explaining *what* code does; use names that say it. A
  comment may only explain *why* something non-obvious is done.

### Commit messages

- Present tense, imperative mood: "Add X", not "Added X".
- Keep the subject line under 72 characters.
- Reference issue numbers where relevant: `Fix #12`.
