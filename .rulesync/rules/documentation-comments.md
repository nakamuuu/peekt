---
root: false
targets: ["*"]
description: Kotlin KDoc and documentation-comment conventions
globs: ["*.kt"]
---

# Documentation comments (Kotlin)

Conventions for `/** ... */`: document **what callers need**, not implementation trivia or name restatement.

## Block shape

- Always **multi-line** KDoc. Do not leave single-line `/** ... */`; convert existing single-line to multi-line.

```kotlin
/**
 * Uniquely identifies an HTTP transaction.
 */
data class HttpTransactionId(val value: String)
```

## What to document

- **Public / `internal`**: add or update KDoc when behavior, contracts, or threading are non-obvious.
- **Private**: optional; short note only to prevent misuse or explain non-obvious invariants.
- Add **why**, **constraints**, or **caller obligations**—not the identifier spelled out as prose.

## Summary, body, tags

- First paragraph = summary (prefer one sentence). Blank KDoc line between summary and detail.
- Side effects, errors, threading: state in summary or next paragraph—not only in `@throws`.
- **`@param` / `@return` / `@throws` / `@see`**: use when they add information beyond the signature. Drop `@return` if it only repeats the summary.
- Link types with **`[ClassName]`**; use backticks for identifiers/literals. **`@sample`** only if the project already uses samples with a real path.
- Small examples only when they prevent misuse; do not paste whole function bodies as “docs.”

## When editing

Update KDoc in the same change when **public** behavior, nullability, threading, or errors change. Match surrounding indentation and tag style.
