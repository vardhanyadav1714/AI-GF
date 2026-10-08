import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

apply(plugin = "com.google.gms.google-services")

val evaUploadStore = providers.environmentVariable("EVA_UPLOAD_STORE_FILE").orNull
val evaUploadStorePassword = providers.environmentVariable("EVA_UPLOAD_STORE_PASSWORD").orNull
val evaUploadAlias = providers.environmentVariable("EVA_UPLOAD_KEY_ALIAS").orNull
val evaUploadKeyPassword = providers.environmentVariable("EVA_UPLOAD_KEY_PASSWORD").orNull
val evaSigningValues = listOf(evaUploadStore, evaUploadStorePassword, evaUploadAlias, evaUploadKeyPassword)
require(evaSigningValues.none { !it.isNullOrBlank() } || evaSigningValues.all { !it.isNullOrBlank() }) {
    "Set all four EVA_UPLOAD signing environment variables together."
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "com.eva.ai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.eva.ai"
        minSdk = 24
        targetSdk = 37
        versionCode = providers.gradleProperty("evaVersionCode").orElse(providers.environmentVariable("EVA_VERSION_CODE")).orElse("1").get().toInt()
        versionName = providers.gradleProperty("evaVersionName").orElse(providers.environmentVariable("EVA_VERSION_NAME")).orElse("1.0").get()
        buildConfigField("boolean", "ALTERNATIVE_BILLING_ENABLED", providers.gradleProperty("alternativeBillingEnabled").orElse("true").get())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    if (evaSigningValues.all { !it.isNullOrBlank() }) {
        signingConfigs.create("evaUpload") {
            storeFile = file(evaUploadStore!!)
            storePassword = evaUploadStorePassword
            keyAlias = evaUploadAlias
            keyPassword = evaUploadKeyPassword
        }
    }

    buildTypes {
        release {
            if (evaSigningValues.all { !it.isNullOrBlank() }) {
                signingConfig = signingConfigs.getByName("evaUpload")
            }
            optimization {
                enable = true
            }
        }
    }
    lint {
        checkReleaseBuilds = false
        abortOnError = false
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.firebase.messaging)
    implementation(libs.googleid)
    implementation("com.android.billingclient:billing-ktx:9.1.0")
    implementation("com.razorpay:checkout:1.6.41") {
        exclude(group = "com.razorpay", module = "standard-core")
    }
    // Replace the SDK's LATEST dependency with the version verified for this release.
    implementation("com.razorpay:standard-core:1.7.19")
    implementation(libs.hilt.android)
    add("ksp", libs.hilt.compiler)
    add("ksp", "com.squareup:kotlinpoet:1.13.2")
    add("ksp", "org.jetbrains.kotlin:kotlin-reflect:1.8.21")
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
