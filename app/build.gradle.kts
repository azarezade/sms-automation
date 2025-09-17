plugins { 
    id("com.android.application")
    kotlin("android") 
}

android {
    namespace = "com.example.smsmvp"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.example.smsmvp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // For SMS broadcast on modern Android
        resValue("string", "app_name", "SMS MVP")
    }
    
    buildTypes {
        getByName("debug") { 
            isMinifyEnabled = false 
        }
        getByName("release") { 
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), 
                "proguard-rules.pro"
            ) 
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

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
