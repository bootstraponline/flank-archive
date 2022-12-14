import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin(Plugins.Kotlin.PLUGIN_JVM)
}

repositories {
    mavenCentral()
}

tasks.test {
    maxHeapSize = "2048m"
    minHeapSize = "512m"
}

tasks.withType<KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

dependencies {
    implementation(Dependencies.KOTLIN_COROUTINES_CORE)
    implementation(Dependencies.KOTLIN_REFLECT)
    implementation(Dependencies.MIXPANEL)
    implementation(Dependencies.JACKSON_KOTLIN)
    implementation(Dependencies.JACKSON_YAML)
    implementation(project(Modules.COMMON))
    implementation(project(Modules.ANALYTICS))

    testImplementation(Dependencies.JUNIT)
    testImplementation(Dependencies.MOCKK)
}
