import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-parcelize")
}

android {
    namespace = "org.openedx.notifications"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
            freeCompilerArgs.addAll(listOf(
                    "-Xstring-concat=inline",
                    "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
            ))
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    flavorDimensions.add("env") // Define the flavor dimension
    productFlavors {
        create("prod") {
            dimension = "env"
        }
        create("develop") {
            dimension = "env"
        }
        create("stage") {
            dimension = "env"
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.activity:activity-compose:1.8.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    testImplementation("junit:junit:${rootProject.extra["junit_version"]}")
    testImplementation("io.mockk:mockk:${rootProject.extra["mockk_version"]}")
    testImplementation("androidx.arch.core:core-testing:${rootProject.extra["android_arch_version"]}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
