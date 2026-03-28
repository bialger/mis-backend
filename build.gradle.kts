import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.6.2"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.aot") version "4.6.2"
}

version = "0.1"
group = "com.bialger"

val kotlinVersion = project.properties.get("kotlinVersion")
val micronautVersion = project.properties.get("micronautVersion") as String

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.micronaut.platform:micronaut-platform:$micronautVersion"))
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.openapi:micronaut-openapi")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.data:micronaut-data-processor")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.data:micronaut-data-tx")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.postgresql:postgresql")
    implementation("io.micronaut.views:micronaut-views-thymeleaf")
    implementation("io.projectreactor:reactor-core:3.6.14")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.openapi:micronaut-openapi-annotations")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    compileOnly("io.micronaut:micronaut-http-client")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("io.projectreactor:reactor-test:3.6.14")
}


application {
    mainClass = "com.bialger.Application"
}
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}


graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("kotest5")
    processing {
        incremental(true)
        annotations("com.bialger.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}


tasks.named<io.micronaut.gradle.docker.NativeImageDockerfile>("dockerfileNative") {
    jdkVersion = "21"
}

tasks.named<ProcessResources>("processResources") {
    from("frontend") {
        into("static")
        include("css/**")
        include("js/**")
        include("api_data/**")
        include("favicon.ico")
        include("design.png")
    }
    // Shell pages: must match js/vue/*-app.js (minimal templates). Root frontend/*.html are standalone UIs and break Vue when embedded.
    from("frontend/pages") {
        into("static/pages")
        include("*.html")
    }
}

