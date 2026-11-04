plugins {
    id("com.android.application")
}

android {
    namespace = "dev.jorgex.teleprompter"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.jorgex.teleprompter"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            // Manual install: the release build is signed with the local debug key.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    dependencies {
        testImplementation("junit:junit:4.13.2")
    }
}
