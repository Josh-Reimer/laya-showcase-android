plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.layaplayground.showcase"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.layaplayground.showcase"
        // 26: java.nio.file (Files/Path) that the vendored laya-java module uses natively lands
        // here; core library desugaring below backports the Java 9-11 String/Collection/time
        // methods it also uses, but NIO.2 itself is only in the desugar_jdk_libs_nio variant from
        // this floor up, so there is no desugaring path below 26 worth taking instead.
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "2.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // The bundled model files are already-compressed binary blobs; zip-compressing them again in
    // the APK only slows packaging and install, and onnx in particular needs to be readable as a
    // normal random-access file, not an APK-compressed entry ONNX Runtime cannot mmap directly.
    androidResources {
        noCompress += listOf("onnx", "data", "json")
    }
}

kotlin {
    sourceSets["main"].kotlin.srcDir("src/main/kotlin")
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // The on-device decision engine: vendored laya-java (see laya-java/build.gradle.kts for why
    // it isn't a Maven dependency) runs on top of ONNX Runtime's Android build. Same `ai.onnxruntime`
    // classes as the desktop jar laya-java compiles against, so the two are interchangeable here.
    implementation(project(":laya-java"))
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.4")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
