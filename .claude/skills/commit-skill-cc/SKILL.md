---
name: commit-skill
description: Inspect git status and diffs, then create small, logical commits with short Conventional-Commit titles (title only by default) — splitting mixed work into separate commits and keeping secrets and unrelated files out. Auto-trigger after every small completed change, not just when explicitly asked.
context: fork
agent: git-committer
---

# Commit

Default to **small, logical commits**: one commit = one coherent change. Inspect the actual diff before writing anything, keep secrets and unrelated files out, verify the change, and use Conventional Commit format.

## Auto-commit

Don't wait for "commit this" request. After finishing any small logical change (single fix, single feature step, single file edit) — run this skill right away, no ask needed.

Skip auto-commit only when:
- Change broken/unverified (step 4 fails).
- Mid multi-step task, change incomplete.
- Secret/credential file involved — stop, flag, no commit.

User can say "hold off commits" / "don't commit yet" to pause this for the session.

By default, write **only a short Conventional Commit title** — no body, no bullets, no changelog-style details. Add a body **only** when the user explicitly asks for a detailed commit message (e.g. "with a detailed body", "explain the changes in the commit").

## Procedure

1. **Inspect.** Run `git status`, then `git diff --stat` (and `git diff --cached --stat` if anything is staged). Read the full diff (`git diff` / `git diff --cached`) for what you'll commit — base the message on what actually changed, never on assumptions. Prefer staged changes if present; otherwise work from unstaged + untracked.

2. **Screen out what shouldn't be committed.** Before staging, scan for:
   - **Secrets/credentials:** `.env`, `*.pem`, `*.key`, `id_rsa`, tokens, API keys, passwords, connection strings. Never stage these; if a tracked file contains a secret value, stop and flag it.
   - **Noise:** build output (`dist/`, `build/`), `node_modules/`, logs, caches, OS/editor files, large binaries — these belong in `.gitignore`, not a commit.
   - **Unrelated files:** anything outside the logical change being committed.
   Stage explicitly (`git add <path>`), never `git add -A` / `git add .`, so nothing slips in.

3. **One commit or several?** If the diff mixes unrelated concerns — a feature plus an unrelated refactor, a fix plus a dependency bump, formatting plus logic — do **not** bundle them. Propose a split: list each logical group, its files, and its title, then (with the user's go-ahead) stage and commit each group separately. When in doubt, smaller is better.

4. **Verify before committing.** Confirm the change has been checked — a fast build/typecheck/lint/test should have passed. If verification hasn't happened and is cheap, run it; if it can't be run, say so rather than implying it's verified. Don't commit known-broken code unless the user asks for a WIP checkpoint.

5. **Write and commit.** Use the format below. Show the title before committing unless the user has said to just commit.

## Format

Default — title only:

```txt
type(scope): short summary
```

- `type(scope): summary` — lowercase, imperative, ≤ ~70 chars; `scope` optional.
- No body, no bullets, no trailing period.

Detailed (only when explicitly requested) — title, one blank line, then concise bullets:

```txt
type(scope): short summary

- area: detail
- area: detail
```

In a detailed body, call out anything a reviewer must know: API/behavior or breaking changes, security/privacy, migrations, dependency bumps.

## Types

`feat` new behavior · `fix` bug fix · `refactor` no behavior change · `perf` performance · `docs` docs · `test` tests · `build` build system/deps · `ci` CI config · `chore` tooling/housekeeping · `style` formatting only.

## Scope

Infer from what's touched — package, module, directory, or feature. Follow the repo's own conventions (check `git log --oneline -10`). Omit the scope when it adds nothing; for a repo-wide change use the broadest accurate scope (e.g. `repo`).

## Rules

- Conventional format exactly: `type(scope): summary` — never `type:(scope):` or `type(scope) :`.
- Title only by default; never add a body, bullets, or changelog details unless explicitly asked.
- Match the repo's existing commit style.
- No emojis, no trailing period on the title, no markdown headings in the body.
- Don't add `Co-Authored-By` / attribution trailers unless the project clearly uses them.
- Don't use `git commit -a`, amend, force-push, or rewrite history unless asked.
- Describe what changed and why from the reader's side — not "updated files".

## Examples

Default (title only):

```txt
feat(frontend): migrate dashboard map to Leaflet
```

```txt
fix(parser): handle empty input without throwing
```

```txt
chore(repo): ignore build artifacts
```

### Proposing a split (step 3)

When the diff mixes unrelated work, present it like this and wait for confirmation:

```txt
This diff mixes two unrelated changes — I'd split it:

1. fix(api): reject negative page sizes
   - src/api/pagination.ts
2. chore(deps): bump eslint to v9
   - package.json, package-lock.json

Commit them separately?
```
