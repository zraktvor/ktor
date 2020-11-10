/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

val KotlinTargetContainerWithNativeShortcuts.darwinTargets: List<KotlinNativeTarget>
    get() = listOf(
        iosArm32(), iosArm64(), iosX64(), macosX64(),
        tvosArm64(), tvosX64(),
        watchosArm32(), watchosArm64(), watchosX86()
    )

val KotlinTargetContainerWithNativeShortcuts.posixTargets: List<KotlinNativeTarget>
    get() = darwinTargets + listOf(linuxX64())

val KotlinTargetContainerWithNativeShortcuts.nativeTargets: List<KotlinNativeTarget>
    get() = posixTargets + mingwX64()


fun KotlinTargetContainerWithNativeShortcuts.darwin(
    configure: KotlinNativeTarget.() -> Unit = {}
) {
    setupCommonNativeSourceSet("darwin", darwinTargets, configure)
}

fun KotlinTargetContainerWithNativeShortcuts.native(
    configure: KotlinNativeTarget.() -> Unit = {}
) {
    setupCommonNativeSourceSet("native", nativeTargets, configure)
}

fun KotlinTargetContainerWithNativeShortcuts.posix(
    configure: KotlinNativeTarget.() -> Unit = {}
) {
    setupCommonNativeSourceSet("posix", posixTargets, configure)
}

private fun KotlinTargetContainerWithNativeShortcuts.setupCommonNativeSourceSet(
    name: String,
    targets: List<KotlinNativeTarget>,
    configure: KotlinNativeTarget.() -> Unit
) {
    internalCreateIntermediateSourceSets(name, defaultSourceSets(targets), mostCommonSourceSets())
    targets.forEach(configure)
}
