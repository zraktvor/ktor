/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.tests.locations

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.HttpMethod
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.util.pipeline.*
import org.junit.Test
import java.math.*
import kotlin.test.*

@OptIn(KtorExperimentalLocationsAPI::class)
private fun withLocationsApplication(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(Locations)
    test()
}

@OptIn(KtorExperimentalLocationsAPI::class)
class LocationsTest {
    @Location("/")
    class index : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/")
    class `index$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location without URL`() = withLocationsApplication {
        val href = application.locations.href(index())
        assertEquals("/", href)
        application.routing {
            get<`index$Context`, index>(::`index$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/index")
    }

    @Test
    fun `locationLocal`() {
        // ^^^ do not add spaces to method name, inline breaks

        @Location("/")
        class indexLocal : Responds<HttpStatusCode.Companion.OKCode>

        @Location("/")
        class `indexLocal$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }

        withLocationsApplication {
            val href = application.locations.href(indexLocal())
            assertEquals("/", href)
            application.routing {
                get<`indexLocal$Context`, indexLocal>(::`indexLocal$Context`) {
                    call.respond(HttpStatusCode.OK)
                }
            }
            urlShouldBeHandled(href)
            urlShouldBeUnhandled("/index")
        }
    }

    @Location("/about")
    class about : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/about")
    class `about$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with URL`() = withLocationsApplication {
        val href = application.locations.href(about())
        assertEquals("/about", href)
        application.routing {
            get<`about$Context`, about>(::`about$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/about/123")
    }

    @Location("/user/{id}")
    class user(val id: Int) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/user/{id}")
    class `user$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) {
            val self: ApplicationCall = this
            self.respond(message)
        }
    }

