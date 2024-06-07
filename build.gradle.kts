import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.20"
    id("com.google.devtools.ksp") version "1.8.21-1.0.11"
}

group = "ru.bot"
version = "1.0-SNAPSHOT"
val koinVersion = "3.5.6"
val kspVersion = "1.3.1"
val exposedVersion = "0.51.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://git.inmo.dev/api/packages/InsanusMokrassar/maven")
    }
}

sourceSets.main {
    java.srcDirs("build/generated/ksp/main/kotlin")
}

val ktlint by configurations.creating

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
    runtimeOnly("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // h2 database
    implementation("com.h2database:h2:2.2.224")

    // flyway
    implementation("org.flywaydb:flyway-core:10.14.0")

    // ktlint
    ktlint("com.pinterest:ktlint:0.43.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.named("check") {
    dependsOn(ktlintCheck)
}

val ktlintCheck by tasks.creating(JavaExec::class) {
    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("**/build.gradle.kts", "src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F", "src/**/*.kt")
}