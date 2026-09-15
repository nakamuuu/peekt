---
root: false
targets: ["cursor", "claudecode"]
description: "Commit granularity and message style"
globs: ["**/*"]
cursor:
  alwaysApply: true
  description: "Commit granularity and message style"
  globs: ["**/*"]
---

# Git commits

Create commits only when the user asks. Match this repository's history: small, single-concern commits and English sentences.

## Granularity

- One concern per commit. Do not mix a bug fix with an unrelated refactor, docs pass, or version bump.
- Prefer a short stack over one large commit when the steps are independently reviewable. Typical splits in this repo:
  - implement sampling/persistence, then add interceptor or README coverage
  - tests-only or docs-only when that is the whole change
- Keep a commit buildable.
- If the user asks for one commit covering a finished change set, keep it to that one concern rather than inventing extra splits.

## Messages

- English. Imperative mood. Sentence case. Do not end the subject with a period.
- No Conventional Commits prefixes (`feat:`, `fix:`, `chore:`) and no ticket IDs in the subject.
- Subject: why or the user-visible outcome, not a file list.
- Body: optional; one or two sentences of motivation when the subject is not enough.

```
# ❌ BAD
fix: update Dao
Update HttpTransactionDao.kt and RealPeektRecorder.kt

# ✅ GOOD
Avoid loading bodies when observing the transaction list

# ✅ GOOD (subject + why)
Sample request bodies as HttpBody instead of nullable text
Binary and one-shot bodies were indistinguishable from "no body" when the sampler only returned a string.
```
