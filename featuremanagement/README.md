# Feature Management Module

A standalone Gradle module providing a vendor-agnostic feature-flag, A/B-test, and remote-config
framework for your Android app.

## Features

- **Compile-Time Safe Keys**
    - Wrap your string literals in a `FeatureKey` value class.
    - All keys live in `FeatureKeys` for IDE autocompletion and typo protection.

- **Unified Request & Response**
    - Single `FeatureRequest` and `FeatureDecision` model handles flags, experiments, and configs.
    - `FeatureDecision` exposes `.isEnabled`, `.variation`, and a rich `metadata` map.

- **Pluggable Back-Ends**
    - Implement `FeatureService` for any SDK (Optimizely support included).
    - Each service returns `FeatureDecision?` or `null` if it can’t answer.

- **Aggregator Facade**
    - `FeatureManagerImpl` takes a list of `FeatureService` instances and returns the first non-null
      decision.
    - Easily prioritize or combine multiple vendors.

- **Zero SDK Footprint**
    - App/UI code only depends on core abstractions (`FeatureKey`, `FeatureManager`,
      `FeatureDecision`).
    - No direct references to Optimizely or other SDKs in your feature-flag logic.

## Setup

1. **Include modules** in your `settings.gradle`:
   ```groovy
   include ':featuremanagement'
   ```
2. **Add dependencies** in your `app/build.gradle`:
   ```groovy
   dependencies {
     implementation project(path: ':core')
     implementation project(path: ':featuremanagement')
   }
   ```
3. **Provide your configuration** in `core`:
   ```kotlin
   data class Config(
     val optimizelyConfig: OptimizelyConfig
   )
   data class OptimizelyConfig(
     val enabled: Boolean,
     val sdkKey: String
   )
   ```
4. **Configure Koin** in your `Application` class:
   ```kotlin
   class MyApplication : Application() {
     override fun onCreate() {
      super.onCreate()
         val koinModules = listOfNotNull(
         FeatureModuleProvider()
           .takeIf { config.getOptimizelyConfig().enabled }
           .getModules()
         ).flatten()
         loadKoinModules(koinModules)
     }
   }
   ```

## Usage

Inject and use the **core** façade (`FeatureManager`) with `FeatureKey`—no SDK references:

```kotlin
class MyViewModel(
    private val featureManager: FeatureManager,
) : BaseViewModel() {

    fun isDemoFeatureEnabled(): Boolean =
        featureManager
            .getDecision(FeatureKeys.DemoFeature)
            ?.isEnabled
            ?: false

    fun onboardingVariant(): String =
        featureManager
            .getDecision(FeatureKeys.OnboardingExperiment)
            ?.variation
            ?: "control"

    fun refreshInterval(): Int =
        featureManager
            .getDecision(FeatureKeys.RefreshIntervalConfig)
            ?.metadata
            ?.get(FeatureKeys.RefreshIntervalBeta.key)
            ?.toString()
            ?.toIntOrNull()
            ?: 60
}
```

> **Tip:** Always use `FeatureKeys.YourKey` rather than raw strings for safety and discoverability.

## Extending

1. **Add a new back-end**
    - Implement `FeatureService` in this module (e.g. `FirebaseFeatureService`).
    - Register it in `FeatureModuleProvider` (with a qualifier and `createdAtStart = false`).
    - Include it in the `services` list of `FeatureManagerImpl`.

2. **Enrich Metadata**
    - Add extra properties to your `FeatureDecision` data class.
    - In your `FeatureService` implementation, populate the `metadata` map with SDK-specific
      details (e.g. reasons, rule keys).

3. **Add more keys**
    - Define new `FeatureKey(...)` entries in `FeatureKeys`.
    - Use them everywhere—IDE autocomplete will guide usage.
```
