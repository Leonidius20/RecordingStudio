import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kapt)
    alias(libs.plugins.hilt)
    id("com.github.alexfu.androidautoversion")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "io.github.leonidius20.recorder"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.leonidius20.recorder"
        minSdk = 21
        targetSdk = 34
        versionCode = androidAutoVersion.versionCode
        versionName = androidAutoVersion.versionName

        resValue("string", "version_name", androidAutoVersion.versionName)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // translated only into english and ukrainian languages,
        // exclude strings from libraries in other languages
        resourceConfigurations.addAll(listOf("en", "uk"))
    }

    signingConfigs {
        create("production") {
            storeFile = project.rootProject.file("android-keystore.jks")
            val secretPropertiesFile = project.rootProject.file("secrets.properties")
            if (!secretPropertiesFile.exists()) {
                // we are on github actions, get from env variables
                storePassword = System.getenv("SIGNATURE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNATURE_KEY_ALIAS")
                keyPassword = System.getenv("SIGNATURE_KEY_PASSWORD")
            } else {
                // we are on local machine, get from file
                val secretProperties = Properties()
                secretProperties.load(secretPropertiesFile.inputStream())

                storePassword = secretProperties.getProperty("SIGNATURE_KEYSTORE_PASSWORD")
                keyAlias = secretProperties.getProperty("SIGNATURE_KEY_ALIAS")
                keyPassword = secretProperties.getProperty("SIGNATURE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("production")

            resValue("string", "build_type_name", "release")
        }

        debug {
            resValue("string", "build_type_name", "debug")

            // if there is an available signature, use it to sign debug builds
            // so as to avoid having to re-install the app if you have a
            // signed release build installed
            if (project.rootProject.file("android-keystore.jks").exists()) {
                signingConfig = signingConfigs.getByName("production")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true

    flavorDimensions += "version"

    productFlavors {

        create("lite") {
            dimension = "version"
            applicationIdSuffix = ".lite"
            // versionNameSuffix = "-lite"
        }

        create("full") {
            dimension = "version"
        }

    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)

    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.espresso.core)

    // for testing with robolectric
    testImplementation(libs.robolectric)

    debugImplementation(libs.androidx.fragment.testing)

    implementation(libs.hilt)
    ksp(libs.hilt.compiler)

    implementation(libs.material.lists)

    implementation(libs.ok.layoutinflater)

    implementation(libs.timeit) // for rec duration timer
    implementation(libs.audioRecordView) // audio visualizer

    implementation(libs.androidx.preference)

    implementation(libs.permissionx)

    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)

    implementation (libs.customactivityoncrash)

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // todo: if flavour = full, include AAP framework
}