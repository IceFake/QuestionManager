# DEPENDENCY INJECTION KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
Dependency injection layer using Hilt (Dagger). Modules provide application‑wide dependencies: Room database, Retrofit services, DataStore, and repositories. All injection is scoped to appropriate Android lifecycles (Application, Activity, ViewModel).

## STRUCTURE
```
di/
├── AppModule.kt                     # Application‑level dependencies
│   ├── provideAppDatabase()         Room database with callback
│   ├── provideQuestionDao()         DAO instances
│   ├── providePromptDao()           Prompt DAO
│   ├── provideQuestionLinkDao()     Link DAO
│   ├── provideSettingsDataStore()   Plain DataStore for settings
│   └── provideSecureDataStore()     Encrypted DataStore for API key
├── NetworkModule.kt                 # Network‑related dependencies
│   ├── provideOkHttpClient()        OkHttp with logging interceptor
│   ├── provideRetrofit()            Retrofit with Kotlinx Serialization
│   ├── provideDeepSeekApiService()  DeepSeek API service
│   └── provideWebParserService()    Jsoup‑based web parser
└── RepositoryModule.kt              # Repository implementations
    ├── provideQuestionRepository()  QuestionRepository instance
    ├── provideAiRepository()        AiRepository with semaphore
    └── provideSettingsRepository()  SettingsRepository instance
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new Room entity/DAO | `AppModule.kt` | Update `provideAppDatabase()` and add DAO provider |
| Add a new API service | `NetworkModule.kt` | Add new `provide*Service()` function |
| Add a new repository | `RepositoryModule.kt` | Create provider injecting required dependencies |
| Change DataStore configuration | `AppModule.kt` | Modify `provideSettingsDataStore()` / `provideSecureDataStore()` |
| Adjust scoping | Module files | Use `@Singleton`, `@ActivityScoped`, `@ViewModelScoped` as needed |
| Add a new UseCase | `RepositoryModule.kt` (or separate) | Provide use case that depends on repositories |

## CONVENTIONS
- **Module organization**: Separate modules by responsibility: `AppModule` (persistence), `NetworkModule` (remote), `RepositoryModule` (business logic).
- **Singleton scope**: Most dependencies are `@Singleton` (database, services, repositories).
- **ViewModel injection**: ViewModels are injected with `@HiltViewModel`; no need for explicit provider.
- **Provider naming**: `provide{Type}()` pattern for all provider functions.
- **Context injection**: Use `@ApplicationContext` for application‑wide `Context`.
- **Coroutine dispatchers**: Use `@IoDispatcher` / `@MainDispatcher` qualifiers if needed (not currently used).
- **Testing**: Each module has a corresponding test module (`TestAppModule`, etc.) for instrumented tests.

## ANTI‑PATTERNS
1. **DO NOT** create circular dependencies between modules – keep dependency graph acyclic.
2. **DO NOT** inject `Activity` or `Fragment` context into singleton‑scoped dependencies – use `@ApplicationContext`.
3. **DO NOT** forget to add new `@Module` to `@InstallIn` (usually `SingletonComponent`).
4. **DO NOT** manually instantiate dependencies that should be injected – use Hilt.
5. **DO NOT** expose internal types from modules – expose only public interfaces.
6. **DO NOT** ignore scope mismatches – e.g., don’t inject an `Activity`‑scoped object into a `Singleton`.
7. **DO NOT** duplicate provider logic across modules – centralize common dependencies.

## UNIQUE STYLES
- **Encrypted DataStore provider**: `provideSecureDataStore()` uses `EncryptedSharedPreferences` with `MasterKey`.
- **Database callback injection**: `provideAppDatabase()` includes `DatabaseCallback` that seeds default prompts.
- **Semaphore‑wrapped repository**: `provideAiRepository()` creates `AiRepository` with `Semaphore(permits = 3)`.
- **Retrofit serialization**: `provideRetrofit()` uses `KotlinxSerializationConverterFactory` for JSON.
- **OkHttp logging**: `provideOkHttpClient()` adds `HttpLoggingInterceptor` in debug builds.

## NOTES
- **Hilt component hierarchy**: Modules are installed in `SingletonComponent` (application‑level).
- **ViewModels**: Automatically injected via `@HiltViewModel`; no manual provider needed.
- **Testing**: Use `@UninstallModules` to replace production modules with test doubles.
- **Code generation**: Hilt generates code at compile time; errors appear as `Hilt_*` class issues.
- **Multi‑module readiness**: Currently single‑module app; modules are organized for future multi‑module splitting.
- **Dependency visibility**: All provided dependencies are `internal` to the `di` package; exposed via interface where needed.
