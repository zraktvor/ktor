/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.locations

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.server.testing.client.*
import io.ktor.util.pipeline.*
import org.junit.Test
import java.net.*
import kotlin.test.*

@OptIn(KtorExperimentalLocationsAPI::class)
class OAuthLocationsTest {
    @Test
    fun testOAuthAtLocation() = withTestApplication {
        application.install(Locations)
        application.install(Authentication) {
            oauth {
                client = this@withTestApplication.client
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings("a", "http://oauth-server/auth",
                            "http://oauth-server/token",
                            clientId = "test", clientSecret = "secret")
                }
                urlProvider = { url(B()) }
            }
        }

        application.install(Routing) {
            authenticate {
                get<`A$Context`> {}
                get<`B$Context`> {}
            }
        }

        handleRequest(HttpMethod.Get, "/A").let { call ->
            assertEquals(HttpStatusCode.Found, call.response.status())
            val locationHeader = call.response.headers[HttpHeaders.Location]
            assertNotNull(locationHeader)
            assertEquals("/auth", URL(locationHeader).path)
            assertEquals("/B", URL(parseQueryString(URL(locationHeader).query)["redirect_uri"]).path)
        }
    }

    @Test
    fun testOAuthAtLocationString() = withTestApplication {
        application.install(Locations)
        application.install(Authentication) {
            oauth {
                client = this@withTestApplication.client
                providerLookup = {
                    OAuthServerSettings.OAuth2ServerSettings("a", "http://oauth-server/auth",
                            "http://oauth-server/token",
                            clientId = "test", clientSecret = "secret")
                }
                urlProvider = { "http://localhost/B" }
            }
        }

        application.install(Routing) {
            authenticate {
                get<`A$Context`> {}
                get<`B$Context`> {}
            }
        }

        handleRequest(HttpMethod.Get, "/A").let { call ->
            assertEquals(HttpStatusCode.Found, call.response.status())
            val locationHeader = call.response.headers[HttpHeaders.Location]
            assertNotNull(locationHeader)
            assertEquals("/auth", URL(locationHeader).path)
            assertEquals("/B", URL(parseQueryString(URL(locationHeader).query)["redirect_uri"]).path)
        }
    }

    @Location("/A")
    class A: RespondsUnit
    class `A$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>): TypedLocation(ctx)

    @Location("/B")
    class B: RespondsUnit
    class `B$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>): TypedLocation(ctx)
}
