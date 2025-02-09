plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.anew"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.anew"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // 高德定位 API
    implementation("com.amap.api:location:6.0.0")

    // Android 基础库
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("com.google.android.material:material:1.4.0")

    // **添加 JUnit 依赖**
    testImplementation("junit:junit:4.13.2") // 用于普通单元测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // 用于 Android Instrumentation 测试
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // UI 测试框架
}
