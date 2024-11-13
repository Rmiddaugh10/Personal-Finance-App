plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.1.0-Beta2"
    id("com.google.devtools.ksp") version "2.1.0-Beta2-1.0.26"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
   // id("dagger.hilt.android.plugin")
  //  id("kotlin-kapt") // Correct placement
}




android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        buildConfigField("String", "TELLER_APPLICATION_ID", "\"${project.properties["TELLER_APP_ID"]}\"")
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
  //  kapt {
  //      correctErrorTypes = true
  //  }
}

dependencies {
    // Core and lifecycle dependencies
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("com.github.PhilJay:MPAndroidChart:3.1.0") // MPAndroidChart dependency
    implementation("com.opencsv:opencsv:5.7.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.36.0")
   // implementation("io.teller:connect-android-sdk:0.1.0")
    implementation ("androidx.security:security-crypto:1.1.0-alpha06")
    implementation ("androidx.compose.material3:material3:1.3.1")
  //  implementation ("com.google.dagger:hilt-android:2.49")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0") // or the latest version
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.datastore:datastore-preferences-core:1.1.1")


    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.6.0")

    // Retrofit
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Gson (if not already included)
    implementation ("com.google.code.gson:gson:2.10.1")
    // Room dependencies
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion") // Use KSP for Room
    implementation("androidx.room:room-ktx:2.6.1") // Ensure the version matches your other Room dependencies

    // Compose-related dependencies
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.ui:ui-graphics:1.7.5")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation ("androidx.compose.material:material:1.7.5")

    // Accompanist for system UI controller
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.27.0")

    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Add the missing Compose UI testing dependencies
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.5")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.7.5")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}

