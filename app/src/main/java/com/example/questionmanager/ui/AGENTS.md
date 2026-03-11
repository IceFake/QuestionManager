# UI LAYER KNOWLEDGE BASE

**Generated:** 2026-03-10
**Commit:** dc0b28e
**Branch:** master

## OVERVIEW
UI layer built with Jetpack Compose, following MVVM pattern. Screens are Composable functions observing `StateFlow` from ViewModels. Navigation uses Navigation Compose with a single‑Activity architecture. Theming follows Material 3.

## STRUCTURE
```
ui/
├── screen/                          # App screens (one per feature)
│   ├── home/                        # Home screen (question list)
│   ├── detail/                      # Question detail screen
│   ├── input/                       # Input screen (URL/manual)
│   ├── search/                      # Search screen
│   ├── drilldown/                   # Drill‑down selection screen
│   └── settings/                    # Settings screen (API key, prompts)
├── component/                       # Reusable UI components
│   ├── QuestionCard.kt              # Card displaying a question
│   ├── AnswerSection.kt             # Answer display with Markdown
│   ├── LoadingIndicator.kt          # Progress indicator
│   ├── SearchBar.kt                 Search bar with debounce
│   └── LinkedQuestionChip.kt        # Chip for linked questions
├── navigation/                      # Navigation graph
│   └── NavGraph.kt                  # Route definitions and composable destinations
└── theme/                           # Material 3 theme definitions
    ├── Color.kt                     # Color palette
    ├── Type.kt                      # Typography scale
    └── Theme.kt                     # Composable theme provider
```

## WHERE TO LOOK
| Task | Location | Notes |
|------|----------|-------|
| Add a new screen | `screen/{name}/` | Create `{Name}Screen.kt` + `{Name}ViewModel.kt` |
| Add a new UI component | `component/` | Stateless Composable with `@Composable` annotation |
| Modify navigation | `navigation/` | Update `NavGraph.kt` with new route |
| Change theme | `theme/` | Edit `Color.kt`, `Type.kt`, or `Theme.kt` |
| Add a new ViewModel | `screen/{name}/` | Inject repositories/use cases; expose `StateFlow` |
| Modify existing screen | `screen/{name}/` | Update UI layout or state handling |

## CONVENTIONS
- **Screen structure**: Each screen directory contains a `*Screen.kt` (Composable) and `*ViewModel.kt` (ViewModel).
- **ViewModel exposure**: ViewModel exposes `StateFlow<UiState>`; UI collects with `collectAsState()`.
- **UI state**: `UiState` is a `data class` with all observable state (loading, error, data).
- **Event handling**: ViewModel provides `fun onEvent(event: Event)` for UI actions.
- **Navigation**: Use `NavGraph` for route definitions; pass arguments via `NavBackStackEntry`.
- **Theming**: Use `MaterialTheme` with custom colors/typography from `theme/` package.
- **Component design**: UI components are stateless; receive data via parameters and callbacks.
- **Error display**: Show errors via `Snackbar` or inline message in `UiState`.
- **Loading states**: Show `LoadingIndicator` when `UiState.isLoading == true`.

## ANTI‑PATTERNS
1. **DO NOT** put business logic in Composable – delegate to ViewModel.
2. **DO NOT** access repositories directly from UI – use ViewModel as intermediary.
3. **DO NOT** use `remember` for business data – store in ViewModel’s state.
4. **DO NOT** hard‑code strings – use resources or `Constants.kt`.
5. **DO NOT** ignore configuration changes – use ViewModel to survive rotation.
6. **DO NOT** create deep nested Composable hierarchies – extract sub‑components.
7. **DO NOT** forget to handle `LaunchedEffect` side‑effect cancellation.
8. **DO NOT** perform heavy computation on UI thread – use `LaunchedEffect` + `Dispatchers.Default`.

## UNIQUE STYLES
- **Markdown rendering**: `AnswerSection` uses a Markdown renderer (likely `androidx.compose.material3:material3`) for AI answers.
- **Debounced search**: `SearchBar` automatically debounces input (300ms) before triggering search.
- **Linked question chips**: `LinkedQuestionChip` displays parent/child links as clickable chips.
- **Loading skeleton**: `QuestionCard` shows a skeleton placeholder while data loads.
- **Theme switching**: Theming supports light/dark mode via `MaterialTheme` color scheme.
- **Streaming answer UI**: `AnswerSection` can display incremental answer updates via `Flow<String>`.

## NOTES
- **Navigation arguments**: Pass `questionId` via `detail/{questionId}`; retrieve with `SavedStateHandle`.
- **ViewModel scope**: ViewModels are scoped to the navigation graph destination (default).
- **Hilt integration**: ViewModels are injected with `@HiltViewModel`; screens annotated with `@AndroidEntryPoint`.
- **Testing**: Use `ComposeTestRule` for UI tests; `ViewModel` can be unit‑tested with mocked dependencies.
- **Preview support**: Composable functions should have `@Preview` annotations for design‑time rendering.
- **Accessibility**: Ensure components support content description, focus order, and touch targets.
