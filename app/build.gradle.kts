import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use(::load)
}

fun signingProp(propKey: String, envKey: String): String? =
    localProps.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFilePath = signingProp("release.storeFile", "ACTIARY_STORE_FILE")
val releaseStorePassword = signingProp("release.storePassword", "ACTIARY_STORE_PASSWORD")
val releaseKeyAlias = signingProp("release.keyAlias", "ACTIARY_KEY_ALIAS")
val releaseKeyPassword = signingProp("release.keyPassword", "ACTIARY_KEY_PASSWORD")
val releaseKeystoreFile = releaseStoreFilePath?.let { rootProject.file(it) }?.takeIf { it.isFile }
val hasReleaseSigning = releaseKeystoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "com.actiary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.actiary"
        minSdk = 31
        targetSdk = 35
        versionCode = 36
        versionName = "1.5.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Uses the local release keystore when configured in local.properties / env vars;
            // falls back to debug signing so contributors can build locally without private keys.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.calendar.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.core)
    implementation(libs.mlkit.text.chinese)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.room.testing)
}
