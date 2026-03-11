# DATA LAYER KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
Data layer responsible for all data operations: local persistence (Room), remote API calls (DeepSeek), and web parsing (Jsoup). Organized into `local/`, `remote/`, and `repository/` subpackages. Follows Repository pattern to abstract data sources from the domain.

## STRUCTURE
```
data/
├── local/                           # Local data sources
│   ├── db/                          # Room database
│   │   ├── dao/                     # Data Access Objects (DAO)
│   │   ├── entity/                  # Room entities
│   │   └── AppDatabase.kt           # Database definition
│   └── datastore/                   # Preferences storage
│       ├── SettingsDataStore.kt     # Plain DataStore for user settings
│       └── SecureDataStore.kt       # Encrypted storage for API key
├── remote/                          # Remote data sources
│   ├── api/                         # Retrofit service interfaces
│   │   ├── DeepSeekApiService.kt    # DeepSeek chat completion
│   │   └── WebParserService.kt      # Web content fetching
│   └── model/                       # API request/response models
└── repository/                      # Repository implementations
    ├── QuestionRepository.kt        # Central question data coordinator
    ├── AiRepository.kt              # AI‑related operations (rate‑limited)
    └── SettingsRepository.kt        # Settings (API key, prompts) management
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new Room entity | `local/db/entity/` | Annotate with `@Entity`; update `AppDatabase` |
| Add a new DAO | `local/db/dao/` | Create `@Dao` interface; add to `AppDatabase` |
| Add a new DataStore | `local/datastore/` | Extend `PreferencesDataStore` or use `EncryptedSharedPreferences` |
| Add a new API endpoint | `remote/api/` | Define Retrofit service; add to `NetworkModule` |
| Add a new request/response model | `remote/model/` | Use `@Serializable` (Kotlinx Serialization) |
| Add a new repository | `repository/` | Inject DAOs and services; expose `Flow`/`suspend` methods |
| Modify database migration | `local/db/` | Increment version; provide `AutoMigration` or `Migration` object |
| Change API rate‑limiting | `repository/AiRepository.kt` | Adjust `Semaphore` permits or retry logic |

## CONVENTIONS
- **DAO design**: Each DAO exposes `Flow<List<T>>` for reactive queries, `suspend` for writes.
- **Entity naming**: Entities end with `Entity` (e.g., `QuestionEntity`). Primary key `id: Long = 0`.
- **Repository interface**: Repositories return domain models (`Question`, `Prompt`, `QuestionLink`), not entities.
- **Error handling**: Use `Result<T>` wrapper for suspend functions that may fail.
- **Concurrency**: Use `Dispatchers.IO` for database/network operations.
- **API key security**: Store in `SecureDataStore`; never log or hard‑code.
- **Rate limiting**: AI API calls guarded by `Semaphore(permits = 3)` to avoid 429 errors.
- **Streaming support**: `AiRepository.generateAnswerStream()` returns `Flow<String>` for incremental answers.

## ANTI‑PATTERNS
1. **DO NOT** expose Room entities outside the data layer – map to domain models first.
2. **DO NOT** perform database/network operations on `Dispatchers.Main` – use `Dispatchers.IO`.
3. **DO NOT** commit unencrypted API keys – always use `SecureDataStore`.
4. **DO NOT** ignore schema versioning – always increment version and provide migration.
5. **DO NOT** write business logic in repositories – delegate to UseCase.
6. **DO NOT** forget to handle `HttpException` (429, 401) in API calls.
7. **DO NOT** create repositories that directly call other repositories – coordinate through a central repository.

## UNIQUE STYLES
- **Semaphore‑based rate limiting**: `AiRepository` uses `kotlinx.coroutines.sync.Semaphore` to cap concurrent API requests.
- **Multi‑strategy JSON parsing**: `AiResponseParser` extracts drill‑down questions from LLM responses (direct JSON, Markdown code block, regex fallback).
- **Web‑parsing fallback detection**: `WebParserService` checks extracted text length; warns if likely SPA page.
- **Encrypted DataStore**: `SecureDataStore` uses Android’s `MasterKey` + `EncryptedSharedPreferences`.
- **Default prompt seeding**: `DatabaseCallback` inserts four default system prompts on first app launch.
- **Batch answer generation**: `AiRepository.generateAnswersBatch()` automatically respects semaphore limits.

## NOTES
- **Room schema export**: Enabled; schemas are written to `app/schemas/`. Must be committed to Git.
- **Database version**: Current version is `1`. Use `AutoMigration` for additive changes (new columns/tables).
- **API base URL**: Configurable via `SettingsRepository`; defaults to `https://api.deepseek.com/`.
- **Retrofit serialization**: Uses Kotlinx Serialization; ensure models are `@Serializable`.
- **Testing**: Use in‑memory database for unit tests; mock API services with `MockK`.
- **Code generation**: After adding/changing entities, run `./gradlew kspKotlin` to regenerate Room code.
