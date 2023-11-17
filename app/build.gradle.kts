plugins {
    id("com.android.application")
}

android {
    namespace = "com.luanon.seledroid.chromium"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.luanon.seledroid.chromium"
        minSdk = 24
        //noinspection EditedTargetSdkVersion
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.webkit:webkit:1.8.0")
}