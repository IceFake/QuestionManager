# DOMAIN LAYER KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
Domain layer containing business logic, domain models, and use cases. Sits between Data and UI layers, enforcing separation of concerns. Models represent core business entities; use cases encapsulate reusable operations.

## STRUCTURE
```
domain/
├── model/                           # Domain models (pure Kotlin data classes)
│   ├── Question.kt                  # Question with status, answer, metadata
│   ├── Prompt.kt                    # System prompt with default flag
│   └── QuestionLink.kt              # Link between questions with relation type
└── usecase/                         # Use cases (business logic units)
    ├── GenerateAnswerUseCase.kt     # Generate AI answer for a question
    ├── ParseUrlUseCase.kt           # Parse URL and extract questions
    ├── ParseContentUseCase.kt       # Parse raw text and extract questions
    ├── SearchQuestionsUseCase.kt    # Search questions by query
    └── DrillDownUseCase.kt          # Generate and create linked questions
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new domain model | `model/` | Pure data class; no Android‑specific dependencies |
| Add a new use case | `usecase/` | Inject repositories; implement `operator fun invoke` |
| Modify business logic | `usecase/` | Keep logic independent of UI or data source details |
| Change model mapping | `model/` | Ensure mapping to/from entities (in Repository) stays consistent |
| Add a new enum | `model/` | Use sealed class/enum for fixed sets (e.g., `QuestionStatus`) |

## CONVENTIONS
- **Model purity**: Domain models are plain Kotlin data classes; no Android framework dependencies.
- **Use case naming**: `{Verb}{Noun}UseCase` (e.g., `GenerateAnswerUseCase`). Single public `operator fun invoke`.
- **Use case responsibility**: Each use case encapsulates one business operation; can call multiple repositories.
- **Error handling**: Use `Result<T>` for suspend functions; propagate errors to caller.
- **Threading**: Use cases are main‑safe; they internally switch dispatchers if needed.
- **Model mapping**: Mapping between entities and domain models is done in repositories, not in use cases.
- **Immutable models**: Domain models are immutable (`data class` with `val` properties).

## ANTI‑PATTERNS
1. **DO NOT** inject UI‑specific dependencies (e.g., `Context`, `Compose`) into use cases.
2. **DO NOT** put data‑source‑specific logic (SQL, API calls) in use cases – delegate to repositories.
3. **DO NOT** let use cases depend on other use cases – keep them independent.
4. **DO NOT** expose mutable state from domain models – use `val` properties.
5. **DO NOT** forget to handle errors in use cases – wrap in `Result` or catch and re‑throw domain exceptions.
6. **DO NOT** write use cases that are too large – split into smaller, focused use cases.

## UNIQUE STYLES
- **Streaming answer generation**: `GenerateAnswerUseCase.stream()` returns `Flow<String>` for incremental UI updates.
- **Drill‑down orchestration**: `DrillDownUseCase` coordinates AI suggestion generation, user selection, and link creation.
- **URL normalization**: `ParseUrlUseCase` automatically adds `https://` prefix if missing.
- **Content validation**: `ParseContentUseCase` rejects empty input before calling AI.
- **Search delegation**: `SearchQuestionsUseCase` delegates directly to repository, acting as a thin wrapper.

## NOTES
- **Optional layer**: The domain layer is optional; UI can call repositories directly. Use cases are provided for complex logic.
- **Testing**: Use cases are easy to unit test – mock the injected repositories.
- **Model‑entity mapping**: Repositories are responsible for converting between `QuestionEntity` and `Question`, etc.
- **Enum conversions**: Companion object methods (`fromValue`) handle string‑to‑enum conversion safely.
- **Flow vs suspend**: Use cases that return ongoing data (e.g., search) expose `Flow`; one‑shot operations use `suspend`.
