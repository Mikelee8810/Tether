plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val tetherVersionName = providers.environmentVariable("TETHER_VERSION_NAME").orNull ?: "0.1.0"
val tetherVersionCode = providers.environmentVariable("TETHER_VERSION_CODE").orNull?.toIntOrNull() ?: 1
val tetherKeystoreFile = providers.environmentVariable("TETHER_KEYSTORE_FILE").orNull
val tetherKeystorePassword = providers.environmentVariable("TETHER_KEYSTORE_PASSWORD").orNull

android {
    namespace = "com.relationshipradar.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.relationshipradar.app"
        minSdk = 31
        targetSdk = 37
        versionCode = tetherVersionCode
        versionName = tetherVersionName
    }

    val tetherReleaseSigning = if (tetherKeystoreFile != null && tetherKeystorePassword != null) {
        signingConfigs.create("tetherRelease") {
            storeFile = file(tetherKeystoreFile)
            storePassword = tetherKeystorePassword
            keyAlias = "tether"
            keyPassword = tetherKeystorePassword
        }
    } else {
        null
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            tetherReleaseSigning?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":whispercpp"))
    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
