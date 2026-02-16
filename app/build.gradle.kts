plugins {
    // Apply the shared build logic from a convention plugin.
    // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
    //id("buildsrc.convention.kotlin-jvm")
    //id("org.jetbrains.kotlin.jvm")
    //kotlin("plugin.serialization") version "1.9.24"
    id("org.jetbrains.kotlin.jvm")
    kotlin("plugin.serialization") version "1.9.24"
    // Apply the Application plugin to add support for building an executable JVM application.
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

dependencies {
    // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils" refers to the top-level "utils" project.)
   // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation(project(":utils"))
}

application {
    // Define the Fully Qualified Name for the application main class
    // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
    mainClass = "org.example.app.ktb_22_multi_users.TelegramKt"
}
