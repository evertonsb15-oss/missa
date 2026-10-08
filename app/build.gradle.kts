plugins {
    id("com.android.application")
}

android {
    namespace = "br.com.canticos.outubromissionario"
    compileSdk = 35

    defaultConfig {
        applicationId = "br.com.canticos.outubromissionario"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "2.1"
    }
}


dependencies {
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
}
