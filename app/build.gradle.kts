import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Load keystore.properties (optional for release signing)
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasKeystore = keystorePropertiesFile.exists()
if (hasKeystore) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

// Function to get property safely
fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key) ?: defaultValue
}

val apiBaseUrl: String = getLocalProperty(
    "API_BASE_URL",
    "https://api.sbm-app.com/api/v1/"
)

android {
    namespace = "com.sbm.application"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (hasKeystore) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
        create("debugProd") {
            isDebuggable = true
            applicationIdSuffix = ".debugprod"
            signingConfig = signingConfigs.getByName("debug")
            // 本番環境の設定を使用
            buildConfigField("String", "API_BASE_URL", "\"https://api.sbm-app.com/api/v1/\"")
        }
        release {
            isMinifyEnabled = false  // ProGuard無効化
            isShrinkResources = false  // リソース圧縮無効化
            
            // リリースビルドで本番APIを使用
            buildConfigField("String", "API_BASE_URL", "\"https://api.sbm-app.com/api/v1/\"")
            
            if (hasKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    defaultConfig {
        applicationId = "com.sbm.application"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // BuildConfigフィールドとしてAPI_BASE_URLを設定
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        
        // AdMob設定
        buildConfigField("String", "ADMOB_APP_ID", "\"${localProperties.getProperty("admob.app.id") ?: ""}\"")
        buildConfigField("String", "ADMOB_BANNER_ID", "\"${localProperties.getProperty("admob.banner.id") ?: ""}\"")
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Manifest placeholders for AdMob ID
        manifestPlaceholders["admobAppId"] = localProperties.getProperty("admob.app.id") ?: ""


    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-compiler:2.48")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // JWT
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Browser
    implementation("androidx.browser:browser:1.7.0")
    
    // Calendar
    implementation("io.github.boguszpawlowski.composecalendar:composecalendar:1.2.0")
    implementation("io.github.boguszpawlowski.composecalendar:kotlinx-datetime:1.2.0")
    
    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:22.6.0")
    
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Core library desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
}
