import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotestVersion = "4.0.6"
val kxCoroutineVersion = "1.3.7"
val literVersion = "0.2.2"
val ktorVersion = "1.3.2"

plugins {
    kotlin("jvm") version "1.3.72"
}

group = "jp.pois"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kxCoroutineVersion")
    implementation("jp.pois:liter:$literVersion")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:$kotestVersion") // for kotest framework
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion") // for kotest core jvm assertions
    testImplementation("io.kotest:kotest-runner-console-jvm:$kotestVersion")
    testImplementation("io.kotest:kotest-property-jvm:$kotestVersion") // for kotest property test
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
}
