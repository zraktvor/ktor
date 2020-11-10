import org.gradle.util.GradleVersion.*
import org.jetbrains.kotlin.utils.*

/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

val configuredVersion: Any = if (project.hasProperty("releaseVersion")) {
    project.property("releaseVersion") ?: project.version
} else {
    project.version
}

val coroutines_version: String by project.extra
val kotlin_version: String by project.extra

plugins {
    kotlin("multiplatform") apply false
    id("kotlinx-atomicfu") apply false
}

allprojects {
    group = "io.ktor"
    version = configuredVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://kotlin.bintray.com/kotlinx")
        jcenter()
    }

    apply(plugin = "kotlin-multiplatform")
    apply(plugin = "kotlinx-atomicfu")

    kotlin {
        jvm()
        js {
            nodejs()
            browser()
        }

        native()
        posix()
        darwin()

        setupSourceSetNames()
        setupLanguageSettings(project)

        sourceSets {
            val commonMain by getting {
                dependencies {
                    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")
                }
            }

            val commonTest by getting {
                dependencies {
                    api("org.jetbrains.kotlin:kotlin-test-common:$kotlin_version")
                    api("org.jetbrains.kotlin:kotlin-test-annotations-common:$kotlin_version")
                }
            }
        }
    }

    configurations {
        val testOutput by creating
    }
}
