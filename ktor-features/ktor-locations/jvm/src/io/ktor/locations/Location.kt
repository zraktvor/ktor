/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")
@file:OptIn(KtorExperimentalLocationsAPI::class)

package io.ktor.locations

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import java.io.*
import java.lang.IllegalStateException
import kotlin.coroutines.*
import kotlin.reflect.*

/**
 * API marked with this annotation is experimental and is not guaranteed to be stable.
 */
@Suppress("DEPRECATION")
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This locations API is experimental. It could be changed or removed in future releases."
)
@Experimental(level = Experimental.Level.WARNING)
annotation class KtorExperimentalLocationsAPI

class TypedApplicationCall(private val applicationCall: ApplicationCall) : ApplicationCall by applicationCall

fun PipelineContext<Unit, ApplicationCall>.typed() = object : PipelineContext<Unit, TypedApplicationCall> {
    private val typedVersion = this@typed

    override val context: TypedApplicationCall get() = TypedApplicationCall(typedVersion.context)
    override val subject: Unit get() = typedVersion.subject
    override fun finish() = typedVersion.finish()
    override suspend fun proceedWith(subject: Unit) = typedVersion.proceedWith(subject)
    override suspend fun proceed() = typedVersion.proceed()
    override val coroutineContext: CoroutineContext get() = typedVersion.coroutineContext

}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respond(message: Any?): Unit =
    throw IllegalStateException("Typed location must not have a default respond method")

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondText(
    text: String,
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    configure: OutgoingContent.() -> Unit = {}
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondText(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    provider: suspend () -> String
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondBytes(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    provider: suspend () -> ByteArray
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondBytes(
    bytes: ByteArray,
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    configure: OutgoingContent.() -> Unit = {}
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondFile(
    baseDir: File,
    fileName: String,
    configure: OutgoingContent.() -> Unit = {}
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondFile(file: File, configure: OutgoingContent.() -> Unit = {}) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondTextWriter(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    writer: suspend Writer.() -> Unit
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
suspend fun TypedApplicationCall.respondOutputStream(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    producer: suspend OutputStream.() -> Unit
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@Deprecated(level = DeprecationLevel.ERROR, message = "Typed location does not have a default respond method")
@KtorExperimentalAPI
suspend fun TypedApplicationCall.respondBytesWriter(
    contentType: ContentType? = null,
    status: HttpStatusCode? = null,
    producer: suspend ByteWriteChannel.() -> Unit
) {
    throw IllegalStateException("Typed location must not have a default respond method")
}

@KtorExperimentalLocationsAPI
open class TypedLocation(val ctx: PipelineContext<Unit, TypedApplicationCall>) :
    PipelineContext<Unit, TypedApplicationCall> by ctx

inline val TypedLocation.call: TypedApplicationCall get() = context

/**
 * Annotation for classes that will act as typed routes.
 * @property path the route path, including class property names wrapped with curly braces.
 */
@KtorExperimentalLocationsAPI
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class Location(val path: String)

/**
 * Gets the [Application.locations] feature
 */
@KtorExperimentalLocationsAPI
val PipelineContext<Unit, ApplicationCall>.locations: Locations
    get() = call.application.locations

/**
 * Gets the [Application.locations] feature
 */
@KtorExperimentalLocationsAPI
val ApplicationCall.locations: Locations
    get() = application.locations

/**
 * Gets the [Application.locations] feature
 */
@KtorExperimentalLocationsAPI
val Application.locations: Locations
    get() = feature(Locations)

/**
 * Renders link to a [location] using current installed locations service
 * @throws MissingApplicationFeatureException is no locations feature installed
 */
@KtorExperimentalLocationsAPI
fun PipelineContext<Unit, ApplicationCall>.href(location: Any): String {
    return call.application.locations.href(location)
}

/**
 * Registers a route [body] for a location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.location(noinline body: Route.() -> Unit): Route {
    return location(T::class, body)
}

/**
 * Registers a typed handler [body] for a `GET` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.untypedGet(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Get) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `GET` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified L : TypedLocation, reified T : Any> Route.get(
    crossinline initL: (PipelineContext<Unit, TypedApplicationCall>) -> L,
    noinline body: suspend L.(T) -> Unit
): Route =
    this.untypedGet<T> { t ->
        initL(this.typed()).body(t)
    }

/**
 * Registers a typed handler [body] for a `OPTIONS` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.options(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Options) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `HEAD` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.head(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Head) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `POST` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.post(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Post) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `PUT` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.put(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Put) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `DELETE` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.delete(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Delete) {
            handle(body)
        }
    }
}

/**
 * Registers a typed handler [body] for a `PATCH` location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [T] as first parameter.
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.patch(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit): Route {
    return location(T::class) {
        method(HttpMethod.Patch) {
            handle(body)
        }
    }
}

/**
 * Registers a route [body] for a location defined by class [data].
 *
 * Class [data] **must** be annotated with [Location].
 */
@KtorExperimentalLocationsAPI
fun <T : Any> Route.location(data: KClass<T>, body: Route.() -> Unit): Route {
    val entry = application.locations.createEntry(this, data)
    return entry.apply(body)
}

/**
 * Registers a handler [body] for a location defined by class [T].
 *
 * Class [T] **must** be annotated with [Location].
 */
@KtorExperimentalLocationsAPI
inline fun <reified T : Any> Route.handle(noinline body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit) {
    return handle(T::class, body)
}

/**
 * Registers a handler [body] for a location defined by class [dataClass].
 *
 * Class [dataClass] **must** be annotated with [Location].
 *
 * @param body receives an instance of typed location [dataClass] as first parameter.
 */
@KtorExperimentalLocationsAPI
fun <T : Any> Route.handle(dataClass: KClass<T>, body: suspend PipelineContext<Unit, ApplicationCall>.(T) -> Unit) {
    intercept(ApplicationCallPipeline.Features) {
        call.attributes.put(LocationInstanceKey, locations.resolve<T>(dataClass, call))
    }

    handle {
        @Suppress("UNCHECKED_CAST")
        val location = call.attributes[LocationInstanceKey] as T

        body(location)
    }
}

/**```
 * Retrieves the current call's location or `null` if it is not available (request is not handled by a location class),
 * or not yet available (invoked too early before the locations feature takes place).
 */
@KtorExperimentalAPI
inline fun <reified T : Any> ApplicationCall.locationOrNull(): T = locationOrNull(T::class)

@PublishedApi
internal fun <T : Any> ApplicationCall.locationOrNull(type: KClass<T>): T =
    attributes.getOrNull(LocationInstanceKey)?.let { instance ->
        type.cast(instance)
    } ?: error("Location instance is not available for this call.)")

private val LocationInstanceKey = AttributeKey<Any>("LocationInstance")

private fun <T : Any> KClass<T>.cast(instance: Any): T {
    return javaObjectType.cast(instance)
}

/**

get<T> { this: T
this.call.respond(...)
}

T.call = T.pipeline.context
fun <T> get<T>(block: T.(T) -> Unit) = oldGet { /* this: Pipeline<Unit, AppCall> */ t ->
T(this.typed()).block(t)
}

fun <T> oldGet<T>(block: Pipeline<Unit, AppCall>.(T) -> Unit) ..

class TypedAppCall(d: AppCall): AppCall by d

class TypedLoc(ctx: Pipeline<Unit, TypedAppCall>): Pipeline<Unit, TypedAppCall> by ctx



 */
