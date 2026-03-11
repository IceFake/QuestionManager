# PACKAGE KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
Root package `com.example.questionmanager` containing all application source code. Organized into three architectural layers (Data, Domain, UI) plus supporting modules (DI, Utils). Follows MVVM with Repository pattern and Hilt for dependency injection.

## STRUCTURE
```
com/example/questionmanager/
├── data/                              # Data layer
│   ├── local/                         # Local data sources (Room, DataStore)
│   ├── remote/                        # Remote data sources (DeepSeek API, web parsing)
│   └── repository/                    # Repository implementations
├── domain/                            # Domain layer (optional)
│   ├── model/                         # Domain models (Question, Prompt, QuestionLink)
│   └── usecase/                       # Use cases encapsulating business logic
├── ui/                                # Presentation layer
│   ├── screen/                        # Compose screens (Home, Detail, Input, etc.)
│   ├── component/                     # Reusable UI components
│   ├── navigation/                    # Navigation graph
│   └── theme/                         # Material 3 theming
├── di/                                # Dependency injection (Hilt modules)
└── util/                              # Shared utilities (extensions, constants)
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new entity | `data/local/db/entity/` | Extend `QuestionEntity`, `PromptEntity`, or `QuestionLinkEntity` |
| Add a new DAO | `data/local/db/dao/` | Implement `@Dao` interface; add to `AppDatabase` |
| Add a new API service | `data/remote/api/` | Define Retrofit interface; add to `NetworkModule` |
| Add a new repository | `data/repository/` | Inject DAOs/API services; expose `Flow`/`suspend` functions |
| Add a new use case | `domain/usecase/` | Inject repositories; implement `operator fun invoke` |
| Add a new screen | `ui/screen/{name}/` | Create `{Name}Screen.kt` + `{Name}ViewModel.kt` |
| Add a new UI component | `ui/component/` | Stateless Composable with `@Composable` annotation |
| Add a new theme component | `ui/theme/` | Colors, typography, shapes in `Color.kt`, `Type.kt`, `Theme.kt` |
| Add a new dependency | `di/` | Create or extend Hilt `@Module` |
| Add a new utility | `util/` | Extension functions, constants, shared helpers |

## CONVENTIONS (Package‑Specific)
- **Layer boundaries**: Data layer must not reference UI or Domain; Domain may reference Data; UI may reference Domain and Data.
- **Repository pattern**: Each data source (local/remote) is abstracted behind a Repository.
- **ViewModel scope**: ViewModels are scoped to screens and survive configuration changes.
- **Use case naming**: Use cases are named `{Verb}{Noun}UseCase` (e.g., `GenerateAnswerUseCase`).
- **Entity vs Domain model**: Room entities (`*Entity`) are separate from domain models (`Question`, `Prompt`, `QuestionLink`). Repositories perform mapping.
- **Error handling**: Use `Result<T>` for suspend functions; propagate errors to UI as `Resource.Loading/Error/Success`.
- **Coroutine dispatchers**: Use `Dispatchers.IO` for database/network, `Dispatchers.Main` for UI updates.

## ANTI‑PATTERNS
1. **DO NOT** expose Room entities directly to UI – map to domain models first.
2. **DO NOT** write business logic in ViewModel – delegate to UseCase.
3. **DO NOT** create circular dependencies between layers.
4. **DO NOT** hard‑code string literals – use `Constants.kt` or resource files.
5. **DO NOT** ignore `MainThread` safety – ensure UI updates are on `Dispatchers.Main`.
6. **DO NOT** forget to add new `@Module` to `@InstallIn` scope.

## UNIQUE STYLES
- **AI rate limiting**: `AiRepository` uses `Semaphore(permits = 3)` to limit concurrent API calls.
- **Streaming answers**: `GenerateAnswerUseCase.stream()` returns `Flow<String>` for incremental updates.
- **Encrypted API key storage**: `SecureDataStore` uses `EncryptedSharedPreferences` from `security‑crypto`.
- **Multi‑strategy JSON parsing**: `AiResponseParser` handles LLM response format variations.
- **Default prompt seeding**: `DatabaseCallback` seeds four default prompts on first app launch.
- **Web‑parsing fallback**: `WebParserService` detects SPA pages and warns the user.

## NOTES
- **Room schema export**: Enabled; schema files are in `app/schemas/`. Must be committed to Git.
- **Hilt component hierarchy**: Application‑level (`SingletonComponent`), Activity‑level (`ActivityComponent`), ViewModel‑level (`ViewModelComponent`).
- **Navigation**: Single‑Activity architecture with `Navigation Compose`; routes defined in `ui/navigation/`.
- **Testing**: Unit tests in `src/test/`, instrumented tests in `src/androidTest/`. Use Hilt test modules for integration tests.
- **Code generation**: Room uses KSP; run `./gradlew kspKotlin` after adding/changing entities.
