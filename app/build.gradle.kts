import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Версия и код версии задаются в gradle.properties (appVersion=1.20, appVersionCode=120):
// одинаково на любом сервере сборки, в том числе в F-Droid.
val appVersion = providers.gradleProperty("appVersion").get()
val appVersionCode = providers.gradleProperty("appVersionCode").get().toInt()

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
        versionCode = appVersionCode
        versionName = appVersion
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

    // full — GitHub и IzzyOnDroid: вход в Google Диск (сервисы Google Play) и проверка обновлений.
    // fdroid — для F-Droid: только свободные библиотеки, без сети; копия на Диск — файлом через «Сохранить в…».
    flavorDimensions += "distribution"
    productFlavors {
        create("full") { dimension = "distribution" }
        create("fdroid") { dimension = "distribution" }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Без ключа релиз собирается неподписанным — так его подписывает F-Droid.
            signingConfig = if (signingStoreFile != null) signingConfigs.getByName("sonnik") else null
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
    // Зашифрованный блок Google со списком зависимостей не нужен и мешает F-Droid / IzzyOnDroid.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
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
    // Автокопии в фоне; вход в Google (только область drive.file) — лишь в сборке full.
    implementation(libs.androidx.work.runtime.ktx)
    "fullImplementation"(libs.play.services.auth)
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
    timeout.set(Duration.ofMinutes(8))
    testLogging {
        events("started", "passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}
