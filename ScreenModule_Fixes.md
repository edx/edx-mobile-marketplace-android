# ScreenModule — Code Audit & Fix Summary

**File:** `app/src/main/java/org/openedx/app/di/ScreenModule.kt`  
**Audit Date:** 6 May 2026  
**Reviewed By:** GitHub Copilot  

---

## 🔴 Critical Issues (Fixed)

| # | Issue | Location | Fix Applied |
|---|-------|----------|-------------|
| 1 | `PipBroadcastReceiverManager` registered as `factory` — creates new instance every injection, making `register()`/`unregister()` lifecycle unmanageable | PiP Data Layer | ✅ Changed to `single` |
| 2 | `PipInteractor` registered as `factory` — creates multiple instances all wrapping the same `single` `PipPlayerRepository`, causing state inconsistency | PiP Domain Layer | ✅ Changed to `single` |
| 3 | `PipBroadcastReceiverManager` uses implicit positional `get()` for context — may resolve wrong context at runtime | PiP Data Layer | ✅ Changed to explicit `androidContext()` |

---

## ⚠️ Medium Issues (Recommendations)

| # | Issue | Location | Recommendation |
|---|-------|----------|----------------|
| 4 | Most ViewModels use unnamed positional `get()` — hard to debug injection failures | All ViewModels | Use named params like `interactor = get()` |
| 5 | `AppViewModel` has 10 positional `get()` params — risk of wrong injection order | `AppViewModel` | Use named params |
| 6 | `SettingsViewModel` has 13 positional `get()` params | `SettingsViewModel` | Use named params |
| 7 | `CourseContainerViewModel` has 19 positional `get()` params | `CourseContainerViewModel` | Consider splitting responsibilities |

---

## ℹ️ Minor Issues

| # | Issue | Location | Recommendation |
|---|-------|----------|----------------|
| 8 | `AuthRepository`, `DashboardRepository` etc. as `factory` — recreated on every injection | Repository Layer | Consider `single` for repositories with network clients |
| 9 | No `checkModules()` test — injection failures only caught at runtime | Test Layer | Add Koin module verification test |
| 10 | `screenModule` is very large (300+ lines) — hard to maintain | Entire file | Split into feature modules e.g. `pipModule`, `courseModule`, `authModule` |

---

## Before vs After (PiP Section)

```kotlin
// ❌ Before
factory { PipBroadcastReceiverManager(get(), get()) }  // wrong scope, implicit context
factory { PipInteractor(get()) }                        // wrong scope

// ✅ After
single {
    PipBroadcastReceiverManager(
        context = androidContext(),    // explicit context
        pipPlayerRepository = get()
    )
}
single { PipInteractor(get()) }       // correct scope
```

---

## Overall Health

| Category | Before | After Fix |
|----------|--------|-----------|
| PiP DI Scoping | ❌ Incorrect | ✅ Fixed |
| Context Injection | ⚠️ Implicit | ✅ Explicit |
| State Consistency | ❌ Risk | ✅ Fixed |
| Build Safety | ✅ Passes | ✅ Passes |
| Readability | ⚠️ Poor | ⚠️ Needs Work |
| Test Coverage | ❌ None | ❌ Recommended |

---

## Recommendations for Future

- [ ] Split `screenModule` into feature-specific modules:
  - `pipModule`
  - `courseModule`
  - `authModule`
  - `profileModule`
  - `discussionModule`
- [ ] Add `checkModules()` Koin test to catch injection failures at build time
- [ ] Use named parameters in all ViewModel injections for clarity
- [ ] Review all `factory` repositories — consider `single` where network client is shared