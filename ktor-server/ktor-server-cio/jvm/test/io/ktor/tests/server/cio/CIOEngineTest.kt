/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.server.cio

import io.ktor.server.cio.*
import io.ktor.routing.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.server.testing.suites.*
import kotlinx.coroutines.*

class CIOCompressionTest : CompressionTestSuite<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO) {
    init {
        enableHttp2 = false
        enableSsl = false
    }
}

class CIOContentTest : ContentTestSuite<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO) {
    init {
        enableHttp2 = false
        enableSsl = false
    }
}

class CIOHttpServerTest : HttpServerTestSuite<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO) {
    init {
        enableHttp2 = false
        enableSsl = false
    }
}

class CIOSustainabilityTest : SustainabilityTestSuite<CIOApplicationEngine, CIOApplicationEngine.Configuration>(CIO) {
    init {
        enableHttp2 = false
        enableSsl = false
    }

    @kotlin.test.Test
    fun test() {
        createAndStartServer {
            get("/") {
                call.respondText("Hello, World!\n")
            }
        }

        println("Port is $port")

        runBlocking<Unit> {
            suspendCancellableCoroutine {
            }
        }
    }
}