    @Test
    fun `location with path param`() = withLocationsApplication {
        val href = application.locations.href(user(123))
        assertEquals("/user/123", href)
        application.routing {
            get<`user$Context`, user>(::`user$Context`) { user ->
                assertEquals(123, user.id)
                call.respond(HttpStatusCode.OK)

                call.respondRedirect("ko.ko.ko", true)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/user?id=123")
    }

    @Location("/user/{id}/{name}")
    class named(val id: Int, val name: String) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/user/{id}/{name}")
    class `named$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with urlencoded path param`() = withLocationsApplication {
        val href = application.locations.href(named(123, "abc def"))
        assertEquals("/user/123/abc%20def", href)
        application.routing {
            get<`named$Context`, named>(::`named$Context`) { named ->
                assertEquals(123, named.id)
                assertEquals("abc def", named.name)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/user?id=123")
        urlShouldBeUnhandled("/user/123")
    }

    @Location("/favorite")
    class favorite(val id: Int) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/favorite")
    class `favorite$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }


    @Test
    fun `location with query param`() = withLocationsApplication {
        val href = application.locations.href(favorite(123))
        assertEquals("/favorite?id=123", href)
        application.routing {
            get<`favorite$Context`, favorite>(::`favorite$Context`) { favorite ->
                assertEquals(123, favorite.id)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/favorite/123")
        urlShouldBeUnhandled("/favorite")
    }

    @Location("/container/{id}")
    class pathContainer(val id: Int) : RespondsUnit {
        @Location("/items")
        class items(val container: pathContainer) : Responds<HttpStatusCode.Companion.OKCode>

        @Location("/items")
        class badItems : RespondsUnit
    }

    @Location("/container/{id}")
    class `pathContainer$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {

        @Location("/items")
        class `items$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
            TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }

        @Location("/items")
        class `badItems$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }
    }

    @Test
    fun `location with path parameter and nested data`() = withLocationsApplication {
        val c = pathContainer(123)
        val href = application.locations.href(pathContainer.items(c))
        assertEquals("/container/123/items", href)
        application.routing {
            get<`pathContainer$Context`.`items$Context`, pathContainer.items>(`pathContainer$Context`::`items$Context`) { items ->
                assertEquals(123, items.container.id)
                call.respond(HttpStatusCode.OK)
            }
            assertFailsWith(LocationRoutingException::class) {
                get<`pathContainer$Context`.`badItems$Context`, pathContainer.badItems>(`pathContainer$Context`::`badItems$Context`) { }
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/container/items")
        urlShouldBeUnhandled("/container/items?id=123")
    }

    @Location("/container")
    class queryContainer(val id: Int) : RespondsUnit {
        @Location("/items")
        class items(val container: queryContainer) : Responds<HttpStatusCode.Companion.OKCode>

        @Location("/items")
        class badItems : RespondsUnit
    }

    @Location("/container")
    class `queryContainer$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        @Location("/items")
        class `items$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
            TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }

        @Location("/items")
        class `badItems$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {}
    }

    @Test
    fun `location with query parameter and nested data`() = withLocationsApplication {
        val c = queryContainer(123)
        val href = application.locations.href(queryContainer.items(c))
        assertEquals("/container/items?id=123", href)
        application.routing {
            get<`queryContainer$Context`.`items$Context`, queryContainer.items>(`queryContainer$Context`::`items$Context`) { items ->
                assertEquals(123, items.container.id)
                call.respond(HttpStatusCode.OK)
            }
            assertFailsWith(LocationRoutingException::class) {
                get<`queryContainer$Context`.`badItems$Context`, queryContainer.badItems>(`queryContainer$Context`::`badItems$Context`) { }
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/container/items")
        urlShouldBeUnhandled("/container/123/items")
    }

    @Location("/container")
    class optionalName(val id: Int, val optional: String? = null) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/container")
    class `optionalName$Context`(
        ctx: PipelineContext<Unit, TypedApplicationCall>
    ) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with missing optional String parameter`() = withLocationsApplication {
        val href = application.locations.href(optionalName(123))
        assertEquals("/container?id=123", href)
        application.routing {
            get<`optionalName$Context`, optionalName>(::`optionalName$Context`) {
                assertEquals(123, it.id)
                assertNull(it.optional)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/container")
        urlShouldBeUnhandled("/container/123")
    }


    @Location("/container")
    class optionalIndex(val id: Int, val optional: Int = 42)

    @Location("/container")
    class `optionalIndex$Context`(
        ctx: PipelineContext<Unit, TypedApplicationCall>
    ) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with missing optional Int parameter`() = withLocationsApplication {
        val href = application.locations.href(optionalIndex(123))
        assertEquals("/container?id=123&optional=42", href)
        application.routing {
            get<`optionalIndex$Context`, optionalIndex>(::`optionalIndex$Context`) {
                assertEquals(123, it.id)
                assertEquals(42, it.optional)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled("/container?id=123")
        urlShouldBeUnhandled("/container")
        urlShouldBeUnhandled("/container/123")
    }

    @Test
    fun `location with specified optional query parameter`() = withLocationsApplication {
        val href = application.locations.href(optionalName(123, "text"))
        assertEquals("/container?id=123&optional=text", href)
        application.routing {
            get<`optionalName$Context`, optionalName>(::`optionalName$Context`) {
                assertEquals(123, it.id)
                assertEquals("text", it.optional)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/container")
        urlShouldBeUnhandled("/container/123")
    }

    @Location("/container/{id?}")
    class optionalContainer(val id: Int? = null) : Responds<HttpStatusCode.Companion.OKCode> {
        @Location("/items")
        class items(val optional: String? = null) : Responds<HttpStatusCode.Companion.OKCode>
    }

    @Location("/container/{id?}")
    class `optionalContainer$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)

        @Location("/items")
        class `items$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
            TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }
    }

    @Test
    fun `location with optional path and query parameter`() = withLocationsApplication {
        val href = application.locations.href(optionalContainer())
        assertEquals("/container", href)
        application.routing {
            get<`optionalContainer$Context`, optionalContainer>(::`optionalContainer$Context`) {
                assertEquals(null, it.id)
                call.respond(HttpStatusCode.OK)
            }
            get<`optionalContainer$Context`.`items$Context`, optionalContainer.items>(`optionalContainer$Context`::`items$Context`) {
                assertEquals("text", it.optional)
                call.respond(HttpStatusCode.OK)
            }

        }
        urlShouldBeHandled(href)
        urlShouldBeHandled("/container")
        urlShouldBeHandled("/container/123/items?optional=text")
    }

    @Location("/container")
    class simpleContainer : Responds<HttpStatusCode.Companion.OKCode> {
        @Location("/items")
        class items
    }

    @Location("/container")
    class `simpleContainer$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)

        @Location("/items")
        class `items$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }
    }

    @Test
    fun `location with simple path container and items`() = withLocationsApplication {
        val href = application.locations.href(simpleContainer.items())
        assertEquals("/container/items", href)
        application.routing {
            get<`simpleContainer$Context`.`items$Context`, simpleContainer.items>(`simpleContainer$Context`::`items$Context`) {
                call.respond(HttpStatusCode.OK)
            }
            get<`simpleContainer$Context`, simpleContainer>(::`simpleContainer$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeHandled("/container")
        urlShouldBeUnhandled("/items")
    }

    @Location("/container/{path...}")
    class tailCard(val path: List<String>) : Responds<String>

    @Location("/container/{path...}")
    class `tailCard$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: String) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with tailcard`() = withLocationsApplication {
        val href = application.locations.href(tailCard(emptyList()))
        assertEquals("/container", href)
        application.routing {
            get<`tailCard$Context`, tailCard>(::`tailCard$Context`) {
                call.respond(it.path.toString())
            }

        }
        urlShouldBeHandled(href, "[]")
        urlShouldBeHandled("/container/some", "[some]")
        urlShouldBeHandled("/container/123/items?optional=text", "[123, items]")
    }

    @Location("/")
    class multiquery(val value: List<Int>) : Responds<String>

    @Location("/container/{path...}")
    class `multiquery$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: String) =
            response.pipeline.execute(this, message)
    }

    @Location("/")
    class multiquery2(val name: List<String>) : Responds<String>

    @Location("/container/{path...}")
    class `multiquery2$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: String) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with multiple query values`() = withLocationsApplication {
        val href = application.locations.href(multiquery(listOf(1, 2, 3)))
        assertEquals("/?value=1&value=2&value=3", href)
        application.routing {
            get<`multiquery$Context`, multiquery>(::`multiquery$Context`) {
                call.respond(it.value.toString())
            }

        }
        urlShouldBeHandled(href, "[1, 2, 3]")
    }

    @Test
    fun `location with multiple query values can select by query params`() = withLocationsApplication {
        val href = application.locations.href(multiquery(listOf(1)))
        assertEquals("/?value=1", href)
        application.routing {
            get<`multiquery$Context`, multiquery>(::`multiquery$Context`) {
                call.respond("1: ${it.value}")
            }
            get<`multiquery2$Context`, multiquery2>(::`multiquery2$Context`){
                call.respond("2: ${it.name}")
            }

        }
        urlShouldBeHandled(href, "1: [1]")
    }

    @Test
    fun `location with multiple query values can select by query params2`() = withLocationsApplication {
        val href = application.locations.href(multiquery2(listOf("john, mary")))
        assertEquals("/?name=john%2C+mary", href)
        application.routing {
            get<`multiquery$Context`, multiquery>(::`multiquery$Context`) {
                call.respond("1: ${it.value}")
            }
            get<`multiquery2$Context`, multiquery2>(::`multiquery2$Context`) {
                call.respond("2: ${it.name}")
            }

        }
        urlShouldBeHandled(href, "2: [john, mary]")
    }

    @Location("/")
    class multiqueryWithDefault(val value: List<Int> = emptyList()) : Responds<String>

    @Location("/")
    class `multiqueryWithDefault$Context`(
        ctx: PipelineContext<Unit, TypedApplicationCall>
    ) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: String) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location with multiple query values and default`() = withLocationsApplication {
        val href = application.locations.href(multiqueryWithDefault(listOf()))
        assertEquals("/", href)
        application.routing {
            get<`multiqueryWithDefault$Context`, multiqueryWithDefault>(::`multiqueryWithDefault$Context`) {
                call.respond(it.value.toString())
            }
        }
        urlShouldBeHandled(href, "[]")
    }

    @Location("/space in")
    class SpaceInPath

    @Location("/plus+in")
    class PlusInPath

    @Test
    fun testURLBuilder() = withLocationsApplication {
        application.routing {
            handle {
                assertEquals("http://localhost/container?id=1&optional=ok", call.url(optionalName(1, "ok")))
                assertEquals(
                    "http://localhost/container?id=1&optional=ok%2B.plus",
                    call.url(optionalName(1, "ok+.plus"))
                )
                assertEquals("http://localhost/container?id=1&optional=ok+space", call.url(optionalName(1, "ok space")))

                assertEquals("http://localhost/space%20in", call.url(SpaceInPath()))
                assertEquals("http://localhost/plus+in", call.url(PlusInPath()))

                call.respondText(call.url(optionalName(1, "ok")))
            }
        }

        urlShouldBeHandled("/", "http://localhost/container?id=1&optional=ok")
    }

    @Location("/")
    object root : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/")
    class `root$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location root by object`() = withLocationsApplication {
        val href = application.locations.href(root)
        assertEquals("/", href)
        application.routing {
            get<`root$Context`, root>(::`root$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/index")
    }

    @Location("/help")
    object help : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/help")
    class `help$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `location by object`() = withLocationsApplication {
        val href = application.locations.href(help)
        assertEquals("/help", href)
        application.routing {
            get<`help$Context`, help>(::`help$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/help/123")
    }

    @Location("/users")
    object users : Responds<HttpStatusCode.Companion.OKCode> {
        @Location("/me")
        object me : Responds<HttpStatusCode.Companion.OKCode>

        @Location("/{id}")
        class user(val id: Int) : Responds<HttpStatusCode.Companion.OKCode>
    }

    @Location("/users")
    class `users$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) :
        TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)

        @Location("/me")
        class `me$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }

        @Location("/{id}")
        class `user$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
                response.pipeline.execute(this, message)
        }
    }

    @Test
    fun `location by object in object`() = withLocationsApplication {
        val href = application.locations.href(users.me)
        assertEquals("/users/me", href)
        application.routing {
            get<`users$Context`.`me$Context`, users.me>(`users$Context`::`me$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/users/123")
    }

    @Test
    fun `location by class in object`() = withLocationsApplication {
        val href = application.locations.href(users.user(123))
        assertEquals("/users/123", href)
        application.routing {
            get<`users$Context`.`user$Context`, users.user>(`users$Context`::`user$Context`) { user ->
                assertEquals(123, user.id)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/users/me")
    }

    @Location("/items/{id}")
    object items

    @Test(expected = IllegalArgumentException::class)
    fun `location by object has bind argument`() =
        withLocationsApplication {
            application.locations.href(items)
        }

    @Location("/items/{itemId}/{extra?}")
    class OverlappingPath1(val itemId: Int, val extra: String?) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/me")
    class `OverlappingPath1$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Location("/items/{extra}")
    class OverlappingPath2(val extra: String) : Responds<HttpStatusCode.Companion.OKCode>

    @Location("/me")
    class `OverlappingPath2$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
            response.pipeline.execute(this, message)
    }

    @Test
    fun `overlapping paths are resolved as expected`() = withLocationsApplication {
        application.install(CallLogging)
        application.routing {
            get<`OverlappingPath1$Context`, OverlappingPath1>(::`OverlappingPath1$Context`) {
                call.respond(HttpStatusCode.OK)
            }
            get<`OverlappingPath2$Context`, OverlappingPath2>(::`OverlappingPath2$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(application.locations.href(OverlappingPath1(1, "Foo")))
        urlShouldBeUnhandled(application.locations.href(OverlappingPath2("1-Foo")))
    }

    enum class LocationEnum {
        A, B, C
    }

    @Location("/")
    class LocationWithEnum(val e: LocationEnum) : Responds<TextContent>

    @Location("/me")
    class `LocationWithEnum$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respondText(
            text: String,
            contentType: ContentType? = null,
            status: HttpStatusCode? = null,
            configure: OutgoingContent.() -> Unit = {}
        ) {
            val self: ApplicationCall = this
            self.respondText(text, contentType, status, configure)
        }
    }

    @Test
    fun `location class with enum value`() = withLocationsApplication {
        application.routing {
            get<`LocationWithEnum$Context`, LocationWithEnum>(::`LocationWithEnum$Context`) {
                call.respondText(call.locations.resolve<LocationWithEnum>(call).e.name)
            }
        }

        urlShouldBeHandled("/?e=A", "A")
        urlShouldBeHandled("/?e=B", "B")

        handleRequest(HttpMethod.Get, "/?e=x").let { call ->
            assertEquals(HttpStatusCode.BadRequest, call.response.status())
        }
    }

    @Location("/")
    class LocationWithBigNumbers(val bd: BigDecimal, val bi: BigInteger) : Responds<TextContent>

    @Location("/me")
    class `LocationWithBigNumbers$Context`(
        ctx: PipelineContext<Unit, TypedApplicationCall>
    ) : TypedLocation(ctx) {
        suspend fun TypedApplicationCall.respond(message: TextContent) =
            response.pipeline.execute(this, message)

        suspend fun TypedApplicationCall.respondText(
            text: String,
            contentType: ContentType? = null,
            status: HttpStatusCode? = null,
            configure: OutgoingContent.() -> Unit = {}
        ) {
            val self: ApplicationCall = this
            self.respondText(text, contentType, status, configure)
        }
    }

    @Test
    fun `location class with big numbers`() = withLocationsApplication {
        val bd = BigDecimal("123456789012345678901234567890")
        val bi = BigInteger("123456789012345678901234567890")

        application.routing {
            get<`LocationWithBigNumbers$Context`, LocationWithBigNumbers>(::`LocationWithBigNumbers$Context`) { location ->
                assertEquals(bd, location.bd)
                assertEquals(bi, location.bi)

                call.respondText(call.locations.href(location))
            }
        }

        urlShouldBeHandled(
            "/?bd=123456789012345678901234567890&bi=123456789012345678901234567890",
            "/?bd=123456789012345678901234567890&bi=123456789012345678901234567890"
        )
    }

    @Test
    fun `location parameter mismatch should lead to bad request status`() = withLocationsApplication {
        @Location("/")
        data class L(val text: String, val number: Int, val longNumber: Long) : Responds<TextContent>

        @Location("/me")
        class `L$Context`(
            ctx: PipelineContext<Unit, TypedApplicationCall>
        ) : TypedLocation(ctx) {
            suspend fun TypedApplicationCall.respond(message: TextContent) =
                response.pipeline.execute(this, message)

            suspend fun TypedApplicationCall.respondText(
                text: String,
                contentType: ContentType? = null,
                status: HttpStatusCode? = null,
                configure: OutgoingContent.() -> Unit = {}
            ) {
                val self: ApplicationCall = this
                self.respondText(text, contentType, status, configure)
            }
        }

        application.routing {
            get<`L$Context`, L>(::`L$Context`) { instance ->
                call.respondText("text = ${instance.text}, number = ${instance.number}, longNumber = ${instance.longNumber}")
            }
        }

        urlShouldBeHandled("/?text=abc&number=1&longNumber=2", "text = abc, number = 1, longNumber = 2")

        // missing parameter text
        handleRequest(HttpMethod.Get, "/?number=1&longNumber=2").let { call ->
            // null because missing parameter leads to routing miss
            assertEquals(null, call.response.status())
        }

        // illegal value for numeric property
        handleRequest(HttpMethod.Get, "/?text=abc&number=z&longNumber=2").let { call ->
            assertEquals(HttpStatusCode.BadRequest, call.response.status())
        }

        // illegal value for numeric property
        handleRequest(HttpMethod.Get, "/?text=abc&number=${Long.MAX_VALUE}&longNumber=2").let { call ->
            assertEquals(HttpStatusCode.BadRequest, call.response.status())
        }
    }
}
