# UTILITIES KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
Utilities package containing shared constants, extension functions, and helper classes used across the application. These are pure Kotlin constructs with no Android‑specific dependencies (except where noted).

## STRUCTURE
```
util/
├── Constants.kt                     # Application‑wide constants
│   ├── DEFAULT_SYSTEM_PROMPT        Default AI system prompt
│   ├── DATABASE_NAME                Room database name
│   ├── API_BASE_URL                 DeepSeek API base URL
│   └── other configuration constants
├── Resource.kt                      # Generic result wrapper
│   ├── sealed class Resource<T>     Success/Loading/Error states
│   └── extension functions          .onSuccess, .onFailure, etc.
└── Extensions.kt                    # Kotlin extension functions
    ├── String extensions            .trimUrl(), .toQuerySafe(), etc.
    ├── Flow extensions              .mapSuccess(), .filterSuccess()
    └── Context extensions           .dpToPx(), .spToPx() (Android‑specific)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new constant | `Constants.kt` | Use `const val` for compile‑time constants |
| Add a new extension function | `Extensions.kt` | Group by receiver type (String, Flow, Context) |
| Add a new result wrapper variant | `Resource.kt` | Extend `sealed class Resource` if needed |
| Change default prompt | `Constants.kt` | Update `DEFAULT_SYSTEM_PROMPT` |
| Add a new utility class | `util/` (new file) | Keep class small, focused, and well‑documented |

## CONVENTIONS
- **Constant naming**: UPPER_SNAKE_CASE for `const val`; grouped by logical domain.
- **Extension packages**: Extensions are top‑level functions in `Extensions.kt`; consider splitting if file grows large.
- **Resource wrapper**: Use `Resource<T>` for UI state representing loading/success/error.
- **Pure utilities**: Keep utility functions free of Android dependencies unless explicitly needed (e.g., `Context` extensions).
- **Documentation**: Each public constant/extension should have a KDoc comment explaining its purpose.
- **Testing**: Utilities should have corresponding unit tests (in `src/test/`).

## ANTI‑PATTERNS
1. **DO NOT** put business logic in extension functions – keep them simple transformations.
2. **DO NOT** create extension functions that hide expensive operations (e.g., network calls).
3. **DO NOT** duplicate constants across files – centralize in `Constants.kt`.
4. **DO NOT** add Android‑specific extensions to general utility files – separate `Context` extensions.
5. **DO NOT** use `Resource` for low‑level data layer results – use `Result` from Kotlin stdlib.
6. **DO NOT** forget to handle nullability in extension functions – use nullable receivers if appropriate.
7. **DO NOT** create overly generic utility functions – keep them focused on the project’s needs.

## UNIQUE STYLES
- **AI response parsing utilities**: `AiResponseParser` (located in `data` layer) is a utility‑style class but placed where it’s used.
- **URL normalization**: `String.trimUrl()` extension adds `https://` prefix and normalizes whitespace.
- **Flow result mapping**: `Flow<Resource<T>>.mapSuccess()` transforms only the success case.
- **Context dimension conversions**: `Context.dpToPx()` used in UI code for pixel‑perfect layouts.
- **Default prompt constant**: `DEFAULT_SYSTEM_PROMPT` provides a fallback when no user prompt is selected.

## NOTES
- **Constants vs configuration**: Constants are fixed at compile time; for user‑configurable values, use `SettingsRepository`.
- **Extension visibility**: Most extensions are `internal` to limit exposure to the rest of the app.
- **Resource vs Result**: `Resource` is for UI state (includes loading); `Result` is for suspend function outcomes.
- **Testing utilities**: Unit tests for utilities are straightforward – no need for Android instrumentation.
- **Future splitting**: If the app grows, consider moving extensions to feature‑specific utility files.
