plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.bbks.mydailytracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bbks.mydailytracker"
        minSdk = 24
        targetSdk = 35
        versionCode = 29
        versionName = "1.1.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation("androidx.compose.ui:ui-graphics")
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.lifecycle:lifecycle-process:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.play.services.ads)
    implementation(libs.sheets.dialogs.core)
    implementation(libs.sheets.dialogs.calendar)
    implementation("androidx.compose.foundation:foundation")

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("com.android.billingclient:billing:7.0.0")

    // Fragment를 최소 1.3.0 이상(권장 최신)으로 명시 고정
    implementation("androidx.fragment:fragment-ktx:1.8.6")

    // Activity Result 관련 API도 최신 계열 권장 (compose 쪽도 함께 올림)
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    kapt(libs.room.compiler)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
