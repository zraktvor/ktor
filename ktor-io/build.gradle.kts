/*
 * Copyright 2014-2020 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

kotlin {
    nativeTargets.forEach {
        it.compilations {
            val main by getting {
                cinterops {
                    val bits by creating {
                        defFile = file("native/interop/bits.def")
                    }

                    val sockets by creating {
                        defFile = file("native/interop/sockets.def")
                    }
                }
            }

            val test by getting {
                cinterops {
                    val testSockets by creating {
                        defFile = file("native/interop/testSockets.def")
                    }
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting
        val commonTest by getting

        val bitsMain by creating {
            configureLanguageSettings(project)
            dependsOn(commonMain)
        }

        val socketsMain by creating {
            configureLanguageSettings(project)
            dependsOn(commonMain)
        }

        val nativeMain by getting {
            dependsOn(bitsMain)
            dependsOn(socketsMain)
        }

        afterEvaluate {
            registerInteropAsSourceSetOutput("bits", bitsMain)
            registerInteropAsSourceSetOutput("sockets", socketsMain)
        }
    }
}
