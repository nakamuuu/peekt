---
root: false
targets: ["cursor", "claudecode"]
description: "GitHub release notes style"
globs: ["**/*"]
cursor:
  alwaysApply: true
  description: "GitHub release notes style"
  globs: ["**/*"]
---

# Release notes

Draft GitHub Release bodies only when the user asks. Match 1.1.0 / 2.0.0: English, consumer-facing, short.

## Audience

Write for library consumers, not maintainers.

- Include public API, user-visible behavior, breaking changes, and fixes callers can feel.
- Omit the sample app, Gradle / AGP / wrapper bumps, AI editor docs, comment-only ProGuard files, and internal refactors.

## Structure

Omit `## What's new` unless there is at least one `[New]` item. Fixes and improvements belong under `## Changes` only.

```
## What's new

<only when [New] exists: one or two sentences of what callers notice>

<optional Kotlin snippet only when a new or changed API needs showing>

## Changes

- [New] ...
- [Breaking] ...
- [Improved] ...
- [Fix] ...

**Full Changelog**: https://github.com/nakamuuu/peekt/compare/<prev>...<this>
```

Use only the labels that apply. Skip the snippet when nothing in the public API changed.

## Tone

- Outcome first. Do not narrate implementation (column projections, `writeTo`, DAO queries).
- Prefer “list observation is faster” over “`observeTransactions()` no longer loads headers or bodies.”
- Do not paste a code sample just to illustrate an existing API.

```
# ❌ BAD
`observeTransactions()` no longer loads headers or bodies. List UIs stay on
the summary columns; open a row with `getTransactionMessage` when you need
the payload.

# ✅ GOOD
`observeTransactions()` is lighter for list UIs.
```
