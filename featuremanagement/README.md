# Feature Management Module

A standalone Gradle module providing a vendor-agnostic feature-flag, A/B-test, and remote-config framework for your Android app.

## Features

- **Type-safe Requests & Responses**
    - Sealed `FeatureRequest<T>` and `FeatureDecision<T>` ensure compile-time safety for Boolean, String, or numeric returns.
- **Pluggable Back-Ends**
    - Implement `FeatureManagementService` for any SDK. Out-of-the-box Optimizely support included.
- **Aggregator Facade**
    - `FeatureManagerImpl` holds one or more services and returns the first non-null decision.
- **Zero SDK Footprint in Feature Modules**
    - UI code depends only on core abstractions—no direct references to Optimizely or other SDKs.

## Setup

1. **Include modules** in your `settings.gradle`:
   ```groovy
   include ':featuremanagement'
   ```
2. **Add dependencies** in `app/build.gradle`:
   ```groovy
   dependencies {
     implementation project(path: ':core')
     implementation project(path: ':featuremanagement')
   }
   ```
3. **Provide your configuration** (in Core or App):
   ```kotlin
   data class Config(
     val optimizelyConfig: OptimizelyConfig,
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
       val koinProviders = listOfNotNull(
         FeatureManagementModuleProvider().takeIf { config.getOptimizelyConfig().enabled }
       )
       loadKoinModules(koinProviders.flatMap { it.getModules() })
     }
   }
   ```

## Usage

Inject and use the **core** façade (`FeatureManager`) anywhere—no SDK references:

```kotlin
class MyViewModel(
  private val featureManager: FeatureManager,
) : ViewModel() {

  fun isBannerEnabled(): Boolean =
    featureManager
      .getDecision(FeatureFlagRequest("show_banner"))
      ?.value
      ?: false

  fun bannerVariant(): String =
    featureManager
      .getDecision(AbTestRequest("banner_color_test"))
      ?.value
      ?: "A"

  fun refreshInterval(): Int =
    featureManager
       .getDecision(RemoteConfigRequest("refresh_interval", flagKey = "beta"))
       ?.value
       ?.toInt() 
       ?: 60
}
```

## Extending

1. **Add a new back-end**:
    - Implement `FeatureManagementService` in this module (e.g., `FirebaseFeatureManagementService`).
    - Register it in `FeatureManagementModuleProvider` and include it in `FeatureManagerImpl`.
2. **Enrich Metadata**:
    - Add fields to your `FeatureDecision` subclasses and populate `metadata` in your implementation.
