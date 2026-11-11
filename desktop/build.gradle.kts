plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

group = "com.fitmate.app"
version = "1.0.0"

repositories {
    google()
    mavenCentral()
}

val javafxVersion = "21.0.7"
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch")

val javafxPlatform = when {
    osName.contains("mac") && osArch == "aarch64" -> "mac-aarch64"
    osName.contains("mac") -> "mac"
    osName.contains("windows") -> "win"
    else -> "linux"
}

dependencies {
    implementation("me.friwi:jcefmaven:152.0.6")
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation("org.openjfx:javafx-web:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-swing:$javafxVersion:$javafxPlatform")
    implementation("org.json:json:20250517")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.fitmate.app.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi
            )

            packageName = "FitMate"
            packageVersion = "1.0.0"
            description = "FitMate Fitness and Workout Planner"
            vendor = "FitMate"
        }
    }
}
