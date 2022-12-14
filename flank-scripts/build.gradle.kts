import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.nio.file.Files
import java.nio.file.Paths

plugins {
    application
    kotlin(Plugins.Kotlin.PLUGIN_JVM)
    kotlin(Plugins.Kotlin.PLUGIN_SERIALIZATION) version Versions.KOTLIN
    id(Plugins.PLUGIN_SHADOW_JAR) version Versions.SHADOW
    id(Plugins.MAVEN_PUBLISH)
    id(Plugins.CHECK_VERSION_UPDATED)
}

val artifactID = "flank-scripts"

val shadowJar: ShadowJar by tasks

shadowJar.apply {
    archiveClassifier.set("")
    archiveFileName.set("$artifactID.jar")
    mergeServiceFiles()

    @Suppress("UnstableApiUsage")
    manifest {
        attributes(mapOf("Main-Class" to "flank.scripts.MainKt"))
    }
}
// <breaking change>.<feature added>.<fix/minor change>
version = "1.9.28"
group = "com.github.flank"

application {
    mainClassName = "flank.scripts.cli.MainKt"
    applicationDefaultJvmArgs = listOf(
        "-Xmx2048m",
        "-Xms512m"
    )
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Flank/flank")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: properties["GITHUB_ACTOR"].toString()
                password = System.getenv("GITHUB_TOKEN") ?: properties["GITHUB_TOKEN"].toString()
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = group.toString()
            artifactId = artifactID
            version = version

            artifact(shadowJar)

            pom {
                name.set("Flank-scripts")
                description.set("Scripts for Flank")
                url.set("https://github.com/Flank/flank")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }

            pom.withXml {
                // Remove deps since we're publishing a fat jar
                val pom = asNode()
                val depNode: groovy.util.NodeList = pom.get("dependencies") as groovy.util.NodeList
                for (child in depNode) {
                    pom.remove(child as groovy.util.Node)
                }
            }
        }
    }
}

tasks.test {
    maxHeapSize = "2048m"
    minHeapSize = "512m"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(Dependencies.KOTLIN_SERIALIZATION)
    implementation(project(Modules.COMMON))
    implementation(Dependencies.CLIKT)
    implementation(Dependencies.JCABI_GITHUB)
    implementation(Dependencies.SLF4J_NOP)
    implementation(Dependencies.GLASSFISH_JSON)
    implementation(Dependencies.Fuel.COROUTINES)

    testImplementation(Dependencies.JUNIT)
    testImplementation(Dependencies.MOCKK)
    testImplementation(Dependencies.TRUTH)
    testImplementation(Dependencies.SYSTEM_RULES)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "1.8" }

val prepareJar by tasks.registering(Copy::class) {
    dependsOn("shadowJar")
    from("$buildDir/libs")
    include("flank-scripts.jar")
    into("$projectDir/bash")
}

val download by tasks.registering(Exec::class) {
    commandLine(
        "gh", "release", "download", "flank-scripts-$version", "-D", System.getenv("GITHUB_WORKSPACE") ?: ".."
    )
    doLast {
        Files.copy(
            Paths.get("$artifactID.jar"),
            Paths.get("flank-scripts", "bash", "$artifactID.jar")
        )
    }
}

val releaseFlankScripts by tasks.registering(Exec::class) {
    dependsOn(":flank-scripts:publish")
    commandLine(
        "gh", "release", "create",
        "flank-scripts-$version", "$buildDir/libs/$artifactID.jar",
        "-t", "Flank Scripts $version",
        "-p"
    )
}
