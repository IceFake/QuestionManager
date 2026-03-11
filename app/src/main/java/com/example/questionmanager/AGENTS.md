AGENTS for root package: com.example.questionmanager

OVERVIEW
- Root Kotlin package; aligns with project-wide architecture; implements Clean Architecture with MVVM.
- Data layer via data/, domain layer via domain/, UI via ui/, DI via di/, util via util/.
- Repositories abstract data sources; Room persistence; Retrofit networking; Compose UI; Hilt DI; Kotlin coroutines + Flow.

LAYER STRUCTURE
- data/: local/db, remote/, repository/
- domain/: model/, usecase/
- ui/: screen/, component/, navigation/, theme/
- di/: modules
- util/: shared utilities

WHERE TO LOOK (by layer)
- data
  - local/db: Entities, DAOs, AppDatabase; migrations
  - remote: Retrofit interfaces, data models
  - repository: QuestionRepository, AiRepository; data coordination and caching
- domain
  - model: domain representations; mappers to/from entities
  - usecase: business logic; orchestrations; boundaries with repositories
- ui
  - screen: Compose screens + ScreenViewModel
  - component: Reusable UI components
  - navigation: NavHost, graph builders
  - theme: Material theme definitions
- di
  - modules: Hilt @Module providers; scopes per component
- util
  - core: extensions, helpers
  - constants: app-wide constants

CONVENTIONS (package-specific)
- Naming: Entity/Dao/Repository/UseCase/ViewModel/Screen suffix conventions
- DI: Hilt modules with @Module/@InstallIn; avoid direct instantiation in UI
- Data<->Domain: mappers between data layer models and domain models
- IO: suspend + withContext(Dispatchers.IO); Flow for streams
- Room: schema export enabled; migrations tracked
- Retrofit: defined API interfaces; models separated from domain
- Compose: UI code in ui subpackages; state in ViewModels

NOTES
- This AGENTS.md reflects package-level responsibilities; refer to root AGENTS.md for project-wide guidelines.
- Do not duplicate root/app AGENTS.md content; keep concise and actionable.
- File lives under the root package directory as a Kotlin/Markdown doc to guide new contributors.