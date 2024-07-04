import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("com.google.devtools.ksp") version "1.8.21-1.0.11"
    kotlin("plugin.serialization") version "1.8.20"
}

group = "ru.bot"
version = "1.0-SNAPSHOT"
val koinVersion = "3.5.6"
val kspVersion = "1.3.1"
val exposedVersion = "0.51.0"
val ktorVersion = "2.3.10"

repositories {
    mavenCentral()
    maven {
        url = uri("https://git.inmo.dev/api/packages/InsanusMokrassar/maven")
    }
}

sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}

dependencies {
    testImplementation(kotlin("test"))

    // telegram bot
    implementation("dev.inmo:tgbotapi:14.0.0")

    // koin - dependency injection
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-annotations:$kspVersion")
    ksp("io.insert-koin:koin-ksp-compiler:$kspVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")

    // exposed - db & orm management
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // h2 database
    implementation("com.h2database:h2:2.2.224")

//    // sqlite
//    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // flyway
    implementation("org.flywaydb:flyway-core:10.14.0")

    // yaml reading
    implementation("com.charleskorn.kaml:kaml:0.59.0")
    implementation("com.squareup.okio:okio:3.5.0")
    implementation("it.krzeminski:snakeyaml-engine-kmp:2.7.4")
    implementation("org.yaml:snakeyaml:1.30")
    implementation("net.thauvin.erik.urlencoder:urlencoder-lib-jvm:1.5.0")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    configurations["compileClasspath"].forEach { file: File ->
        println(file.absoluteFile)
        from(zipTree(file.absoluteFile)).also {
            println(it)
        }
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}