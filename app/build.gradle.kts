plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// На GitHub Actions номер версии растёт с каждой сборкой, чтобы APK ставился поверх старого.
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

// Ключ подписи передаётся через переменные окружения (секреты GitHub Actions),
// чтобы все сборки были подписаны одинаково и обновляли установленное приложение.
val signingStoreFile = System.getenv("SIGNING_STORE_FILE")

android {
    namespace = "com.sonnik.topower"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sonnik.topower"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNumber
        versionName = providers.gradleProperty("appVersion").get()
    }

    signingConfigs {
        if (signingStoreFile != null) {
            create("sonnik") {
                storeFile = file(signingStoreFile)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (signingStoreFile != null) {
                signingConfigs.getByName("sonnik")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // Журнал изменений (changelog/*.md) встраивается в приложение.
    sourceSets.getByName("main").assets.srcDir("$rootDir/changelog")
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
}

// Зависший тест не должен держать сборку бесконечно.
tasks.withType<Test>().configureEach {
    timeout.set(java.time.Duration.ofMinutes(8))
}
