plugins {
    alias(libs.plugins.androidLibrary)
    //id ("kotlin-android")
    // id ("org.jetbrains.dokka")
    //id ("maven-publish")
    //id ("signing")
}

// apply { from ("../common.gradle") }

// What a mess...
val enable_asan: Boolean = false

android {
    namespace = "org.androidaudioplugin.manager"
    compileSdk = 34

    defaultConfig {
        minSdk = 29

        externalNativeBuild {
            cmake {
                arguments ("-DANDROID_STL=c++_shared", "-DAAP_ENABLE_ASAN=" + (if (enable_asan) "1" else "0"))
                cppFlags("-g") // enables debug info
            }
        }

        consumerProguardFiles("consumer-rules.pro")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            packaging.jniLibs.keepDebugSymbols.add("**/*.so")
            isJniDebuggable = true
            externalNativeBuild {
                cmake {
                    // we cannot error out cmidi2.h as we don't compile cmidi2_test.h
                    //cppFlags ("-Werror")
                    cppFlags("-g") // enables debug info
                }
            }
            ndk {
                debugSymbolLevel ="FULL"  // Ensures full debug symbols are included
            }
        }
        release {
            isJniDebuggable = true
            ndk {
                debugSymbolLevel ="FULL"  // Ensures full debug symbols are included
            }
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    ndkVersion = libs.versions.ndk.get()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        prefab = true
        prefabPublishing = true
    }
    externalNativeBuild {
        cmake {
            version = libs.versions.cmake.get()
            path ("src/main/cpp/CMakeLists.txt")
        }
    }
    prefab {
        create("androidaudioplugin-manager") {
            name = "androidaudioplugin-manager"
        }
    }
    // https://github.com/google/prefab/issues/127
    packaging {
        jniLibs.excludes.add("**/libc++_shared.so")
        jniLibs.excludes.add("**/libandroidaudioplugin.so") // package it separately
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
    }
}

// apply { from ("../publish-pom.gradle") }

dependencies {
    implementation (libs.androidAudioPlugin)
    implementation (libs.androidx.core.ktx)
    implementation (libs.startup.runtime)
    implementation (libs.kotlinx.coroutines)
    implementation (libs.oboe)
    implementation (libs.ktmidi)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

/*
// Starting AGP 7.0.0-alpha05, AGP stopped caring build dependencies and it broke builds.
// This is a forcible workarounds to build libandroidaudioplugin.so in prior to referencing it.
gradle.projectsEvaluated {
    tasks.findByPath(":androidaudioplugin-manager:buildCMakeDebug")!!.dependsOn(":androidaudioplugin:prefabDebugPackage")
    tasks.findByPath(":androidaudioplugin-manager:buildCMakeRelWithDebInfo")!!.dependsOn(":androidaudioplugin:prefabReleasePackage")
}
*/
