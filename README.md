[![Codacy Badge](https://api.codacy.com/project/badge/Grade/9c1decbe5aad4ce3989eab10eab08e5d)](https://www.codacy.com/gh/codacy/codacy-sonar-visual-basic?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=codacy/codacy-sonar-visual-basic&amp;utm_campaign=Badge_Grade)
[![Build Status](https://circleci.com/gh/codacy/codacy-sonar-visual-basic.svg?style=shield&circle-token=:circle-token)](https://circleci.com/gh/codacy/codacy-sonar-visual-basic)

# Codacy Sonar Visual Basic

This is the docker engine we use at Codacy to run [SonarC#](https://github.com/SonarSource/sonar-dotnet) developed by SonarSource.

You can also create a docker to integrate the tool and language of your choice!
Check the **Docs** section for more information.

## Usage

### Publish the docker

```bash
make publish
docker build -t codacy-sonar-visual-basic .
```

### Run the docker

```bash
docker run --user=docker --rm=true -v <PATH-TO-CODE>:/src -v <PATH-TO>/.codacyrc:/.codacyrc codacy-sonar-visual-basic
```

> Make sure all the volumes mounted have the right permissions for user `docker`

### Generate Docs

**Requirements:**

-   `xmllint` utility installed on your system:

    -   On ubuntu:

            apt-get install libxml2-utils

    -   On alpine

            apk add libxml2-utils

**Generate:**

```sh
make documentation
```

## Docs

[Tool Developer Guide](https://support.codacy.com/hc/en-us/articles/207994725-Tool-Developer-Guide)

[Tool Developer Guide - Using Scala](https://support.codacy.com/hc/en-us/articles/207280379-Tool-Developer-Guide-Using-Scala)

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped `SonarAnalyzer.VisualBasic` version, but also base image / CircleCI orb / other NuGet dependency bumps. Follow it top to bottom.

### 1. What this repository is

This is a **Codacy engine**: a .NET Framework (`net461`) console app (`src/Analyzer`, built on `Codacy.Engine.Seed`) that wraps [SonarSource's VB.NET analyzer](https://github.com/SonarSource/sonar-dotnet) (`SonarAnalyzer.VisualBasic`, distributed as a NuGet package) and packages it as a Docker image Codacy's platform runs against customer source code. The image is built via a multi-stage Dockerfile: a `mcr.microsoft.com/dotnet/core/sdk:2.2` + Mono builder stage compiles/publishes the app, and the runtime stage is `alpine` + Mono, entrypoint `mono /opt/docker/bin/Analyzer.exe`.

The `docs/` directory is machine-consumed configuration, not just documentation:

- `docs/patterns.json` — the list of Sonar VB.NET rules ("patterns") Codacy knows about, their categories/severities/parameters, and which are enabled by default. Generated file, do not hand-edit. **Note:** as of this writing its `"version"` field reads `8.13` while `Analyzer.csproj` pins `SonarAnalyzer.VisualBasic` at `8.15.0.24505` — regeneration has historically lagged behind dependency bumps, so don't assume the two are in sync unless you check.
- `docs/description/description.json` + `docs/description/*.md` — human-readable titles/descriptions per pattern, used in the Codacy UI. Generated file, do not hand-edit.
- `docs/multiple-tests/*` — fixtures used by `codacy-plugins-test`.
- `docs/tool-description.md` — short blurb about the tool, hand-maintained.

The generator is **`src/DocsGenerator`** (`src/DocsGenerator/Program.cs`), a separate .NET console project. It reads a `.res/sonar-vbnet-rules.xml` file (extracted from the real Sonar VB.NET plugin JAR, see below) and a `.SONAR_VERSION` file, and writes `docs/patterns.json` + `docs/description/*`. It needs the `.res` folder and `.SONAR_VERSION` to already be populated — the `Makefile` orchestrates this (`make documentation` = `update-docs` + `build-docs` + running the generator).

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `src/Analyzer/Analyzer.csproj` → `SonarAnalyzer.VisualBasic` PackageReference | Which Sonar VB.NET analyzer release is bundled and analyzed against | Bump the `Version` attribute. This is also what `make` reads via `xmllint` (`SONAR_VERSION` in the `Makefile`) to download the matching plugin JAR for doc generation — confirm a matching GitHub release tag (`<version>`) exists at [SonarSource/sonar-dotnet releases](https://github.com/SonarSource/sonar-dotnet/releases). |
| `src/Analyzer/Analyzer.csproj` → `Microsoft.CodeAnalysis.VisualBasic.Workspaces` | Roslyn VB workspace APIs the analyzer host depends on | Check [NuGet](https://www.nuget.org/packages/Microsoft.CodeAnalysis.VisualBasic.Workspaces) for compatibility with the target Sonar analyzer version before bumping. |
| `src/Analyzer/Analyzer.csproj` → `Codacy.Engine.Seed` | Codacy's .NET engine SDK | Check NuGet for newer versions if asked to update it; not tied to Sonar bumps. |
| `src/Analyzer/Analyzer.csproj` → `ReverseMarkdown`, `Newtonsoft.Json`, `SQLitePCLRaw.core` | Supporting libraries (used by `DocsGenerator`/`Analyzer`) | Handled routinely by Dependabot; bump manually only if asked or if blocking another change. |
| `.github/dependabot.yml` → `ignore` list | Dependabot is currently told to **skip** several newer `SonarAnalyzer.VisualBasic` and `ReverseMarkdown` releases (e.g. `8.17.0.26580` through `8.21.0.30542`) | If you are deliberately bumping past one of these ignored versions, remove/update the corresponding `ignore` entry so Dependabot doesn't immediately re-flag or conflict; if those versions were ignored due to a known incompatibility, investigate why before un-ignoring. |
| `.circleci/config.yml` → `codacy/base` orb, `codacy/plugins-test` orb | Shared CircleCI steps (checkout, versioning, docker publish, tagging) and the `codacy-plugins-test` runner | Check the latest published orb versions if asked to bump CI tooling; `git log -p .circleci/config.yml` shows prior bump history as a fallback reference. |
| `Dockerfile` → builder base image (`mcr.microsoft.com/dotnet/core/sdk:2.2`) and runtime base image (`alpine:3.17`) | SDK used to build, and the OS/Mono runtime the packaged app runs on | Only bump if asked explicitly, or if the current base images are EOL/unavailable — don't bump opportunistically since Mono/`net461` compatibility is finicky here. |

### 3. Step-by-step update procedure

1. **Bump the version(s)** in `src/Analyzer/Analyzer.csproj` (and `.github/dependabot.yml` ignore list / `.circleci/config.yml` orbs, if applicable) as scoped by the task.
2. **Regenerate the docs.** Requires `xmllint` (`apt-get install libxml2-utils` or `apk add libxml2-utils`), Mono, and `dotnet`, plus network access to download the Sonar plugin JAR: `make documentation`. This runs `update-docs` (downloads `sonar-vbnet-plugin-<version>.jar` from the sonar-dotnet GitHub release matching the bumped version, unzips it, extracts `sonar-vbnet-rules.xml`), then `build-docs` (compiles `src/DocsGenerator`), then runs `DocsGenerator.exe` to regenerate `docs/patterns.json` and `docs/description/*`. Review the diff for new/removed/renamed patterns and stale fixture references. Confirm `docs/patterns.json`'s `"version"` field now matches the bumped version (it can otherwise silently drift, as noted above).
3. **Build the app locally**: `make configure && make build` (requires `dotnet` and Mono; `FrameworkPathOverride` is set by the `Makefile` for `net461`/Mono compatibility).
4. **Build the Docker image**: `docker build -t codacy-sonar-visual-basic .`
5. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run its DockerTest commands (including the multiple-tests suite, since CI runs with `run_multiple_tests: true`) against your local image tag.
6. **Iterate on failures**, re-running only the relevant DockerTest command after each fix.
7. **Commit** the version bump(s) together with any regenerated `docs/` files in one change.
8. **Push and open a PR.** CI (CircleCI, see `.circleci/config.yml`) runs `codacy/checkout_and_version` -> `publish_local` (docker build) -> `plugins_test` -> `codacy/publish_docker` (master only) -> `codacy/tag_version`.
9. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (the CircleCI job page/API for the failing job — don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human — in which case explain it in the PR rather than guessing.

### 4. Common failure modes and fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| `update-docs` (`make documentation`) fails to download the plugin JAR | The bumped version string doesn't match an actual `sonar-dotnet` GitHub release tag | Verify the exact release tag/version format on [SonarSource/sonar-dotnet releases](https://github.com/SonarSource/sonar-dotnet/releases) |
| `docs/patterns.json` version field doesn't match the bumped analyzer version after `make documentation` | Doc regeneration was skipped or stale docs were left in place from a prior bump | Re-run `make documentation` and confirm the diff updates the `"version"` field |
| `plugins_test` DockerTest fails on a specific rule/fixture | Rule renamed/removed/added upstream between Sonar VB.NET versions | Re-run `make documentation`; confirm the change matches the upstream Sonar changelog, update fixtures under `docs/multiple-tests/` if the expected output legitimately changed |
| Dependabot immediately reopens a PR bumping past your manually chosen version, or conflicts with it | `.github/dependabot.yml` `ignore` list is out of date relative to your change | Update the `ignore` entries to match the version you actually landed on |
| Docker build fails compiling with Mono/`net461` | Base image or NuGet package bump introduced an incompatibility with the Mono toolchain used in the builder stage | Check the specific compiler/build error; this stack is sensitive to `net461`/Mono version skew, so pin to a known-working combination rather than the newest available |

### 5. Definition of done

- Version bump(s) reflected in `src/Analyzer/Analyzer.csproj` and any related CI/dependabot config.
- Docs regenerated via `make documentation`, with `docs/patterns.json`'s version field matching, and fixture inconsistencies resolved.
- Local build (`make build`) succeeds.
- Docker image builds successfully.
- `codacy-plugins-test` (including the multiple-tests suite) passes locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure (fetch the real CI log, fix, push, re-poll) until all pass — a passing local build is not sufficient, because the CI toolchain can differ from your local one (see step 9).

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration.
You can follow the instructions there to make sure your tool is working as expected.

## What is Codacy?

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features:

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
