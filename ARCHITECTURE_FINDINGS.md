# Architecture Findings & Recommendations

**Reviewed by:** Senior Android Developer  
**Date:** 22 April 2026  
**Scope:** ARCHITECTURE.md & Project Structure Review

---

## Table of Contents

1. [Critical Findings](#critical-findings)
2. [Potential Future Breaks](#potential-future-breaks)
3. [Suggestions & Improvements](#suggestions--improvements)
4. [DI Concerns](#di-concerns)
5. [State Management Issues](#state-management-issues)
6. [Module Structure Risks](#module-structure-risks)
7. [Testing Gaps](#testing-gaps)
8. [Migration & Deprecation Risks](#migration--deprecation-risks)

---

## Critical Findings

### 1. Mixed Build Script Languages (Groovy + Kotlin DSL)

**Finding:** The project uses `build.gradle` (Groovy) for most modules but `build.gradle.kts` (Kotlin DSL) for `featuremanagement` and `notifications`. This inconsistency will cause:

- Developer confusion when switching between modules
- Different syntax for the same dependency declarations
- Harder to maintain shared build logic (convention plugins)

**Recommendation:** Migrate all modules to `build.gradle.kts` in a single coordinated effort. Use a version catalog (`libs.versions.toml`) to centralize dependency versions.

---

### 2. Notifier Pattern is an Untyped Event Bus

**Finding:** The `Notifier` pattern (e.g., `CourseNotifier`, `VideoNotifier`, `DownloadNotifier`) is essentially a global event bus using `MutableSharedFlow`. This has known issues:

- **No backpressure guarantees** — `extraBufferCapacity = 0` means events can be dropped if no collector is active.
- **No delivery guarantee** — If a ViewModel hasn't started collecting yet, it misses the event entirely.
- **Hard to trace** — Events are fire-and-forget; debugging event flows across modules is extremely difficult.
- **Implicit coupling** — Modules appear decoupled but are tightly coupled through event contracts.

**Future Break:** Adding new event types to a sealed class forces recompilation of all consumers. As the project grows, Notifier sealed classes become god-objects.

**Recommendation:**
- Move to a mediator/coordinator pattern in the `app` module instead of global event buses.
- For guaranteed delivery, consider `Channel` (rendezvous or buffered) instead of `SharedFlow` with zero buffer.
- Evaluate Android's `Navigation` result APIs or `SavedStateHandle` for inter-screen communication.

---

### 3. Repository Error Handling Strategy is Risky

**Finding:** The architecture doc recommends handling errors in the Repository layer (Best Practice #7). While the intent is good, this creates problems:

- The Repository swallows exceptions and silently falls back to cache. The ViewModel has **no way to know** the API call failed.
- UI cannot distinguish between "showing cached data because offline" vs "showing fresh data."
- Stale data is served without any staleness indicator.

**Recommendation:** Use a `Result<T>` or `Resource<T>` wrapper:
```kotlin
sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class CachedWithError<T>(val data: T, val error: Throwable) : Resource<T>()
    data class Error<T>(val error: Throwable) : Resource<T>()
    class Loading<T> : Resource<T>()
}
```
This allows the UI to show cached data AND inform the user that a refresh failed.

---

## Potential Future Breaks

### 4. Koin Runtime DI — No Compile-Time Safety

**Finding:** Koin resolves dependencies at runtime. If a dependency is missing or misconfigured:

- The app **crashes at runtime**, not at compile time.
- Feature flag toggling that loads/unloads Koin modules can leave dangling references.
- Lazy module loading (`loadKoinModules`) with feature flags is fragile — if module A depends on module B, and B is disabled by a flag, the app crashes.

**Future Break:** As the number of feature-flagged modules grows, the combinatorial explosion of enabled/disabled states becomes untestable.

**Recommendation:**
- Add Koin's `checkModules()` in CI/CD pipeline test suites.
- Consider migrating to **Hilt** for compile-time DI verification — especially as the project grows.
- At minimum, write integration tests that boot all feature flag combinations.

---

### 5. `channelFlowWithAwait` is Not a Standard API

**Finding:** The caching example uses `channelFlowWithAwait`, which is **not** a standard Kotlin coroutines API. This appears to be a custom extension.

- If this is a custom utility, it must be well-tested and documented.
- If it's from a third-party library, it may break on coroutines version upgrades.

**Recommendation:** Verify the source of `channelFlowWithAwait`. Consider using standard `channelFlow` or `flow { }` builders for portability.

---

### 6. Gson is Deprecated in Favor of Kotlin Serialization

**Finding:** The project uses Gson for JSON serialization. Gson has known issues with Kotlin:

- No support for Kotlin default parameter values
- No support for non-nullable types (Gson can set a non-null Kotlin property to `null`)
- No support for `sealed class` deserialization without custom adapters
- Performance is worse than kotlinx.serialization

**Future Break:** As the project migrates more to Kotlin-idiomatic patterns, Gson will cause subtle null-safety bugs at runtime.

**Recommendation:** Plan a phased migration to `kotlinx.serialization`. Start with new modules and gradually migrate existing ones.

---

### 7. SharedPreferences Scalability

**Finding:** `PreferencesManager` wraps `SharedPreferences`. As the app grows:

- SharedPreferences is fully loaded into memory on first access.
- All writes are serialized — performance degrades with large datasets.
- No type safety — wrong key or type causes silent failures.

**Recommendation:** Migrate to Jetpack `DataStore` (Preferences or Proto). It provides:
- Coroutine-based async access
- Type safety (Proto DataStore)
- No UI thread blocking

---

## Suggestions & Improvements

### 8. Missing Error Boundary / Global Error Handler

**Finding:** There is no documented strategy for global error handling. Individual ViewModels catch errors independently, leading to:

- Inconsistent error messages across screens
- No centralized error reporting beyond Crashlytics
- Token expiry / 401 errors may not be handled uniformly

**Recommendation:** Implement a global `ErrorHandler` in `core`:
```kotlin
class GlobalErrorHandler @Inject constructor(
    private val authNotifier: AuthNotifier,
    private val analytics: AnalyticsManager,
) {
    fun handle(error: Throwable): ErrorAction {
        return when (error) {
            is HttpException -> when (error.code()) {
                401 -> { authNotifier.send(SessionExpired); ErrorAction.LOGOUT }
                503 -> ErrorAction.SHOW_MAINTENANCE
                else -> ErrorAction.SHOW_GENERIC
            }
            is IOException -> ErrorAction.SHOW_OFFLINE
            else -> ErrorAction.SHOW_GENERIC
        }
    }
}
```

---

### 9. No Offline-First Strategy Documented

**Finding:** The 3-level caching (Memory → DB → API) is described, but there is no documented strategy for:

- Offline write operations (enqueue and sync later)
- Conflict resolution when syncing
- Cache invalidation policies (TTL, version-based)
- Database migration strategy for Room schema changes

**Recommendation:** Document an explicit offline-first strategy. Consider WorkManager for deferred sync operations.

---

### 10. No Compose Navigation Strategy

**Finding:** The project uses Jetpack Navigation Component with Fragments, and is gradually adopting Compose. There is no documented strategy for:

- How Compose screens integrate with Fragment-based navigation
- Whether the project will migrate to Compose Navigation
- How `Router` interfaces will evolve for Compose destinations

**Future Break:** Maintaining two navigation systems (Fragment Nav + Compose Nav) leads to duplicated navigation logic and route definitions.

**Recommendation:** Define a clear roadmap: either commit to Compose Navigation for new features, or define a `Router` abstraction that works for both.

---

### 11. No Pagination Strategy

**Finding:** No documented pagination pattern for lists (enrolled courses, discussions, search results). Without a standard:

- Each feature implements pagination differently
- No reuse of paging logic
- Risk of memory issues with large lists

**Recommendation:** Adopt Jetpack `Paging 3` library as the standard. Create a base paging source in `core`.

---

### 12. No Accessibility (a11y) Architecture

**Finding:** No documented accessibility patterns. For an education platform, this is a significant gap that could lead to:

- Legal compliance issues (ADA, WCAG)
- Poor experience for users with disabilities
- Retrofit accessibility is much harder than building it in

**Recommendation:** Add accessibility guidelines to the architecture doc. Define semantic descriptions, content descriptions, and focus order patterns.

---

## DI Concerns

### 13. ScreenModule is a God Module

**Finding:** All ViewModel definitions are in a single `ScreenModule.kt`. As the project grows:

- This file becomes unmaintainably large
- All modules are coupled through a single DI file
- Feature teams step on each other when modifying it

**Recommendation:** Each feature module should declare its own Koin module. The `app` module should only aggregate them:
```kotlin
// In each feature module
val courseModule = module {
    viewModel { CourseOutlineViewModel(get(), get()) }
    factory { CourseInteractor(get()) }
}

// In app module
startKoin {
    modules(coreModule, courseModule, dashboardModule, ...)
}
```

---

### 14. Interactors as Factory — Wasteful

**Finding:** Interactors are defined as `factory` (new instance every time). If an Interactor holds no mutable state (which it shouldn't), creating a new instance per injection is wasteful.

**Recommendation:** Use `single` for stateless Interactors, `factory` only for stateful ones.

---

## State Management Issues

### 15. No Loading + Data Composite State

**Finding:** The sealed UIState pattern (`Loading | Data | Error`) doesn't support showing data while refreshing (pull-to-refresh, background sync). The UI must choose between showing a loader OR showing data.

**Recommendation:** Allow composite states:
```kotlin
sealed class UIState {
    object InitialLoading : UIState()
    data class Data(
        val content: CourseStructure,
        val isRefreshing: Boolean = false,
        val error: String? = null, // non-fatal error overlay
    ) : UIState()
    data class FatalError(val message: String) : UIState()
}
```

---

### 16. LiveData Still Used for Shared State

**Finding:** `SharedViewModel` uses `LiveData` while the rest of the project uses `StateFlow`. Mixing reactive types causes confusion and inconsistent collection patterns.

**Recommendation:** Standardize on `StateFlow` everywhere. Replace `LiveData` with `StateFlow` in shared ViewModels.

---

## Testing Gaps

### 17. No Integration Test Strategy

**Finding:** Only unit tests are documented. There is no strategy for:

- Repository integration tests (API + DB together)
- DI module verification tests (`checkModules()`)
- End-to-end UI tests (Espresso / Compose Testing)

**Recommendation:** Add at least:
- Koin `checkModules()` test for every DI configuration
- Repository tests with `MockWebServer` + in-memory Room DB
- Critical path UI tests with Compose test rules

---

### 18. No Test Fixtures / Fake Data Strategy

**Finding:** No documented approach for shared test data (fake courses, fake users). Each test file likely creates its own mocks independently.

**Recommendation:** Create a `testFixtures` source set in `core` with reusable fake data builders.

---

## Migration & Deprecation Risks

| Component | Current | Risk | Suggested Migration |
|-----------|---------|------|-------------------|
| Gson | Active | Kotlin null-safety bypassed | `kotlinx.serialization` |
| SharedPreferences | Active | Blocking I/O on main thread | Jetpack DataStore |
| XML Layouts | Active | Dual UI system maintenance | Jetpack Compose |
| LiveData (partial) | Active | Mixed with StateFlow | StateFlow everywhere |
| JUnit 4 | Active | Missing coroutine test support | JUnit 5 + Turbine |
| ExoPlayer | Active | Migrating to Media3 | AndroidX Media3 |

---

## Summary Priority Matrix

| Priority | Finding | Impact | Effort |
|----------|---------|--------|--------|
| 🔴 High | Notifier event drops (#2) | Data loss, missed updates | Medium |
| 🔴 High | No compile-time DI safety (#4) | Runtime crashes | High |
| 🔴 High | Gson null-safety (#6) | Silent runtime bugs | High |
| 🟡 Medium | Mixed build scripts (#1) | Developer friction | Medium |
| 🟡 Medium | Error handling gaps (#8) | Inconsistent UX | Medium |
| 🟡 Medium | ScreenModule god module (#13) | Merge conflicts, coupling | Low |
| 🟡 Medium | No offline-first strategy (#9) | Poor offline UX | High |
| 🟢 Low | SharedPreferences (#7) | Performance at scale | Medium |
| 🟢 Low | Compose navigation (#10) | Future migration cost | Low |
| 🟢 Low | Pagination (#11) | Memory, inconsistency | Medium |

---

**Document Version:** 1.0  
**Last Updated:** 22 April 2026  
**Author:** Senior Android Developer — Architecture Review
