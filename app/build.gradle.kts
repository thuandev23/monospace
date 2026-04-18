import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.monospace.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.monospace.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đặt false khi chưa có backend. Khi backend sẵn sàng đổi thành true.
        buildConfigField("boolean", "BACKEND_ENABLED", "false")
        buildConfigField("String", "BASE_URL", "\"https://api.monospace.app/v1/\"")

        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }
                ?.reader()?.use { load(it) }
        }
        buildConfigField("String", "NOTION_CLIENT_ID", "\"${localProps.getProperty("NOTION_CLIENT_ID", "")}\"")
        buildConfigField("String", "NOTION_CLIENT_SECRET", "\"${localProps.getProperty("NOTION_CLIENT_SECRET", "")}\"")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.places)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler) // generates @HiltWorker entry points
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager
    implementation(libs.androidx.work.runtime)
    androidTestImplementation(libs.androidx.work.testing)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Security & DataStore
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Kotlin Metadata
    implementation(libs.kotlinx.metadata.jvm)
    ksp(libs.kotlinx.metadata.jvm)

    // Glance Widgets
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)

    // Google Play Billing
    implementation(libs.billing)

    // Drag-to-reorder
    implementation(libs.reorderable)
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
