import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun secureProperty(name: String): String? {
    return localProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
}

// Release signing is loaded from ignored local.properties or environment variables.
// Required keys: PHOTO_GRID_RELEASE_STORE_FILE, PHOTO_GRID_RELEASE_STORE_PASSWORD,
// PHOTO_GRID_RELEASE_KEY_ALIAS, PHOTO_GRID_RELEASE_KEY_PASSWORD.
val releaseStoreFilePath = secureProperty("PHOTO_GRID_RELEASE_STORE_FILE")
val releaseStorePassword = secureProperty("PHOTO_GRID_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = secureProperty("PHOTO_GRID_RELEASE_KEY_ALIAS")
val releaseKeyPassword = secureProperty("PHOTO_GRID_RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.photogridplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.photogridplanner"
        minSdk = 26
        targetSdk = 36
        versionCode = 29
        versionName = "1.5.12-beta"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
