import org.jetbrains.kotlin.gradle.dsl.*

/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

internal data class DefaultSourceSets(val main: KotlinSourceSet, val test: KotlinSourceSet)

internal fun KotlinTargetContainerWithNativeShortcuts.internalCreateIntermediateSourceSets(
    namePrefix: String,
    children: List<DefaultSourceSets>,
    parent: DefaultSourceSets? = null
): DefaultSourceSets {
    val main = createIntermediateSourceSet("${namePrefix}Main", children.map { it.main }, parent?.main)
    val test = createIntermediateSourceSet("${namePrefix}Test", children.map { it.test }, parent?.test)
    return DefaultSourceSets(main, test)
}

internal fun KotlinTargetContainerWithNativeShortcuts.defaultSourceSets(
    targets: List<KotlinNativeTarget>
): List<DefaultSourceSets> = targets.map { it.defaultSourceSets() }

internal fun KotlinTargetContainerWithNativeShortcuts.mostCommonSourceSets() = DefaultSourceSets(
    sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME),
    sourceSets.getByName(KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME)
)

internal fun KotlinTargetContainerWithNativeShortcuts.createIntermediateSourceSet(
    name: String,
    children: List<KotlinSourceSet>,
    parent: KotlinSourceSet? = null
): KotlinSourceSet = sourceSets.maybeCreate(name).apply {
    parent?.let { dependsOn(parent) }
    children.forEach {
        it.dependsOn(this)
    }
}

internal fun KotlinNativeTarget.defaultSourceSets(): DefaultSourceSets = DefaultSourceSets(
    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).defaultSourceSet,
    compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME).defaultSourceSet
)
