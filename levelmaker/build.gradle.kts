plugins {
    kotlin("jvm")
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("com.retroraja.wordnoid.levelmaker.MainKt")
    applicationDefaultJvmArgs = listOf("-Xmx3g")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
