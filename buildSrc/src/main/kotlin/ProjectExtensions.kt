/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.gradle.api.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.kotlin(configure: Action<KotlinMultiplatformExtension>) {
    val extension = extensions.findByType<KotlinMultiplatformExtension>()
        ?: error("Consider apply kotlin-multiplatform plugin first")

    extension.also {
        configure.invoke(it)
    }
}

fun KotlinMultiplatformExtension.setupSourceSetNames() {
    sourceSets.forEach {
        val srcDir = if (it.name.endsWith("Main")) "src" else "test"
        val resourcesPrefix = if (it.name.endsWith("Test")) "test-" else ""
        val platform = it.name.dropLast(4)

        it.kotlin.srcDirs("$platform/$srcDir")
        it.resources.srcDirs("$platform/${resourcesPrefix}resources")
    }
}

fun KotlinMultiplatformExtension.setupLanguageSettings(project: Project) {
    if (project.name !in DISABLED_EXPLICIT_API_MODE_PROJECTS) {
        explicitApi()
    }

    sourceSets.forEach {
        it.configureLanguageSettings(project)
    }

    project.tasks.withType<KotlinCompile> {
        kotlinOptions.freeCompilerArgs += "-Xinline-classes"
    }
}

fun KotlinSourceSet.configureLanguageSettings(project: Project) {
    languageSettings.apply {
        languageSettings.apply {
            progressiveMode = true

            EXPERIMENTAL_ANNOTATIONS.forEach {
                useExperimentalAnnotation(it)
            }

            if (project.path.startsWith(":ktor-server:ktor-server") && project.name != "ktor-server-core") {
                useExperimentalAnnotation("io.ktor.server.engine.EngineAPI")
            }
        }
    }
}
