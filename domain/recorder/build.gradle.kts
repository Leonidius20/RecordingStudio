plugins {
    id("java-library")
    alias(libs.plugins.jetbrainsKotlinJvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines)
}
