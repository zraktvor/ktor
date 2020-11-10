/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.gradle.tasks.*

fun Project.registerInteropAsSourceSetOutput(name: String, sourceSet: KotlinSourceSet) {
    kotlin {
        val nativeTargets = targets.matching { it.platformType.name == "native" }.filterIsInstance<KotlinNativeTarget>()
        val target = nativeTargets.find { HostManager.host == it.konanTarget } ?: return@kotlin

        val interop = target.compilations.getByName("main").cinterops.getByName(name)
        val interopTask = tasks.named(interop.interopProcessingTaskName) as TaskProvider<CInteropProcess>
        val kLibs = interopTask.map { it.outputFile }
        val fakeInteropCompilation = targets.getByName("metadata").compilations[sourceSet.name]!!
        val tempDir = "$buildDir/tmp/${sourceSet.name}UnpackedInteropKlib"
        val destination = (fakeInteropCompilation.compileKotlinTask as KotlinNativeCompile).destinationDir!!

        val prepareKlibTaskProvider = tasks.register<Sync>("prepare${sourceSet.name.capitalize()}InteropKlib") {
            from(files(zipTree(kLibs).matching {
                exclude("targets/**", "default/targets/**")
            }).builtBy(interopTask))

            into(tempDir)

            doLast {
                val manifest140 = file("$tempDir/default/manifest")
                val manifest1371 = file("$tempDir/manifest")
                val manifest = if (manifest140.exists()) {
                    manifest140
                } else {
                    manifest1371
                }

                val lines = manifest.readLines()
                val transformedManifest = lines.map { line ->
                    val result = if (line.startsWith("depends=")) {
                        "depends=stdlib " + if (manifest == manifest140) {
                            "org.jetbrains.kotlin.native.platform.posix"
                        } else {
                            "posix"
                        }
                    } else {
                        if (line.startsWith("native_targets=")) {
                            "native_targets="
                        } else {
                            line
                        }
                    }

                    result
                }

                manifest.writeText(transformedManifest.joinToString("\n"))
            }
        }

        val copyCinteropTaskProvider = tasks.register<Zip>("copy${sourceSet.name.capitalize()}CinteropKlib") {
            val files = fileTree(tempDir).builtBy(prepareKlibTaskProvider)
            from(files)
            destinationDirectory.set(destination)
            archiveFileName.set("${project.name}_${fakeInteropCompilation.name}.klib")
            dependsOn(interopTask)
        }

        fakeInteropCompilation.output.classesDirs.from(files().builtBy(copyCinteropTaskProvider))

        sourceSets.matching {
            val visited = mutableSetOf<KotlinSourceSet>()

            fun visit(current: KotlinSourceSet) {
                if (visited.add(current)) {
                    it.dependsOn.forEach { visit(it) }
                }
            }

            visit(it)
            sourceSet in visited
        }.forEach {
            dependencies.add(it.implementationMetadataConfigurationName, files(kLibs))
        }
    }
}
