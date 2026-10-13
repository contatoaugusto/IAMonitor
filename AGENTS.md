# Repository Guidelines

## Project structure

This repository contains only the Java desktop application. `src/main/java/br/iamonitor/desktop/` contains the Swing UI, CLI adapters, event logic, SQLite store and PDF export. `src/main/resources/` contains channel branding. `src/test/java/br/iamonitor/desktop/` contains JUnit tests. `data/` holds local user sessions and must be preserved. `target/` is generated output, including the packaged Windows app and an optional local JDK/Maven toolchain.

## Build and run

Use Java 21 or later and Maven 3 on Windows. The selected AI CLI must be installed and authenticated for real sessions.

- `.\run.ps1` starts the packaged app and uses `data/` by default.
- `.\build.ps1` runs tests and builds the JAR.
- `.\build.ps1 -AppImage` also builds the Windows app image.
- `mvn test` runs JUnit tests when Maven is on PATH.

`build.ps1` can use the toolchain in `target/toolchain/` if JDK or Maven is not on PATH.

## Code style

Keep Java classes in `br.iamonitor.desktop` with two-space indentation. Use descriptive camelCase methods and PascalCase classes and records. `Dashboard` owns Swing presentation and user actions; `CodexRunner` manages CLI processes; `Provider` defines CLI commands and normalized provider events; `Store` owns persistence; `EventLogic` accounts for usage and classifies artifacts. Avoid blocking the Swing event dispatch thread.

## Testing

Add focused JUnit tests for event accounting, provider adapters, artifact detection, SQLite compatibility and process behavior. Use `@TempDir`, fake CLI events and a fake Codex process. Never depend on personal `data/` contents. Run the Java tests and package build before proposing desktop changes. Review the rendered Swing window or a component preview when changing layout.

## Artifacts

Do not store video production scripts, recording prompts, didactic projects or media in this repository. Keep this repository focused on the desktop application and its user documentation.

Do not commit `data/`, `target/`, credentials or captured session content. Keep `run.ps1`, `build.ps1`, `pom.xml`, `README.md` and this file at the repository root.

## OpenSpec workflow

`openspec/specs/` is the current behavior contract. For every user-visible behavior or architecture change, create one focused change under `openspec/changes/` before implementation. Record intent in `proposal.md`, testable deltas in `specs/<capability>/spec.md`, technical decisions and alternatives in `design.md`, and implementation plus verification in `tasks.md`. Update task checkboxes as work lands. If the user requested implementation, continue through apply, tests and archive in the same task; a separate approval is unnecessary. Validate the change and archive it only after the specs match the delivered behavior. Keep the proposal, design, tasks and deltas in the archive. Write OpenSpec artifacts in Brazilian Portuguese.

Use the generated Codex workflows in `.agents/skills/` or the pinned CLI via `npm exec --yes --package=@fission-ai/openspec@1.14.1 -- openspec ...`. Do not put personal session data, real prompts, credentials or captured file contents into specs. Existing baseline specs describe observed behavior, not an invented development history.
