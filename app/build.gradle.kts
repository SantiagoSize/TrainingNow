plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.5"
}

android {
    namespace = "com.shagox.apptrainingnow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shagox.apptrainingnow"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Host de los microservicios: 10.0.2.2=emulador, 192.168.x.x=dispositivo físico
        val apiHost = project.findProperty("API_HOST") ?: "10.0.2.2"
        buildConfigField("String", "API_HOST", "\"$apiHost\"")
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

// Configuración de KSP para Room
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //librerias nuevas
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    // Material icons (necesarios para Visibility / VisibilityOff)
    implementation("androidx.compose.material:material-icons-extended")

    // Room (SQLite) - runtime y extensiones KTX
    implementation("androidx.room:room-runtime:2.8.4")    // <-- NUEVO
    implementation("androidx.room:room-ktx:2.8.4")        // <-- NUEVO

    // Compilador de Room vía KSP
    ksp("androidx.room:room-compiler:2.8.4")               // <-- NUEVO

    //cargar imagenes (para mostrarlas en la UI)
    implementation("io.coil-kt:coil-compose:2.7.0")
    // Lectura de orientación EXIF para comprimir imágenes correctamente
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    //Data Store Preferencia
    implementation("androidx.datastore:datastore-preferences:1.2.0")

    // ==== AGREGADOS PARA REST ====
    // Retrofit base
    implementation("com.squareup.retrofit2:retrofit:3.0.0") // <-- NUEVO
    // Convertidor JSON con Gson
    implementation("com.squareup.retrofit2:converter-gson:3.0.0") // <-- NUEVO
    // OkHttp y logging interceptor
    implementation("com.squareup.okhttp3:okhttp:5.3.2") // <-- NUEVO
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2") // <-- NUEVO

    //Test locales
    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    //test UI
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
}