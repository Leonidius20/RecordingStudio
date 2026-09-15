plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.github.leonidius20.recorder.audio_config.ui"
    compileSdk {
        version = release(34)
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(projects.common.ui)
    implementation(projects.entities) // todo: remove
    implementation(projects.audioConfig.presentation)
    implementation(projects.audioConfig.domain.impl)

    implementation(libs.mviKotlin)
    implementation(libs.mviKotlin.main)
    implementation(libs.mviKotlin.extensions.coroutines)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(libs.flexbox)

    implementation(libs.androidx.viewpager2)
}
