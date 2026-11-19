import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)


}

val propiedadesLocales = Properties().apply {
    val archivo = rootProject.file("local.properties")
    if (archivo.exists()) archivo.inputStream().use { load(it) }
}

android {
    namespace = "com.abarca.tallermecanico"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.abarca.tallermecanico"
        minSdk = 24
        targetSdk = 37
        versionCode = 9
        versionName = "4.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val webpayTestUrl = propiedadesLocales.getProperty("webpay.testUrl", "http://127.0.0.1:8787")
        require(webpayTestUrl.matches(Regex("https?://[a-zA-Z0-9.:-]+/?")))
        buildConfigField("String", "WEBPAY_TEST_URL", "\"$webpayTestUrl\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${propiedadesLocales.getProperty("google.webClientId", "")}\"")
        buildConfigField("String", "CORREO_URL", "\"${propiedadesLocales.getProperty("correo.url", "")}\"")
        buildConfigField("String", "CORREO_TOKEN", "\"${propiedadesLocales.getProperty("correo.token", "")}\"")
        buildConfigField("String", "TALLER_DIRECCION", "\"${propiedadesLocales.getProperty("taller.direccion", "").replace("\"", "\\\"")}\"")
        buildConfigField("String", "TALLER_TELEFONO", "\"${propiedadesLocales.getProperty("taller.telefono", "").replace("\"", "\\\"")}\"")
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
        viewBinding = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.viewpager2)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.ai)
    releaseImplementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    implementation(libs.mlkit.translate)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

