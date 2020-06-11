/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.ktor.tests.locations

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.junit.Test
import kotlin.contracts.*
import kotlin.reflect.*
import kotlin.test.*

private fun withLocationsApplication(test: TestApplicationEngine.() -> Unit) = withTestApplication {
    application.install(CustomLocationsFeature)
    test()
}

class CustomLocationRouteService : LocationRouteService {
    override fun findRoute(locationClass: KClass<*>): String? = locationClass.simpleName
}

object CustomLocationsFeature : ApplicationFeature<Application, Locations, Locations> {
    override val key: AttributeKey<Locations> = Locations.key

    override fun install(pipeline: Application, configure: Locations.() -> Unit): Locations {
        return Locations(pipeline, CustomLocationRouteService()).apply(configure)
    }
}

class index: Responds<HttpStatusCode.Companion.OKCode>
class `index$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
    suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
        response.pipeline.execute(this, message)
}

class bye(val value: String): Responds<HttpStatusCode.Companion.OKCode>
class `bye$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
    suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
        response.pipeline.execute(this, message)
}

@Location("entity/{id}")
class entity(val id: EntityID): Responds<HttpStatusCode.Companion.OKCode>

@Location("entity/{id}")
class `entity$Context`(ctx: PipelineContext<Unit, TypedApplicationCall>) : TypedLocation(ctx) {
    suspend fun TypedApplicationCall.respond(message: HttpStatusCode.Companion.OKCode) =
        response.pipeline.execute(this, message)
}

data class EntityID(val typeId: Int, val entityId: Int)

@OptIn(KtorExperimentalLocationsAPI::class)
class CustomLocationsTest {

    @Test
    fun `custom location index`() = withLocationsApplication {
        val href = application.locations.href(index())
        assertEquals("/index", href)
        application.routing {
            get<`index$Context`, index>(::`index$Context`) {
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/")
    }

    @Test
    fun `custom location bye`() = withLocationsApplication {
        val href = application.locations.href(bye("farewall"))
        assertEquals("/bye?value=farewall", href)
        application.routing {
            get<`bye$Context`, bye>(::`bye$Context`) {
                assertEquals("farewall", it.value)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/")
    }

    @Test
    fun `custom data conversion`() = withTestApplication {
        application.install(Locations)
        application.install(DataConversion) {
            convert<EntityID> {
                decode { values, _ ->
                    val (typeId, entityId) = values.single().split('-').map { it.toInt() }
                    EntityID(typeId, entityId)
                }

                encode { value ->
                    when (value) {
                        null -> listOf()
                        is EntityID -> listOf("${value.typeId}-${value.entityId}")
                        else -> throw DataConversionException("Cannot convert $value as EntityID")
                    }
                }
            }
        }

        val href = application.locations.href(entity(EntityID(42, 999)))
        assertEquals("/entity/42-999", href)
        application.routing {
            get<`entity$Context`, entity>(::`entity$Context`) {
                assertEquals(42, it.id.typeId)
                assertEquals(999, it.id.entityId)
                call.respond(HttpStatusCode.OK)
            }
        }
        urlShouldBeHandled(href)
        urlShouldBeUnhandled("/")
    }
}
