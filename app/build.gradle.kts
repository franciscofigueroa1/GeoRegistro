import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val claves = Properties()
val archivoClaves = rootProject.file("secrets.properties")
if (archivoClaves.exists()) {
    archivoClaves.inputStream().use { claves.load(it) }
}
val claveMaps = claves.getProperty("MAPS_API_KEY", "").trim()

android {
    namespace = "cl.francisco.georegistro"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.francisco.georegistro"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        manifestPlaceholders["MAPS_API_KEY"] = claveMaps.ifEmpty { "NO_CONFIGURADO" }
        buildConfigField("boolean", "MAPA_CONFIGURADO", claveMaps.isNotEmpty().toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:35.0.2"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
